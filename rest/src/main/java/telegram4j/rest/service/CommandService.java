package telegram4j.rest.service;

import reactor.core.publisher.Mono;
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
}
