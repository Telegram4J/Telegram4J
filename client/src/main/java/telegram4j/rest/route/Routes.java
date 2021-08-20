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

    /** @see <a href="https://core.telegram.org/bots/api#getuserprofilephotos">https://core.telegram.org/bots/api#getuserprofilephotos</a> */
    public static final Route GET_USER_PROFILE_PHOTOS = Route.get("/getUserProfilePhotos");

    /** @see <a href="https://core.telegram.org/bots/api#getfile">https://core.telegram.org/bots/api#getfile</a> */
    public static final Route GET_FILE = Route.get("/getFile");

    /** @see <a href="https://core.telegram.org/bots/api#banchatmember">https://core.telegram.org/bots/api#banchatmember</a> */
    public static final Route BAN_CHAT_MEMBER = Route.post("/banChatMember");

    /** @see <a href="https://core.telegram.org/bots/api#unbanchatmember">https://core.telegram.org/bots/api#unbanchatmember</a> */
    public static final Route UNBAN_CHAT_MEMBER = Route.post("/unbanChatMember");

    /** @see <a href="https://core.telegram.org/bots/api#restrictchatmember">https://core.telegram.org/bots/api#restrictchatmember</a> */
    public static final Route RESTRICT_CHAT_MEMBER = Route.post("/restrictChatMember");

    /** @see <a href="https://core.telegram.org/bots/api#promotechatmember">https://core.telegram.org/bots/api#promotechatmember</a> */
    public static final Route PROMOTE_CHAT_MEMBER = Route.post("/promoteChatMember");

    /** @see <a href="https://core.telegram.org/bots/api#setchatadministratorcustomtitle">https://core.telegram.org/bots/api#setchatadministratorcustomtitle</a> */
    public static final Route SET_CHAT_ADMINISTRATOR_CUSTOM_TITLE = Route.post("/setChatAdministratorCustomTitle");

    /** @see <a href="https://core.telegram.org/bots/api#setchatpermissions">https://core.telegram.org/bots/api#setchatpermissions</a> */
    public static final Route SET_CHAT_PERMISSIONS = Route.post("/setChatPermissions");

    /** @see <a href="https://core.telegram.org/bots/api#exportchatinvitelink">https://core.telegram.org/bots/api#exportchatinvitelink</a> */
    public static final Route EXPORT_CHAT_INVITE_LINK = Route.post("/exportChatInviteLink");

    /** @see <a href="https://core.telegram.org/bots/api#createchatinvitelink">https://core.telegram.org/bots/api#createchatinvitelink</a> */
    public static final Route CREATE_CHAT_INVITE_LINK = Route.post("/createChatInviteLink");

    /** @see <a href="https://core.telegram.org/bots/api#editchatinvitelink">https://core.telegram.org/bots/api#editchatinvitelink</a> */
    public static final Route EDIT_CHAT_INVITE_LINK = Route.post("/editChatInviteLink");

    /** @see <a href="https://core.telegram.org/bots/api#revokechatinvitelink">https://core.telegram.org/bots/api#revokechatinvitelink</a> */
    public static final Route REVOKE_CHAT_INVITE_LINK = Route.post("/revokeChatInviteLink");

    /** @see <a href="https://core.telegram.org/bots/api#setchatphoto">https://core.telegram.org/bots/api#setchatphoto</a> */
    public static final Route SET_CHAT_PHOTO = Route.post("/setChatPhoto");

    /** @see <a href="https://core.telegram.org/bots/api#deletechatphoto">https://core.telegram.org/bots/api#deletechatphoto</a> */
    public static final Route DELETE_CHAT_PHOTO = Route.post("/deleteChatPhoto");

    /** @see <a href="https://core.telegram.org/bots/api#setchattitle">https://core.telegram.org/bots/api#setchattitle</a> */
    public static final Route SET_CHAT_TITLE = Route.post("/setChatTitle");

    /** @see <a href="https://core.telegram.org/bots/api#setchatdescription">https://core.telegram.org/bots/api#setchatdescription</a> */
    public static final Route SET_CHAT_DESCRIPTION = Route.post("/setChatDescription");

    /** @see <a href="https://core.telegram.org/bots/api#pinchatmessage">https://core.telegram.org/bots/api#pinchatmessage</a> */
    public static final Route PIN_CHAT_MESSAGE = Route.post("/pinChatMessage");

    /** @see <a href="https://core.telegram.org/bots/api#unpinchatmessage">https://core.telegram.org/bots/api#unpinchatmessage</a> */
    public static final Route UNPIN_CHAT_MESSAGE = Route.post("/unpinChatMessage");

    /** @see <a href="https://core.telegram.org/bots/api#unpinallchatmessages">https://core.telegram.org/bots/api#unpinallchatmessages</a> */
    public static final Route UNPIN_ALL_CHAT_MESSAGES = Route.post("/unpinAllChatMessages");

    /** @see <a href="https://core.telegram.org/bots/api#leavechat">https://core.telegram.org/bots/api#leavechat</a> */
    public static final Route LEAVE_CHAT = Route.post("/leaveChat");

    /** @see <a href="https://core.telegram.org/bots/api#getchat">https://core.telegram.org/bots/api#getchat</a> */
    public static final Route GET_CHAT = Route.get("/getChat");

    /** @see <a href="https://core.telegram.org/bots/api#getchatadministrators">https://core.telegram.org/bots/api#getchatadministrators</a> */
    public static final Route GET_CHAT_ADMINISTRATORS = Route.get("/getChatAdministrators");

    /** @see <a href="https://core.telegram.org/bots/api#getchatmembercount">https://core.telegram.org/bots/api#getchatmembercount</a> */
    public static final Route GET_CHAT_MEMBER_COUNT = Route.get("/getChatMemberCount");

    /** @see <a href="https://core.telegram.org/bots/api#getchatmember">https://core.telegram.org/bots/api#getchatmember</a> */
    public static final Route GET_CHAT_MEMBER = Route.get("/getChatMember");

    /** @see <a href="https://core.telegram.org/bots/api#setchatstickerset">https://core.telegram.org/bots/api#setchatstickerset</a> */
    public static final Route SET_CHAT_STICKER_SET = Route.post("/setChatStickerSet");

    /** @see <a href="https://core.telegram.org/bots/api#deletechatstickerset">https://core.telegram.org/bots/api#deletechatstickerset</a> */
    public static final Route DELETE_CHAT_STICKER_SET = Route.post("/deleteChatStickerSet");

    /** @see <a href="https://core.telegram.org/bots/api#answercallbackquery">https://core.telegram.org/bots/api#answercallbackquery</a> */
    public static final Route ANSWER_CALLBACK_QUERY = Route.post("/answerCallbackQuery");

    /** @see <a href="https://core.telegram.org/bots/api#setmycommands">https://core.telegram.org/bots/api#setmycommands</a> */
    public static final Route SET_MY_COMMANDS = Route.post("/setMyCommands");

    /** @see <a href="https://core.telegram.org/bots/api#deletemycommands">https://core.telegram.org/bots/api#deletemycommands</a> */
    public static final Route DELETE_MY_COMMANDS = Route.post("/deleteMyCommands");

    /** @see <a href="https://core.telegram.org/bots/api#getmycommands">https://core.telegram.org/bots/api#getmycommands</a> */
    public static final Route GET_MY_COMMANDS = Route.get("/getMyCommands");
}
