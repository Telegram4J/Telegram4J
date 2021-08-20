package telegram4j.rest.route;

/**
 * Class-collection with all available {@link Route}s.
 */
public final class Routes {

    private Routes() {}

    /** The base URL for Telegram API endpoints. */
    public static final String BASE_URL = "https://api.telegram.org";

    /** @see <a href="https://core.telegram.org/bots/api#getupdates">https://core.telegram.org/bots/api#getupdates</a> */
    public static final Route GET_UPDATES = Route.get("/getUpdates");

    /** @see <a href="https://core.telegram.org/bots/api#getme">https://core.telegram.org/bots/api#getme</a> */
    public static final Route GET_ME = Route.get("/getMe");

    /** @see <a href="https://core.telegram.org/bots/api#logout">https://core.telegram.org/bots/api#logout</a> */
    public static final Route LOG_OUT = Route.get("/logOut");

    /** @see <a href="https://core.telegram.org/bots/api#close">https://core.telegram.org/bots/api#close</a> */
    public static final Route CLOSE = Route.get("/close");

    /** @see <a href="https://core.telegram.org/bots/api#sendmessage">https://core.telegram.org/bots/api#sendmessage</a> */
    public static final Route SEND_MESSAGE = Route.get("/sendMessage");

    /** @see <a href="https://core.telegram.org/bots/api#forwardmessage">https://core.telegram.org/bots/api#forwardmessage</a> */
    public static final Route FORWARD_MESSAGE = Route.get("/forwardMessage");

    /** @see <a href="https://core.telegram.org/bots/api#copymessage">https://core.telegram.org/bots/api#copymessage</a> */
    public static final Route COPY_MESSAGE = Route.get("/copyMessage");

    /** @see <a href="https://core.telegram.org/bots/api#sendphoto">https://core.telegram.org/bots/api#sendphoto</a> */
    public static final Route SEND_PHOTO = Route.get("/sendPhoto");

    /** @see <a href="https://core.telegram.org/bots/api#sendaudio">https://core.telegram.org/bots/api#sendaudio</a> */
    public static final Route SEND_AUDIO = Route.get("/sendAudio");

    /** @see <a href="https://core.telegram.org/bots/api#senddocument">https://core.telegram.org/bots/api#senddocument</a> */
    public static final Route SEND_DOCUMENT = Route.get("/sendDocument");

    /** @see <a href="https://core.telegram.org/bots/api#sendvideo">https://core.telegram.org/bots/api#sendvideo</a> */
    public static final Route SEND_VIDEO = Route.get("/sendVideo");

    /** @see <a href="https://core.telegram.org/bots/api#sendanimation">https://core.telegram.org/bots/api#sendanimation</a> */
    public static final Route SEND_ANIMATION = Route.get("/sendAnimation");
}
