/*
 * Copyright 2023 Telegram4J
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package telegram4j.mtproto.service;

import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Mono;
import telegram4j.mtproto.client.MTProtoClientGroup;
import telegram4j.mtproto.service.Compatible.Type;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.CodeSettings;
import telegram4j.tl.InputCheckPasswordSRP;
import telegram4j.tl.auth.*;
import telegram4j.tl.request.auth.*;

public class AuthService extends RpcService {

    public AuthService(MTProtoClientGroup clientGroup, StoreLayout storeLayout) {
        super(clientGroup, storeLayout);
    }

    // auth namespace
    // =========================

    @Compatible(Type.BOTH)
    public Mono<Authorization> importAuthorization(int dcId, ByteBuf bytes) {
        return Mono.defer(() -> sendMain(ImmutableImportAuthorization.of(dcId, bytes)));
    }

    @Compatible(Type.BOTH)
    public Mono<ExportedAuthorization> exportAuthorization(int dcId) {
        return sendMain(ImmutableExportAuthorization.of(dcId));
    }

    @Compatible(Type.BOTH)
    public Mono<ByteBuf> logOut() {
        return sendMain(LogOut.instance())
                .mapNotNull(LoggedOut::futureAuthToken);
    }

    @Compatible(Type.BOTH)
    public Mono<Boolean> bindTempAuthKey(long permAuthKeyId, long nonce, int expiresAt, ByteBuf encryptedMessage) {
        return Mono.defer(() -> sendMain(ImmutableBindTempAuthKey.of(permAuthKeyId, nonce, expiresAt, encryptedMessage)));
    }

    @Compatible(Type.BOTH)
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
