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
    public static final Route SEND_MESSAGE = Route.post("/sendMessage");

    /** @see <a href="https://core.telegram.org/bots/api#forwardmessage">https://core.telegram.org/bots/api#forwardmessage</a> */
    public static final Route FORWARD_MESSAGE = Route.post("/forwardMessage");

    /** @see <a href="https://core.telegram.org/bots/api#copymessage">https://core.telegram.org/bots/api#copymessage</a> */
    public static final Route COPY_MESSAGE = Route.post("/copyMessage");

    /** @see <a href="https://core.telegram.org/bots/api#sendphoto">https://core.telegram.org/bots/api#sendphoto</a> */
    public static final Route SEND_PHOTO = Route.post("/sendPhoto");

    /** @see <a href="https://core.telegram.org/bots/api#sendaudio">https://core.telegram.org/bots/api#sendaudio</a> */
    public static final Route SEND_AUDIO = Route.post("/sendAudio");

    /** @see <a href="https://core.telegram.org/bots/api#senddocument">https://core.telegram.org/bots/api#senddocument</a> */
    public static final Route SEND_DOCUMENT = Route.post("/sendDocument");

    /** @see <a href="https://core.telegram.org/bots/api#sendvideo">https://core.telegram.org/bots/api#sendvideo</a> */
    public static final Route SEND_VIDEO = Route.post("/sendVideo");

    /** @see <a href="https://core.telegram.org/bots/api#sendanimation">https://core.telegram.org/bots/api#sendanimation</a> */
    public static final Route SEND_ANIMATION = Route.post("/sendAnimation");

    /** @see <a href="https://core.telegram.org/bots/api#sendvoice">https://core.telegram.org/bots/api#sendvoice</a> */
    public static final Route SEND_VOICE = Route.post("/sendVoice");

    /** @see <a href="https://core.telegram.org/bots/api#sendvoicenote">https://core.telegram.org/bots/api#sendvoicenote</a> */
    public static final Route SEND_VOICE_NOTE = Route.post("/sendVoiceNote");

    /** @see <a href="https://core.telegram.org/bots/api#sendmediagroup">https://core.telegram.org/bots/api#sendmediagroup</a> */
    public static final Route SEND_MEDIA_GROUP = Route.post("/sendMediaGroup");

    /** @see <a href="https://core.telegram.org/bots/api#location">https://core.telegram.org/bots/api#location</a> */
    public static final Route SEND_LOCATION = Route.post("/sendLocation");

    /** @see <a href="https://core.telegram.org/bots/api#editmessagelivelocation">https://core.telegram.org/bots/api#editmessagelivelocation</a> */
    public static final Route EDIT_MESSAGE_LIVE_LOCATION = Route.post("/editMessageLiveLocation");

    /** @see <a href="https://core.telegram.org/bots/api#stopmessagelivelocation">https://core.telegram.org/bots/api#stopmessagelivelocation</a> */
    public static final Route STOP_MESSAGE_LIVE_LOCATION = Route.get("/stopMessageLiveLocation");

    /** @see <a href="https://core.telegram.org/bots/api#sendvenue">https://core.telegram.org/bots/api#sendvenue</a> */
    public static final Route SEND_VENUE = Route.post("/sendVenue");

    /** @see <a href="https://core.telegram.org/bots/api#sendcontact">https://core.telegram.org/bots/api#sendcontact</a> */
    public static final Route SEND_CONTACT = Route.post("/sendContact");

    /** @see <a href="https://core.telegram.org/bots/api#sendpoll">https://core.telegram.org/bots/api#sendpoll</a> */
    public static final Route SEND_POLL = Route.post("/sendPoll");

    /** @see <a href="https://core.telegram.org/bots/api#senddice">https://core.telegram.org/bots/api#senddice</a> */
    public static final Route SEND_DICE = Route.post("/sendDice");

    /** @see <a href="https://core.telegram.org/bots/api#sendchataction">https://core.telegram.org/bots/api#sendchataction</a> */
    public static final Route SEND_CHAT_ACTION = Route.post("/sendChatAction");
}
