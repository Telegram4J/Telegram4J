package telegram4j.rest.service;

import reactor.core.publisher.Mono;
import telegram4j.json.UserProfilePhotosData;
import telegram4j.json.request.GetUserProfilePhotos;
import telegram4j.rest.RestRouter;
import telegram4j.rest.route.Routes;

public class UserService extends RestService {

    public UserService(RestRouter router) {
        super(router);
    }

    public Mono<UserProfilePhotosData> getUserProfilePhotos(GetUserProfilePhotos getUserProfilePhotos) {
        return Routes.GET_USER_PROFILE_PHOTOS.newRequest()
                .body(getUserProfilePhotos)
                .exchange(router)
                .bodyTo(UserProfilePhotosData.class);
    }
}
