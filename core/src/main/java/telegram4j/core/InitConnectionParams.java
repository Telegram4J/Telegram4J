/*
 * Copyright 2023 Telegram4J
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package telegram4j.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import reactor.util.annotation.Nullable;
import telegram4j.tl.InputClientProxy;

import java.lang.module.ModuleDescriptor;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;

/**
 * Striped down version of {@link telegram4j.tl.request.InitConnection} settings,
 * used in connection initialization.
 */
public final class InitConnectionParams {
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

    /**
     * Computes system-dependent init connection params with
     * system version in format {@code Linux 5.10.93-1-MANJARO amd64} and
     * device model {@code Telegram4J} with lib version from module info and
     * system languages and parameters with extracted from TZ offset seconds.
     *
     * @return The new default computed init connection parameters.
     */
    public static InitConnectionParams getDefault() {
        String appVersion = Optional.ofNullable(InitConnectionParams.class.getModule().getDescriptor())
                .flatMap(ModuleDescriptor::rawVersion)
                .orElse("0.1.0-SNAPSHOT");
        String deviceModel = "Telegram4J";
        String systemVersion = String.join(" ", System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("os.arch"));

        String langCode = Locale.getDefault().getLanguage().toLowerCase(Locale.ROOT);
        JsonNode node = JsonNodeFactory.instance.objectNode()
                .put("tz_offset", TimeZone.getDefault().getRawOffset() / 1000d);

        return new InitConnectionParams(appVersion, deviceModel, langCode,
                "", systemVersion, langCode, null, node);
    }

    /**
     * Creates {@code InitConnectionParams} from specified settings.
     *
     * @see #getParams()
     * @param appVersion The application model.
     * @param deviceModel The device model.
     * @param langCode The client language code in ISO 639-1 format.
     * @param langPack The name of official language pack.
     * @param systemVersion The device's OS version.
     * @param systemLangCode The device's OS language code in ISO 639-1 format.
     * @param proxy The MTProto proxy settings.
     * @param params The {@link ObjectNode} with {@code tz_offset} in seconds, if present.
     */
    public InitConnectionParams(String appVersion, String deviceModel, String langCode,
                                String langPack, String systemVersion, String systemLangCode,
                                @Nullable InputClientProxy proxy, @Nullable JsonNode params) {
        this.appVersion = Objects.requireNonNull(appVersion);
        this.deviceModel = Objects.requireNonNull(deviceModel);
        this.langCode = Objects.requireNonNull(langCode);
        this.langPack = Objects.requireNonNull(langPack);
        this.systemVersion = Objects.requireNonNull(systemVersion);
        this.systemLangCode = Objects.requireNonNull(systemLangCode);
        this.proxy = proxy;
        this.params = params;
    }

    /**
     * Gets the application version, e.g. {@code "0.1.0"}.
     *
     * @return The application version.
     */
    public String getAppVersion() {
        return appVersion;
    }

    /**
     * Gets the device model, e.g. phone/pc model.
     *
     * @return The device model.
     */
    public String getDeviceModel() {
        return deviceModel;
    }

    /**
     * Gets the client OS ISO 639-1 language code.
     *
     * <p>Example to get ISO 639-1 lang codes.
     * <pre>{@code
     * String langCode = Locale.getDefault().getLanguage().toLowerCase(Locale.ROOT).
     * }</pre>
     *
     * @return The ISO 639-1 lang code of the client.
     */
    public String getLangCode() {
        return langCode;
    }

    /**
     * Gets <a href="https://translations.telegram.org/">language pack</a> of client,
     * currently, lang packs available only for official clients,
     * but you can safely use their language packs.
     *
     * @return The language pack name, if present, otherwise {@code ""}.
     */
    public String getLangPack() {
        return langPack;
    }

    /**
     * Gets system version.
     *
     * @return The system version.
     */
    public String getSystemVersion() {
        return systemVersion;
    }

    /**
     * Gets device's OS ISO 639-1 language code.
     *
     * <p>Example to get ISO 639-1 lang codes.
     * <pre>
     * String langCode = Locale.getDefault().getLanguage().toLowerCase(Locale.ROOT).
     * </pre>
     *
     * @return The ISO 639-1 lang code of the device's OS language.
     */
    public String getSystemLangCode() {
        return systemLangCode;
    }

    /**
     * Gets MTProto proxy parameters, if present.
     *
     * @return The MTProto proxy parameters, if present.
     */
    public Optional<InputClientProxy> getProxy() {
        return Optional.ofNullable(proxy);
    }

    /**
     * Gets additional json options.
     * For now, only {@code tz_offset} field is supported,
     * for specifying timezone offset in seconds.
     *
     * <p> Example of getting time zone offset
     * <pre>{@code
     * JsonNode node = JsonNodeFactory.instance.objectNode()
     *         .put("tz_offset", java.util.TimeZone.getDefault()
     *                 .getRawOffset() / 1000d);
     * }</pre>
     *
     * @return The json object node with additional {@literal double} property {@code tz_offset}, if present
     */
    public Optional<JsonNode> getParams() {
        return Optional.ofNullable(params);
    }

    @Override
    public String toString() {
        return "InitConnectionParams{" +
                "appVersion='" + appVersion + '\'' +
                ", deviceModel='" + deviceModel + '\'' +
                ", langCode='" + langCode + '\'' +
                ", langPack='" + langPack + '\'' +
                ", systemVersion='" + systemVersion + '\'' +
                ", systemLangCode='" + systemLangCode + '\'' +
                ", proxy=" + proxy +
                ", params=" + params +
                '}';
    }
}
