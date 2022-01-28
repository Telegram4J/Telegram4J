package telegram4j.mtproto.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.mtproto.MTProtoClient;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.*;
import telegram4j.tl.account.AutoDownloadSettings;
import telegram4j.tl.account.*;
import telegram4j.tl.auth.SentCode;
import telegram4j.tl.request.account.ImmutableUpdateNotifySettings;
import telegram4j.tl.request.account.UpdateTheme;
import telegram4j.tl.request.account.*;

import java.util.function.Function;

public class AccountService extends RpcService {

    public AccountService(MTProtoClient client, StoreLayout storeLayout) {
        super(client, storeLayout);
    }

    public Mono<Boolean> registerDevice(RegisterDevice request) {
        return client.sendAwait(request);
    }

    public Mono<Boolean> unregisterDevice(int tokenType, String token, Iterable<Long> otherUids) {
        return client.sendAwait(UnregisterDevice.builder()
                .tokenType(tokenType)
                .token(token)
                .otherUids(otherUids)
                .build());
    }

    public Mono<Boolean> updateNotifySettings(InputNotifyPeer peer, InputPeerNotifySettings settings) {
        return client.sendAwait(ImmutableUpdateNotifySettings.of(peer, settings));
    }

    public Mono<PeerNotifySettings> getNotifySettings(InputNotifyPeer peer) {
        return client.sendAwait(ImmutableGetNotifySettings.of(peer));
    }

    public Mono<Boolean> resetNotifySettings() {
        return client.sendAwait(ResetNotifySettings.instance());
    }

    public Mono<User> updateProfile(@Nullable String firstName, @Nullable String lastName, @Nullable String about) {
        return client.sendAwait(UpdateProfile.builder()
                .firstName(firstName)
                .lastName(lastName)
                .about(about)
                .build());
    }

    public Mono<Boolean> updateStatus(boolean offline) {
        return client.sendAwait(ImmutableUpdateStatus.of(offline));
    }

    public Mono<WallPapers> getWallPapers(long hash) {
        return client.sendAwait(ImmutableGetWallPapers.of(hash));
    }

    public Mono<Boolean> reportPeer(InputPeer peer, ReportReason reportReason, String message) {
        return client.sendAwait(ImmutableReportPeer.of(peer, reportReason, message));
    }

    public Mono<Boolean> checkUsername(String username) {
        return client.sendAwait(ImmutableCheckUsername.of(username));
    }

    public Mono<User> updateUsername(String username) {
        return client.sendAwait(ImmutableUpdateUsername.of(username));
    }

    public Mono<PrivacyRules> getPrivacy(InputPrivacyKey key) {
        return client.sendAwait(ImmutableGetPrivacy.of(key));
    }

    public Mono<PrivacyRules> setPrivacy(InputPrivacyKey key, Iterable<? extends InputPrivacyRule> rules) {
        return client.sendAwait(SetPrivacy.builder()
                .key(key)
                .rules(rules)
                .build());
    }

    public Mono<Boolean> deleteAccount(String reason) {
        return client.sendAwait(ImmutableDeleteAccount.of(reason));
    }

    public Mono<Integer> getAccountTtl() {
        return client.sendAwait(GetAccountTTL.instance())
                .map(AccountDaysTTL::days);
    }

    public Mono<Boolean> setAccountTtl(int days) {
        return client.sendAwait(ImmutableSetAccountTTL.of(ImmutableAccountDaysTTL.of(days)));
    }

    public Mono<SentCode> sendChangePhoneCode(String phoneNumber, CodeSettings settings) {
        return client.sendAwait(ImmutableSendChangePhoneCode.of(phoneNumber, settings));
    }

    public Mono<User> changePhone(String phoneNumber, String phoneCodeHash, String phoneCode) {
        return client.sendAwait(ImmutableChangePhone.of(phoneNumber, phoneCodeHash, phoneCode));
    }

    public Mono<Boolean> updateDeviceLocked(int period) {
        return client.sendAwait(ImmutableUpdateDeviceLocked.of(period));
    }

    public Mono<Authorizations> getAuthorizations() {
        return client.sendAwait(GetAuthorizations.instance());
    }

    public Mono<Boolean> resetAuthorization(long hash) {
        return client.sendAwait(ImmutableResetAuthorization.of(hash));
    }

    public Mono<Password> getPassword() {
        return client.sendAwait(GetPassword.instance());
    }

    public Mono<PasswordSettings> getPasswordSettings(InputCheckPasswordSRP password) {
        return client.sendAwait(ImmutableGetPasswordSettings.of(password));
    }

    public Mono<Boolean> updatePasswordSettings(InputCheckPasswordSRP password, PasswordInputSettings settings) {
        return client.sendAwait(ImmutableUpdatePasswordSettings.of(password, settings));
    }

    public Mono<SentCode> sendConfirmPhoneCode(String hash, CodeSettings settings) {
        return client.sendAwait(ImmutableSendConfirmPhoneCode.of(hash, settings));
    }

    public Mono<Boolean> confirmPhone(String phoneCodeHash, String phoneCode) {
        return client.sendAwait(ImmutableConfirmPhone.of(phoneCodeHash, phoneCode));
    }

    public Mono<TmpPassword> getTmpPassword(InputCheckPasswordSRP password, int period) {
        return client.sendAwait(ImmutableGetTmpPassword.of(password, period));
    }

    public Mono<WebAuthorizations> getWebAuthorizations() {
        return client.sendAwait(GetWebAuthorizations.instance());
    }

    public Mono<Boolean> resetWebAuthorization(long hash) {
        return client.sendAwait(ImmutableResetWebAuthorization.of(hash));
    }

    public Mono<Boolean> resetWebAuthorizations() {
        return client.sendAwait(ResetWebAuthorizations.instance());
    }

    public Flux<SecureValue> getAllSecureValues() {
        return client.sendAwait(GetAllSecureValues.instance())
                .flatMapIterable(Function.identity());
    }

    public Flux<SecureValue> getSecureValue(Iterable<SecureValueType> types) {
        return client.sendAwait(GetSecureValue.builder()
                .types(types)
                .build())
                .flatMapIterable(Function.identity());
    }

    public Mono<SecureValue> saveSecureValue(InputSecureValue value, long secureSecretId) {
        return client.sendAwait(ImmutableSaveSecureValue.of(value, secureSecretId));
    }

    public Mono<Boolean> deleteSecureValue(Iterable<SecureValueType> types) {
        return client.sendAwait(DeleteSecureValue.builder()
                .types(types)
                .build());
    }

    public Mono<AuthorizationForm> getAuthorizationForm(long botId, String scope, String publicKey) {
        return client.sendAwait(ImmutableGetAuthorizationForm.of(botId, scope, publicKey));
    }

    public Mono<Boolean> acceptAuthorization(long botId, String scope, String publicKey,
                                             Iterable<? extends SecureValueHash> valueHashes,
                                             SecureCredentialsEncrypted credentials) {
        return client.sendAwait(AcceptAuthorization.builder()
                .botId(botId)
                .scope(scope)
                .publicKey(publicKey)
                .valueHashes(valueHashes)
                .credentials(credentials)
                .build());
    }

    public Mono<SentCode> sendVerifyPhoneCode(String phoneNumber, CodeSettings settings) {
        return client.sendAwait(ImmutableSendVerifyPhoneCode.of(phoneNumber, settings));
    }

    public Mono<Boolean> verifyPhone(String phoneNumber, String phoneCodeHash, String phoneCode) {
        return client.sendAwait(ImmutableVerifyPhone.of(phoneNumber, phoneCodeHash, phoneCode));
    }

    public Mono<SentEmailCode> sendVerifyEmailCode(String email) {
        return client.sendAwait(ImmutableSendVerifyEmailCode.of(email));
    }

    public Mono<Boolean> verifyEmail(String email, String code) {
        return client.sendAwait(ImmutableVerifyEmail.of(email, code));
    }

    public Mono<Takeout> initTakeoutSession(InitTakeoutSession request) {
        return client.sendAwait(request);
    }

    public Mono<Boolean> finishTakeoutSession(FinishTakeoutSession request) {
        return client.sendAwait(request);
    }

    public Mono<Boolean> confirmPasswordEmail(String code) {
        return client.sendAwait(ImmutableConfirmPasswordEmail.of(code));
    }

    public Mono<Boolean> resendPasswordEmail() {
        return client.sendAwait(ResendPasswordEmail.instance());
    }

    public Mono<Boolean> cancelPasswordEmail() {
        return client.sendAwait(CancelPasswordEmail.instance());
    }

    public Mono<Boolean> getContactSignUpNotification() {
        return client.sendAwait(GetContactSignUpNotification.instance());
    }

    public Mono<Boolean> setContactSignUpNotification(boolean silent) {
        return client.sendAwait(ImmutableSetContactSignUpNotification.of(silent));
    }

    // TODO: check updates type
    public Mono<Updates> getNotifyExceptions(GetNotifyExceptions request) {
        return client.sendAwait(request);
    }

    public Mono<WallPaper> getWallPaper(InputWallPaper wallPaper) {
        return client.sendAwait(ImmutableGetWallPaper.of(wallPaper));
    }

    public Mono<WallPaper> uploadWallPaper(InputFile file, String mimeType, WallPaperSettings settings) {
        return client.sendAwait(ImmutableUploadWallPaper.of(file, mimeType, settings));
    }

    public Mono<Boolean> saveWallPaper(InputWallPaper wallPaper, boolean unsave, WallPaperSettings settings) {
        return client.sendAwait(ImmutableSaveWallPaper.of(wallPaper, unsave, settings));
    }

    public Mono<Boolean> installWallPaper(InputWallPaper wallPaper, WallPaperSettings settings) {
        return client.sendAwait(ImmutableInstallWallPaper.of(wallPaper, settings));
    }

    public Mono<Boolean> resetWallPapers() {
        return client.sendAwait(ResetWallPapers.instance());
    }

    public Mono<AutoDownloadSettings> getAutoDownloadSettings() {
        return client.sendAwait(GetAutoDownloadSettings.instance());
    }

    public Mono<Boolean> saveAutoDownloadSettings(SaveAutoDownloadSettings request) {
        return client.sendAwait(request);
    }

    public Mono<Document> uploadTheme(InputFile file, @Nullable InputFile thumb,
                                      String fileName, String mimyType) {
        return client.sendAwait(UploadTheme.builder()
                .file(file)
                .thumb(thumb)
                .fileName(fileName)
                .mimeType(mimyType)
                .build());
    }

    public Mono<Theme> createTheme(String slug, String title, @Nullable String documentFileReferenceId,
                                   @Nullable Iterable<? extends InputThemeSettings> settings) {
        return Mono.defer(() -> client.sendAwait(CreateTheme.builder()
                .slug(slug)
                .title(title)
                .document(documentFileReferenceId != null
                        ? FileReferenceId.deserialize(documentFileReferenceId)
                        .asInputDocument() : null)
                .settings(settings)
                .build()));
    }

    public Mono<Theme> updateTheme(UpdateTheme request) {
        return client.sendAwait(request);
    }

    public Mono<Boolean> saveTheme(InputTheme theme, boolean unsave) {
        return client.sendAwait(ImmutableSaveTheme.of(theme, unsave));
    }

    public Mono<Boolean> installTheme(InstallTheme request) {
        return client.sendAwait(request);
    }

    public Mono<Theme> getTheme(String format, InputTheme theme, long documentId) {
        return client.sendAwait(ImmutableGetTheme.of(format, theme, documentId));
    }

    public Mono<Themes> getThemes(String format, long hash) {
        return client.sendAwait(ImmutableGetThemes.of(format, hash));
    }

    public Mono<Boolean> setContentSettings(SetContentSettings request) {
        return client.sendAwait(request);
    }

    public Mono<ContentSettings> getContentSettings() {
        return client.sendAwait(GetContentSettings.instance());
    }

    public Flux<WallPaper> getMultiWallPapers(Iterable<? extends InputWallPaper> wallPapers) {
        return client.sendAwait(GetMultiWallPapers.builder()
                .wallpapers(wallPapers)
                .build())
                .flatMapIterable(Function.identity());
    }

    public Mono<GlobalPrivacySettings> getGlobalPrivacySettings() {
        return client.sendAwait(GetGlobalPrivacySettings.instance());
    }

    public Mono<GlobalPrivacySettings> setGlobalPrivacySettings(GlobalPrivacySettings settings) {
        return client.sendAwait(ImmutableSetGlobalPrivacySettings.of(settings));
    }

    public Mono<Boolean> reportProfilePhoto(InputPeer peer, String photoFileReferenceId,
                                            ReportReason reason, String message) {
        return Mono.defer(() -> client.sendAwait(ImmutableReportProfilePhoto.of(peer,
                FileReferenceId.deserialize(photoFileReferenceId).asInputPhoto(), reason, message)));
    }

    public Mono<Boolean> declinePasswordReset() {
        return client.sendAwait(DeclinePasswordReset.instance());
    }

    public Mono<Themes> getChatThemes(int hash) {
        return client.sendAwait(ImmutableGetChatThemes.of(hash));
    }
}
