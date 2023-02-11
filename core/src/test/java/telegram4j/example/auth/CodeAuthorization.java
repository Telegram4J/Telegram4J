package telegram4j.example.auth;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;
import reactor.core.publisher.Sinks;
import reactor.util.retry.Retry;
import telegram4j.core.AuthorizationResources;
import telegram4j.mtproto.DataCenter;
import telegram4j.mtproto.RpcException;
import telegram4j.mtproto.client.MTProtoClientGroup;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.ImmutableCodeSettings;
import telegram4j.tl.auth.BaseAuthorization;
import telegram4j.tl.auth.BaseSentCode;
import telegram4j.tl.auth.CodeType;
import telegram4j.tl.request.auth.ImmutableCancelCode;
import telegram4j.tl.request.auth.ImmutableResendCode;
import telegram4j.tl.request.auth.ImmutableSendCode;
import telegram4j.tl.request.auth.SignIn;

import java.time.Instant;
import java.util.Scanner;
import java.util.StringJoiner;

import static reactor.core.publisher.Sinks.EmitFailureHandler.FAIL_FAST;

/**
 * Simple implementation of user auth flow, which
 * can only handle SMS and APP codes.
 */
public class CodeAuthorization {

    static final String delimiter = "=".repeat(32);

    CodeAuthorization(MTProtoClientGroup clientGroup, StoreLayout storeLayout,
                      AuthorizationResources authResources, MonoSink<BaseAuthorization> completeSink) {
        this.clientGroup = clientGroup;
        this.storeLayout = storeLayout;
        this.authResources = authResources;
        this.completeSink = completeSink;
    }

    BaseSentCode currentCode;
    String phoneNumber;
    boolean firstNumber = true;
    boolean validPhone = false;

    final MTProtoClientGroup clientGroup;
    final AuthorizationResources authResources;
    final StoreLayout storeLayout;
    final MonoSink<BaseAuthorization> completeSink;
    final Sinks.Many<State> state = Sinks.many().replay()
            .latestOrDefault(State.SEND_CODE);
    final Scanner sc = new Scanner(System.in);

    Mono<Void> begin() {
        return state.asFlux()
                .flatMap(s -> {
                    switch (s) {
                        case SEND_CODE:
                            if (!validPhone) {
                                synchronized (System.out) {
                                    System.out.println(delimiter);
                                    if (!firstNumber) {
                                        System.out.print("Invalid phone number, write your phone number again: ");
                                    } else {
                                        System.out.print("Write your phone number: ");
                                        firstNumber = false;
                                    }
                                }

                                phoneNumber = readPhoneNumber(sc);
                            }

                            return clientGroup.main()
                                    .sendAwait(ImmutableSendCode.of(phoneNumber, authResources.getApiId(),
                                            authResources.getApiHash(), ImmutableCodeSettings.of()))
                                    .onErrorResume(RpcException.isErrorMessage("PHONE_NUMBER_INVALID"), e ->
                                            Mono.fromRunnable(() -> state.emitNext(State.SEND_CODE, FAIL_FAST)))
                                    .onErrorResume(RpcException.isErrorCode(303), e -> {
                                        RpcException rpcExc = (RpcException) e;
                                        String msg = rpcExc.getError().errorMessage();
                                        if (!msg.startsWith("PHONE_MIGRATE_"))
                                            return Mono.error(new IllegalStateException("Unexpected type of DC redirection", e));

                                        int dcId = Integer.parseInt(msg.substring(14));
                                        validPhone = true;

                                        synchronized (System.out) {
                                            System.out.println(delimiter);
                                            System.out.println("Redirecting to the DC " + dcId);
                                        }

                                        return storeLayout.getDcOptions()
                                                .map(dcOpts -> dcOpts.find(DataCenter.Type.REGULAR, dcId)
                                                        .orElseThrow(() -> new IllegalStateException("Could not find DC " + dcId
                                                                + " for redirecting main client")))
                                                .flatMap(clientGroup::setMain)
                                                .then(Mono.fromRunnable(() -> state.emitNext(State.SEND_CODE, FAIL_FAST)));
                                    })
                                    // TODO: why new subtype of SentCode was added?
                                    .cast(BaseSentCode.class)
                                    .flatMapMany(this::applyCode);
                        case RESEND_CODE:
                            return clientGroup.main()
                                    .sendAwait(ImmutableResendCode.of(phoneNumber, currentCode.phoneCodeHash()))
                                    .cast(BaseSentCode.class)
                                    .flatMapMany(this::applyCode);
                        case AWAIT_CODE:
                            synchronized (System.out) {
                                System.out.println(delimiter);
                                System.out.println("Invalid phone code, please write it again");
                            }
                            return applyCode(currentCode);
                        case CANCEL_CODE:
                            System.out.println("Goodbye, " + System.getProperty("user.name"));
                            state.emitComplete(FAIL_FAST);
                            completeSink.success();
                            return Mono.empty();
                        case SIGN_IN:
                            state.emitComplete(FAIL_FAST);
                            return Mono.empty();
                        default:
                            return Flux.error(new IllegalStateException());
                    }
                })
                .then();
    }

    public static Mono<BaseAuthorization> authorize(MTProtoClientGroup clientGroup, StoreLayout storeLayout,
                                                    AuthorizationResources authResources) {
        return Mono.create(sink -> {
            CodeAuthorization instance = new CodeAuthorization(clientGroup, storeLayout, authResources, sink);

            sink.onCancel(instance.begin()
                    .subscribe(null, sink::error));
        });
    }

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

    Publisher<?> applyCode(BaseSentCode scode) {
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

            state.emitNext(State.RESEND_CODE, FAIL_FAST);
            return Mono.empty();
        }

        if (code.equalsIgnoreCase("cancel")) {
            return clientGroup.main().sendAwait(ImmutableCancelCode.of(phoneNumber, scode.phoneCodeHash()))
                    .doOnNext(b -> {
                        synchronized (System.out) {
                            System.out.println(delimiter);
                            if (b) {
                                System.out.println("Phone code successfully canceled");
                            } else {
                                System.out.println("Failed to cancel phone code");
                            }
                        }

                        state.emitNext(State.CANCEL_CODE, FAIL_FAST);
                    });
        }

        return clientGroup.main()
                .sendAwait(SignIn.builder()
                        .phoneNumber(phoneNumber)
                        .phoneCode(code)
                        .phoneCodeHash(scode.phoneCodeHash())
                        .build())
                .onErrorResume(RpcException.isErrorMessage("PHONE_CODE_INVALID"), e -> Mono.fromRunnable(() ->
                        state.emitNext(State.AWAIT_CODE, FAIL_FAST)))
                .onErrorResume(RpcException.isErrorMessage("SESSION_PASSWORD_NEEDED"), e -> {
                    TwoFactorAuthHandler tfa = new TwoFactorAuthHandler(clientGroup, completeSink);
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

                    state.emitNext(State.SIGN_IN, FAIL_FAST);
                    completeSink.success(a);
                });
    }
}
