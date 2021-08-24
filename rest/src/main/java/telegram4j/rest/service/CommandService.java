package telegram4j.rest.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.json.BotCommandData;
import telegram4j.json.request.DeleteMyCommands;
import telegram4j.json.request.GetMyCommands;
import telegram4j.json.request.SetMyCommands;
import telegram4j.rest.RestRouter;
import telegram4j.rest.route.Routes;

public class CommandService extends RestService {

    public CommandService(RestRouter router) {
        super(router);
    }

    public Mono<Boolean> setMyCommands(SetMyCommands setMyCommands) {
        return Routes.SET_MY_COMMANDS.newRequest()
                .body(setMyCommands)
                .exchange(router)
                .bodyTo(Boolean.class);
    }

    public Mono<Boolean> deleteMyCommands(DeleteMyCommands deleteMyCommands) {
        return Routes.DELETE_MY_COMMANDS.newRequest()
                .body(deleteMyCommands)
                .exchange(router)
                .bodyTo(Boolean.class);
    }

    public Flux<BotCommandData> getMyCommands(GetMyCommands getMyCommands) {
        return Routes.GET_MY_COMMANDS.newRequest()
                .body(getMyCommands)
                .exchange(router)
                .bodyTo(BotCommandData[].class)
                .flatMapMany(Flux::fromArray);
    }
}
