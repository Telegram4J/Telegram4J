package telegram4j.mtproto.service;

import reactor.core.publisher.Mono;
import telegram4j.mtproto.BotCompatible;
import telegram4j.mtproto.MTProtoClient;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.*;
import telegram4j.tl.request.bots.*;

import java.util.List;

@BotCompatible
public class BotService extends RpcService {

    public BotService(MTProtoClient client, StoreLayout storeLayout) {
        super(client, storeLayout);
    }

    // bots namespace
    // =========================

    public Mono<String> sendCustomRequest(String customMethod, String params) {
        return client.sendAwait(ImmutableSendCustomRequest.of(customMethod, ImmutableDataJSON.of(params)))
                .map(DataJSON::data);
    }

    public Mono<Boolean> answerWebhookJsonQuery(long queryId, String dataJson) {
        return client.sendAwait(ImmutableAnswerWebhookJSONQuery.of(queryId, ImmutableDataJSON.of(dataJson)));
    }

    public Mono<Boolean> setBotCommands(BotCommandScope scope, String langCode, Iterable<? extends BotCommand> commands) {
        return Mono.defer(() -> client.sendAwait(ImmutableSetBotCommands.of(scope, langCode, commands)));
    }

    public Mono<Boolean> resetBotCommands(BotCommandScope scope, String langCode) {
        return client.sendAwait(ImmutableResetBotCommands.of(scope, langCode));
    }

    public Mono<List<BotCommand>> getBotCommands(BotCommandScope scope, String langCode) {
        return client.sendAwait(ImmutableGetBotCommands.of(scope, langCode));
    }

    public Mono<Boolean> setBotMenuButton(InputUser user, BotMenuButton button) {
        return client.sendAwait(ImmutableSetBotMenuButton.of(user, button));
    }

    public Mono<BotMenuButton> getBotMenuButton(InputUser user) {
        return client.sendAwait(ImmutableGetBotMenuButton.of(user));
    }

    public Mono<Boolean> setBotBroadcastDefaultAdminRights(ChatAdminRights adminRights) {
        return client.sendAwait(ImmutableSetBotBroadcastDefaultAdminRights.of(adminRights));
    }

    public Mono<Boolean> setBotGroupDefaultAdminRights(ChatAdminRights adminRights) {
        return client.sendAwait(ImmutableSetBotGroupDefaultAdminRights.of(adminRights));
    }
}
