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
import telegram4j.tl.request.bots.*;

import java.util.List;

@Compatible(Type.BOT)
public class BotService extends RpcService {

    public BotService(MTProtoClientGroup clientGroup, StoreLayout storeLayout) {
        super(clientGroup, storeLayout);
    }

    // bots namespace
    // =========================

    public Mono<String> sendCustomRequest(String customMethod, String params) {
        return sendMain(ImmutableSendCustomRequest.of(customMethod, ImmutableDataJSON.of(params)))
                .map(DataJSON::data);
    }

    public Mono<Boolean> answerWebhookJsonQuery(long queryId, String dataJson) {
        return sendMain(ImmutableAnswerWebhookJSONQuery.of(queryId, ImmutableDataJSON.of(dataJson)));
    }

    public Mono<Boolean> setBotCommands(BotCommandScope scope, String langCode, Iterable<? extends BotCommand> commands) {
        return sendMain(ImmutableSetBotCommands.of(scope, langCode, commands));
    }

    public Mono<Boolean> resetBotCommands(BotCommandScope scope, String langCode) {
        return sendMain(ImmutableResetBotCommands.of(scope, langCode));
    }

    public Mono<List<BotCommand>> getBotCommands(BotCommandScope scope, String langCode) {
        return sendMain(ImmutableGetBotCommands.of(scope, langCode));
    }

    public Mono<Boolean> setBotMenuButton(InputUser user, BotMenuButton button) {
        return sendMain(ImmutableSetBotMenuButton.of(user, button));
    }

    public Mono<BotMenuButton> getBotMenuButton(InputUser user) {
        return sendMain(ImmutableGetBotMenuButton.of(user));
    }

    public Mono<Boolean> setBotBroadcastDefaultAdminRights(ChatAdminRights adminRights) {
        return sendMain(ImmutableSetBotBroadcastDefaultAdminRights.of(adminRights));
    }

    public Mono<Boolean> setBotGroupDefaultAdminRights(ChatAdminRights adminRights) {
        return sendMain(ImmutableSetBotGroupDefaultAdminRights.of(adminRights));
    }
}
