package telegram4j.mtproto.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.mtproto.BotCompatible;
import telegram4j.mtproto.MTProtoClient;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.BotCommand;
import telegram4j.tl.BotCommandScope;
import telegram4j.tl.DataJSON;
import telegram4j.tl.ImmutableDataJSON;
import telegram4j.tl.request.bots.*;

import java.util.function.Function;

@BotCompatible
public class BotService extends RpcService {

    public BotService(MTProtoClient client, StoreLayout storeLayout) {
        super(client, storeLayout);
    }

    public Mono<String> sendCustomRequest(String customMethod, String params) {
        return client.sendAwait(ImmutableSendCustomRequest.of(customMethod, ImmutableDataJSON.of(params)))
                .map(DataJSON::data);
    }

    public Mono<Boolean> answerWebhookJsonQuery(long queryId, String dataJson) {
        return client.sendAwait(ImmutableAnswerWebhookJSONQuery.of(queryId, ImmutableDataJSON.of(dataJson)));
    }

    public Mono<Boolean> setBotCommands(BotCommandScope scope, String langCode, Iterable<? extends BotCommand> commands) {
        return client.sendAwait(SetBotCommands.builder()
                .scope(scope)
                .langCode(langCode)
                .commands(commands)
                .build());
    }

    public Mono<Boolean> resetBotCommands(BotCommandScope scope, String langCode) {
        return client.sendAwait(ImmutableResetBotCommands.of(scope, langCode));
    }

    public Flux<BotCommand> getBotCommands(BotCommandScope scope, String langCode) {
        return client.sendAwait(ImmutableGetBotCommands.of(scope, langCode))
                .flatMapIterable(Function.identity());
    }
}
