package telegram4j.mtproto.service;

import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Mono;
import telegram4j.mtproto.MTProtoClientGroup;
import telegram4j.mtproto.service.Compatible.Type;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.*;
import telegram4j.tl.help.*;
import telegram4j.tl.request.help.*;

import java.util.List;

public class HelpService extends RpcService {

    public HelpService(MTProtoClientGroup clientGroup, StoreLayout storeLayout) {
        super(clientGroup, storeLayout);
    }

    // help namespace
    // =========================

    @Compatible(Type.BOTH)
    public Mono<Config> getConfig() {
        return sendMain(GetConfig.instance());
    }

    public Mono<NearestDc> getNearestDc() {
        return sendMain(GetNearestDc.instance());
    }

    public Mono<BaseAppUpdate> getAppUpdate(String source) {
        return sendMain(ImmutableGetAppUpdate.of(source))
                .ofType(BaseAppUpdate.class);
    }

    public Mono<String> getInviteText() {
        return sendMain(GetInviteText.instance()).map(InviteText::message);
    }

    public Mono<Support> getSupport() {
        return sendMain(GetSupport.instance());
    }

    public Mono<Updates> getAppChangelog(String prevAppVersion) {
        return sendMain(ImmutableGetAppChangelog.of(prevAppVersion));
    }

    @Compatible(Type.BOT)
    public Mono<Boolean> setBotUpdatesStatus(int pendingUpdatesCount, String message) {
        return sendMain(ImmutableSetBotUpdatesStatus.of(pendingUpdatesCount, message));
    }

    @Compatible(Type.BOTH)
    public Mono<List<CdnPublicKey>> getCdnConfig() {
        return sendMain(GetCdnConfig.instance())
                .map(CdnConfig::publicKeys);
    }

    public Mono<RecentMeUrls> getRecentMeUrls(String referer) {
        return sendMain(ImmutableGetRecentMeUrls.of(referer));
    }

    public Mono<TermsOfServiceUpdate> getTermsOfServiceUpdate() {
        return sendMain(GetTermsOfServiceUpdate.instance());
    }

    public Mono<Boolean> acceptTermsOfService(String idJson) {
        return sendMain(ImmutableAcceptTermsOfService.of(ImmutableDataJSON.of(idJson)));
    }

    public Mono<BaseDeepLinkInfo> getDeepLinkInfo(String path) {
        return sendMain(ImmutableGetDeepLinkInfo.of(path))
                .ofType(BaseDeepLinkInfo.class);
    }

    public Mono<JsonNode> getAppConfig() {
        return sendMain(GetAppConfig.instance());
    }

    public Mono<Boolean> saveAppLog(Iterable<? extends InputAppEvent> events) {
        return Mono.defer(() -> sendMain(ImmutableSaveAppLog.of(events)));
    }

    public Mono<BasePassportConfig> getPassportConfig(int hash) {
        return sendMain(ImmutableGetPassportConfig.of(hash))
                .ofType(BasePassportConfig.class);
    }

    public Mono<String> getSupportName() {
        return sendMain(GetSupportName.instance()).map(SupportName::name);
    }

    public Mono<UserInfo> getUserInfo(InputUser userId) {
        return sendMain(ImmutableGetUserInfo.of(userId));
    }

    public Mono<UserInfo> getUserInfo(InputUser userId, String message, List<MessageEntity> entities) {
        return Mono.defer(() -> sendMain(ImmutableEditUserInfo.of(userId, message, entities)));
    }

    public Mono<PromoData> getPromoData() {
        return sendMain(GetPromoData.instance());
    }

    public Mono<Boolean> hidePromoData(InputPeer peer) {
        return sendMain(ImmutableHidePromoData.of(peer));
    }

    public Mono<Boolean> dismissSuggestion(InputPeer peer, String suggestion) {
        return sendMain(ImmutableDismissSuggestion.of(peer, suggestion));
    }

    public Mono<CountriesList> getCountriesList(String langCode, int hash) {
        return sendMain(ImmutableGetCountriesList.of(langCode, hash));
    }

    public Mono<PremiumPromo> getPremiumPromo() {
        return sendMain(GetPremiumPromo.instance());
    }
}
