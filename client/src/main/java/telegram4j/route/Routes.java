package telegram4j.route;

/**
 * Class-collection with all available {@link Route}s.
 */
public final class Routes {

    private Routes() {}

    /** @see <a href="https://core.telegram.org/bots/api#getme">https://core.telegram.org/bots/api#getme</a> */
    public static final Route GET_ME = Route.get("/getMe");

    /** @see <a href="https://core.telegram.org/bots/api#logout">https://core.telegram.org/bots/api#logout</a> */
    public static final Route LOG_OUT = Route.get("/logOut");

    /** @see <a href="https://core.telegram.org/bots/api#close">https://core.telegram.org/bots/api#close</a> */
    public static final Route CLOSE = Route.get("/close");
}
