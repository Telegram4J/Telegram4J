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

    public Mono<SentCode> sendCode(String phoneNumber, int appId, String appHash, CodeSettings settings) {
        return client.sendAwait(ImmutableSendCode.of(phoneNumber, appId, appHash, settings));
    }

    public Mono<Authorization> signUp(String phoneNumber, String phoneNumberHash, String firstName, String lastName) {
        return client.sendAwait(ImmutableSignUp.of(phoneNumber, phoneNumberHash, firstName, lastName));
    }

    public Mono<Authorization> signIn(String phoneNumber, String phoneNumberHash, String phoneCode) {
        return Mono.defer(() -> client.sendAwait(ImmutableSignIn.of(phoneNumber.replaceAll("\\s+\\+", ""),
                phoneNumberHash, phoneCode)));
    }

    @BotCompatible
    public Mono<ByteBuf> logOut() {
        return client.sendAwait(LogOut.instance())
                .flatMap(f -> Mono.justOrEmpty(f.futureAuthToken()));
    }

    public Mono<Boolean> resetAuthorizations() {
        return client.sendAwait(ResetAuthorizations.instance());
    }

    @BotCompatible
    public Mono<ExportedAuthorization> exportAuthorization(int dcId) {
        return client.sendAwait(ImmutableExportAuthorization.of(dcId));
    }

    @BotCompatible
    public Mono<Authorization> importAuthorization(int dcId, ByteBuf bytes) {
        return client.sendAwait(ImmutableImportAuthorization.of(dcId)
                .withBytes(bytes));
    }

    @BotCompatible
    public Mono<Boolean> bindTempAuthKey(long permAuthKeyId, long nonce, int expiresAt, ByteBuf encryptedMessage) {
        return client.sendAwait(ImmutableBindTempAuthKey.of(permAuthKeyId, nonce, expiresAt)
                .withEncryptedMessage(encryptedMessage));
    }

    @BotCompatible
    public Mono<Authorization> importBotAuthorization(int flags, int appId, String apiHash, String botAuthToken) {
        return client.sendAwait(ImmutableImportBotAuthorization.of(flags, appId, apiHash, botAuthToken));
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
        return client.sendAwait(DropTempAuthKeys.builder()
                .exceptAuthKeys(exceptAuthKeys)
                .build());
    }

    public Mono<LoginToken> exportLoginToken(int appId, String apiHash, Iterable<Long> exceptIds) {
        return client.sendAwait(ExportLoginToken.builder()
                .apiId(appId)
                .apiHash(apiHash)
                .exceptIds(exceptIds)
                .build());
    }

    public Mono<LoginToken> importLoginToken(ByteBuf token) {
        return client.sendAwait(ImmutableImportLoginToken.builder()
                .token(token)
                .build());
    }

    public Mono<telegram4j.tl.Authorization> acceptLoginToken(ByteBuf token) {
        return client.sendAwait(ImmutableAcceptLoginToken.builder()
                .token(token)
                .build());
    }

    public Mono<Boolean> checkRecoveryPassword(String code) {
        return client.sendAwait(ImmutableCheckRecoveryPassword.of(code));
    }
}
