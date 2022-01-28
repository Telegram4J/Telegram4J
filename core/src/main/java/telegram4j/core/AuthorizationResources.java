package telegram4j.core;

import org.reactivestreams.Publisher;
import reactor.util.annotation.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/** Settings of user and bot auth resources. */
public class AuthorizationResources {
    private final int appId;
    private final String appHash;
    @Nullable
    private final String botAuthToken;
    @Nullable
    private final Function<MTProtoTelegramClient, Publisher<?>> authHandler;
    private final Type type;

    AuthorizationResources(int appId, String appHash, String botAuthToken) {
        this.appId = appId;
        this.appHash = Objects.requireNonNull(appHash, "appHash");
        this.botAuthToken = Objects.requireNonNull(botAuthToken, "botAuthToken");
        this.type = Type.BOT;

        this.authHandler = null;
    }

    AuthorizationResources(int appId, String appHash, Function<MTProtoTelegramClient, Publisher<?>> authHandler) {
        this.appId = appId;
        this.appHash = Objects.requireNonNull(appHash, "appHash");
        this.authHandler = Objects.requireNonNull(authHandler, "authHandler");
        this.type = Type.USER;

        this.botAuthToken = null;
    }

    /**
     * Gets an <a href="https://core.telegram.org/api/obtaining_api_id#obtaining-api-id">api id</a> of application.
     *
     * @return An api id of application.
     */
    public int getAppId() {
        return appId;
    }

    /**
     * Gets an <a href="https://core.telegram.org/api/obtaining_api_id#obtaining-api-id">api hash</a> of application.
     *
     * @return An api hash of application.
     */
    public String getAppHash() {
        return appHash;
    }

    /**
     * Gets a bot auth token.
     *
     * @return A bot auth token.
     */
    public Optional<String> getBotAuthToken() {
        return Optional.ofNullable(botAuthToken);
    }

    /**
     * Gets the authorization handlers that's implements
     * user authorization via phone or qr code.
     *
     * @return The authorization handler.
     */
    public Optional<Function<MTProtoTelegramClient, Publisher<?>>> getAuthHandler() {
        return Optional.ofNullable(authHandler);
    }

    /**
     * Gets the type of authorization.
     *
     * @return The type of authorization.
     */
    public Type getType() {
        return type;
    }

    /** Types of authorization. */
    public enum Type {
        /** A <a href="https://core.telegram.org/bots">bot authorization</a> type. */
        BOT,

        /** A <a href="https://core.telegram.org/api/auth">user authorization</a> type. */
        USER
    }
}
