package telegram4j.core.internal;

import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.annotation.Nullable;
import reactor.util.function.Tuples;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Document;
import telegram4j.core.object.ExportedChatInvite;
import telegram4j.core.object.Message;
import telegram4j.core.object.MessageAction;
import telegram4j.core.object.MessageMedia;
import telegram4j.core.object.Photo;
import telegram4j.core.object.Reaction;
import telegram4j.core.object.User;
import telegram4j.core.object.UserStatus;
import telegram4j.core.object.*;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.object.chat.ChatParticipant;
import telegram4j.core.object.chat.*;
import telegram4j.core.object.media.PhotoCachedSize;
import telegram4j.core.object.media.PhotoPathSize;
import telegram4j.core.object.media.PhotoSize;
import telegram4j.core.object.media.PhotoSizeProgressive;
import telegram4j.core.object.media.PhotoStrippedSize;
import telegram4j.core.object.media.*;
import telegram4j.core.spec.inline.*;
import telegram4j.core.util.Id;
import telegram4j.core.util.Variant2;
import telegram4j.core.util.parser.EntityParserSupport;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.mtproto.file.FileReferenceId.DocumentType;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.BaseChat;
import telegram4j.tl.Channel;
import telegram4j.tl.*;
import telegram4j.tl.api.TlObject;
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

    public static UserStatus createUserStatus(telegram4j.tl.UserStatus data) {
        switch (data.identifier()) {
            case UserStatusLastMonth.ID: return new UserStatus(UserStatus.Type.LAST_MONTH);
            case UserStatusLastWeek.ID: return new UserStatus(UserStatus.Type.LAST_WEEK);
            case UserStatusOffline.ID:
                UserStatusOffline userStatusOffline = (UserStatusOffline) data;
                Instant wasOnlineTimestamp = Instant.ofEpochSecond(userStatusOffline.wasOnline());
                return new UserStatus(UserStatus.Type.OFFLINE, null, wasOnlineTimestamp);
            case UserStatusOnline.ID:
                UserStatusOnline userStatusOnline = (UserStatusOnline) data;
                Instant expiresTimestamp = Instant.ofEpochSecond(userStatusOnline.expires());
                return new UserStatus(UserStatus.Type.ONLINE, expiresTimestamp, null);
            case UserStatusRecently.ID: return new UserStatus(UserStatus.Type.RECENTLY);
            // and UserStatusEmpty
            default: throw new IllegalArgumentException("Unknown UserStatus type: " + data);
        }
    }

    public static Message createMessage(MTProtoTelegramClient client, telegram4j.tl.Message data, Id resolvedChatId) {
        switch (data.identifier()) {
            case BaseMessage.ID: return new Message(client, (BaseMessage) data, resolvedChatId);
            case MessageService.ID: return new Message(client, (MessageService) data, resolvedChatId);
            default: throw new IllegalArgumentException("Unknown Message type: " + data);
        }
    }

    @Nullable
    public static Chat createChat(MTProtoTelegramClient client, TlObject possibleChat,
                                  @Nullable User selfUser) {
        switch (possibleChat.identifier()) {
            case UserEmpty.ID:
            case ChatEmpty.ID: return null;
            case ChatForbidden.ID:
                switch (client.getMtProtoResources().getUnavailableChatPolicy()) {
                    case THROWING: throw UnavailableChatException.from((ChatForbidden) possibleChat);
                    case NULL_MAPPING: return null;
                }
            case ChannelForbidden.ID:
                switch (client.getMtProtoResources().getUnavailableChatPolicy()) {
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
                                    switch (client.getMtProtoResources().getUnavailableChatPolicy()) {
                                        case THROWING: throw UnavailableChatException.from((ChatForbidden) possibleChat);
                                        case NULL_MAPPING: return null;
                                    }
                                case ChannelForbidden.ID:
                                    switch (client.getMtProtoResources().getUnavailableChatPolicy()) {
                                        case THROWING: throw UnavailableChatException.from((ChannelForbidden) possibleChat);
                                        case NULL_MAPPING: return null;
                                    }
                                case BaseChat.ID:
                                case Channel.ID:
                                    return c;
                                default: throw new IllegalArgumentException("Unknown Chat type: " + c);
                            }
                        })
                        .orElse(null);

                if (minData == null) {
                    return null;
                }

                var exportedChatInvite = Optional.of(chatFull.fullChat())
                        .map(telegram4j.tl.ChatFull::exportedInvite)
                        .map(e -> TlEntityUtil.unmapEmpty(e, ChatInviteExported.class))
                        .map(d -> new ExportedChatInvite(client, d, chatFull.users().stream()
                                // This list is *usually* small, so there is no point in computing map
                                .filter(u -> u.id() == d.adminId())
                                .findFirst()
                                .map(u -> createUser(client, u))
                                .orElseThrow()))
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
                                .map(c -> List.of(new ChatParticipant(client, usersMap.get(c.userId()), c, chatId)))
                                .orElse(null);
                        break;
                    }
                    default: throw new IllegalStateException("Unknown ChatParticipants type: " + chat.participants());
                }

                return new GroupChat(client, chat, (telegram4j.tl.BaseChat) minData, exportedChatInvite, chatParticipants);
            default:
                throw new IllegalArgumentException("Unknown Chat type: " + possibleChat);
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
                throw new IllegalArgumentException("Unknown User type: " + possibleUser);
        }
    }

    public static MessageAction createMessageAction(MTProtoTelegramClient client, telegram4j.tl.MessageAction data,
                                                    InputPeer peer, int messageId) {
        switch (data.identifier()) {
            case telegram4j.tl.MessageActionBotAllowed.ID:
                return new MessageAction.BotAllowed(client, (telegram4j.tl.MessageActionBotAllowed) data);
            case telegram4j.tl.MessageActionChannelCreate.ID:
                return new MessageAction.ChannelCreate(client, (telegram4j.tl.MessageActionChannelCreate) data);
            case telegram4j.tl.MessageActionChannelMigrateFrom.ID:
                return new MessageAction.ChannelMigrateFrom(client, (telegram4j.tl.MessageActionChannelMigrateFrom) data);
            case telegram4j.tl.MessageActionChatAddUser.ID:
                return new MessageAction.ChatAddUser(client, (telegram4j.tl.MessageActionChatAddUser) data);
            case telegram4j.tl.MessageActionChatCreate.ID:
                return new MessageAction.ChatCreate(client, (telegram4j.tl.MessageActionChatCreate) data);
            case telegram4j.tl.MessageActionChatDeleteUser.ID:
                return new MessageAction.ChatDeleteUser(client, (telegram4j.tl.MessageActionChatDeleteUser) data);
            case telegram4j.tl.MessageActionChatDeletePhoto.ID: return new MessageAction.ChatEditPhoto(client);
            case telegram4j.tl.MessageActionChatEditPhoto.ID:
                return new MessageAction.ChatEditPhoto(client, (telegram4j.tl.MessageActionChatEditPhoto) data, peer, messageId);
            case telegram4j.tl.MessageActionChatEditTitle.ID:
                return new MessageAction.ChatEditTitle(client, (telegram4j.tl.MessageActionChatEditTitle) data);
            case telegram4j.tl.MessageActionChatJoinedByLink.ID:
                return new MessageAction.ChatJoinedByLink(client, (telegram4j.tl.MessageActionChatJoinedByLink) data);
            case telegram4j.tl.MessageActionChatJoinedByRequest.ID:
                return new MessageAction(client, MessageAction.Type.CHAT_JOINED_BY_REQUEST);
            case telegram4j.tl.MessageActionChatMigrateTo.ID:
                return new MessageAction.ChatMigrateTo(client, (telegram4j.tl.MessageActionChatMigrateTo) data);
            case telegram4j.tl.MessageActionContactSignUp.ID:
                return new MessageAction(client, MessageAction.Type.CONTACT_SIGN_UP);
            case telegram4j.tl.MessageActionCustomAction.ID:
                return new MessageAction.Custom(client, (telegram4j.tl.MessageActionCustomAction) data);
            case telegram4j.tl.MessageActionGameScore.ID:
                return new MessageAction.GameScore(client, (telegram4j.tl.MessageActionGameScore) data);
            case telegram4j.tl.MessageActionGeoProximityReached.ID:
                return new MessageAction.GeoProximityReached(client, (telegram4j.tl.MessageActionGeoProximityReached) data);
            case telegram4j.tl.MessageActionGroupCall.ID:
                return new MessageAction.GroupCall(client, (telegram4j.tl.MessageActionGroupCall) data);
            case telegram4j.tl.MessageActionGroupCallScheduled.ID:
                return new MessageAction.GroupCallScheduled(client, (telegram4j.tl.MessageActionGroupCallScheduled) data);
            case telegram4j.tl.MessageActionHistoryClear.ID:
                return new MessageAction(client, MessageAction.Type.HISTORY_CLEAR);
            case telegram4j.tl.MessageActionInviteToGroupCall.ID:
                return new MessageAction.InviteToGroupCall(client, (telegram4j.tl.MessageActionInviteToGroupCall) data);
            case telegram4j.tl.MessageActionPaymentSent.ID:
                return new MessageAction.PaymentSent(client, (telegram4j.tl.MessageActionPaymentSent) data);
            case telegram4j.tl.MessageActionPaymentSentMe.ID:
                return new MessageAction.PaymentSentMe(client, (telegram4j.tl.MessageActionPaymentSentMe) data);
            case telegram4j.tl.MessageActionPhoneCall.ID:
                return new MessageAction.PhoneCall(client, (telegram4j.tl.MessageActionPhoneCall) data);
            case telegram4j.tl.MessageActionPinMessage.ID:
                return new MessageAction(client, MessageAction.Type.PIN_MESSAGE);
            case telegram4j.tl.MessageActionScreenshotTaken.ID:
                return new MessageAction(client, MessageAction.Type.SCREENSHOT_TAKEN);
            case telegram4j.tl.MessageActionSecureValuesSent.ID:
                return new MessageAction.SecureValuesSent(client, (telegram4j.tl.MessageActionSecureValuesSent) data);
            case telegram4j.tl.MessageActionSecureValuesSentMe.ID:
                return new MessageAction.SecureValuesSentMe(client, (telegram4j.tl.MessageActionSecureValuesSentMe) data);
            case telegram4j.tl.MessageActionSetChatTheme.ID:
                return new MessageAction.SetChatTheme(client, (telegram4j.tl.MessageActionSetChatTheme) data);
            case telegram4j.tl.MessageActionSetMessagesTTL.ID:
                return new MessageAction.SetMessagesTtl(client, (telegram4j.tl.MessageActionSetMessagesTTL) data);
            default:
                throw new IllegalArgumentException("Unknown MessageAction type: " + data);
        }
    }

    public static MessageMedia createMessageMedia(MTProtoTelegramClient client, telegram4j.tl.MessageMedia data,
                                                  int messageId, InputPeer peer) {
        switch (data.identifier()) {
            case telegram4j.tl.MessageMediaGeo.ID:
                return new MessageMedia.Geo(client, (telegram4j.tl.MessageMediaGeo) data);
            case telegram4j.tl.MessageMediaContact.ID:
                return new MessageMedia.Contact(client, (telegram4j.tl.MessageMediaContact) data);
            case telegram4j.tl.MessageMediaUnsupported.ID:
                return new MessageMedia(client, MessageMedia.Type.UNSUPPORTED);
            case telegram4j.tl.MessageMediaPhoto.ID:
            case telegram4j.tl.MessageMediaDocument.ID:
                return new MessageMedia.Document(client, data, messageId, peer);
            case telegram4j.tl.MessageMediaWebPage.ID:
                return new MessageMedia.WebPage(client, (telegram4j.tl.MessageMediaWebPage) data);
            case telegram4j.tl.MessageMediaVenue.ID:
                return new MessageMedia.Venue(client, (telegram4j.tl.MessageMediaVenue) data);
            case telegram4j.tl.MessageMediaGame.ID:
                return new MessageMedia.Game(client, (telegram4j.tl.MessageMediaGame) data, messageId, peer);
            case telegram4j.tl.MessageMediaInvoice.ID:
                return new MessageMedia.Invoice(client, (telegram4j.tl.MessageMediaInvoice) data, messageId, peer);
            case telegram4j.tl.MessageMediaGeoLive.ID:
                return new MessageMedia.GeoLive(client, (telegram4j.tl.MessageMediaGeoLive) data);
            case telegram4j.tl.MessageMediaPoll.ID:
                return new MessageMedia.Poll(client, (telegram4j.tl.MessageMediaPoll) data);
            case telegram4j.tl.MessageMediaDice.ID:
                return new MessageMedia.Dice(client, (telegram4j.tl.MessageMediaDice) data);
            default:
                throw new IllegalArgumentException("Unknown MessageMedia type: " + data);
        }
    }

    public static PhotoSize createPhotoSize(telegram4j.tl.PhotoSize data) {
        switch (data.identifier()) {
            case telegram4j.tl.BasePhotoSize.ID: return new DefaultPhotoSize((telegram4j.tl.BasePhotoSize) data);
            case telegram4j.tl.PhotoCachedSize.ID: return new PhotoCachedSize((telegram4j.tl.PhotoCachedSize) data);
            case telegram4j.tl.PhotoPathSize.ID: return new PhotoPathSize((telegram4j.tl.PhotoPathSize) data);
            case telegram4j.tl.PhotoSizeProgressive.ID: return new PhotoSizeProgressive((telegram4j.tl.PhotoSizeProgressive) data);
            case telegram4j.tl.PhotoStrippedSize.ID: return new PhotoStrippedSize((telegram4j.tl.PhotoStrippedSize) data);
            default: throw new IllegalArgumentException("Unknown photo size type: " + data);
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
        telegram4j.tl.DocumentAttributeCustomEmoji emojiData = null;
        String fileName = null;
        for (var a : data.attributes()) {
            switch (a.identifier()) {
                case telegram4j.tl.DocumentAttributeCustomEmoji.ID:
                    emojiData = (DocumentAttributeCustomEmoji) a;
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
                    throw new IllegalArgumentException("Unknown DocumentAttribute type: " + a);
            }
        }

        if (stickerData != null || emojiData != null) {
            return new Sticker(client, data, fileName, messageId, peer,
                    Variant2.of(stickerData, emojiData), Variant2.of(sizeData, videoData));
        }

        if (audioData != null) {
            return new Audio(client, data, fileName, messageId, peer, audioData);
        }

        if (videoData != null) {
            return new Video(client, data, fileName, messageId, peer,
                    videoData, hasStickers, animated);
        }

        if (sizeData != null) {
            return new Photo(client, data, fileName,
                    messageId, peer, sizeData);
        }

        return new Document(client, data, fileName, messageId, peer);
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

            builder.content(InputWebDocument.builder()
                    .url(r.url())
                    .size(0)
                    .attributes(List.of())
                    .mimeType("text/html")
                    .build());

            r.thumb().map(EntityFactory::createThumbnail).ifPresent(builder::thumb);

            return sendMessage.map(builder::sendMessage)
                    .map(ImmutableBaseInputBotInlineResult.Builder::build);
        } else if (spec instanceof InlineResultDocumentSpec) {
            InlineResultDocumentSpec r = (InlineResultDocumentSpec) spec;

            DocumentType documentType = r.type().orElse(DocumentType.GENERAL);
            String type = documentType == DocumentType.GENERAL ? "file" : documentType.name().toLowerCase(Locale.ROOT);

            try {
                FileReferenceId fileRefId = FileReferenceId.deserialize(r.file());

                var fileRefIdType = fileRefId.getDocumentType()
                        .or(() -> fileRefId.getFileType() == FileReferenceId.Type.PHOTO
                                ? Optional.of(DocumentType.PHOTO) : Optional.empty()) // possible jpeg photo
                        .orElseThrow();
                if (r.type().map(t -> fileRefIdType != t).orElse(false)) {
                    throw new IllegalArgumentException("Document type mismatch. File ref id: "
                            + fileRefIdType + ", type: " + documentType);
                }

                if (fileRefIdType == DocumentType.PHOTO) {
                    InputPhoto photo = fileRefId.asInputPhoto();
                    var builder = InputBotInlineResultPhoto.builder()
                            .type("photo")
                            .photo(photo)
                            .id(r.id());

                    return sendMessage.map(builder::sendMessage)
                            .map(ImmutableInputBotInlineResultPhoto.Builder::build);
                }

                InputDocument document = fileRefId.asInputDocument();
                var builder = InputBotInlineResultDocument.builder()
                        .type(type)
                        .title(r.title().orElse(null))
                        .description(r.description().orElse(null))
                        .document(document)
                        .id(r.id());

                return sendMessage.map(builder::sendMessage)
                        .map(ImmutableInputBotInlineResultDocument.Builder::build);
            } catch (IllegalArgumentException e) { // may be just an url
                String mimeType = r.mimeType()
                        .or(() -> documentType == DocumentType.PHOTO ? Optional.of("image/jpeg") : Optional.empty())
                        .orElseThrow(() -> new IllegalArgumentException("Mime type must be included with urls."));

                if (documentType == DocumentType.GENERAL &&
                        !mimeType.equalsIgnoreCase("application/pdf") &&
                        !mimeType.equalsIgnoreCase("application/zip")) {
                    throw new IllegalStateException("Not allowed mime type for web file: " + mimeType);
                }

                var builder = BaseInputBotInlineResult.builder()
                        .type(type)
                        .title(r.title().orElse(null))
                        .description(r.description().orElse(null))
                        .id(r.id())
                        .url(r.file());

                var contentBuilder = InputWebDocument.builder()
                        .url(r.file())
                        .size(0)
                        .attributes(List.of())
                        .mimeType(mimeType);

                switch (documentType) {
                    case PHOTO: {
                        contentBuilder.addAttribute(r.size().map(WebDocumentFields.Size::asData)
                                .orElseThrow(() -> new IllegalStateException("Size for photos must be set.")));
                        break;
                    }
                    case VIDEO:
                    case GIF: {
                        int duration = r.duration()
                                .map(Duration::getSeconds)
                                .map(Math::toIntExact)
                                .orElseThrow(() -> new IllegalStateException("Duration for video/gif documents must be set."));

                        var size = r.size().orElseThrow(() -> new IllegalStateException("Size for video/gif documents must be set."));

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
                                .title(r.title().orElse(null))
                                .performer(r.performer().orElse(null))
                                .build());
                        break;
                }

                r.filename().ifPresent(s -> contentBuilder.addAttribute(ImmutableDocumentAttributeFilename.of(s)));

                var content = contentBuilder.build();
                builder.content(content);

                r.thumb().map(EntityFactory::createThumbnail).ifPresentOrElse(builder::thumb, () -> {
                    if (documentType == DocumentType.GIF || documentType == DocumentType.VIDEO ||
                            documentType == DocumentType.PHOTO)
                        builder.thumb(content);
                });

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

        spec.size().map(WebDocumentFields.Size::asData).ifPresent(thumbBuilder::addAttribute);
        spec.filename().ifPresent(s -> thumbBuilder.addAttribute(ImmutableDocumentAttributeFilename.of(s)));

        return thumbBuilder.build();
    }

    private static Mono<InputBotInlineMessage> createInlineMessage(MTProtoTelegramClient client, InlineMessageSpec m) {
        var replyMarkup = Mono.justOrEmpty(m.replyMarkup())
                .flatMap(s -> s.asData(client));

        switch (m.type()) {
            case MEDIA_AUTO: {
                String c = m.message().orElseThrow().trim();
                var parser = m.parser()
                        .or(() -> client.getMtProtoResources().getDefaultEntityParser())
                        .map(t -> EntityParserSupport.parse(client, t.apply(c)))
                        .orElseGet(() -> Mono.just(Tuples.of(c, List.of())));

                return parser.map(TupleUtils.function((txt, ent) -> InputBotInlineMessageMediaAuto.builder()
                                .message(txt)
                                .entities(ent)))
                        .flatMap(builder -> replyMarkup.doOnNext(builder::replyMarkup)
                                .then(Mono.fromSupplier(builder::build)));
            }
            case TEXT:
                String c = m.message().orElseThrow().trim();
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
            case GAME:
                return Mono.just(ImmutableInputBotInlineMessageGame.of())
                        .flatMap(r -> replyMarkup.map(r::withReplyMarkup)
                                .defaultIfEmpty(r));
            case VENUE:
                var venue = m.venue().orElseThrow();
                return Mono.just(InputBotInlineMessageMediaVenue.builder()
                                .geoPoint(BaseInputGeoPoint.builder()
                                        .lat(venue.latitude())
                                        .longitude(venue.longitude())
                                        .accuracyRadius(venue.accuracyRadius().orElse(null))
                                        .build())
                                .title(venue.title())
                                .provider(venue.provider())
                                .venueType(venue.venueType())
                                .venueId(venue.venueId())
                                .address(venue.address()))
                        .flatMap(builder -> replyMarkup.doOnNext(builder::replyMarkup)
                                .then(Mono.fromSupplier(builder::build)));
            default: throw new IllegalStateException();
        }
    }
}