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

import reactor.core.publisher.Mono;
import telegram4j.mtproto.client.MTProtoClientGroup;
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
