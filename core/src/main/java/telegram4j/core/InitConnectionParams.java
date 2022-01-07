package telegram4j.core;

import com.fasterxml.jackson.databind.JsonNode;
import reactor.util.annotation.Nullable;
import telegram4j.tl.InputClientProxy;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public class InitConnectionParams {
    private final String appVersion;
    private final String deviceModel;
    private final String langCode;
    private final String langPack;
    private final String systemVersion;
    private final String systemLangCode;
    @Nullable
    private final InputClientProxy proxy;
    @Nullable
    private final JsonNode params;

    public static InitConnectionParams getDefault() {
        String appVersion = "0.1.0";
        String deviceModel = "Telegram4J";
        String systemVersion = String.join(" ", System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("os.arch"));

        String langCode = Locale.getDefault().getLanguage().toLowerCase(Locale.ROOT);

        return new InitConnectionParams(appVersion, deviceModel, langCode,
                "", systemVersion, langCode, null, null);
    }

    public InitConnectionParams(String appVersion, String deviceModel, String langCode,
                                String langPack, String systemVersion, String systemLangCode,
                                @Nullable InputClientProxy proxy, @Nullable JsonNode params) {
        this.appVersion = Objects.requireNonNull(appVersion, "appVersion");
        this.deviceModel = Objects.requireNonNull(deviceModel, "deviceModel");
        this.langCode = Objects.requireNonNull(langCode, "langCode");
        this.langPack = Objects.requireNonNull(langPack, "langPack");
        this.systemVersion = Objects.requireNonNull(systemVersion, "systemVersion");
        this.systemLangCode = Objects.requireNonNull(systemLangCode, "systemLangCode");
        this.proxy = proxy;
        this.params = params;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public String getLangCode() {
        return langCode;
    }

    public String getLangPack() {
        return langPack;
    }

    public String getSystemVersion() {
        return systemVersion;
    }

    public String getSystemLangCode() {
        return systemLangCode;
    }

    public Optional<InputClientProxy> getProxy() {
        return Optional.ofNullable(proxy);
    }

    public Optional<JsonNode> getParams() {
        return Optional.ofNullable(params);
    }
}
