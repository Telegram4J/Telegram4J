package telegram4j.core;

import reactor.util.annotation.Nullable;

import java.util.Objects;
import java.util.Optional;

public class AuthorizationResources {
    private final int appId;
    private final String appHash;
    @Nullable
    private final String botAuthToken;
    private final Type type;

    AuthorizationResources(int appId, String appHash, @Nullable String botAuthToken, Type type) {
        this.appId = appId;
        this.appHash = Objects.requireNonNull(appHash, "appHash");
        this.botAuthToken = botAuthToken;
        this.type = Objects.requireNonNull(type, "type");
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

    public Type getType() {
        return type;
    }

    public enum Type {
        BOT,
        USER
    }
}
