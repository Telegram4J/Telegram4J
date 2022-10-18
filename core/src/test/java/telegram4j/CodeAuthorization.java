package telegram4j;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.ImmutableCodeSettings;
import telegram4j.tl.auth.BaseAuthorization;
import telegram4j.tl.auth.SentCode;
import telegram4j.tl.request.auth.SignIn;

import java.time.Duration;
import java.time.Instant;
import java.util.Scanner;

public class CodeAuthorization {

    private static final String delimiter = "=".repeat(32);
    private static final Throwable RESEND = new Throwable();

    private static volatile SentCode currentCode;

    private static String readPhoneNumber(Scanner sc) {
        String phoneNumber = sc.nextLine();
        if (phoneNumber.startsWith("+"))
            phoneNumber = phoneNumber.substring(1);
        phoneNumber = phoneNumber.replaceAll(" ",  "");
        return phoneNumber;
    }

    public static Publisher<?> authorize(MTProtoTelegramClient client) {
        return Mono.defer(() -> {
            Scanner sc = new Scanner(System.in);
            synchronized (System.out) {
                System.out.println(delimiter);
                System.out.print("Write your phone number: ");
            }
            String phoneNumber = readPhoneNumber(sc);
            Instant send = Instant.now();

            return Mono.defer(() -> {
                        SentCode c = currentCode;
                        if (c == null) {
                            int apiId = client.getAuthResources().getApiId();
                            String apiHash = client.getAuthResources().getApiHash();
                            return client.getServiceHolder().getAuthService()
                                    .sendCode(phoneNumber, apiId, apiHash, ImmutableCodeSettings.of());
                        }
                        return client.getServiceHolder().getAuthService()
                                .resendCode(phoneNumber, c.phoneCodeHash());
                    })
                    .flatMap(scode -> {

                        currentCode = scode;

                        String code;
                        synchronized (System.out) {
                            System.out.println(delimiter);
                            System.out.println("Sent code: " + scode);
                            code = sc.nextLine();
                        }

                        Integer t = scode.timeout();
                        if (code.equals("resend") || t != null && send.plusSeconds(t).isAfter(Instant.now())) {
                            return Mono.error(RESEND);
                        }
                        if (code.equals("cancel")) {
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
                                    });
                        }

                        return client.getServiceHolder().getAuthService()
                                .signIn(SignIn.builder()
                                        .phoneNumber(phoneNumber)
                                        .phoneCode(code)
                                        .phoneCodeHash(scode.phoneCodeHash())
                                        .build())
                                .cast(BaseAuthorization.class)
                                .doOnNext(a -> {
                                    synchronized (System.out) {
                                        System.out.println(delimiter);
                                        System.out.println("Authorization is successful: " + a);
                                    }
                                });
                    })
                    .retryWhen(Retry.fixedDelay(5, Duration.ofSeconds(3))
                            .filter(t -> t == RESEND));
        });
    }
}
