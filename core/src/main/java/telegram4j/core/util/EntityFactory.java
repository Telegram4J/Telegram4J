package telegram4j.core.util;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Document;
import telegram4j.core.object.ExportedChatInvite;
import telegram4j.core.object.Message;
import telegram4j.core.object.User;
import telegram4j.core.object.UserStatus;
import telegram4j.core.object.*;
import telegram4j.core.object.action.MessageAction;
import telegram4j.core.object.action.MessageActionBotAllowed;
import telegram4j.core.object.action.MessageActionChannelCreate;
import telegram4j.core.object.action.MessageActionChannelMigrateFrom;
import telegram4j.core.object.action.MessageActionChatAddUser;
import telegram4j.core.object.action.MessageActionChatCreate;
import telegram4j.core.object.action.MessageActionChatDeleteUser;
import telegram4j.core.object.action.MessageActionChatEditPhoto;
import telegram4j.core.object.action.MessageActionChatEditTitle;
import telegram4j.core.object.action.MessageActionChatJoinedByLink;
import telegram4j.core.object.action.MessageActionChatMigrateTo;
import telegram4j.core.object.action.MessageActionGameScore;
import telegram4j.core.object.action.MessageActionGeoProximityReached;
import telegram4j.core.object.action.MessageActionGroupCall;
import telegram4j.core.object.action.MessageActionGroupCallScheduled;
import telegram4j.core.object.action.MessageActionInviteToGroupCall;
import telegram4j.core.object.action.MessageActionPaymentSent;
import telegram4j.core.object.action.MessageActionPaymentSentMe;
import telegram4j.core.object.action.MessageActionPhoneCall;
import telegram4j.core.object.action.MessageActionSecureValuesSent;
import telegram4j.core.object.action.MessageActionSecureValuesSentMe;
import telegram4j.core.object.action.MessageActionSetChatTheme;
import telegram4j.core.object.action.*;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.object.chat.ChatParticipant;
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
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.BaseChat;
import telegram4j.tl.*;
import telegram4j.tl.api.TlObject;
import telegram4j.tl.contacts.ResolvedPeer;
import telegram4j.tl.messages.ChatFull;
import telegram4j.tl.users.UserFull;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
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

    @Nullable
    public static Chat createChat(MTProtoTelegramClient client, TlObject possibleChat,
                                  @Nullable BaseUser selfUserData) {
        switch (possibleChat.identifier()) {
            case UserFull.ID: {
                UserFull userFull = (UserFull) possibleChat;

                var minData = userFull.users().stream()
                        .filter(u -> u.identifier() == BaseUser.ID &&
                                u.id() == userFull.fullUser().id())
                        .map(u -> (BaseUser) u)
                        .findFirst()
                        .orElse(null);

                if (minData == null) {
                    return null;
                }

                User mappedFullUser = new User(client, userFull.fullUser(), minData);
                User selfUser = selfUserData != null ? new User(client, selfUserData) : null;

                return new PrivateChat(client, mappedFullUser, selfUser);
            }
            case BaseUser.ID:
                BaseUser baseUser = (BaseUser) possibleChat;

                User mappedMinUser = new User(client, baseUser);
                User selfUser = selfUserData != null ? new User(client, selfUserData) : null;

                return new PrivateChat(client, mappedMinUser, selfUser);
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

                var minData = chatFull.chats().stream()
                        .filter(c -> TlEntityUtil.isAvailableChat(c) && c.id() == chatFull.fullChat().id())
                        .findFirst()
                        .orElse(null);

                if (minData == null) {
                    return null;
                }

                var exportedChatInvite = Optional.of(chatFull.fullChat())
                        .map(c -> {
                            switch (c.identifier()) {
                                case ChannelFull.ID: return ((ChannelFull) c).exportedInvite();
                                case BaseChatFull.ID: return ((BaseChatFull) c).exportedInvite();
                                default: throw new IllegalStateException();
                            }
                        })
                        .map(d -> {
                            var admin = createUser(client, chatFull.users().stream()
                                    // This list is *usually* small, so there is no point in computing map
                                    .filter(u -> u.identifier() == BaseUser.ID && u.id() == d.adminId())
                                    .findFirst()
                                    .orElseThrow());

                            return new ExportedChatInvite(client, d, admin);
                        })
                        .orElse(null);

                if (chatFull.fullChat().identifier() == ChannelFull.ID) {
                    ChannelFull channelFull = (ChannelFull) chatFull.fullChat();
                    var channelMin = (telegram4j.tl.Channel) minData;

                    if (channelMin.megagroup()) {
                        return new SupergroupChat(client, channelFull, channelMin, exportedChatInvite);
                    }

                    return new BroadcastChannel(client, channelFull, channelMin, exportedChatInvite);
                }

                var usersMap = chatFull.users().stream()
                        .filter(u -> u.identifier() == BaseUser.ID)
                        .map(d -> createUser(client, d))
                        .collect(Collectors.toMap(u -> u.getId().asLong(), Function.identity()));

                var chat = (telegram4j.tl.BaseChatFull) chatFull.fullChat();
                List<ChatParticipant> chatParticipants;
                Id chatId = Id.ofChat(chat.id());
                switch (chat.participants().identifier()) {
                    case BaseChatParticipants.ID: {
                        BaseChatParticipants d = (BaseChatParticipants) chat.participants();
                        chatParticipants = d.participants().stream()
                                .map(c -> new ChatParticipant(client, usersMap.get(c.userId()), c, chatId))
                                .collect(Collectors.toUnmodifiableList());
                        break;
                    }
                    case ChatParticipantsForbidden.ID: {
                        ChatParticipantsForbidden d = (ChatParticipantsForbidden) chat.participants();
                        chatParticipants = Optional.ofNullable(d.selfParticipant())
                                .map(c -> new ChatParticipant(client, usersMap.get(c.userId()), c, chatId))
                                .map(List::of)
                                .orElse(null);
                        break;
                    }
                    default: throw new IllegalStateException("Unknown chat participants type: " + chat.participants());
                }

                return new GroupChat(client, chat, (telegram4j.tl.BaseChat) minData, exportedChatInvite, chatParticipants);
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

    public static MessageAction createMessageAction(MTProtoTelegramClient client, telegram4j.tl.MessageAction data,
                                                    InputPeer peer, int messageId) {
        switch (data.identifier()) {
            case telegram4j.tl.MessageActionBotAllowed.ID:
                return new MessageActionBotAllowed(client, (telegram4j.tl.MessageActionBotAllowed) data);
            case telegram4j.tl.MessageActionChannelCreate.ID:
                return new MessageActionChannelCreate(client, (telegram4j.tl.MessageActionChannelCreate) data);
            case telegram4j.tl.MessageActionChannelMigrateFrom.ID:
                return new MessageActionChannelMigrateFrom(client, (telegram4j.tl.MessageActionChannelMigrateFrom) data);
            case telegram4j.tl.MessageActionChatAddUser.ID:
                return new MessageActionChatAddUser(client, (telegram4j.tl.MessageActionChatAddUser) data);
            case telegram4j.tl.MessageActionChatCreate.ID:
                return new MessageActionChatCreate(client, (telegram4j.tl.MessageActionChatCreate) data);
            case telegram4j.tl.MessageActionChatDeletePhoto.ID:
                return new BaseMessageAction(client, MessageAction.Type.CHAT_DELETE_PHOTO);
            case telegram4j.tl.MessageActionChatDeleteUser.ID:
                return new MessageActionChatDeleteUser(client, (telegram4j.tl.MessageActionChatDeleteUser) data);
            case telegram4j.tl.MessageActionChatEditPhoto.ID:
                return new MessageActionChatEditPhoto(client, (telegram4j.tl.MessageActionChatEditPhoto) data, peer, messageId);
            case telegram4j.tl.MessageActionChatEditTitle.ID:
                return new MessageActionChatEditTitle(client, (telegram4j.tl.MessageActionChatEditTitle) data);
            case telegram4j.tl.MessageActionChatJoinedByLink.ID:
                return new MessageActionChatJoinedByLink(client, (telegram4j.tl.MessageActionChatJoinedByLink) data);
            case telegram4j.tl.MessageActionChatJoinedByRequest.ID:
                return new BaseMessageAction(client, MessageAction.Type.CHAT_JOINED_BY_REQUEST);
            case telegram4j.tl.MessageActionChatMigrateTo.ID:
                return new MessageActionChatMigrateTo(client, (telegram4j.tl.MessageActionChatMigrateTo) data);
            case telegram4j.tl.MessageActionContactSignUp.ID:
                return new BaseMessageAction(client, MessageAction.Type.CONTACT_SIGN_UP);
            case telegram4j.tl.MessageActionCustomAction.ID:
                return new MessageActionCustom(client, (telegram4j.tl.MessageActionCustomAction) data);
            case telegram4j.tl.MessageActionGameScore.ID:
                return new MessageActionGameScore(client, (telegram4j.tl.MessageActionGameScore) data);
            case telegram4j.tl.MessageActionGeoProximityReached.ID:
                return new MessageActionGeoProximityReached(client, (telegram4j.tl.MessageActionGeoProximityReached) data);
            case telegram4j.tl.MessageActionGroupCall.ID:
                return new MessageActionGroupCall(client, (telegram4j.tl.MessageActionGroupCall) data);
            case telegram4j.tl.MessageActionGroupCallScheduled.ID:
                return new MessageActionGroupCallScheduled(client, (telegram4j.tl.MessageActionGroupCallScheduled) data);
            case telegram4j.tl.MessageActionHistoryClear.ID:
                return new BaseMessageAction(client, MessageAction.Type.HISTORY_CLEAR);
            case telegram4j.tl.MessageActionInviteToGroupCall.ID:
                return new MessageActionInviteToGroupCall(client, (telegram4j.tl.MessageActionInviteToGroupCall) data);
            case telegram4j.tl.MessageActionPaymentSent.ID:
                return new MessageActionPaymentSent(client, (telegram4j.tl.MessageActionPaymentSent) data);
            case telegram4j.tl.MessageActionPaymentSentMe.ID:
                return new MessageActionPaymentSentMe(client, (telegram4j.tl.MessageActionPaymentSentMe) data);
            case telegram4j.tl.MessageActionPhoneCall.ID:
                return new MessageActionPhoneCall(client, (telegram4j.tl.MessageActionPhoneCall) data);
            case telegram4j.tl.MessageActionPinMessage.ID:
                return new BaseMessageAction(client, MessageAction.Type.PIN_MESSAGE);
            case telegram4j.tl.MessageActionScreenshotTaken.ID:
                return new BaseMessageAction(client, MessageAction.Type.SCREENSHOT_TAKEN);
            case telegram4j.tl.MessageActionSecureValuesSent.ID:
                return new MessageActionSecureValuesSent(client, (telegram4j.tl.MessageActionSecureValuesSent) data);
            case telegram4j.tl.MessageActionSecureValuesSentMe.ID:
                return new MessageActionSecureValuesSentMe(client, (telegram4j.tl.MessageActionSecureValuesSentMe) data);
            case telegram4j.tl.MessageActionSetChatTheme.ID:
                return new MessageActionSetChatTheme(client, (telegram4j.tl.MessageActionSetChatTheme) data);
            case telegram4j.tl.MessageActionSetMessagesTTL.ID:
                return new MessageActionSetMessagesTtl(client, (telegram4j.tl.MessageActionSetMessagesTTL) data);
            default:
                throw new IllegalArgumentException("Unknown message action type: " + data);
        }
    }

    public static MessageMedia createMessageMedia(MTProtoTelegramClient client, telegram4j.tl.MessageMedia data,
                                                  int messageId, InputPeer peer) {
        switch (data.identifier()) {
            case telegram4j.tl.MessageMediaPhoto.ID:
                return new MessageMediaPhoto(client, (telegram4j.tl.MessageMediaPhoto) data, messageId, peer);
            case telegram4j.tl.MessageMediaGeo.ID:
                return new MessageMediaGeo(client, (telegram4j.tl.MessageMediaGeo) data);
            case telegram4j.tl.MessageMediaContact.ID:
                return new MessageMediaContact(client, (telegram4j.tl.MessageMediaContact) data);
            case telegram4j.tl.MessageMediaUnsupported.ID:
                return new BaseMessageMedia(client, MessageMedia.Type.UNSUPPORTED);
            case telegram4j.tl.MessageMediaDocument.ID:
                return new MessageMediaDocument(client, (telegram4j.tl.MessageMediaDocument) data, messageId, peer);
            case telegram4j.tl.MessageMediaWebPage.ID:
                return new MessageMediaWebPage(client, (telegram4j.tl.MessageMediaWebPage) data);
            case telegram4j.tl.MessageMediaVenue.ID:
                return new MessageMediaVenue(client, (telegram4j.tl.MessageMediaVenue) data);
            case telegram4j.tl.MessageMediaGame.ID:
                return new MessageMediaGame(client, (telegram4j.tl.MessageMediaGame) data, messageId, peer);
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

    public static Document createDocument(MTProtoTelegramClient client, telegram4j.tl.BaseDocument data,
                                          int messageId, InputPeer peer) {
        boolean animated = false;
        boolean hasStickers = false;
        telegram4j.tl.DocumentAttributeVideo videoData = null;
        telegram4j.tl.DocumentAttributeAudio audioData = null;
        telegram4j.tl.DocumentAttributeSticker stickerData = null;
        telegram4j.tl.DocumentAttributeImageSize sizeData = null;
        String fileName = null;
        for (var a : data.attributes()) {
            switch (a.identifier()) {
                case telegram4j.tl.DocumentAttributeHasStickers.ID:
                    hasStickers = true;
                    break;
                case telegram4j.tl.DocumentAttributeAnimated.ID:
                    animated = true;
                    break;
                case telegram4j.tl.DocumentAttributeAudio.ID:
                    audioData = (telegram4j.tl.DocumentAttributeAudio) a;
                    break;
                case telegram4j.tl.DocumentAttributeFilename.ID:
                    fileName = ((telegram4j.tl.DocumentAttributeFilename) a).fileName();
                    break;
                case telegram4j.tl.DocumentAttributeImageSize.ID:
                    sizeData = (telegram4j.tl.DocumentAttributeImageSize) a;
                    break;
                case telegram4j.tl.DocumentAttributeSticker.ID:
                    stickerData = (telegram4j.tl.DocumentAttributeSticker) a;
                    break;
                case telegram4j.tl.DocumentAttributeVideo.ID:
                    videoData = (telegram4j.tl.DocumentAttributeVideo) a;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown document attribute type: " + a);
            }
        }

        if (stickerData != null) {
            return new Sticker(client, data, fileName, messageId, peer, stickerData, sizeData);
        }

        if (audioData != null) {
            return new Audio(client, data, fileName, messageId, peer, audioData);
        }

        if (videoData != null) {
            return new Video(client, data, fileName, messageId, peer,
                    videoData, hasStickers, animated);
        }

        return new Document(client, data, fileName, messageId, peer);
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
            case PeerChannel.ID: return createChat(client, p.chats().get(0), null);
            case PeerUser.ID: return createUser(client, p.users().get(0));
            default: throw new IllegalArgumentException("Unknown peer type: " + p.peer());
        }
    }
}
