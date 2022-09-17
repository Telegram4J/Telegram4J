package telegram4j.core.util;

import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.annotation.Nullable;
import reactor.util.function.Tuples;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Document;
import telegram4j.core.object.ExportedChatInvite;
import telegram4j.core.object.Message;
import telegram4j.core.object.Reaction;
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
import telegram4j.core.spec.inline.*;
import telegram4j.core.util.parser.EntityParserSupport;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.mtproto.file.FileReferenceId.DocumentType;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.BaseChat;
import telegram4j.tl.Channel;
import telegram4j.tl.*;
import telegram4j.tl.api.TlObject;
import telegram4j.tl.contacts.ResolvedPeer;
import telegram4j.tl.messages.ChatFull;
import telegram4j.tl.users.UserFull;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class EntityFactory {

    private EntityFactory() {
    }

    public static UserStatus createUserStatus(MTProtoTelegramClient client, telegram4j.tl.UserStatus data) {
        switch (data.identifier()) {
            case UserStatusLastMonth.ID: return new UserStatus(client, UserStatus.Type.LAST_MONTH);
            case UserStatusLastWeek.ID: return new UserStatus(client, UserStatus.Type.LAST_WEEK);
            case UserStatusOffline.ID:
                UserStatusOffline userStatusOffline = (UserStatusOffline) data;
                Instant wasOnlineTimestamp = Instant.ofEpochSecond(userStatusOffline.wasOnline());
                return new UserStatus(client, UserStatus.Type.OFFLINE, null, wasOnlineTimestamp);
            case UserStatusOnline.ID:
                UserStatusOnline userStatusOnline = (UserStatusOnline) data;
                Instant expiresTimestamp = Instant.ofEpochSecond(userStatusOnline.expires());
                return new UserStatus(client, UserStatus.Type.ONLINE, expiresTimestamp, null);
            case UserStatusRecently.ID: return new UserStatus(client, UserStatus.Type.RECENTLY);
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
                                  @Nullable User selfUser) {
        switch (possibleChat.identifier()) {
            case UserEmpty.ID:
            case ChatEmpty.ID: return null;
            case ChatForbidden.ID:
                switch (client.getUnavailableChatPolicy()) {
                    case THROWING: throw UnavailableChatException.from((ChatForbidden) possibleChat);
                    case NULL_MAPPING: return null;
                }
            case ChannelForbidden.ID:
                switch (client.getUnavailableChatPolicy()) {
                    case THROWING: throw UnavailableChatException.from((ChannelForbidden) possibleChat);
                    case NULL_MAPPING: return null;
                }

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

                return new PrivateChat(client, mappedFullUser, selfUser);
            }
            case BaseUser.ID:
                BaseUser baseUser = (BaseUser) possibleChat;

                User mappedMinUser = new User(client, baseUser);

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
                        .filter(c -> c.id() == chatFull.fullChat().id())
                        .findFirst()
                        .map(c -> {
                            switch (c.identifier()) {
                                case ChatEmpty.ID: return null;
                                case ChatForbidden.ID:
                                    switch (client.getUnavailableChatPolicy()) {
                                        case THROWING: throw UnavailableChatException.from((ChatForbidden) possibleChat);
                                        case NULL_MAPPING: return null;
                                    }
                                case ChannelForbidden.ID:
                                    switch (client.getUnavailableChatPolicy()) {
                                        case THROWING: throw UnavailableChatException.from((ChannelForbidden) possibleChat);
                                        case NULL_MAPPING: return null;
                                    }
                                case BaseChat.ID:
                                case Channel.ID:
                                    return c;
                                default: throw new IllegalArgumentException("Unknown chat type: " + c);
                            }
                        })
                        .orElse(null);

                if (minData == null) {
                    return null;
                }

                var exportedChatInvite = Optional.of(chatFull.fullChat())
                        .map(telegram4j.tl.ChatFull::exportedInvite)
                        .map(e -> TlEntityUtil.unmapEmpty(e, ChatInviteExported.class))
                        .map(d -> {
                            var admin = chatFull.users().stream()
                                    // This list is *usually* small, so there is no point in computing map
                                    .filter(u -> u.id() == d.adminId())
                                    .findFirst()
                                    .map(u -> createUser(client, u))
                                    .orElseThrow();

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
                        .map(d -> createUser(client, d))
                        .filter(Objects::nonNull)
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

    @Nullable
    public static User createUser(MTProtoTelegramClient client, TlObject possibleUser) {
        switch (possibleUser.identifier()) {
            case UserEmpty.ID: return null;
            case UserFull.ID: {
                UserFull userFull = (UserFull) possibleUser;

                var minData = userFull.users().stream()
                        .filter(u -> u.identifier() == BaseUser.ID &&
                                u.id() == userFull.fullUser().id())
                        .map(u -> (BaseUser) u)
                        .findFirst()
                        .orElse(null);

                if (minData == null) {
                    return null;
                }

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
            case telegram4j.tl.MessageActionChatDeleteUser.ID:
                return new MessageActionChatDeleteUser(client, (telegram4j.tl.MessageActionChatDeleteUser) data);
            case telegram4j.tl.MessageActionChatDeletePhoto.ID: return new MessageActionChatEditPhoto(client);
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
                return new MessageMediaInvoice(client, (telegram4j.tl.MessageMediaInvoice) data, messageId, peer);
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

    public static Document createDocument(MTProtoTelegramClient client, telegram4j.tl.BaseDocumentFields data,
                                          int messageId, InputPeer peer) {
        boolean animated = false;
        boolean hasStickers = false;
        telegram4j.tl.DocumentAttributeVideo videoData = null;
        telegram4j.tl.DocumentAttributeAudio audioData = null;
        telegram4j.tl.DocumentAttributeSticker stickerData = null;
        telegram4j.tl.DocumentAttributeImageSize sizeData = null;
        // TODO: test
        telegram4j.tl.DocumentAttributeCustomEmoji customEmojiData = null;
        String fileName = null;
        for (var a : data.attributes()) {
            switch (a.identifier()) {
                case telegram4j.tl.DocumentAttributeCustomEmoji.ID:
                    customEmojiData = (DocumentAttributeCustomEmoji) a;
                    break;
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
            return new Sticker(client, data, fileName,
                    messageId, peer, stickerData, sizeData, videoData);
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

    @Nullable
    public static PeerEntity createPeerEntity(MTProtoTelegramClient client, ResolvedPeer p) {
        switch (p.peer().identifier()) {
            case PeerChannel.ID: return createChat(client, p.chats().get(0), null);
            case PeerUser.ID: return createUser(client, p.users().get(0));
            default: throw new IllegalArgumentException("Unknown peer type: " + p.peer());
        }
    }

    public static Mono<InputBotInlineResult> createInlineResult(MTProtoTelegramClient client, InlineResultSpec spec) {
        Mono<InputBotInlineMessage> sendMessage = createInlineMessage(client, spec.message());

        if (spec instanceof InlineResultArticleSpec) {
            InlineResultArticleSpec r = (InlineResultArticleSpec) spec;

            var builder = BaseInputBotInlineResult.builder()
                    .type("article")
                    .title(r.title())
                    .id(r.id())
                    .url(r.url());

            r.description().ifPresent(builder::description);

            var contentBuilder = InputWebDocument.builder()
                    .url(r.url())
                    .size(0)
                    .mimeType("text/html");

            Optional.ofNullable(getFilenameFromUrl(r.url())).ifPresent(s -> contentBuilder.addAttribute(
                    ImmutableDocumentAttributeFilename.of(s)));

            builder.content(contentBuilder.build());

            r.thumb().map(EntityFactory::createThumbnail).ifPresent(builder::thumb);

            return sendMessage.map(builder::sendMessage)
                    .map(ImmutableBaseInputBotInlineResult.Builder::build);
        } else if (spec instanceof InlineResultPhotoSpec) {
            InlineResultPhotoSpec r = (InlineResultPhotoSpec) spec;

            try {
                InputPhoto photo = FileReferenceId.deserialize(r.photo()).asInputPhoto();

                var builder = InputBotInlineResultPhoto.builder()
                        .type("photo")
                        .photo(photo)
                        .id(r.id());

                return sendMessage.map(builder::sendMessage)
                        .map(ImmutableInputBotInlineResultPhoto.Builder::build);
            } catch (IllegalArgumentException e) { // may be just an url
                String url = r.photo();
                WebDocumentSpec thumb = r.thumb().orElseGet(() -> WebDocumentSpec.of(url));

                var builder = BaseInputBotInlineResult.builder()
                        .type("photo")
                        .id(r.id())
                        .url(url);

                var contentBuilder = InputWebDocument.builder()
                        .url(url)
                        .size(0)
                        .mimeType("image/jpeg");

                Optional.ofNullable(getFilenameFromUrl(url)).ifPresent(s -> contentBuilder.addAttribute(
                        ImmutableDocumentAttributeFilename.of(s)));

                r.photoSize().ifPresent(s -> contentBuilder.addAttribute(
                        ImmutableDocumentAttributeImageSize.of(s.width(), s.height())));

                builder.content(contentBuilder.build());
                builder.thumb(createThumbnail(thumb));

                return sendMessage.map(builder::sendMessage)
                        .map(ImmutableBaseInputBotInlineResult.Builder::build);
            }
        } else if (spec instanceof InlineResultDocumentSpec) {
            InlineResultDocumentSpec r = (InlineResultDocumentSpec) spec;

            DocumentType documentType = r.type().orElse(DocumentType.GENERAL);
            if (documentType == DocumentType.UNKNOWN) {
                return Mono.error(new IllegalArgumentException("Unexpected document type."));
            }

            String type = documentType == DocumentType.GENERAL ? "file" : documentType.name().toLowerCase(Locale.ROOT);
            try {
                FileReferenceId fileRefId = FileReferenceId.deserialize(r.file());
                InputDocument document = fileRefId.asInputDocument();

                if (r.type().map(t -> fileRefId.getDocumentType() != t).orElse(false)) {
                    return Mono.error(new IllegalArgumentException("Document type mismatch. File ref id: "
                            + fileRefId.getDocumentType() + ", type: " + documentType));
                }

                var builder = InputBotInlineResultDocument.builder()
                        .type(type)
                        .title(r.title())
                        .description(r.description().orElse(null))
                        .document(document)
                        .id(r.id());

                return sendMessage.map(builder::sendMessage)
                        .map(ImmutableInputBotInlineResultDocument.Builder::build);
            } catch (IllegalArgumentException e) { // may be just an url
                String url = r.file();
                String mimeType = r.mimeType().orElseThrow(() -> new IllegalArgumentException(
                        "Mime type must be included with urls."));

                if (!mimeType.equalsIgnoreCase("application/pdf") &&
                        !mimeType.equalsIgnoreCase("application/zip")) {
                    return Mono.error(new IllegalStateException("Not allowed mime type for web file: " + mimeType));
                }

                var builder = BaseInputBotInlineResult.builder()
                        .type(type)
                        .title(r.title())
                        .description(r.description().orElse(null))
                        .id(r.id())
                        .url(url);

                var contentBuilder = InputWebDocument.builder()
                        .url(url)
                        .size(0)
                        .mimeType(mimeType);

                switch (documentType) {
                    case VIDEO:
                    case GIF: {
                        int duration = r.duration()
                                .map(Duration::getSeconds)
                                .map(Math::toIntExact)
                                .orElseThrow(() -> new IllegalStateException("Duration for video/gif documents must be set."));

                        SizeSpec size = r.size()
                                .orElseThrow(() -> new IllegalStateException("Size for video/gif documents must be set."));

                        contentBuilder.addAttribute(DocumentAttributeVideo.builder()
                                .h(size.height())
                                .w(size.width())
                                .duration(duration)
                                .build());
                        break;
                    }
                    case AUDIO:
                    case VOICE:
                        int duration = r.duration()
                                .map(Duration::getSeconds)
                                .map(Math::toIntExact)
                                .orElseThrow(() -> new IllegalStateException("Duration for voice/audio documents must be set."));

                        contentBuilder.addAttribute(DocumentAttributeAudio.builder()
                                .voice(documentType == DocumentType.VOICE)
                                .duration(duration)
                                .build());
                        break;
                }

                Optional.ofNullable(getFilenameFromUrl(url)).ifPresent(s -> contentBuilder.addAttribute(
                        ImmutableDocumentAttributeFilename.of(s)));

                builder.content(contentBuilder.build());
                r.thumb().map(EntityFactory::createThumbnail).ifPresent(builder::thumb);

                return sendMessage.map(builder::sendMessage)
                        .map(ImmutableBaseInputBotInlineResult.Builder::build);
            }
        } else if (spec instanceof InlineResultGameSpec) {
            InlineResultGameSpec r = (InlineResultGameSpec) spec;

            var builder = InputBotInlineResultGame.builder()
                    .id(r.id())
                    .shortName(r.shortName());

            return sendMessage.map(builder::sendMessage)
                    .map(ImmutableInputBotInlineResultGame.Builder::build);
        } else {
            throw new IllegalStateException();
        }
    }

    public static Reaction createReaction(telegram4j.tl.Reaction reaction) {
        switch (reaction.identifier()) {
            case ReactionCustomEmoji.ID:
                ReactionCustomEmoji custom = (ReactionCustomEmoji) reaction;
                return new Reaction(custom.documentId());
            case ReactionEmoji.ID:
                ReactionEmoji emoticon = (ReactionEmoji) reaction;
                return new Reaction(emoticon.emoticon());
            // and ReactionEmpty
            default:
                throw new IllegalArgumentException("Unknown reaction type: " + reaction);
        }
    }

    @Nullable
    public static Variant2<Boolean, List<Reaction>> createChatReactions(ChatReactions data) {
        switch (data.identifier()) {
            case ChatReactionsAll.ID:
                ChatReactionsAll all = (ChatReactionsAll) data;
                return Variant2.ofT1(all.allowCustom());
            case ChatReactionsSome.ID:
                ChatReactionsSome some = (ChatReactionsSome) data;
                return Variant2.ofT2(some.reactions().stream()
                        .map(EntityFactory::createReaction)
                        .collect(Collectors.toUnmodifiableList()));
            case ChatReactionsNone.ID: return null;
            default: throw new IllegalStateException("Unknown ChatReactions type: " + data);
        }
    }

    // Internal utility methods
    // ===========================

    private static InputWebDocument createThumbnail(WebDocumentSpec spec) {

        var thumbBuilder = InputWebDocument.builder()
                .mimeType(spec.mimeType().orElse("image/jpeg"))
                .size(0)
                .url(spec.url());

        spec.imageSize().ifPresent(s -> thumbBuilder.addAttribute(
                ImmutableDocumentAttributeImageSize.of(s.width(), s.height())));

        Optional.ofNullable(getFilenameFromUrl(spec.url())).ifPresent(s -> thumbBuilder.addAttribute(
                ImmutableDocumentAttributeFilename.of(s)));

        return thumbBuilder.build();
    }

    private static Mono<InputBotInlineMessage> createInlineMessage(MTProtoTelegramClient client, InlineMessageSpec message) {
        var replyMarkup = Mono.justOrEmpty(message.replyMarkup())
                .flatMap(s -> s.asData(client));

        if (message instanceof InlineMessageTextSpec) {
            InlineMessageTextSpec m = (InlineMessageTextSpec) message;

            String c = m.message().trim();
            var parser = m.parser()
                    .or(() -> client.getMtProtoResources().getDefaultEntityParser())
                    .map(t -> EntityParserSupport.parse(client, t.apply(c)))
                    .orElseGet(() -> Mono.just(Tuples.of(c, List.of())));

            return parser.map(TupleUtils.function((txt, ent) -> InputBotInlineMessageText.builder()
                            .message(txt)
                            .entities(ent)
                            .noWebpage(m.noWebpage())))
                    .flatMap(builder -> replyMarkup.doOnNext(builder::replyMarkup)
                            .then(Mono.fromSupplier(builder::build)));
        } else if (message instanceof InlineMessageMediaGameSpec) {
            return Mono.just(ImmutableInputBotInlineMessageGame.of())
                    .flatMap(r -> replyMarkup.map(r::withReplyMarkup)
                            .defaultIfEmpty(r));

        } else if (message instanceof InlineMessageMediaAutoSpec) {
            InlineMessageMediaAutoSpec m = (InlineMessageMediaAutoSpec) message;

            String c = m.message().trim();
            var parser = m.parser()
                    .or(() -> client.getMtProtoResources().getDefaultEntityParser())
                    .map(t -> EntityParserSupport.parse(client, t.apply(c)))
                    .orElseGet(() -> Mono.just(Tuples.of(c, List.of())));

            return parser.map(TupleUtils.function((txt, ent) -> InputBotInlineMessageMediaAuto.builder()
                            .message(txt)
                            .entities(ent)))
                    .flatMap(builder -> replyMarkup.doOnNext(builder::replyMarkup)
                            .then(Mono.fromSupplier(builder::build)));
        } else if (message instanceof InlineMessageMediaVenueSpec) {
            InlineMessageMediaVenueSpec m = (InlineMessageMediaVenueSpec) message;

            return Mono.just(InputBotInlineMessageMediaVenue.builder()
                            .geoPoint(BaseInputGeoPoint.builder()
                                    .lat(m.media().latitude())
                                    .longitude(m.media().longitude())
                                    .accuracyRadius(m.media().accuracyRadius().orElse(null))
                                    .build())
                            .title(m.media().title())
                            .provider(m.media().provider())
                            .venueType(m.media().venueType())
                            .venueId(m.media().venueId())
                            .address(m.media().address()))
                    .flatMap(builder -> replyMarkup.doOnNext(builder::replyMarkup)
                            .then(Mono.fromSupplier(builder::build)));
        } else {
            throw new IllegalStateException();
        }
    }

    @Nullable
    private static String getFilenameFromUrl(String url) {
        int begin = url.lastIndexOf('/') + 1;
        url = url.substring(begin);

        int end = Math.min(url.indexOf('?'), url.indexOf('#'));
        if (end == -1) {
            end = url.length();
        }

        String res = url.substring(0, end);
        return res.isEmpty() ? null : res;
    }
}
