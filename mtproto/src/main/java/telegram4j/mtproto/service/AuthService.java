package telegram4j.mtproto.service;

import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Mono;
import telegram4j.mtproto.MTProtoClientGroupManager;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.CodeSettings;
import telegram4j.tl.InputCheckPasswordSRP;
import telegram4j.tl.auth.*;
import telegram4j.tl.request.auth.*;

public class AuthService extends RpcService {

    public AuthService(MTProtoClientGroupManager groupManager, StoreLayout storeLayout) {
        super(groupManager, storeLayout);
    }

    // auth namespace
    // =========================

    @BotCompatible
    public Mono<Authorization> importAuthorization(int dcId, ByteBuf bytes) {
        return Mono.defer(() -> sendMain(ImmutableImportAuthorization.of(dcId, bytes)));
    }

    @BotCompatible
    public Mono<ExportedAuthorization> exportAuthorization(int dcId) {
        return sendMain(ImmutableExportAuthorization.of(dcId));
    }

    @BotCompatible
    public Mono<ByteBuf> logOut() {
        return sendMain(LogOut.instance())
                .mapNotNull(LoggedOut::futureAuthToken);
    }

    @BotCompatible
    public Mono<Boolean> bindTempAuthKey(long permAuthKeyId, long nonce, int expiresAt, ByteBuf encryptedMessage) {
        return Mono.defer(() -> sendMain(ImmutableBindTempAuthKey.of(permAuthKeyId, nonce, expiresAt, encryptedMessage)));
    }

    @BotCompatible
    public Mono<BaseAuthorization> importBotAuthorization(int flags, int apiId, String apiHash, String botAuthToken) {
        return sendMain(ImmutableImportBotAuthorization.of(flags, apiId, apiHash, botAuthToken))
                .cast(BaseAuthorization.class);
    }

    public Mono<SentCode> sendCode(String phoneNumber, int apiId, String apiHash, CodeSettings settings) {
        return sendMain(ImmutableSendCode.of(phoneNumber, apiId, apiHash, settings));
    }

    public Mono<Authorization> signUp(String phoneNumber, String phoneNumberHash, String firstName, String lastName) {
        return sendMain(ImmutableSignUp.of(phoneNumber, phoneNumberHash, firstName, lastName));
    }

    public Mono<Authorization> signIn(SignIn request) {
        return sendMain(request);
    }

    public Mono<Boolean> resetAuthorizations() {
        return sendMain(ResetAuthorizations.instance());
    }

    public Mono<Authorization> checkPassword(InputCheckPasswordSRP password) {
        return sendMain(ImmutableCheckPassword.of(password));
    }

    public Mono<String> requestPasswordRecovery() {
        return sendMain(RequestPasswordRecovery.instance()).map(PasswordRecovery::emailPattern);
    }

    public Mono<Authorization> recoverPassword(RecoverPassword request) {
        return sendMain(request);
    }

    public Mono<SentCode> resendCode(String phoneNumber, String phoneCodeHash) {
        return sendMain(ImmutableResendCode.of(phoneNumber, phoneCodeHash));
    }

    public Mono<Boolean> cancelCode(String phoneNumber, String phoneCodeHash) {
        return sendMain(ImmutableCancelCode.of(phoneNumber, phoneCodeHash));
    }

    public Mono<Boolean> dropTempAuthKeys(Iterable<Long> exceptAuthKeys) {
        return Mono.defer(() -> sendMain(ImmutableDropTempAuthKeys.of(exceptAuthKeys)));
    }

    public Mono<LoginToken> exportLoginToken(int apiId, String apiHash, Iterable<Long> exceptIds) {
        return Mono.defer(() -> sendMain(ImmutableExportLoginToken.of(apiId, apiHash, exceptIds)));
    }

    public Mono<LoginToken> importLoginToken(ByteBuf token) {
        return Mono.defer(() -> sendMain(ImmutableImportLoginToken.of(token)));
    }

    public Mono<telegram4j.tl.Authorization> acceptLoginToken(ByteBuf token) {
        return Mono.defer(() -> sendMain(ImmutableAcceptLoginToken.of(token)));
    }

    public Mono<Boolean> checkRecoveryPassword(String code) {
        return sendMain(ImmutableCheckRecoveryPassword.of(code));
    }
}
