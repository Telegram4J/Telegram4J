package telegram4j.core;

import telegram4j.rest.RestResources;

import java.util.Objects;

public class TelegramClientBuilder {

    /**Bot secret key, issued by a BotFather(Telegram bot for create bot's accounts) when creating new bot*/
    private String token;
    /**Access to rest content*/
    private RestResources restResources;
    /**Access to events and updates handling*/
    private ClientResources clientResources;

    TelegramClientBuilder() {}

    /**
     * Token setter
     * @param token Bot secret key. Cannot be null
     * @return updated object with defined token
     */
    public TelegramClientBuilder setToken(String token) {
        this.token = Objects.requireNonNull(token, "token");
        return this;
    }

    /**
     * Rest resources setter
     * @param restResources rest resources. Cannot be null
     * @return updated object with defined rest
     */
    public TelegramClientBuilder setRestResources(RestResources restResources) {
        this.restResources = Objects.requireNonNull(restResources, "restResources");
        return this;
    }

    /**
     * Client resources setter
     * @param clientResources client resources. Cannot be null
     * @return
     */
    public TelegramClientBuilder setClientResources(ClientResources clientResources) {
        this.clientResources = Objects.requireNonNull(clientResources, "clientResources");
        return this;
    }

    /**
     * Method for build a bot. make sure that you have set the token(for set use {@link TelegramClientBuilder#setToken(String)}
     * @return {@link TelegramClient} ready to login
     */
    public TelegramClient build() {
        RestResources restResources = getRestResources();
        ClientResources clientResources = getClientResources();
        return new TelegramClient(token, restResources, clientResources);
    }

    /**
     * private method for get REST resources
     * @return if {@link TelegramClientBuilder#restResources} is null return new instance, else return value of {@link TelegramClientBuilder#restResources}
     */
    private RestResources getRestResources() {
        if (restResources != null) {
            return restResources;
        }
        return new RestResources();
    }

    /**
     * private method for get Client resources
     * @return if {@link TelegramClientBuilder#clientResources} is null return new instance, else return value of {@link TelegramClientBuilder#clientResources}
     */
    private ClientResources getClientResources() {
        if (clientResources != null) {
            return clientResources;
        }
        return new ClientResources();
    }
}
