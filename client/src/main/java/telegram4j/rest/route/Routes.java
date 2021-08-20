package telegram4j.rest.route;

/**
 * Class-collection with all available {@link Route}s.
 */
public final class Routes {

    private Routes() {}

    public static final String BASE_URL = "https://api.telegram.org";

    /** @see <a href="https://core.telegram.org/bots/api#getme">https://core.telegram.org/bots/api#getme</a> */
    public static final Route GET_ME = Route.get("/getMe");

    /** @see <a href="https://core.telegram.org/bots/api#logout">https://core.telegram.org/bots/api#logout</a> */
    public static final Route LOG_OUT = Route.get("/logOut");

    /** @see <a href="https://core.telegram.org/bots/api#close">https://core.telegram.org/bots/api#close</a> */
    public static final Route CLOSE = Route.get("/close");

    /** @see <a href="https://core.telegram.org/bots/api#sendMessage">https://core.telegram.org/bots/api#sendMessage</a> */
    public static final Route SEND_MESSAGE = Route.post("/sendMessage");
}
