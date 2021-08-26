package telegram4j.core;

import reactor.core.publisher.Mono;
import telegram4j.rest.RestResources;


/**
 * Base class for access to library functions
 */
public final class TelegramClient {
    /**Bot secret key, issued by a BotFather(Telegram bot for create bot's accounts) when creating new bot*/
    private final String token;
    /**Access to rest content*/
    private final RestResources restResources;
    /**Access to events and updates handling*/
    private final ClientResources clientResources;

    /**
     * Constructor for this class. Not recommended for use. For create class instance use TelegramClientBuilder
     * @see TelegramClientBuilder
     * @param token access token
     * @param restResources REST instance
     * @param clientResources Events and Updates handling instance
     */
    TelegramClient(String token, RestResources restResources, ClientResources clientResources) {
        this.token = token;
        this.restResources = restResources;
        this.clientResources = clientResources;
    }

    /**
     * Fast create of TelegramClient instance
     * @param token bot access token
     * @return ready to login class instance
     *
     * @see TelegramClient#login()
     */
    public static TelegramClient create(String token) {
        return builder().setToken(token).build();
    }

    /**
     * @see TelegramClientBuilder
     * @return new {@link TelegramClientBuilder}
     */
    public static TelegramClientBuilder builder() {
        return new TelegramClientBuilder();
    }

    /**
     * Method for auth bot
     * @return nothing or error for handle
     */
    public Mono<Void> login() {
        // Currently nothing
        return Mono.empty();
    }
}
