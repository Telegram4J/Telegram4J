package telegram4j.core.util;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Message;
import telegram4j.core.object.User;
import telegram4j.core.object.UserStatus;
import telegram4j.core.object.*;
import telegram4j.core.object.action.MessageAction;
import telegram4j.core.object.action.MessageActionChatCreate;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.object.chat.*;
import telegram4j.core.object.markup.ReplyInlineMarkup;
import telegram4j.core.object.markup.ReplyKeyboardForceReply;
import telegram4j.core.object.markup.ReplyKeyboardHide;
import telegram4j.core.object.markup.ReplyKeyboardMarkup;
import telegram4j.core.object.markup.ReplyMarkup;
import telegram4j.core.object.media.DocumentAttribute;
import telegram4j.core.object.media.DocumentAttributeAudio;
import telegram4j.core.object.media.DocumentAttributeFilename;
import telegram4j.core.object.media.DocumentAttributeImageSize;
import telegram4j.core.object.media.DocumentAttributeSticker;
import telegram4j.core.object.media.DocumentAttributeVideo;
import telegram4j.core.object.media.MessageMedia;
import telegram4j.core.object.media.MessageMediaContact;
import telegram4j.core.object.media.MessageMediaDice;
import telegram4j.core.object.media.MessageMediaDocument;
import telegram4j.core.object.media.MessageMediaGame;
import telegram4j.core.object.media.MessageMediaGeo;
import telegram4j.core.object.media.MessageMediaGeoLive;
import telegram4j.core.object.media.MessageMediaInvoice;
import telegram4j.core.object.media.MessageMediaPhoto;
import telegram4j.core.object.media.MessageMediaPoll;
import telegram4j.core.object.media.MessageMediaVenue;
import telegram4j.core.object.media.MessageMediaWebPage;
import telegram4j.core.object.media.PhotoCachedSize;
import telegram4j.core.object.media.PhotoPathSize;
import telegram4j.core.object.media.PhotoSize;
import telegram4j.core.object.media.PhotoSizeProgressive;
import telegram4j.core.object.media.PhotoStrippedSize;
import telegram4j.core.object.media.*;
import telegram4j.tl.BaseChat;
import telegram4j.tl.Channel;
import telegram4j.tl.*;
import telegram4j.tl.api.TlObject;
import telegram4j.tl.contacts.ResolvedPeer;
import telegram4j.tl.messages.ChatFull;
import telegram4j.tl.users.UserFull;

import java.time.Instant;

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
                Instant wasOnlineTimestamp = Instant.ofEpochSecond(userStatusOffline.wasOnline());
                return new UserStatus(client, UserStatus.Type.OFFLINE, null, wasOnlineTimestamp);
            case UserStatusOnline.ID:
                UserStatusOnline userStatusOnline = (UserStatusOnline) data;
                Instant expiresTimestamp = Instant.ofEpochSecond(userStatusOnline.expires());
                return new UserStatus(client, UserStatus.Type.ONLINE, expiresTimestamp, null);
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
            case UserFull.ID: {
                UserFull userFull = (UserFull) possibleChat;

                var minData = userFull.users().stream()
                        .filter(u -> u.identifier() == BaseUser.ID &&
                                u.id() == userFull.fullUser().id())
                        .map(u -> (BaseUser) u)
                        .findFirst()
                        .orElseThrow();

                User mappedFullUser = new User(client, userFull.fullUser(), minData);

                return new PrivateChat(client, mappedFullUser);
            }
            case BaseUser.ID:
                BaseUser baseUser = (BaseUser) possibleChat;

                User mappedMinUser = new User(client, baseUser);

                return new PrivateChat(client, mappedMinUser);
            case BaseChat.ID:
                BaseChat baseChat = (BaseChat) possibleChat;

                return new GroupChat(client, baseChat);
            case telegram4j.tl.Channel.ID:
                telegram4j.tl.Channel channel = (telegram4j.tl.Channel) possibleChat;

                if (channel.megagroup()) {
                    return new SupergroupChat(client, channel);
                }

                return new BroadcastChannel(client, channel);
            case ChatFull.ID:
                ChatFull chatFull = (ChatFull) possibleChat;
                // TODO: I haven't been able to figure out what chatFull.users() is for yet, so I'm just ignoring it

                var minData = chatFull.chats().stream()
                        .filter(c -> (c.identifier() == BaseChat.ID || c.identifier() == Channel.ID) &&
                                c.id() == chatFull.fullChat().id())
                        .findFirst()
                        .orElseThrow();

                if (chatFull.fullChat().identifier() == ChannelFull.ID) {
                    ChannelFull channelFull = (ChannelFull) chatFull.fullChat();
                    var channelMin = (telegram4j.tl.Channel) minData;

                    if (channelMin.megagroup()) {
                        return new SupergroupChat(client, channelFull, channelMin);
                    }

                    return new BroadcastChannel(client, channelFull, channelMin);
                }

                return new GroupChat(client, (telegram4j.tl.BaseChatFull) chatFull.fullChat(), (telegram4j.tl.BaseChat) minData);
            default:
                throw new IllegalArgumentException("Unknown chat type: " + possibleChat);
        }
    }

    public static User createUser(MTProtoTelegramClient client, TlObject possibleUser) {
        switch (possibleUser.identifier()) {
            case UserFull.ID: {
                UserFull userFull = (UserFull) possibleUser;

                var minData = userFull.users().stream()
                        .filter(u -> u.identifier() == BaseUser.ID &&
                                u.id() == userFull.fullUser().id())
                        .map(u -> (BaseUser) u)
                        .findFirst()
                        .orElseThrow();

                return new User(client, userFull.fullUser(), minData);
            }
            case BaseUser.ID:
                BaseUser baseUser = (BaseUser) possibleUser;

                return new User(client, baseUser);
            default:
                throw new IllegalArgumentException("Unknown user type: " + possibleUser);
        }
    }

    public static MessageAction createMessageAction(MTProtoTelegramClient client, telegram4j.tl.MessageAction data) {
        switch (data.identifier()) {
            case telegram4j.tl.MessageActionChatCreate.ID:
                return new MessageActionChatCreate(client, (telegram4j.tl.MessageActionChatCreate) data);

            // TODO: implement

            default:
                throw new IllegalArgumentException("Unknown message action type: " + data);
        }
    }

    public static MessageMedia createMessageMedia(MTProtoTelegramClient client, telegram4j.tl.MessageMedia data) {
        switch (data.identifier()) {
            case telegram4j.tl.MessageMediaPhoto.ID:
                return new MessageMediaPhoto(client, (telegram4j.tl.MessageMediaPhoto) data);

            case telegram4j.tl.MessageMediaGeo.ID:
                return new MessageMediaGeo(client, (telegram4j.tl.MessageMediaGeo) data);

            case telegram4j.tl.MessageMediaContact.ID:
                return new MessageMediaContact(client, (telegram4j.tl.MessageMediaContact) data);

            case telegram4j.tl.MessageMediaUnsupported.ID:
                return new BaseMessageMedia(client, MessageMedia.Type.UNSUPPORTED);

            case telegram4j.tl.MessageMediaDocument.ID:
                return new MessageMediaDocument(client, (telegram4j.tl.MessageMediaDocument) data);

            case telegram4j.tl.MessageMediaWebPage.ID:
                return new MessageMediaWebPage(client, (telegram4j.tl.MessageMediaWebPage) data);

            case telegram4j.tl.MessageMediaVenue.ID:
                return new MessageMediaVenue(client, (telegram4j.tl.MessageMediaVenue) data);

            case telegram4j.tl.MessageMediaGame.ID:
                return new MessageMediaGame(client, (telegram4j.tl.MessageMediaGame) data);

            case telegram4j.tl.MessageMediaInvoice.ID:
                return new MessageMediaInvoice(client, (telegram4j.tl.MessageMediaInvoice) data);

            case telegram4j.tl.MessageMediaGeoLive.ID:
                return new MessageMediaGeoLive(client, (telegram4j.tl.MessageMediaGeoLive) data);

            case telegram4j.tl.MessageMediaPoll.ID:
                return new MessageMediaPoll(client, (telegram4j.tl.MessageMediaPoll) data);

            case telegram4j.tl.MessageMediaDice.ID:
                return new MessageMediaDice(client, (telegram4j.tl.MessageMediaDice) data);
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

    public static PhotoSize createPhotoSize(MTProtoTelegramClient client, telegram4j.tl.PhotoSize data) {
        switch (data.identifier()) {
            case telegram4j.tl.BasePhotoSize.ID:
                return new DefaultPhotoSize(client, (telegram4j.tl.BasePhotoSize) data);
            case telegram4j.tl.PhotoCachedSize.ID:
                return new PhotoCachedSize(client, (telegram4j.tl.PhotoCachedSize) data);
            case telegram4j.tl.PhotoPathSize.ID:
                return new PhotoPathSize(client, (telegram4j.tl.PhotoPathSize) data);
            case telegram4j.tl.PhotoSizeProgressive.ID:
                return new PhotoSizeProgressive(client, (telegram4j.tl.PhotoSizeProgressive) data);
            case telegram4j.tl.PhotoStrippedSize.ID:
                return new PhotoStrippedSize(client, (telegram4j.tl.PhotoStrippedSize) data);
            default:
                throw new IllegalArgumentException("Unknown photo size type: " + data);
        }
    }

    public static DocumentAttribute createDocumentAttribute(MTProtoTelegramClient client, telegram4j.tl.DocumentAttribute data) {
        switch (data.identifier()) {
            case telegram4j.tl.DocumentAttributeHasStickers.ID:
                return new BaseDocumentAttribute(client, DocumentAttribute.Type.HAS_STICKERS);
            case telegram4j.tl.DocumentAttributeAnimated.ID:
                return new BaseDocumentAttribute(client, DocumentAttribute.Type.ANIMATED);
            case telegram4j.tl.DocumentAttributeAudio.ID:
                return new DocumentAttributeAudio(client, (telegram4j.tl.DocumentAttributeAudio) data);
            case telegram4j.tl.DocumentAttributeFilename.ID:
                return new DocumentAttributeFilename(client, (telegram4j.tl.DocumentAttributeFilename) data);
            case telegram4j.tl.DocumentAttributeImageSize.ID:
                return new DocumentAttributeImageSize(client, (telegram4j.tl.DocumentAttributeImageSize) data);
            case telegram4j.tl.DocumentAttributeSticker.ID:
                return new DocumentAttributeSticker(client, (telegram4j.tl.DocumentAttributeSticker) data);
            case telegram4j.tl.DocumentAttributeVideo.ID:
                return new DocumentAttributeVideo(client, (telegram4j.tl.DocumentAttributeVideo) data);
            default:
                throw new IllegalArgumentException("Unknown document attribute type: " + data);
        }
    }

    public static PeerEntity createPeerEntity(MTProtoTelegramClient client, ResolvedPeer p) {
        switch (p.peer().identifier()) {
            case PeerChannel.ID: return createChat(client, p.chats().get(0));
            case PeerUser.ID: return createUser(client, p.users().get(0));
            default: throw new IllegalArgumentException("Unknown peer type: " + p.peer());
        }
    }
}
