package telegram4j.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.Logger;
import reactor.util.Loggers;
import telegram4j.core.auth.AuthorizationHandler.Resources;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.auth.CodeAuthorizationHandler;
import telegram4j.core.auth.CodeAuthorizationHandler.CodeAction;
import telegram4j.core.auth.CodeAuthorizationHandler.PhoneNumberAction;
import telegram4j.core.auth.QRAuthorizationHandler;
import telegram4j.core.auth.TwoFactorHandler;
import telegram4j.core.event.DefaultUpdatesManager;
import telegram4j.core.event.DefaultUpdatesManager.Options;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.retriever.PreferredEntityRetriever;
import telegram4j.mtproto.DcId;
import telegram4j.mtproto.MTProtoRetrySpec;
import telegram4j.mtproto.MethodPredicate;
import telegram4j.mtproto.ResponseTransformer;
import telegram4j.mtproto.store.FileStoreLayout;
import telegram4j.mtproto.store.StoreLayoutImpl;
import telegram4j.tl.json.TlModule;
import telegram4j.tl.request.account.ImmutableUpdateStatus;
import telegram4j.tl.request.help.GetConfig;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class UserBotExample {

    private static final Logger log = Loggers.getLogger(UserBotExample.class);

    static class Base2FACallback implements TwoFactorHandler.Callback {
        protected final Scanner sc = new Scanner(System.in);

        @Override
        public Mono<String> on2FAPassword(Resources res, TwoFactorHandler.Context ctx) {
            return Mono.fromCallable(() -> {
                String base = "The account is protected by 2FA, please write password";
                String hint = ctx.srp().hint();
                if (hint != null) {
                    base += " (Hint: '" + hint + "')";
                }
                ctx.log(base);

                return sc.nextLine();
            });
        }
    }

    static class StdINCallback extends Base2FACallback implements CodeAuthorizationHandler.Callback {

        static String cleanPhoneNumber(String phoneNumber) {
            if (phoneNumber.startsWith("+"))
                phoneNumber = phoneNumber.substring(1);
            phoneNumber = phoneNumber.replaceAll(" ", "");
            return phoneNumber;
        }

        @Override
        public Mono<PhoneNumberAction> onPhoneNumber(Resources res, CodeAuthorizationHandler.PhoneNumberContext ctx) {
            return Mono.fromCallable(() -> {
                ctx.log("Please write your phone number");

                String phoneNumber = sc.nextLine();
                if (phoneNumber.equalsIgnoreCase("cancel")) {
                    return PhoneNumberAction.cancel();
                }
                return PhoneNumberAction.of(cleanPhoneNumber(phoneNumber));
            });
        }

        @Override
        public Mono<CodeAction> onSentCode(Resources res, CodeAuthorizationHandler.PhoneCodeContext ctx) {
            return Mono.fromCallable(() -> {
                ctx.log("Code has been sent, write it");

                String codeOrCommand = sc.nextLine();
                return switch (codeOrCommand.toLowerCase(Locale.ROOT)) {
                    case "resend" -> {
                        ctx.log("Resending code...");
                        yield CodeAction.resend();
                    }
                    case "cancel" -> CodeAction.cancel();
                    default -> CodeAction.of(codeOrCommand);
                };
            });
        }
    }

    static class QRCallback extends Base2FACallback implements QRAuthorizationHandler.Callback {
        @Override
        public Mono<ActionType> onLoginToken(Resources res, QRAuthorizationHandler.Context ctx) {
            return Mono.fromSupplier(() -> {
                ctx.log("New QR code (you have " + ctx.expiresIn().toSeconds() + " seconds to scan it)");

                System.out.println(generateQr(ctx.loginUrl()));
                return ActionType.RETRY;
            });
        }

        static String generateQr(String text) {
            try {
                Process v = new ProcessBuilder("qrencode", "-t", "UTF8", text)
                        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                        .start();

                return v.inputReader(StandardCharsets.UTF_8).lines()
                        .collect(Collectors.joining("\n"));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    public static void main(String[] args) {
        // only for testing
        Hooks.onOperatorDebug();
        ResourceLeakDetector.setLevel(Level.PARANOID);

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new TlModule());

        int apiId = Integer.parseInt(System.getenv("TEST_API_ID"));
        String apiHash = System.getenv("TEST_API_HASH");

        MTProtoTelegramClient.create(apiId, apiHash,
                        // Linux way
                        Boolean.getBoolean("useQrAuth")
                                ? new QRAuthorizationHandler(new QRCallback())
                                : new CodeAuthorizationHandler(new StdINCallback())
                )
                .setEntityRetrieverStrategy(EntityRetrievalStrategy.preferred(
                        EntityRetrievalStrategy.STORE_FALLBACK_RPC, PreferredEntityRetriever.Setting.FULL,
                        PreferredEntityRetriever.Setting.FULL))
                .setStoreLayout(new FileStoreLayout(new StoreLayoutImpl(Function.identity()),
                        Path.of("core/src/test/resources/t4j.bin")))
                .addResponseTransformer(ResponseTransformer.retryFloodWait(MethodPredicate.all(),
                        MTProtoRetrySpec.max(d -> d.getSeconds() < 30, Long.MAX_VALUE)))
                .setUpdatesManager(client -> new DefaultUpdatesManager(client,
                        new Options(Options.DEFAULT_CHECKIN,
                                Options.MAX_USER_CHANNEL_DIFFERENCE,
                                true)))
                .withConnection(client -> {

                    Mono<Void> eventLog = client.getMtProtoClientGroup().updates().all()
                            .flatMap(u -> Mono.fromCallable(() -> mapper.writeValueAsString(u)))
                            .doOnNext(log::info)
                            .then();

                    var online = new boolean[]{false};
                    // *bad way* to make fake online
                    // update user status in fixed random interval
                    Mono<Void> status = Flux.<Integer>create(sink -> {
                                var scheduler = Schedulers.newSingle("t4j-user-status", true);
                                var task = scheduler.schedule(() -> {
                                    while (!Thread.currentThread().isInterrupted()) {
                                        try {
                                            int sleep = ThreadLocalRandom.current().nextInt(45, 160);
                                            log.info("Delaying {} status for {} seconds", online[0] ? "offline" : "online", sleep);
                                            TimeUnit.SECONDS.sleep(sleep);
                                            sink.next(1);
                                        } catch (InterruptedException e) {
                                            sink.complete();
                                            break;
                                        }
                                    }
                                });
                                sink.onCancel(Disposables.composite(scheduler, task));
                            })
                            .takeUntilOther(client.onDisconnect())
                            .flatMap(e -> {
                                boolean state = online[0];
                                online[0] = !state;
                                return client.getMtProtoClientGroup().send(DcId.main(), ImmutableUpdateStatus.of(state));
                            })
                            .then();

                    return Mono.when(eventLog, status);
                })
                .block();
    }
}
