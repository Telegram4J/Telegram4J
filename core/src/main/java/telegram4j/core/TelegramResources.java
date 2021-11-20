package telegram4j.core;

import java.util.Objects;

public class TelegramResources {
    private final int appId;
    private final String appHash;
    private final String botAuthToken;

    public TelegramResources(int appId, String appHash, String botAuthToken) {
        this.appId = appId;
        this.appHash = Objects.requireNonNull(appHash, "appHash");
        this.botAuthToken = Objects.requireNonNull(botAuthToken, "botAuthToken");
    }

    public int getAppId() {
        return appId;
    }

    public String getAppHash() {
        return appHash;
    }

    public String getBotAuthToken() {
        return botAuthToken;
    }
}
