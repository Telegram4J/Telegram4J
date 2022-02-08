package telegram4j.core;

import org.reactivestreams.Publisher;
import reactor.util.annotation.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/** Settings of user and bot auth resources. */
public class AuthorizationResources {
    private final int apiId;
    private final String apiHash;
    @Nullable
    private final String botAuthToken;
    @Nullable
    private final Function<MTProtoTelegramClient, Publisher<?>> authHandler;
    private final Type type;

    AuthorizationResources(int apiId, String apiHash, String botAuthToken) {
        this.apiId = apiId;
        this.apiHash = Objects.requireNonNull(apiHash, "appHash");
        this.botAuthToken = Objects.requireNonNull(botAuthToken, "botAuthToken");
        this.type = Type.BOT;

        this.authHandler = null;
    }

    AuthorizationResources(int apiId, String apiHash, Function<MTProtoTelegramClient, Publisher<?>> authHandler) {
        this.apiId = apiId;
        this.apiHash = Objects.requireNonNull(apiHash, "appHash");
        this.authHandler = Objects.requireNonNull(authHandler, "authHandler");
        this.type = Type.USER;

        this.botAuthToken = null;
    }

    /**
     * Gets an <a href="https://core.telegram.org/api/obtaining_api_id#obtaining-api-id">api id</a> of application.
     *
     * @return An api id of application.
     */
    public int getApiId() {
        return apiId;
    }

    /**
     * Gets an <a href="https://core.telegram.org/api/obtaining_api_id#obtaining-api-id">api hash</a> of application.
     *
     * @return An api hash of application.
     */
    public String getApiHash() {
        return apiHash;
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
