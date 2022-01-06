package telegram4j.mtproto.service;

import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import telegram4j.mtproto.MTProtoClient;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.*;
import telegram4j.tl.help.*;
import telegram4j.tl.request.help.*;
import telegram4j.tl.request.langpack.GetStrings;
import telegram4j.tl.request.langpack.ImmutableGetLangPack;

import java.util.function.Function;

public class HelpService extends RpcService {

    public HelpService(MTProtoClient client, StoreLayout storeLayout) {
        super(client, storeLayout);
    }

    public Mono<Config> getConfig() {
        return client.sendAwait(GetConfig.instance());
    }

    public Mono<NearestDc> getNearstDc() {
        return client.sendAwait(GetNearestDc.instance());
    }

    public Mono<AppUpdate> getAppUpdate(String source) {
        return client.sendAwait(ImmutableGetAppUpdate.of(source));
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
                    switch (updates.identifier()) {
                        case BaseUpdates.ID:
                            BaseUpdates casted = (BaseUpdates) updates;
                            client.updates().emitNext(updates, Sinks.EmitFailureHandler.FAIL_FAST);

                            return Flux.fromIterable(casted.updates());
                        default:
                            return Flux.error(new IllegalArgumentException("Unknown updates type: " + updates.identifier()));
                    }
                })
                .ofType(UpdateServiceNotification.class);
    }

    public Mono<Boolean> setBotUpdatesStatus(int pendingUpdates, String message) {
        return client.sendAwait(ImmutableSetBotUpdatesStatus.of(pendingUpdates, message));
    }

    public Mono<CdnConfig> getCdnConfig() {
        return client.sendAwait(GetCdnConfig.instance());
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

    public Mono<DeepLinkInfo> getDeepLinkInfo(String path) {
        return client.sendAwait(ImmutableGetDeepLinkInfo.of(path));
    }

    public Mono<JsonNode> getAppConfig() {
        return client.sendAwait(GetAppConfig.instance());
    }

    public Mono<Boolean> saveAppLog(Iterable<? extends InputAppEvent> events) {
        return client.sendAwait(SaveAppLog.builder().events(events).build());
    }

    public Mono<PassportConfig> getPassportConfig(int hash) {
        return client.sendAwait(ImmutableGetPassportConfig.of(hash));
    }

    public Mono<String> getSupportName() {
        return client.sendAwait(GetSupportName.instance()).map(SupportName::name);
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
}