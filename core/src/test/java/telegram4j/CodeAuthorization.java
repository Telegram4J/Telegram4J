package telegram4j;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.mtproto.RpcException;
import telegram4j.tl.ImmutableCodeSettings;
import telegram4j.tl.auth.BaseAuthorization;
import telegram4j.tl.auth.CodeType;
import telegram4j.tl.auth.SentCode;
import telegram4j.tl.request.auth.SignIn;

import java.time.Instant;
import java.util.Scanner;
import java.util.StringJoiner;

/**
 * Simple implementation of user auth flow, which
 * can only handle SMS and APP codes.
 */
public class CodeAuthorization {

    static final String delimiter = "=".repeat(32);

    CodeAuthorization(MTProtoTelegramClient client, MonoSink<BaseAuthorization> completeSink) {
        this.client = client;
        this.completeSink = completeSink;
    }

    SentCode currentCode;
    String phoneNumber;

    final MTProtoTelegramClient client;
    final Sinks.Many<State> state = Sinks.many().replay()
            .latestOrDefault(State.SEND_CODE);
    final Scanner sc = new Scanner(System.in);
    final MonoSink<BaseAuthorization> completeSink;
    boolean firstNumber = true;

    enum State {
        SEND_CODE,
        AWAIT_CODE,
        RESEND_CODE,
        CANCEL_CODE,
        SIGN_IN
    }

    static String readPhoneNumber(Scanner sc) {
        String phoneNumber = sc.nextLine();
        if (phoneNumber.startsWith("+"))
            phoneNumber = phoneNumber.substring(1);
        phoneNumber = phoneNumber.replaceAll(" ", "");
        return phoneNumber;
    }

    Mono<Void> begin() {
        return state.asFlux()
                .flatMap(s -> {
                    switch (s) {
                        case SEND_CODE:
                            synchronized (System.out) {
                                System.out.println(delimiter);
                                if (!firstNumber) {
                                    firstNumber = false;
                                    System.out.println("Invalid phone number");
                                }

                                System.out.print("Write your phone number: ");
                            }

                            phoneNumber = readPhoneNumber(sc);
                            int apiId = client.getAuthResources().getApiId();
                            String apiHash = client.getAuthResources().getApiHash();

                            return client.getServiceHolder().getAuthService()
                                    .sendCode(phoneNumber, apiId, apiHash, ImmutableCodeSettings.of())
                                    .onErrorResume(RpcException.isErrorMessage("PHONE_NUMBER_INVALID"), e -> {
                                        state.emitNext(State.SEND_CODE, Sinks.EmitFailureHandler.FAIL_FAST);
                                        return Mono.empty();
                                    })
                                    .flatMapMany(this::applyCode);
                        case RESEND_CODE:
                            return client.getServiceHolder().getAuthService()
                                    .resendCode(phoneNumber, currentCode.phoneCodeHash())
                                    .flatMapMany(this::applyCode);
                        case AWAIT_CODE:
                            synchronized (System.out) {
                                System.out.println(delimiter);
                                System.out.println("Invalid phone code, please write it again");
                            }
                            return applyCode(currentCode);
                        case CANCEL_CODE:
                            System.out.println("Goodbye, " + System.getProperty("user.name"));
                            state.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);
                            return client.disconnect();
                        case SIGN_IN:
                            state.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);
                            return Mono.empty();
                        default:
                            return Flux.error(new IllegalStateException());
                    }
                })
                .then();
    }

    public static Mono<BaseAuthorization> authorize(MTProtoTelegramClient client) {
        return Mono.create(sink -> {
            var auth = new CodeAuthorization(client, sink);

            sink.onCancel(auth.begin()
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe());
        });
    }

    Publisher<?> applyCode(SentCode scode) {
        boolean resend = currentCode != null;
        currentCode = scode;

        Instant sendTimestamp = Instant.now();
        Integer t = scode.timeout();
        synchronized (System.out) {
            System.out.println(delimiter);
            StringJoiner j = new StringJoiner(", ");
            j.add("code type: " + scode.type());
            if (t != null) {
                j.add("expires at: " + sendTimestamp.plusSeconds(t));
            }
            CodeType nextType = scode.nextType();
            if (nextType != null) {
                j.add("next code type: " + nextType);
            }
            System.out.println(j);
            System.out.print((resend ? "New code" : "Code") + " has been sent, write it: ");
            j.add((resend ? "New" : "Sent") + " code: ");
        }

        String code = sc.nextLine();
        boolean expired = t != null && sendTimestamp.plusSeconds(t).isAfter(Instant.now());
        if (expired || code.equalsIgnoreCase("resend")) {
            if (expired) {
                synchronized (System.out) {
                    System.out.println(delimiter);
                    System.out.println("Code has expired... Sending a new one");
                }
            }

            state.emitNext(State.RESEND_CODE, Sinks.EmitFailureHandler.FAIL_FAST);
            return Mono.empty();
        }

        if (code.equalsIgnoreCase("cancel")) {
            return client.getServiceHolder().getAuthService()
                    .cancelCode(phoneNumber, scode.phoneCodeHash())
                    .doOnNext(b -> {
                        synchronized (System.out) {
                            System.out.println(delimiter);
                            if (b) {
                                System.out.println("Phone code successfully canceled");
                            } else {
                                System.out.println("Failed to cancel phone code");
                            }
                        }

                        state.emitNext(State.CANCEL_CODE, Sinks.EmitFailureHandler.FAIL_FAST);
                    });
        }

        return client.getServiceHolder().getAuthService()
                .signIn(SignIn.builder()
                        .phoneNumber(phoneNumber)
                        .phoneCode(code)
                        .phoneCodeHash(scode.phoneCodeHash())
                        .build())
                .onErrorResume(RpcException.isErrorMessage("PHONE_CODE_INVALID"), e -> Mono.fromRunnable(() ->
                        state.emitNext(State.AWAIT_CODE, Sinks.EmitFailureHandler.FAIL_FAST)))
                .onErrorResume(RpcException.isErrorMessage("SESSION_PASSWORD_NEEDED"), e -> {
                    TwoFactorAuthHandler tfa = new TwoFactorAuthHandler(client, completeSink);
                    return tfa.begin2FA()
                            .retryWhen(Retry.indefinitely()
                                    .filter(RpcException.isErrorMessage("PASSWORD_HASH_INVALID")));
                })
                .cast(BaseAuthorization.class)
                .doOnNext(a -> {
                    synchronized (System.out) {
                        System.out.println(delimiter);
                        System.out.println("Authorization is successful: " + a);
                    }

                    state.emitNext(State.SIGN_IN, Sinks.EmitFailureHandler.FAIL_FAST);
                    completeSink.success(a);
                });
    }
}
