package telegram4j.mtproto.service;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.mtproto.MTProtoClientGroup;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.*;
import telegram4j.tl.account.AutoDownloadSettings;
import telegram4j.tl.account.*;
import telegram4j.tl.auth.SentCode;
import telegram4j.tl.request.account.ImmutableUpdateNotifySettings;
import telegram4j.tl.request.account.UpdateTheme;
import telegram4j.tl.request.account.*;

import java.util.List;

public class AccountService extends RpcService {

    public AccountService(MTProtoClientGroup clientGroup, StoreLayout storeLayout) {
        super(clientGroup, storeLayout);
    }

    public Mono<Boolean> registerDevice(RegisterDevice request) {
        return sendMain(request);
    }

    public Mono<Boolean> unregisterDevice(int tokenType, String token, Iterable<Long> otherUids) {
        return Mono.defer(() -> sendMain(ImmutableUnregisterDevice.of(tokenType, token, otherUids)));
    }

    public Mono<Boolean> updateNotifySettings(InputNotifyPeer peer, InputPeerNotifySettings settings) {
        return sendMain(ImmutableUpdateNotifySettings.of(peer, settings));
    }

    public Mono<PeerNotifySettings> getNotifySettings(InputNotifyPeer peer) {
        return sendMain(ImmutableGetNotifySettings.of(peer));
    }

    public Mono<Boolean> resetNotifySettings() {
        return sendMain(ResetNotifySettings.instance());
    }

    public Mono<User> updateProfile(@Nullable String firstName, @Nullable String lastName, @Nullable String about) {
        return sendMain(UpdateProfile.builder()
                .firstName(firstName)
                .lastName(lastName)
                .about(about)
                .build());
    }

    public Mono<Boolean> updateStatus(boolean offline) {
        return sendMain(ImmutableUpdateStatus.of(offline));
    }

    public Mono<BaseWallPapers> getWallPapers(long hash) {
        return sendMain(ImmutableGetWallPapers.of(hash))
                .ofType(BaseWallPapers.class);
    }

    public Mono<Boolean> reportPeer(InputPeer peer, ReportReason reportReason, String message) {
        return sendMain(ImmutableReportPeer.of(peer, reportReason, message));
    }

    public Mono<Boolean> checkUsername(String username) {
        return sendMain(ImmutableCheckUsername.of(username));
    }

    public Mono<User> updateUsername(String username) {
        return sendMain(ImmutableUpdateUsername.of(username));
    }

    public Mono<PrivacyRules> getPrivacy(InputPrivacyKey key) {
        return sendMain(ImmutableGetPrivacy.of(key));
    }

    public Mono<PrivacyRules> setPrivacy(InputPrivacyKey key, Iterable<? extends InputPrivacyRule> rules) {
        return Mono.defer(() -> sendMain(ImmutableSetPrivacy.of(key, rules)));
    }

    public Mono<Boolean> deleteAccount(DeleteAccount request) {
        return sendMain(request);
    }

    public Mono<Integer> getAccountTtl() {
        return sendMain(GetAccountTTL.instance())
                .map(AccountDaysTTL::days);
    }

    public Mono<Boolean> setAccountTtl(int days) {
        return sendMain(ImmutableSetAccountTTL.of(ImmutableAccountDaysTTL.of(days)));
    }

    public Mono<SentCode> sendChangePhoneCode(String phoneNumber, CodeSettings settings) {
        return sendMain(ImmutableSendChangePhoneCode.of(phoneNumber, settings));
    }

    public Mono<User> changePhone(String phoneNumber, String phoneCodeHash, String phoneCode) {
        return sendMain(ImmutableChangePhone.of(phoneNumber, phoneCodeHash, phoneCode));
    }

    public Mono<Boolean> updateDeviceLocked(int period) {
        return sendMain(ImmutableUpdateDeviceLocked.of(period));
    }

    public Mono<Authorizations> getAuthorizations() {
        return sendMain(GetAuthorizations.instance());
    }

    public Mono<Boolean> resetAuthorization(long hash) {
        return sendMain(ImmutableResetAuthorization.of(hash));
    }

    public Mono<Password> getPassword() {
        return sendMain(GetPassword.instance());
    }

    public Mono<PasswordSettings> getPasswordSettings(InputCheckPasswordSRP password) {
        return sendMain(ImmutableGetPasswordSettings.of(password));
    }

    public Mono<Boolean> updatePasswordSettings(InputCheckPasswordSRP password, PasswordInputSettings settings) {
        return sendMain(ImmutableUpdatePasswordSettings.of(password, settings));
    }

    public Mono<SentCode> sendConfirmPhoneCode(String hash, CodeSettings settings) {
        return sendMain(ImmutableSendConfirmPhoneCode.of(hash, settings));
    }

    public Mono<Boolean> confirmPhone(String phoneCodeHash, String phoneCode) {
        return sendMain(ImmutableConfirmPhone.of(phoneCodeHash, phoneCode));
    }

    public Mono<TmpPassword> getTmpPassword(InputCheckPasswordSRP password, int period) {
        return sendMain(ImmutableGetTmpPassword.of(password, period));
    }

    public Mono<WebAuthorizations> getWebAuthorizations() {
        return sendMain(GetWebAuthorizations.instance());
    }

    public Mono<Boolean> resetWebAuthorization(long hash) {
        return sendMain(ImmutableResetWebAuthorization.of(hash));
    }

    public Mono<Boolean> resetWebAuthorizations() {
        return sendMain(ResetWebAuthorizations.instance());
    }

    public Mono<List<SecureValue>> getAllSecureValues() {
        return sendMain(GetAllSecureValues.instance());
    }

    public Mono<List<SecureValue>> getSecureValue(Iterable<SecureValueType> types) {
        return Mono.defer(() -> sendMain(ImmutableGetSecureValue.of(types)));
    }

    public Mono<SecureValue> saveSecureValue(InputSecureValue value, long secureSecretId) {
        return sendMain(ImmutableSaveSecureValue.of(value, secureSecretId));
    }

    public Mono<Boolean> deleteSecureValue(Iterable<SecureValueType> types) {
        return sendMain(DeleteSecureValue.builder()
                .types(types)
                .build());
    }

    public Mono<AuthorizationForm> getAuthorizationForm(long botId, String scope, String publicKey) {
        return sendMain(ImmutableGetAuthorizationForm.of(botId, scope, publicKey));
    }

    public Mono<Boolean> acceptAuthorization(AcceptAuthorization request) {
        return sendMain(request);
    }

    public Mono<SentCode> sendVerifyPhoneCode(String phoneNumber, CodeSettings settings) {
        return sendMain(ImmutableSendVerifyPhoneCode.of(phoneNumber, settings));
    }

    public Mono<Boolean> verifyPhone(String phoneNumber, String phoneCodeHash, String phoneCode) {
        return sendMain(ImmutableVerifyPhone.of(phoneNumber, phoneCodeHash, phoneCode));
    }

    public Mono<SentEmailCode> sendVerifyEmailCode(EmailVerifyPurpose purpose, String email) {
        return sendMain(ImmutableSendVerifyEmailCode.of(purpose, email));
    }

    public Mono<EmailVerified> verifyEmail(EmailVerifyPurpose purpose, EmailVerification verification) {
        return sendMain(ImmutableVerifyEmail.of(purpose, verification));
    }

    public Mono<Long> initTakeoutSession(InitTakeoutSession request) {
        return sendMain(request).map(Takeout::id);
    }

    public Mono<Boolean> finishTakeoutSession(FinishTakeoutSession request) {
        return sendMain(request);
    }

    public Mono<Boolean> confirmPasswordEmail(String code) {
        return sendMain(ImmutableConfirmPasswordEmail.of(code));
    }

    public Mono<Boolean> resendPasswordEmail() {
        return sendMain(ResendPasswordEmail.instance());
    }

    public Mono<Boolean> cancelPasswordEmail() {
        return sendMain(CancelPasswordEmail.instance());
    }

    public Mono<Boolean> getContactSignUpNotification() {
        return sendMain(GetContactSignUpNotification.instance());
    }

    public Mono<Boolean> setContactSignUpNotification(boolean silent) {
        return sendMain(ImmutableSetContactSignUpNotification.of(silent));
    }

    public Mono<Updates> getNotifyExceptions(GetNotifyExceptions request) {
        return sendMain(request);
    }

    public Mono<WallPaper> getWallPaper(InputWallPaper wallPaper) {
        return sendMain(ImmutableGetWallPaper.of(wallPaper));
    }

    public Mono<WallPaper> uploadWallPaper(InputFile file, String mimeType, WallPaperSettings settings) {
        return sendMain(ImmutableUploadWallPaper.of(file, mimeType, settings));
    }

    public Mono<Boolean> saveWallPaper(InputWallPaper wallPaper, boolean unsave, WallPaperSettings settings) {
        return sendMain(ImmutableSaveWallPaper.of(wallPaper, unsave, settings));
    }

    public Mono<Boolean> installWallPaper(InputWallPaper wallPaper, WallPaperSettings settings) {
        return sendMain(ImmutableInstallWallPaper.of(wallPaper, settings));
    }

    public Mono<Boolean> resetWallPapers() {
        return sendMain(ResetWallPapers.instance());
    }

    public Mono<AutoDownloadSettings> getAutoDownloadSettings() {
        return sendMain(GetAutoDownloadSettings.instance());
    }

    public Mono<Boolean> saveAutoDownloadSettings(SaveAutoDownloadSettings request) {
        return sendMain(request);
    }

    public Mono<BaseDocument> uploadTheme(InputFile file, @Nullable InputFile thumb,
                                          String fileName, String mimeType) {
        return sendMain(ImmutableUploadTheme.of(file, fileName, mimeType)
                        .withThumb(thumb))
                .ofType(BaseDocument.class);
    }

    public Mono<Theme> createTheme(String slug, String title, @Nullable InputDocument document,
                                   @Nullable Iterable<? extends InputThemeSettings> settings) {
        return Mono.defer(() -> sendMain(CreateTheme.builder()
                .slug(slug)
                .title(title)
                .document(document)
                .settings(settings)
                .build()));
    }

    public Mono<Theme> updateTheme(UpdateTheme request) {
        return sendMain(request);
    }

    public Mono<Boolean> saveTheme(InputTheme theme, boolean unsave) {
        return sendMain(ImmutableSaveTheme.of(theme, unsave));
    }

    public Mono<Boolean> installTheme(InstallTheme request) {
        return sendMain(request);
    }

    public Mono<Theme> getTheme(String format, InputTheme theme) {
        return sendMain(ImmutableGetTheme.of(format, theme));
    }

    public Mono<BaseThemes> getThemes(String format, long hash) {
        return sendMain(ImmutableGetThemes.of(format, hash))
                .ofType(BaseThemes.class);
    }

    public Mono<Boolean> setContentSettings(SetContentSettings request) {
        return sendMain(request);
    }

    public Mono<ContentSettings> getContentSettings() {
        return sendMain(GetContentSettings.instance());
    }

    public Mono<List<WallPaper>> getMultiWallPapers(Iterable<? extends InputWallPaper> wallPapers) {
        return Mono.defer(() -> sendMain(ImmutableGetMultiWallPapers.of(wallPapers)));
    }

    public Mono<GlobalPrivacySettings> getGlobalPrivacySettings() {
        return sendMain(GetGlobalPrivacySettings.instance());
    }

    public Mono<GlobalPrivacySettings> setGlobalPrivacySettings(GlobalPrivacySettings settings) {
        return sendMain(ImmutableSetGlobalPrivacySettings.of(settings));
    }

    public Mono<Boolean> reportProfilePhoto(InputPeer peer, InputPhoto photo,
                                            ReportReason reason, String message) {
        return sendMain(ImmutableReportProfilePhoto.of(peer, photo, reason, message));
    }

    public Mono<Boolean> declinePasswordReset() {
        return sendMain(DeclinePasswordReset.instance());
    }

    public Mono<BaseThemes> getChatThemes(int hash) {
        return sendMain(ImmutableGetChatThemes.of(hash))
                .ofType(BaseThemes.class);
    }
}
