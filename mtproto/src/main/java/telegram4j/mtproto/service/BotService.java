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
