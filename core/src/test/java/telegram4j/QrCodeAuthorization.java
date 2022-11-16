package telegram4j;

import reactor.core.Disposables;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.retry.Retry;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.mtproto.RpcException;
import telegram4j.mtproto.util.CryptoUtil;
import telegram4j.tl.BaseUser;
import telegram4j.tl.UpdateLoginToken;
import telegram4j.tl.UpdateShort;
import telegram4j.tl.auth.BaseAuthorization;
import telegram4j.tl.auth.BaseLoginToken;
import telegram4j.tl.auth.LoginTokenSuccess;

import java.io.InputStreamReader;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.StringJoiner;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * QR-code user authorization implementation that displays a qrcode of unicode chars to stdout.
 * <p>
 * This implementation can work only on linux with installed <b>qrencode</b> lib.
 */
public class QrCodeAuthorization {

    private static final Duration defaultTimeout = Duration.ofSeconds(30);
    private static final Logger log = Loggers.getLogger(QrCodeAuthorization.class);

    public static Mono<BaseAuthorization> authorize(MTProtoTelegramClient client) {
        return Mono.create(sink -> {
            AtomicReference<Duration> timeout = new AtomicReference<>(defaultTimeout);
            AtomicBoolean complete = new AtomicBoolean();

            int apiId = client.getAuthResources().getApiId();
            String apiHash = client.getAuthResources().getApiHash();

            var listenTokens = client.getMtProtoClientGroup().main().updates().asFlux()
                    .takeUntil(u -> complete.get())
                    .ofType(UpdateShort.class)
                    .filter(u -> u.update().identifier() == UpdateLoginToken.ID)
                    .flatMap(l -> client.getServiceHolder()
                            .getAuthService()
                            .exportLoginToken(apiId, apiHash, List.of()))
                    // TODO: handling for other types
                    .cast(LoginTokenSuccess.class)
                    .map(LoginTokenSuccess::authorization)
                    .onErrorResume(RpcException.isErrorMessage("SESSION_PASSWORD_NEEDED"), e -> {
                        TwoFactorAuthHandler tfa = new TwoFactorAuthHandler(client, sink);
                        return tfa.begin2FA()
                                .retryWhen(Retry.indefinitely()
                                        .filter(RpcException.isErrorMessage("PASSWORD_HASH_INVALID")));
                    })
                    .cast(BaseAuthorization.class)
                    .doOnNext(l -> {
                        BaseUser b = (BaseUser) l.user();
                        StringJoiner j = new StringJoiner(" ");
                        Optional.ofNullable(b.firstName()).ifPresent(j::add);
                        Optional.ofNullable(b.lastName()).ifPresent(j::add);
                        String name = (name = j.toString()).isEmpty() ? "unknown" : name;
                        log.info("Successfully login as {}", name);
                        complete.set(true);
                        sink.success(l);
                    })
                    .subscribe();

            var qrDisplay = Mono.defer(() -> client.getServiceHolder().getAuthService()
                            .exportLoginToken(apiId, apiHash, List.of())
                            .cast(BaseLoginToken.class)
                            .doOnNext(b -> timeout.set(Duration.ofSeconds(b.expires() - (System.currentTimeMillis() / 1000))))
                            .doOnNext(b -> {
                                String token = java.util.Base64.getUrlEncoder().encodeToString(CryptoUtil.toByteArray(b.token()));

                                String url = "tg://login?token=" + token;
                                synchronized (System.out) {
                                    System.out.println("QR code (you have " + timeout.get().getSeconds() + " seconds):");
                                    System.out.println(generateQr(url));
                                    System.out.println();
                                }
                            })
                            .timeout(timeout.get()))
                    .retryWhen(Retry.indefinitely()
                            .filter(e -> e instanceof TimeoutException && !complete.get())
                            .doAfterRetry(signal -> log.info("Token expired. Regenerating qr code...")))
                    .subscribe();

            sink.onCancel(Disposables.composite(listenTokens, qrDisplay));
        });
    }

    private static String generateQr(String text) {
        try {
            Process v = new ProcessBuilder("qrencode", "-t", "UTF8", text)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .start();

            Scanner stre = new Scanner(new InputStreamReader(v.getInputStream()));

            StringBuilder b = new StringBuilder();
            while (stre.hasNext()) {
                b.append(stre.nextLine()).append('\n');
            }
            return b.toString();
        } catch (Throwable t) {
            throw Exceptions.propagate(t);
        }
    }
}
