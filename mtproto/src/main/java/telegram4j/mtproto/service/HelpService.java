package telegram4j.mtproto.service;

import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import telegram4j.mtproto.BotCompatible;
import telegram4j.mtproto.MTProtoClient;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.*;
import telegram4j.tl.help.*;
import telegram4j.tl.request.help.*;

import java.util.List;

public class HelpService extends RpcService {

    public HelpService(MTProtoClient client, StoreLayout storeLayout) {
        super(client, storeLayout);
    }

    // help namespace
    // =========================

    @BotCompatible
    public Mono<Config> getConfig() {
        return client.sendAwait(GetConfig.instance());
    }

    public Mono<NearestDc> getNearestDc() {
        return client.sendAwait(GetNearestDc.instance());
    }

    public Mono<BaseAppUpdate> getAppUpdate(String source) {
        return client.sendAwait(ImmutableGetAppUpdate.of(source))
                .ofType(BaseAppUpdate.class);
    }

    public Mono<String> getInviteText() {
        return client.sendAwait(GetInviteText.instance()).map(InviteText::message);
    }

    public Mono<Support> getSupport() {
        return client.sendAwait(GetSupport.instance());
    }

    public Flux<UpdateServiceNotification> getAppChangelog(String prevAppVersion) {
        return client.sendAwait(ImmutableGetAppChangelog.of(prevAppVersion))
                .flatMapMany(updates -> {
                    if (updates.identifier() == BaseUpdates.ID) {
                        BaseUpdates casted = (BaseUpdates) updates;
                        client.updates().emitNext(updates, Sinks.EmitFailureHandler.FAIL_FAST);

                        return Flux.fromIterable(casted.updates());
                    }
                    return Flux.error(new IllegalArgumentException("Unknown updates type: " + updates.identifier()));
                })
                .ofType(UpdateServiceNotification.class);
    }

    @BotCompatible
    public Mono<Boolean> setBotUpdatesStatus(int pendingUpdatesCount, String message) {
        return client.sendAwait(ImmutableSetBotUpdatesStatus.of(pendingUpdatesCount, message));
    }

    @BotCompatible
    public Mono<List<CdnPublicKey>> getCdnConfig() {
        return client.sendAwait(GetCdnConfig.instance())
                .map(CdnConfig::publicKeys);
    }

    public Mono<RecentMeUrls> getRecentMeUrls(String referer) {
        return client.sendAwait(ImmutableGetRecentMeUrls.of(referer));
    }

    public Mono<TermsOfServiceUpdate> getTermsOfServiceUpdate() {
        return client.sendAwait(GetTermsOfServiceUpdate.instance());
    }

    public Mono<Boolean> acceptTermsOfService(String idJson) {
        return client.sendAwait(ImmutableAcceptTermsOfService.of(ImmutableDataJSON.of(idJson)));
    }

    public Mono<BaseDeepLinkInfo> getDeepLinkInfo(String path) {
        return client.sendAwait(ImmutableGetDeepLinkInfo.of(path))
                .ofType(BaseDeepLinkInfo.class);
    }

    public Mono<JsonNode> getAppConfig() {
        return client.sendAwait(GetAppConfig.instance());
    }

    public Mono<Boolean> saveAppLog(Iterable<? extends InputAppEvent> events) {
        return Mono.defer(() -> client.sendAwait(ImmutableSaveAppLog.of(events)));
    }

    public Mono<BasePassportConfig> getPassportConfig(int hash) {
        return client.sendAwait(ImmutableGetPassportConfig.of(hash))
                .ofType(BasePassportConfig.class);
    }

    public Mono<String> getSupportName() {
        return client.sendAwait(GetSupportName.instance()).map(SupportName::name);
    }

    public Mono<UserInfo> getUserInfo(InputUser userId) {
        return client.sendAwait(ImmutableGetUserInfo.of(userId));
    }

    public Mono<UserInfo> getUserInfo(InputUser userId, String message, List<MessageEntity> entities) {
        return Mono.defer(() -> client.sendAwait(ImmutableEditUserInfo.of(userId, message, entities)));
    }

    public Mono<PromoData> getPromoData() {
        return client.sendAwait(GetPromoData.instance());
    }

    public Mono<Boolean> hidePromoData(InputPeer peer) {
        return client.sendAwait(ImmutableHidePromoData.of(peer));
    }

    public Mono<Boolean> dismissSuggestion(InputPeer peer, String suggestion) {
        return client.sendAwait(ImmutableDismissSuggestion.of(peer, suggestion));
    }

    public Mono<CountriesList> getCountriesList(String langCode, int hash) {
        return client.sendAwait(ImmutableGetCountriesList.of(langCode, hash));
    }

    public Mono<PremiumPromo> getPremiumPromo() {
        return client.sendAwait(GetPremiumPromo.instance());
    }
}
