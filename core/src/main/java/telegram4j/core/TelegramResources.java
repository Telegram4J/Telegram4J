package telegram4j.core;

import reactor.util.annotation.Nullable;

import java.util.Objects;
import java.util.Optional;

public class TelegramResources {
    private final int appId;
    private final String appHash;
    @Nullable
    private final String botAuthToken;
    private final AuthorizationType authorizationType;

    public TelegramResources(int appId, String appHash, @Nullable String botAuthToken, AuthorizationType authorizationType) {
        this.appId = appId;
        this.appHash = Objects.requireNonNull(appHash, "appHash");
        this.botAuthToken = botAuthToken;
        this.authorizationType = Objects.requireNonNull(authorizationType, "authorizationType");
    }

    public int getAppId() {
        return appId;
    }

    public String getAppHash() {
        return appHash;
    }

    public Optional<String> getBotAuthToken() {
        return Optional.ofNullable(botAuthToken);
    }

    public AuthorizationType getAuthorizationType() {
        return authorizationType;
    }
}
