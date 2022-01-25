package telegram4j.core;

import org.reactivestreams.Publisher;
import reactor.util.annotation.Nullable;
import telegram4j.tl.CodeSettings;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public class AuthorizationResources {
    private final int appId;
    private final String appHash;
    @Nullable
    private final String botAuthToken;
    @Nullable
    private final String phoneNumber;
    @Nullable
    private final CodeSettings settings;
    @Nullable
    private final Function<MTProtoTelegramClient, Publisher<?>> authHandler;
    private final Type type;

    AuthorizationResources(int appId, String appHash, String botAuthToken) {
        this.appId = appId;
        this.appHash = Objects.requireNonNull(appHash, "appHash");
        this.botAuthToken = Objects.requireNonNull(botAuthToken, "botAuthToken");
        this.type = Type.BOT;

        this.phoneNumber = null;
        this.settings = null;
        this.authHandler = null;
    }

    AuthorizationResources(int appId, String appHash, String phoneNumber, CodeSettings settings,
                           Function<MTProtoTelegramClient, Publisher<?>> authHandler) {
        this.appId = appId;
        this.appHash = Objects.requireNonNull(appHash, "appHash");
        this.phoneNumber = Objects.requireNonNull(phoneNumber, "phoneNumber");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.authHandler = Objects.requireNonNull(authHandler, "authHandler");
        this.type = Type.USER;

        this.botAuthToken = null;
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

    public Optional<String> getPhoneNumber() {
        return Optional.ofNullable(phoneNumber);
    }

    public Optional<CodeSettings> getSettings() {
        return Optional.ofNullable(settings);
    }

    public Optional<Function<MTProtoTelegramClient, Publisher<?>>> getAuthHandler() {
        return Optional.ofNullable(authHandler);
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        BOT,
        USER
    }
}
