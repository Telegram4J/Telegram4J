package telegram4j.core.util;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.*;
import telegram4j.core.object.Message;
import telegram4j.core.object.PhotoSize;
import telegram4j.core.object.User;
import telegram4j.core.object.UserStatus;
import telegram4j.core.object.action.MessageAction;
import telegram4j.core.object.action.MessageActionChatCreate;
import telegram4j.core.object.chat.Channel;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.object.chat.GroupChat;
import telegram4j.core.object.chat.PrivateChat;
import telegram4j.core.object.markup.KeyboardButton;
import telegram4j.core.object.markup.ReplyInlineMarkup;
import telegram4j.core.object.markup.ReplyKeyboardForceReply;
import telegram4j.core.object.markup.ReplyKeyboardHide;
import telegram4j.core.object.markup.ReplyKeyboardMarkup;
import telegram4j.core.object.markup.ReplyMarkup;
import telegram4j.core.object.media.MessageMedia;
import telegram4j.core.object.media.MessageMediaPhoto;
import telegram4j.tl.*;
import telegram4j.tl.api.TlObject;
import telegram4j.tl.messages.ChatFull;

import java.util.List;
import java.util.stream.Collectors;

public final class EntityFactory {

    private EntityFactory() {
    }

    public static UserStatus createUserStatus(MTProtoTelegramClient client, telegram4j.tl.UserStatus data) {
        switch (data.identifier()) {
            case UserStatusEmpty.ID:
                return new UserStatus(client, UserStatus.Type.EMPTY);
            case UserStatusLastMonth.ID:
                return new UserStatus(client, UserStatus.Type.LAST_MONTH);
            case UserStatusLastWeek.ID:
                return new UserStatus(client, UserStatus.Type.LAST_WEEK);
            case UserStatusOffline.ID:
                UserStatusOffline userStatusOffline = (UserStatusOffline) data;
                return new UserStatus(client, UserStatus.Type.OFFLINE, null, userStatusOffline.wasOnline());
            case UserStatusOnline.ID:
                UserStatusOnline userStatusOnline = (UserStatusOnline) data;
                return new UserStatus(client, UserStatus.Type.ONLINE, userStatusOnline.expires(), null);
            case UserStatusRecently.ID:
                return new UserStatus(client, UserStatus.Type.RECENTLY);
            default: throw new IllegalArgumentException("Unknown user status type: " + data);
        }
    }

    public static Message createMessage(MTProtoTelegramClient client, telegram4j.tl.Message data, Id resolvedChatId) {
        switch (data.identifier()) {
            case BaseMessage.ID: return new Message(client, (BaseMessage) data, resolvedChatId);
            case MessageService.ID: return new Message(client, (MessageService) data, resolvedChatId);
            default: throw new IllegalArgumentException("Unknown message type: " + data);
        }
    }

    public static Chat createChat(MTProtoTelegramClient client, TlObject possibleChat) {
        switch (possibleChat.identifier()) {
            case UserFull.ID:
                UserFull userFull = (UserFull) possibleChat;

                User mappedFullUser = new User(client, userFull);

                return new PrivateChat(client, mappedFullUser);
            case BaseUser.ID:
                BaseUser baseUser = (BaseUser) possibleChat;

                User mappedMinUser = new User(client, baseUser);

                return new PrivateChat(client, mappedMinUser);
            case BaseChat.ID:
                BaseChat baseChat = (BaseChat) possibleChat;

                return new GroupChat(client, baseChat);
            case telegram4j.tl.Channel.ID:
                telegram4j.tl.Channel channel = (telegram4j.tl.Channel) possibleChat;

                return new Channel(client, channel);
            case ChatFull.ID:
                ChatFull chatFull = (ChatFull) possibleChat;

                List<Chat> chats = chatFull.chats().stream()
                        .map(d -> createChat(client, d))
                        .collect(Collectors.toList());

                List<User> users = chatFull.users().stream()
                        .map(d -> new User(client, (BaseUser) d))
                        .collect(Collectors.toList());

                telegram4j.tl.Chat minData = chatFull.chats().stream()
                        .filter(c -> c.id() == chatFull.fullChat().id())
                        .findFirst()
                        .orElseThrow(IllegalStateException::new);

                if (chatFull.fullChat() instanceof ChannelFull) {
                    return new Channel(client, (ChannelFull) chatFull.fullChat(),
                            (telegram4j.tl.Channel) minData, chats, users);
                }

                return new GroupChat(client, (telegram4j.tl.BaseChatFull) chatFull.fullChat(),
                        (telegram4j.tl.BaseChat) minData, chats, users);
            default:
                throw new IllegalArgumentException("Unknown chat type: " + possibleChat);
        }
    }

    public static MessageAction createMessageAction(MTProtoTelegramClient client, telegram4j.tl.MessageAction data) {
        switch (data.identifier()) {
            case telegram4j.tl.MessageActionChatCreate.ID:
                return new MessageActionChatCreate(client, (telegram4j.tl.MessageActionChatCreate) data);

            default:
                throw new IllegalArgumentException("Unknown message action type: " + data);
        }
    }

    public static MessageMedia createMessageMedia(MTProtoTelegramClient client, telegram4j.tl.MessageMedia data) {
        switch (data.identifier()) {
            case telegram4j.tl.MessageMediaPhoto.ID:
                return new MessageMediaPhoto(client, (telegram4j.tl.MessageMediaPhoto) data);

            default:
                throw new IllegalArgumentException("Unknown message action type: " + data);
        }
    }

    public static ReplyMarkup createReplyMarkup(MTProtoTelegramClient client, telegram4j.tl.ReplyMarkup data) {
        switch (data.identifier()) {
            case telegram4j.tl.ReplyInlineMarkup.ID:
                return new ReplyInlineMarkup(client, (telegram4j.tl.ReplyInlineMarkup) data);

            case telegram4j.tl.ReplyKeyboardHide.ID:
                return new ReplyKeyboardHide(client, (telegram4j.tl.ReplyKeyboardHide) data);

            case telegram4j.tl.ReplyKeyboardMarkup.ID:
                return new ReplyKeyboardMarkup(client, (telegram4j.tl.ReplyKeyboardMarkup) data);

            case telegram4j.tl.ReplyKeyboardForceReply.ID:
                return new ReplyKeyboardForceReply(client, (telegram4j.tl.ReplyKeyboardForceReply) data);

            default:
                throw new IllegalArgumentException("Unknown reply markup type: " + data);
        }
    }

    public static KeyboardButton createKeyboardButton(MTProtoTelegramClient client, telegram4j.tl.KeyboardButton data) {
        switch (data.identifier()) {
            case BaseKeyboardButton.ID:
                return new KeyboardButton(client, KeyboardButton.Type.DEFAULT, data.text());
            case KeyboardButtonBuy.ID:
                return new KeyboardButton(client, KeyboardButton.Type.BUY, data.text());

            case KeyboardButtonCallback.ID:
                KeyboardButtonCallback keyboardButtonCallback = (KeyboardButtonCallback) data;
                return new KeyboardButton(client, KeyboardButton.Type.CALLBACK, keyboardButtonCallback.text(),
                        keyboardButtonCallback.data(), keyboardButtonCallback.requiresPassword(),
                        null, null, null, null, null, null);

            case KeyboardButtonGame.ID:
                return new KeyboardButton(client, KeyboardButton.Type.GAME, data.text());

            case KeyboardButtonRequestGeoLocation.ID:
                return new KeyboardButton(client, KeyboardButton.Type.REQUEST_GEO_LOCATION, data.text());

            case KeyboardButtonRequestPhone.ID:
                return new KeyboardButton(client, KeyboardButton.Type.REQUEST_PHONE, data.text());

            case KeyboardButtonRequestPoll.ID:
                KeyboardButtonRequestPoll keyboardButtonRequestPoll = (KeyboardButtonRequestPoll) data;
                return new KeyboardButton(client, KeyboardButton.Type.REQUEST_POLL, data.text(),
                        null, null, keyboardButtonRequestPoll.quiz(),
                        null, null, null, null, null);

            case KeyboardButtonSwitchInline.ID:
                KeyboardButtonSwitchInline keyboardButtonSwitchInline = (KeyboardButtonSwitchInline) data;
                return new KeyboardButton(client, KeyboardButton.Type.SWITCH_INLINE, data.text(),
                        null, null, null, keyboardButtonSwitchInline.query(),
                        null, null, null, null);

            case KeyboardButtonUrl.ID:
                KeyboardButtonUrl keyboardButtonUrl = (KeyboardButtonUrl) data;
                return new KeyboardButton(client, KeyboardButton.Type.URL, data.text(),
                        null, null, null, null, keyboardButtonUrl.url(), null, null, null);

            case KeyboardButtonUrlAuth.ID:
                KeyboardButtonUrlAuth KeyboardButtonUrlAuth = (KeyboardButtonUrlAuth) data;
                return new KeyboardButton(client, KeyboardButton.Type.URL_AUTH, data.text(),
                        null, null, null, null, KeyboardButtonUrlAuth.url(), KeyboardButtonUrlAuth.fwdText(),
                        KeyboardButtonUrlAuth.buttonId(), null);

            case InputKeyboardButtonUrlAuth.ID:
                InputKeyboardButtonUrlAuth inputKeyboardButtonUrlAuth = (InputKeyboardButtonUrlAuth) data;
                return new KeyboardButton(client, KeyboardButton.Type.INPUT_URH_AUTH, data.text(),
                        null, null, null, null, inputKeyboardButtonUrlAuth.url(),
                        inputKeyboardButtonUrlAuth.fwdText(), null, null);

            default:
                throw new IllegalArgumentException("Unknown keyboard button type: " + data);
        }
    }

    public static PhotoSize createPhotoSize(MTProtoTelegramClient client, telegram4j.tl.PhotoSize data) {
        switch (data.identifier()) {
            case BasePhotoSize.ID:
            case PhotoCachedSize.ID:
            case PhotoPathSize.ID:
            case PhotoSizeEmpty.ID:
            case PhotoSizeProgressive.ID:
            case PhotoStrippedSize.ID:
                // TODO
            default:
                throw new IllegalArgumentException("Unknown photo size type: " + data);
        }
    }
}
