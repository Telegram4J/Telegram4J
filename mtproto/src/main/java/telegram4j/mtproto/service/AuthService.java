package telegram4j.mtproto.service;

import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Mono;
import telegram4j.mtproto.BotCompatible;
import telegram4j.mtproto.MTProtoClient;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.CodeSettings;
import telegram4j.tl.InputCheckPasswordSRP;
import telegram4j.tl.auth.*;
import telegram4j.tl.request.auth.*;

public class AuthService extends RpcService {

    public AuthService(MTProtoClient client, StoreLayout storeLayout) {
        super(client, storeLayout);
    }

    // auth namespace
    // =========================

    @BotCompatible
    public Mono<Authorization> importAuthorization(int dcId, ByteBuf bytes) {
        return Mono.defer(() -> client.sendAwait(ImmutableImportAuthorization.of(dcId, bytes)));
    }

    @BotCompatible
    public Mono<ExportedAuthorization> exportAuthorization(int dcId) {
        return client.sendAwait(ImmutableExportAuthorization.of(dcId));
    }

    @BotCompatible
    public Mono<ByteBuf> logOut() {
        return client.sendAwait(LogOut.instance())
                .flatMap(f -> Mono.justOrEmpty(f.futureAuthToken()));
    }

    @BotCompatible
    public Mono<Boolean> bindTempAuthKey(long permAuthKeyId, long nonce, int expiresAt, ByteBuf encryptedMessage) {
        return Mono.defer(() -> client.sendAwait(ImmutableBindTempAuthKey.of(permAuthKeyId, nonce, expiresAt, encryptedMessage)));
    }

    @BotCompatible
    public Mono<BaseAuthorization> importBotAuthorization(int flags, int apiId, String apiHash, String botAuthToken) {
        return client.sendAwait(ImmutableImportBotAuthorization.of(flags, apiId, apiHash, botAuthToken))
                .cast(BaseAuthorization.class);
    }

    public Mono<SentCode> sendCode(String phoneNumber, int apiId, String apiHash, CodeSettings settings) {
        return client.sendAwait(ImmutableSendCode.of(phoneNumber, apiId, apiHash, settings));
    }

    public Mono<Authorization> signUp(String phoneNumber, String phoneNumberHash, String firstName, String lastName) {
        return client.sendAwait(ImmutableSignUp.of(phoneNumber, phoneNumberHash, firstName, lastName));
    }

    public Mono<Authorization> signIn(SignIn request) {
        return client.sendAwait(request);
    }

    public Mono<Boolean> resetAuthorizations() {
        return client.sendAwait(ResetAuthorizations.instance());
    }

    public Mono<Authorization> checkPassword(InputCheckPasswordSRP password) {
        return client.sendAwait(ImmutableCheckPassword.of(password));
    }

    public Mono<String> requestPasswordRecovery() {
        return client.sendAwait(RequestPasswordRecovery.instance()).map(PasswordRecovery::emailPattern);
    }

    public Mono<Authorization> recoverPassword(RecoverPassword request) {
        return client.sendAwait(request);
    }

    public Mono<SentCode> resendCode(String phoneNumber, String phoneCodeHash) {
        return client.sendAwait(ImmutableResendCode.of(phoneNumber, phoneCodeHash));
    }

    public Mono<Boolean> cancelCode(String phoneNumber, String phoneCodeHash) {
        return client.sendAwait(ImmutableCancelCode.of(phoneNumber, phoneCodeHash));
    }

    public Mono<Boolean> dropTempAuthKeys(Iterable<Long> exceptAuthKeys) {
        return Mono.defer(() -> client.sendAwait(ImmutableDropTempAuthKeys.of(exceptAuthKeys)));
    }

    public Mono<LoginToken> exportLoginToken(int apiId, String apiHash, Iterable<Long> exceptIds) {
        return Mono.defer(() -> client.sendAwait(ImmutableExportLoginToken.of(apiId, apiHash, exceptIds)));
    }

    public Mono<LoginToken> importLoginToken(ByteBuf token) {
        return Mono.defer(() -> client.sendAwait(ImmutableImportLoginToken.of(token)));
    }

    public Mono<telegram4j.tl.Authorization> acceptLoginToken(ByteBuf token) {
        return Mono.defer(() -> client.sendAwait(ImmutableAcceptLoginToken.of(token)));
    }

    public Mono<Boolean> checkRecoveryPassword(String code) {
        return client.sendAwait(ImmutableCheckRecoveryPassword.of(code));
    }
}
