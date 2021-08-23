package telegram4j.rest.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.json.UpdateData;
import telegram4j.json.UserData;
import telegram4j.json.request.GetUpdates;
import telegram4j.rest.RestRouter;
import telegram4j.rest.route.Routes;

public class ApplicationService extends RestService {

    public ApplicationService(RestRouter router) {
        super(router);
    }

    public Mono<UserData> getMe() {
        return Routes.GET_ME.newRequest()
                .exchange(router)
                .bodyTo(UserData.class);
    }

    public Flux<UpdateData> getUpdates(GetUpdates getUpdates) {
        return Routes.GET_UPDATES.newRequest()
                .body(getUpdates)
                .exchange(router)
                .bodyTo(UpdateData[].class)
                .flatMapMany(Flux::fromArray);
    }

    public Mono<Boolean> logOut() {
        return Routes.LOG_OUT.newRequest()
                .exchange(router)
                .bodyTo(Boolean.class);
    }


    public Mono<Boolean> close() {
        return Routes.CLOSE.newRequest()
                .exchange(router)
                .bodyTo(Boolean.class);
    }
}
