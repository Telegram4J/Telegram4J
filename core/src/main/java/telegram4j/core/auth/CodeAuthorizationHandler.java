package telegram4j.core.auth;

import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.annotation.Nullable;
import telegram4j.mtproto.DataCenter;
import telegram4j.mtproto.DcId;
import telegram4j.mtproto.RpcException;
import telegram4j.mtproto.internal.Preconditions;
import telegram4j.tl.CodeSettings;
import telegram4j.tl.ImmutableCodeSettings;
import telegram4j.tl.auth.Authorization;
import telegram4j.tl.auth.BaseAuthorization;
import telegram4j.tl.auth.BaseSentCode;
import telegram4j.tl.auth.SentCode;
import telegram4j.tl.mtproto.RpcError;
import telegram4j.tl.request.auth.*;

import java.util.Objects;

/**
 * Default implementation of verification code auth flow.
 *
 * @apiNote This class is good for sharing as and thread-safe.
 */
public class CodeAuthorizationHandler implements AuthorizationHandler {
    protected static final Logger log = Loggers.getLogger(CodeAuthorizationHandler.class);

    protected final Callback callback;

    public CodeAuthorizationHandler(Callback callback) {
        this.callback = Objects.requireNonNull(callback);
    }

    @Override
    public Mono<BaseAuthorization> process(Resources res) {
        return Mono.defer(() -> {
            var ctx = new Context();

            return sendCode(res, ctx)
                    .flatMap(sentCode -> onSentCode(res, ctx, sentCode))
                    .onErrorResume(RpcException.isErrorMessage("SESSION_PASSWORD_NEEDED"), e -> {
                        var tfa = new TwoFactorHandler(res, callback, ctx.id);

                        return tfa.process();
                    })
                    // sign up unsupported
                    .cast(BaseAuthorization.class);

        });
    }

    /**
     * Interface for controlling auth code flow.
     *
     * @implSpec It's preferable to make the implementation ready for sharing.
     */
    public interface Callback extends TwoFactorHandler.Callback {

        /**
         * Computes {@code CodeSettings} for further processing. By default, returns empty settings.
         *
         * @param res An auth flow resources.
         * @return A {@link Mono} emitting on successful completion a {@code CodeSettings}.
         * Any emitted errors or empty signals will terminate auth flow.
         */
        default Mono<CodeSettings> getCodeSettings(Resources res) {
            return Mono.just(ImmutableCodeSettings.of());
        }

        /**
         * Reads phone number from input.
         *
         * @param res An auth flow resources.
         * @param ctx Current context of code flow. Do not cache this value.
         * @return A {@link Mono} emitting on successful completion a {@code PhoneNumberAction} action.
         * Any emitted errors or empty signals will terminate auth flow.
         */
        Mono<PhoneNumberAction> onPhoneNumber(Resources res, PhoneNumberContext ctx);

        /**
         * Handles error occurred when attempting to check specified phone number.
         *
         * @param res An auth flow resources.
         * @param ctx Current context of code flow. Do not cache this value.
         * @param rpcError The original RPC error.
         * @return A {@link Mono} emitting on successful completion action to do.
         * Any emitted errors or empty signals will terminate auth flow.
         */
        default Mono<ActionType> onPhoneNumberError(Resources res, PhoneNumberContext ctx, RpcError rpcError) {
            return Mono.fromSupplier(() -> {
                if (rpcError.errorCode() == 500) {
                    ctx.log("Internal error occurred during authorization, code: " + rpcError.errorCode()
                            + ", message: " + rpcError.errorMessage());
                    return ActionType.STOP;
                }

                ctx.log("Specified phone number is invalid, retrying...");
                return ActionType.RETRY;
            });
        }

        /**
         * Reads verification code from input.
         *
         * @param res An auth flow resources.
         * @param ctx Current context of code flow. Do not cache this value.
         * @return A {@link Mono} emitting on successful completion a {@code CodeAction} action.
         * Any emitted errors or empty signals will terminate auth flow.
         */
        Mono<CodeAction> onSentCode(Resources res, PhoneCodeContext ctx);

        /**
         * Handles error occurred when attempting to check specified verification code.
         *
         * @param res An auth flow resources.
         * @param ctx Current context of code flow. Do not cache this value.
         * @param rpcError The original RPC error.
         * @return A {@link Mono} emitting on successful completion action to do.
         * Any emitted errors or empty signals will terminate auth flow.
         */
        default Mono<ActionType> onSentCodeError(Resources res, PhoneCodeContext ctx, RpcError rpcError) {
            return Mono.fromSupplier(() -> {
                if (rpcError.errorCode() == 500) {
                    ctx.log("Internal error occurred during authorization, code: " + rpcError.errorCode()
                            + ", message: " + rpcError.errorMessage());
                    return ActionType.STOP;
                }

                ctx.log("Specified phone code is invalid, resending new...");
                return ActionType.RETRY;
            });
        }
    }

    /**
     * Context available when entering a phone number.
     *
     * @apiNote Class not intended to be cached when passed to the callback methods, as it
     * not thread-safe and mutable.
     */
    public interface PhoneNumberContext {

        /**
         * Logs specified message to internal logger with custom formatting.
         *
         * @param message The message to log.
         */
        void log(String message);
    }

    /** The type of actions that can be selected when entering a phone number. */
    public sealed interface PhoneNumberAction {
        /**
         * Creates new {@code PhoneNumber} with specified phone number.
         * Note, phone number must contain only digits without special chars and spaces.
         *
         * @param value The phone number.
         * @return A new instance of {@code PhoneNumber}.
         */
        static PhoneNumber of(String value) {
            return new PhoneNumber(value);
        }

        /**
         * Gets common instance of {@code CancelAction}
         * to cancel auth flow.
         *
         * @return Always same instance of {@code CancelAction}.
         */
        static CancelAction cancel() {
            return CancelAction.instance;
        }
    }

    /**
     * A value-based record with phone number.
     * This class doesn't perform any validation checks
     * on the specified phone number, except for null-check.
     *
     * @param value The phone number itself.
     */
    public record PhoneNumber(String value) implements PhoneNumberAction {
        public PhoneNumber {
            Objects.requireNonNull(value);
        }
    }

    /**
     * Subtype of {@code PhoneNumberAction} indicating stopping of auth flow.
     *
     * @see PhoneNumberAction#cancel()
     */
    public static final class CancelAction implements PhoneNumberAction {
        private static final CancelAction instance = new CancelAction();

        private CancelAction() {}
    }

    /**
     * Context available when entering a sent verification code.
     *
     * @apiNote Class not intended to be cached when passed to the callback methods, as it
     * not thread-safe and mutable.
     */
    public interface PhoneCodeContext extends PhoneNumberContext {

        /** {@return A cached phone number produced by previous auth step} */
        String phoneNumber();

        /** {@return Information about current verification code} */
        BaseSentCode sentCode();
    }

    /** The type of actions that can be selected when entering a verification code. */
    public sealed interface CodeAction {
        /**
         * Creates new {@code Code} with specified verification code.
         *
         * @param value The verification code.
         * @return A new instance of {@code Code}.
         */
        static Code of(String value) {
            return new Code(value);
        }

        /**
         * Gets common instance of {@code CancelCodeAction}
         * to cancel current verification code and stop auth flow.
         *
         * @return Always same instance of {@code CancelCodeAction}.
         */
        static CancelCodeAction cancel() {
            return CancelCodeAction.instance;
        }

        /**
         * Gets common instance of {@code ResendCodeAction}
         * to resend verification code.
         *
         * @return Always same instance of {@code ResendCodeAction}.
         */
        static ResendCodeAction resend() {
            return ResendCodeAction.instance;
        }
    }

    /**
     * A value-based record with verification code.
     * This class doesn't perform any validation checks
     * on the specified verification code, except for null-check.
     *
     * @param value The verification code itself.
     */
    public record Code(String value) implements CodeAction {
        public Code {
            Objects.requireNonNull(value);
        }
    }

    /**
     * Subtype of {@code CodeAction} indicating cancelling
     * of current verification code and auth flow.
     *
     * @see CodeAction#cancel()
     */
    public static final class CancelCodeAction implements CodeAction {
        private static final CancelCodeAction instance = new CancelCodeAction();

        private CancelCodeAction() {}
    }

    /**
     * Subtype of {@code ResendCodeAction} indicating resending of verification code.
     *
     * @see CodeAction#resend()
     */
    public static final class ResendCodeAction implements CodeAction {
        private static final ResendCodeAction instance = new ResendCodeAction();

        private ResendCodeAction() {}
    }

    // Implementation code
    // ======================

    protected Mono<SentCode> migrateTo(Resources res, Context ctx,
                                       int dcId, SendCode originalRequest) {

        ctx.log("Redirecting to DC " + dcId);

        return res.storeLayout().getDcOptions()
                .map(dcOpts -> dcOpts.find(DataCenter.Type.REGULAR, dcId)
                        .orElseThrow(() -> new IllegalStateException("Could not find DC " + dcId
                                + " for redirecting main client")))
                .flatMap(res.clientGroup()::setMain)
                .flatMap(client -> client.send(originalRequest));
    }

    protected Mono<Authorization> onSentCode(Resources res, Context ctx, SentCode sentCode) {
        // TODO: why new subtype of SentCode was added?
        if (!(sentCode instanceof BaseSentCode b)) {
            return Mono.error(new IllegalStateException());
        }

        ctx.sentCode = b;
        return sentCode(res, ctx);
    }

    protected Mono<Authorization> sentCode(Resources res, Context ctx) {
        String phoneNumber = ctx.phoneNumber();
        BaseSentCode sentCode = ctx.sentCode();
        return callback.onSentCode(res, ctx)
                .flatMap(codeAction -> {
                    if (codeAction == CodeAction.cancel()) {
                        return cancelCode(res, ctx);
                    } else if (codeAction == CodeAction.resend()) {
                        return resendCode(res, ctx);
                    } else if (codeAction instanceof Code c) {
                        return res.clientGroup().send(DcId.main(), SignIn.builder()
                                .phoneNumber(phoneNumber)
                                .phoneCode(c.value)
                                .phoneCodeHash(sentCode.phoneCodeHash())
                                .build());
                    } else {
                        throw new IllegalStateException();
                    }
                })
                .onErrorResume(RpcException.class, e ->
                        callback.onSentCodeError(res, ctx, e.getError())
                                .flatMap(actionType -> switch (actionType) {
                                    case RETRY -> resendCode(res, ctx);
                                    case STOP -> this.<Authorization>cancelCode(res, ctx);
                                }));
    }

    protected Mono<Authorization> resendCode(Resources res, Context ctx) {
        String phoneNumber = ctx.phoneNumber();
        BaseSentCode sentCode = ctx.sentCode();
        return res.clientGroup().send(DcId.main(), ImmutableResendCode.of(phoneNumber, sentCode.phoneCodeHash()))
                .flatMap(newSentCode -> onSentCode(res, ctx, newSentCode));
    }

    protected <T> Mono<T> cancelCode(Resources res, Context ctx) {
        String phoneNumber = ctx.phoneNumber();
        BaseSentCode sentCode = ctx.sentCode();
        return res.clientGroup().send(DcId.main(), ImmutableCancelCode.of(phoneNumber, sentCode.phoneCodeHash()))
                .then(cancel(res, ctx));
    }

    protected <T> Mono<T> cancel(Resources res, Context ctx) {
        // To cancel authorization enough to emit empty signals
        return Mono.empty();
    }

    protected Mono<SentCode> sendCode(Resources res, Context ctx) {
        return callback.onPhoneNumber(res, ctx)
                .flatMap(action -> {
                    if (action == PhoneNumberAction.cancel()) {
                        return cancel(res, ctx);
                    } else if (action instanceof PhoneNumber p) {
                        return callback.getCodeSettings(res)
                                .map(sett -> ImmutableSendCode.of(p.value, res.authResources().getApiId(),
                                        res.authResources().getApiHash(), sett));
                    } else {
                        throw new IllegalStateException();
                    }
                })
                .flatMap(sendCode -> {
                    ctx.phoneNumber = sendCode.phoneNumber();

                    return res.clientGroup().send(DcId.main(), sendCode);
                })
                .onErrorResume(RpcException.isErrorCode(303), e -> {
                    var rpcExc = (RpcException) e;
                    String msg = rpcExc.getError().errorMessage();
                    if (!msg.startsWith("PHONE_MIGRATE_"))
                        return Mono.error(new IllegalStateException("Unexpected type of DC redirection", e));

                    int dcId = Integer.parseInt(msg.substring(14));

                    return migrateTo(res, ctx, dcId, (SendCode) rpcExc.getMethod());
                })
                .onErrorResume(RpcException.class, e ->
                        callback.onPhoneNumberError(res, ctx, e.getError())
                                .flatMap(type -> switch (type) {
                                    case STOP -> this.<SentCode>cancel(res, ctx);
                                    case RETRY -> sendCode(res, ctx);
                                }));
    }

    protected static class Context extends AuthContext implements PhoneCodeContext {
        @Nullable
        protected String phoneNumber;
        @Nullable
        protected BaseSentCode sentCode;

        @Override
        protected void log0(String message) {
            log.info(message);
        }

        // Public API
        // =============

        @Override
        public final String phoneNumber() {
            String pn = phoneNumber;
            Preconditions.requireState(pn != null, "phoneNumber has not initialized yet");
            return pn;
        }

        @Override
        public final BaseSentCode sentCode() {
            BaseSentCode sc = sentCode;
            Preconditions.requireState(sc != null, "sentCode has not initialized yet");
            return sc;
        }
    }
}
