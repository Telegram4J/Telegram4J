package telegram4j.core.internal;

import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.annotation.Nullable;
import reactor.util.function.Tuples;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.BotInfo;
import telegram4j.core.object.Document;
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
import telegram4j.core.object.chat.ChatReactions;
import telegram4j.core.object.chat.*;
import telegram4j.core.object.markup.KeyboardButton;
import telegram4j.core.object.media.*;
import telegram4j.core.spec.inline.*;
import telegram4j.core.util.Id;
import telegram4j.core.util.Variant2;
import telegram4j.core.util.parser.EntityParserSupport;
import telegram4j.mtproto.file.Context;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.mtproto.file.FileReferenceId.DocumentType;
import telegram4j.mtproto.file.StickerSetContext;
import telegram4j.mtproto.store.object.ChatData;
import telegram4j.mtproto.store.object.PeerData;
import telegram4j.tl.BaseChat;
import telegram4j.tl.Channel;
import telegram4j.tl.*;
import telegram4j.tl.api.TlObject;
import telegram4j.tl.channels.ChannelParticipant;
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

public class EntityFactory {

    private EntityFactory() {
    }

    public static UserStatus createUserStatus(telegram4j.tl.UserStatus data) {
        return switch (data.identifier()) {
            case UserStatusLastMonth.ID -> new UserStatus(UserStatus.Type.LAST_MONTH);
            case UserStatusLastWeek.ID -> new UserStatus(UserStatus.Type.LAST_WEEK);
            case UserStatusOffline.ID -> {
                var userStatusOffline = (UserStatusOffline) data;
                Instant wasOnlineTimestamp = Instant.ofEpochSecond(userStatusOffline.wasOnline());
                yield new UserStatus(UserStatus.Type.OFFLINE, null, wasOnlineTimestamp);
            }
            case UserStatusOnline.ID -> {
                var userStatusOnline = (UserStatusOnline) data;
                Instant expiresTimestamp = Instant.ofEpochSecond(userStatusOnline.expires());
                yield new UserStatus(UserStatus.Type.ONLINE, expiresTimestamp, null);
            }
            case UserStatusRecently.ID -> new UserStatus(UserStatus.Type.RECENTLY);
            // and UserStatusEmpty
            default -> throw new IllegalArgumentException("Unknown UserStatus type: " + data);
        };
    }

    public static Message createMessage(MTProtoTelegramClient client, telegram4j.tl.Message data, Id resolvedChatId) {
        return switch (data.identifier()) {
            case BaseMessage.ID -> new Message(client, Variant2.ofT1((BaseMessage) data), resolvedChatId);
            case MessageService.ID -> new Message(client, Variant2.ofT2((MessageService) data), resolvedChatId);
            default -> throw new IllegalArgumentException("Unknown Message type: " + data);
        };
    }

    public static Chat createChat(MTProtoTelegramClient client, PeerData<?, ?> data, @Nullable User selfUser) {
        if (data.minData instanceof BaseChat) {
            @SuppressWarnings("unchecked")
            var chatData = (ChatData<BaseChat, BaseChatFull>) data;

            if (chatData.fullData != null) {
                var usersMap = chatData.users.stream()
                        .map(d -> createUser(client, d))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(u -> u.getId().asLong(), Function.identity()));
                Id chatId = Id.ofChat(chatData.minData.id());
                var botInfo = Optional.ofNullable(chatData.fullData.botInfo())
                        .map(list -> list.stream()
                                .map(d -> new BotInfo(client, d, chatId,
                                        usersMap.get(Objects.requireNonNull(d.userId()))))
                                .collect(Collectors.toUnmodifiableList()))
                        .orElse(null);

                List<ChatParticipant> chatParticipants;
                switch (chatData.fullData.participants().identifier()) {
                    case BaseChatParticipants.ID -> {
                        var d = (BaseChatParticipants) chatData.fullData.participants();
                        chatParticipants = d.participants().stream()
                                .map(c -> new ChatParticipant(client, usersMap.get(c.userId()), c, chatId))
                                .collect(Collectors.toUnmodifiableList());
                    }
                    case ChatParticipantsForbidden.ID -> {
                        var d = (ChatParticipantsForbidden) chatData.fullData.participants();
                        chatParticipants = Optional.ofNullable(d.selfParticipant())
                                .map(c -> List.of(new ChatParticipant(client, usersMap.get(c.userId()), c, chatId)))
                                .orElse(null);
                    }
                    default -> throw new IllegalStateException("Unknown ChatParticipants type: " + chatData.fullData.participants());
                }

                return new GroupChat(client, chatData.fullData,
                        chatData.minData, chatParticipants, botInfo);
            }
            return new GroupChat(client, chatData.minData);
        } else if (data.minData instanceof BaseUser) {
            @SuppressWarnings("unchecked")
            var userData = (PeerData<BaseUser, telegram4j.tl.UserFull>) data;

            var user = new User(client, userData.minData, userData.fullData);
            return new PrivateChat(client, user, selfUser);
        } else if (data.minData instanceof Channel) {
            @SuppressWarnings("unchecked")
            var channelData = (ChatData<Channel, ChannelFull>) data;

            if (channelData.fullData != null) {
                var usersMap = channelData.users.stream()
                        .map(d -> createUser(client, d))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(u -> u.getId().asLong(), Function.identity()));
                Long acc = channelData.minData.min() ? null : channelData.minData.accessHash();
                Id channelId = Id.ofChannel(channelData.minData.id(), acc);
                var botInfo = channelData.fullData.botInfo().stream()
                        .map(d -> new BotInfo(client, d, channelId,
                                usersMap.get(Objects.requireNonNull(d.userId()))))
                        .collect(Collectors.toUnmodifiableList());

                return channelData.minData.broadcast()
                        ? new BroadcastChannel(client, channelData.fullData, channelData.minData, botInfo)
                        : new SupergroupChat(client, channelData.fullData, channelData.minData, botInfo);
            }

            return channelData.minData.broadcast()
                    ? new BroadcastChannel(client, channelData.minData)
                    : new SupergroupChat(client, channelData.minData);
        } else {
            throw new IllegalStateException();
        }
    }

    @Nullable
    public static Chat createChat(MTProtoTelegramClient client, TlObject possibleChat,
                                  @Nullable User selfUser) {
        return switch (possibleChat.identifier()) {
            case UserEmpty.ID, ChatEmpty.ID -> null;
            case ChatForbidden.ID -> switch (client.getMtProtoResources().getUnavailableChatPolicy()) {
                case THROWING -> throw UnavailableChatException.from((ChatForbidden) possibleChat);
                case NULL_MAPPING -> null;
            };
            case ChannelForbidden.ID -> switch (client.getMtProtoResources().getUnavailableChatPolicy()) {
                case THROWING -> throw UnavailableChatException.from((ChannelForbidden) possibleChat);
                case NULL_MAPPING -> null;
            };
            case UserFull.ID -> {
                var userFull = (UserFull) possibleChat;

                var minData = userFull.users().stream()
                        .filter(u -> u.identifier() == BaseUser.ID &&
                                u.id() == userFull.fullUser().id())
                        .map(u -> (BaseUser) u)
                        .findFirst()
                        .orElse(null);

                if (minData == null) {
                    yield null;
                }

                User mappedFullUser = new User(client, minData, userFull.fullUser());
                yield new PrivateChat(client, mappedFullUser, selfUser);
            }
            case BaseUser.ID -> {
                var baseUser = (BaseUser) possibleChat;
                User mappedMinUser = new User(client, baseUser, null);
                yield new PrivateChat(client, mappedMinUser, selfUser);
            }
            case BaseChat.ID -> {
                var baseChat = (BaseChat) possibleChat;
                yield new GroupChat(client, baseChat);
            }
            case Channel.ID -> {
                var channel = (Channel) possibleChat;
                if (channel.megagroup()) {
                    yield new SupergroupChat(client, channel);
                }
                yield new BroadcastChannel(client, channel);
            }
            case ChatFull.ID -> {
                var chatFull = (ChatFull) possibleChat;
                var minData = chatFull.chats().stream()
                        .filter(c -> c.id() == chatFull.fullChat().id())
                        .findFirst()
                        .map(c -> switch (c.identifier()) {
                            case ChatEmpty.ID -> null;
                            case ChatForbidden.ID -> switch (client.getMtProtoResources().getUnavailableChatPolicy()) {
                                case THROWING -> throw UnavailableChatException.from((ChatForbidden) possibleChat);
                                case NULL_MAPPING -> null;
                            };
                            case ChannelForbidden.ID -> switch (client.getMtProtoResources().getUnavailableChatPolicy()) {
                                case THROWING -> throw UnavailableChatException.from((ChannelForbidden) possibleChat);
                                case NULL_MAPPING -> null;
                            };
                            case BaseChat.ID, Channel.ID -> c;
                            default -> throw new IllegalArgumentException("Unknown Chat type: " + c);
                        })
                        .orElse(null);
                if (minData == null) {
                    yield null;
                }
                var usersMap = chatFull.users().stream()
                        .map(d -> createUser(client, d))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(u -> u.getId().asLong(), Function.identity()));
                if (chatFull.fullChat() instanceof ChannelFull channelFull) {
                    var channelMin = (Channel) minData;

                    Long acc = channelMin.min() ? null : channelMin.accessHash();
                    Id channelId = Id.ofChannel(channelMin.id(), acc);
                    var botInfo = channelFull.botInfo().stream()
                            .map(d -> new BotInfo(client, d, channelId,
                                    usersMap.get(Objects.requireNonNull(d.userId()))))
                            .collect(Collectors.toUnmodifiableList());

                    yield channelMin.megagroup()
                            ? new SupergroupChat(client, channelFull, channelMin, botInfo)
                            : new BroadcastChannel(client, channelFull, channelMin, botInfo);
                }
                var chat = (BaseChatFull) chatFull.fullChat();
                Id chatId = Id.ofChat(chat.id());
                var botInfo = Optional.ofNullable(chat.botInfo())
                        .map(list -> list.stream()
                                .map(d -> new BotInfo(client, d, chatId,
                                        usersMap.get(Objects.requireNonNull(d.userId()))))
                                .collect(Collectors.toUnmodifiableList()))
                        .orElse(null);
                List<ChatParticipant> chatParticipants;
                switch (chat.participants().identifier()) {
                    case BaseChatParticipants.ID -> {
                        var d = (BaseChatParticipants) chat.participants();
                        chatParticipants = d.participants().stream()
                                .map(c -> new ChatParticipant(client, usersMap.get(c.userId()), c, chatId))
                                .collect(Collectors.toUnmodifiableList());
                    }
                    case ChatParticipantsForbidden.ID -> {
                        var d = (ChatParticipantsForbidden) chat.participants();
                        chatParticipants = Optional.ofNullable(d.selfParticipant())
                                .map(c -> List.of(new ChatParticipant(client, usersMap.get(c.userId()), c, chatId)))
                                .orElse(null);
                    }
                    default -> throw new IllegalStateException("Unknown ChatParticipants type: " + chat.participants());
                }
                yield new GroupChat(client, chat, (BaseChat) minData, chatParticipants, botInfo);
            }
            default -> throw new IllegalArgumentException("Unknown Chat type: " + possibleChat);
        };
    }

    @Nullable
    public static User createUser(MTProtoTelegramClient client, telegram4j.tl.users.UserFull userFull) {
        var minData = (BaseUser) userFull.users().stream()
                .filter(u -> u.identifier() == BaseUser.ID &&
                        u.id() == userFull.fullUser().id())
                .findFirst()
                .orElse(null);

        if (minData == null) {
            return null;
        }
        return new User(client, minData, userFull.fullUser());
    }

    @Nullable
    public static User createUser(MTProtoTelegramClient client, telegram4j.tl.User anyUser) {
        return switch (anyUser.identifier()) {
            case UserEmpty.ID -> null;
            case BaseUser.ID -> new User(client, (BaseUser) anyUser, null);
            default -> throw new IllegalArgumentException("Unknown User type: " + anyUser);
        };
    }

    public static MessageAction createMessageAction(MTProtoTelegramClient client, telegram4j.tl.MessageAction data,
                                                    Id peer, int messageId) {
        return switch (data.identifier()) {
            case MessageActionBotAllowed.ID -> new MessageAction.BotAllowed(client, (MessageActionBotAllowed) data);
            case MessageActionChannelCreate.ID -> new MessageAction.ChannelCreate(client, (MessageActionChannelCreate) data);
            case MessageActionChannelMigrateFrom.ID -> new MessageAction.ChannelMigrateFrom(client, (MessageActionChannelMigrateFrom) data);
            case MessageActionChatAddUser.ID -> new MessageAction.ChatJoinUsers(client, (MessageActionChatAddUser) data);
            case MessageActionChatCreate.ID -> new MessageAction.ChatCreate(client, (MessageActionChatCreate) data);
            case MessageActionChatDeleteUser.ID -> new MessageAction.ChatLeftUser(client, (MessageActionChatDeleteUser) data);
            case MessageActionChatDeletePhoto.ID -> new MessageAction.UpdateChatPhoto(client);
            case MessageActionChatEditPhoto.ID -> new MessageAction.UpdateChatPhoto(client, (MessageActionChatEditPhoto) data,
                    Context.createChatPhotoContext(client.asResolvedInputPeer(peer), messageId));
            case MessageActionChatEditTitle.ID -> new MessageAction.ChatEditTitle(client, (MessageActionChatEditTitle) data);
            case MessageActionChatJoinedByLink.ID -> new MessageAction.ChatJoinedByLink(client, (MessageActionChatJoinedByLink) data);
            case MessageActionChatJoinedByRequest.ID -> new MessageAction(client, MessageAction.Type.CHAT_JOINED_BY_REQUEST);
            case MessageActionChatMigrateTo.ID -> new MessageAction.ChatMigrateTo(client, (MessageActionChatMigrateTo) data);
            case MessageActionContactSignUp.ID -> new MessageAction(client, MessageAction.Type.CONTACT_SIGN_UP);
            case MessageActionCustomAction.ID -> new MessageAction.Custom(client, (MessageActionCustomAction) data);
            case MessageActionGameScore.ID -> new MessageAction.GameScore(client, (MessageActionGameScore) data);
            case MessageActionGeoProximityReached.ID -> new MessageAction.GeoProximityReached(client, (MessageActionGeoProximityReached) data);
            case MessageActionGroupCall.ID -> new MessageAction.GroupCall(client, (MessageActionGroupCall) data);
            case MessageActionGroupCallScheduled.ID -> new MessageAction.GroupCallScheduled(client, (MessageActionGroupCallScheduled) data);
            case MessageActionHistoryClear.ID -> new MessageAction(client, MessageAction.Type.HISTORY_CLEAR);
            case MessageActionInviteToGroupCall.ID -> new MessageAction.InviteToGroupCall(client, (MessageActionInviteToGroupCall) data);
            case MessageActionPaymentSent.ID -> new MessageAction.PaymentSent(client, (MessageActionPaymentSent) data);
            case MessageActionPaymentSentMe.ID -> new MessageAction.PaymentSentMe(client, (MessageActionPaymentSentMe) data);
            case MessageActionPhoneCall.ID -> new MessageAction.PhoneCall(client, (MessageActionPhoneCall) data);
            case MessageActionPinMessage.ID -> new MessageAction(client, MessageAction.Type.PIN_MESSAGE);
            case MessageActionScreenshotTaken.ID -> new MessageAction(client, MessageAction.Type.SCREENSHOT_TAKEN);
            case MessageActionSecureValuesSent.ID -> new MessageAction.SecureValuesSent(client, (MessageActionSecureValuesSent) data);
            case MessageActionSecureValuesSentMe.ID -> new MessageAction.SecureValuesSentMe(client, (MessageActionSecureValuesSentMe) data);
            case MessageActionSetChatTheme.ID -> new MessageAction.SetChatTheme(client, (MessageActionSetChatTheme) data);
            case MessageActionSetMessagesTTL.ID -> new MessageAction.SetMessagesTtl(client, (MessageActionSetMessagesTTL) data);
            case MessageActionTopicCreate.ID -> new MessageAction.TopicCreate(client, (MessageActionTopicCreate) data);
            case MessageActionTopicEdit.ID -> new MessageAction.TopicEdit(client, (MessageActionTopicEdit) data);
            case MessageActionSuggestProfilePhoto.ID -> new MessageAction.SuggestProfilePhoto(client, (MessageActionSuggestProfilePhoto) data,
                    Context.createActionContext(peer.asPeer(), messageId));
            case MessageActionAttachMenuBotAllowed.ID -> new MessageAction(client, MessageAction.Type.ATTACH_MENU_BOT_ALLOWED);
            default -> throw new IllegalArgumentException("Unknown MessageAction type: " + data);
        };
    }

    public static MessageMedia createMessageMedia(MTProtoTelegramClient client, telegram4j.tl.MessageMedia data,
                                                  int messageId, Id peer) {
        return switch (data.identifier()) {
            case MessageMediaGeo.ID -> new MessageMedia.Geo(client, (MessageMediaGeo) data);
            case MessageMediaContact.ID -> new MessageMedia.Contact(client, (MessageMediaContact) data);
            case MessageMediaUnsupported.ID -> new MessageMedia(client, MessageMedia.Type.UNSUPPORTED);
            case MessageMediaPhoto.ID -> new MessageMedia.Document(client, (MessageMediaPhoto) data,
                    Context.createMediaContext(peer.asPeer(), messageId));
            case MessageMediaDocument.ID -> new MessageMedia.Document(client, (MessageMediaDocument) data,
                    Context.createMediaContext(peer.asPeer(), messageId));
            case MessageMediaWebPage.ID -> new MessageMedia.WebPage(client, (MessageMediaWebPage) data);
            case MessageMediaVenue.ID -> new MessageMedia.Venue(client, (MessageMediaVenue) data);
            case MessageMediaGame.ID -> new MessageMedia.Game(client, (MessageMediaGame) data,
                    Context.createMediaContext(peer.asPeer(), messageId));
            case MessageMediaInvoice.ID -> new MessageMedia.Invoice(client, (MessageMediaInvoice) data,
                    Context.createMediaContext(peer.asPeer(), messageId));
            case MessageMediaGeoLive.ID -> new MessageMedia.GeoLive(client, (MessageMediaGeoLive) data);
            case MessageMediaPoll.ID -> new MessageMedia.Poll(client, (MessageMediaPoll) data, peer);
            case MessageMediaDice.ID -> new MessageMedia.Dice(client, (MessageMediaDice) data);
            default -> throw new IllegalArgumentException("Unknown MessageMedia type: " + data);
        };
    }

    public static AnimatedThumbnail createAnimatedThumbnail(MTProtoTelegramClient client, VideoSize data) {
        return switch (data.identifier()) {
            case BaseVideoSize.ID -> new VideoThumbnail((BaseVideoSize) data);
            case VideoSizeEmojiMarkup.ID -> new StickerThumbnail(client, (VideoSizeEmojiMarkup) data);
            case VideoSizeStickerMarkup.ID -> new StickerThumbnail(client, (VideoSizeStickerMarkup) data);
            default -> throw new IllegalArgumentException("Unknown VideoSize type: " + data);
        };
    }

    public static Thumbnail createThumbnail(telegram4j.tl.PhotoSize data) {
        return switch (data.identifier()) {
            case BasePhotoSize.ID -> new PhotoThumbnail((BasePhotoSize) data);
            case PhotoCachedSize.ID -> new CachedThumbnail((PhotoCachedSize) data);
            case PhotoPathSize.ID -> new VectorThumbnail((PhotoPathSize) data);
            case PhotoSizeProgressive.ID -> new ProgressiveThumbnail((PhotoSizeProgressive) data);
            case PhotoStrippedSize.ID -> new StrippedThumbnail((PhotoStrippedSize) data);
            default -> throw new IllegalArgumentException("Unknown PhotoSize type: " + data);
        };
    }

    public static Document createDocument(MTProtoTelegramClient client, telegram4j.tl.BaseDocument data, Context context) {
        return createDocument0(client, data, context);
    }

    public static Document createDocument(MTProtoTelegramClient client, telegram4j.tl.WebDocument data, Context context) {
        return createDocument0(client, data, context);
    }

    private static Document createDocument0(MTProtoTelegramClient client, TlObject any, Context context) {
        var webDocument = any instanceof WebDocument w ? w : null;
        var baseDocument = any instanceof BaseDocument b ? b : null;

        boolean animated = false;
        boolean hasStickers = false;
        telegram4j.tl.DocumentAttributeVideo videoData = null;
        telegram4j.tl.DocumentAttributeAudio audioData = null;
        telegram4j.tl.DocumentAttributeSticker stickerData = null;
        telegram4j.tl.DocumentAttributeImageSize sizeData = null;
        telegram4j.tl.DocumentAttributeCustomEmoji emojiData = null;
        String fileName = null;
        var attrs = webDocument != null ? webDocument.attributes() : Objects.requireNonNull(baseDocument).attributes();
        for (var a : attrs) {
            switch (a.identifier()) {
                case DocumentAttributeCustomEmoji.ID -> emojiData = (DocumentAttributeCustomEmoji) a;
                case DocumentAttributeHasStickers.ID -> hasStickers = true;
                case DocumentAttributeAnimated.ID -> animated = true;
                case DocumentAttributeAudio.ID -> audioData = (DocumentAttributeAudio) a;
                case DocumentAttributeFilename.ID -> fileName = ((DocumentAttributeFilename) a).fileName();
                case DocumentAttributeImageSize.ID -> sizeData = (DocumentAttributeImageSize) a;
                case DocumentAttributeSticker.ID -> stickerData = (DocumentAttributeSticker) a;
                case DocumentAttributeVideo.ID -> videoData = (DocumentAttributeVideo) a;
                default -> throw new IllegalArgumentException("Unknown DocumentAttribute type: " + a);
            }
        }

        if (stickerData != null || emojiData != null) {
            var stickerInfo = Variant2.of(stickerData, emojiData);
            if (!(context instanceof StickerSetContext)) {
                // This context is more reliable, because even after deleting
                // the context message with sticker, sticker will remain available.
                // And this assignment doesn't contradict the concept of context, which
                // consist in providing 'the most reliable way to update a file reference'
                context = Context.createStickerSetContext(stickerInfo
                        .map(DocumentAttributeSticker::stickerset, DocumentAttributeCustomEmoji::stickerset));
            }
            return webDocument != null
                    ? new Sticker(client, webDocument, fileName, context, stickerInfo, Variant2.of(sizeData, videoData))
                    : new Sticker(client, baseDocument, fileName, context, stickerInfo, Variant2.of(sizeData, videoData));
        }

        if (audioData != null) {
            return webDocument != null
                    ? new Audio(client, webDocument, fileName, context, audioData)
                    : new Audio(client, baseDocument, fileName, context, audioData);
        }

        if (videoData != null) {
            return webDocument != null
                    ? new Video(client, webDocument, fileName, context, videoData, hasStickers, animated)
                    : new Video(client, baseDocument, fileName, context, videoData, hasStickers, animated);
        }

        if (sizeData != null) {
            return webDocument != null
                    ? new Photo(client, webDocument, fileName, context, sizeData)
                    : new Photo(client, baseDocument, fileName, context, sizeData);
        }
        return webDocument != null
                ? new Document(client, webDocument, fileName, context)
                : new Document(client, baseDocument, fileName, context);
    }

    public static Mono<InputBotInlineResult> createInlineResult(MTProtoTelegramClient client, InlineResultSpec spec) {
        Mono<InputBotInlineMessage> sendMessage = createInlineMessage(client, spec.message());

        if (spec instanceof InlineResultArticleSpec r) {

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
        } else if (spec instanceof InlineResultDocumentSpec r) {

            DocumentType documentType = r.type().orElse(DocumentType.GENERAL);
            String type = documentType == DocumentType.GENERAL ? "file" : documentType.name().toLowerCase(Locale.ROOT);

            String url = r.document().getT1().orElse(null);
            if (url != null) {
                String mimeType = r.mimeType()
                        .or(() -> documentType == DocumentType.PHOTO ? Optional.of("image/jpeg") : Optional.empty())
                        .orElseThrow(() -> new IllegalArgumentException("Mime type must be included with urls"));

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
                        .url(url);

                var contentBuilder = InputWebDocument.builder()
                        .url(url)
                        .size(0)
                        .attributes(List.of())
                        .mimeType(mimeType);

                switch (documentType) {
                    case PHOTO -> contentBuilder.addAttribute(r.size().map(WebDocumentFields.Size::asData)
                            .orElseThrow(() -> new IllegalStateException("Size for photos must be set.")));
                    case VIDEO, GIF -> {
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
                    }
                    case AUDIO, VOICE -> {
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
                    }
                }

                r.filename().ifPresent(s -> contentBuilder.addAttribute(ImmutableDocumentAttributeFilename.of(s)));

                var content = contentBuilder.build();
                builder.content(content);

                r.thumb().map(EntityFactory::createThumbnail).ifPresentOrElse(builder::thumb, () -> {
                    switch (documentType) {
                        case GIF, VIDEO, PHOTO -> builder.thumb(content);
                    }
                });

                return sendMessage.map(builder::sendMessage)
                        .map(ImmutableBaseInputBotInlineResult.Builder::build);
            }

            FileReferenceId fileRefId = r.document().getT2().orElseThrow();
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
        } else if (spec instanceof InlineResultGameSpec r) {

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
        return switch (reaction.identifier()) {
            case ReactionCustomEmoji.ID -> {
                var custom = (ReactionCustomEmoji) reaction;
                yield new Reaction(custom.documentId());
            }
            case ReactionEmoji.ID -> {
                var emoticon = (ReactionEmoji) reaction;
                yield new Reaction(emoticon.emoticon());
            }
            // and ReactionEmpty
            default -> throw new IllegalArgumentException("Unknown reaction type: " + reaction);
        };
    }

    @Nullable
    public static ChatReactions createChatReactions(telegram4j.tl.ChatReactions data) {
        return switch (data.identifier()) {
            case ChatReactionsAll.ID -> {
                var all = (ChatReactionsAll) data;
                yield new ChatReactions(all.allowCustom());
            }
            case ChatReactionsSome.ID -> {
                var some = (ChatReactionsSome) data;
                yield new ChatReactions(some.reactions().stream()
                        .map(EntityFactory::createReaction)
                        .collect(Collectors.toUnmodifiableList()));
            }
            case ChatReactionsNone.ID -> null;
            default -> throw new IllegalStateException("Unknown ChatReactions type: " + data);
        };
    }

    public static ChatParticipant createChannelParticipant(MTProtoTelegramClient client, ChannelParticipant p,
                                                           Id chatId, Id peerId) {
        MentionablePeer peer = switch (peerId.getType()) {
            case CHANNEL -> p.chats().stream()
                    .filter(u -> u.id() == peerId.asLong())
                    .findFirst()
                    .map(u -> (MentionablePeer) EntityFactory.createChat(client, u, null))
                    .orElse(null);
            case USER -> p.users().stream()
                    .filter(u -> u.id() == peerId.asLong())
                    .findFirst()
                    .map(u -> EntityFactory.createUser(client, u))
                    .orElse(null);
            default -> throw new IllegalStateException();
        };
        return new ChatParticipant(client, peer, p.participant(), chatId);
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

        return switch (m.type()) {
            case MEDIA_AUTO -> {
                String c = m.message().orElseThrow().trim();
                var parser = m.parser()
                        .or(() -> client.getMtProtoResources().getDefaultEntityParser())
                        .map(t -> EntityParserSupport.parse(client, t.apply(c)))
                        .orElseGet(() -> Mono.just(Tuples.of(c, List.of())));

                yield parser.map(TupleUtils.function((txt, ent) -> InputBotInlineMessageMediaAuto.builder()
                                .message(txt)
                                .entities(ent)))
                        .flatMap(builder -> replyMarkup.doOnNext(builder::replyMarkup)
                                .then(Mono.fromSupplier(builder::build)));
            }
            case TEXT -> {
                String c = m.message().orElseThrow().trim();
                var parser = m.parser()
                        .or(() -> client.getMtProtoResources().getDefaultEntityParser())
                        .map(t -> EntityParserSupport.parse(client, t.apply(c)))
                        .orElseGet(() -> Mono.just(Tuples.of(c, List.of())));
                yield parser.map(TupleUtils.function((txt, ent) -> InputBotInlineMessageText.builder()
                                .message(txt)
                                .entities(ent)
                                .noWebpage(m.noWebpage())))
                        .flatMap(builder -> replyMarkup.doOnNext(builder::replyMarkup)
                                .then(Mono.fromSupplier(builder::build)));
            }
            case GAME -> Mono.just(ImmutableInputBotInlineMessageGame.of())
                    .flatMap(r -> replyMarkup.map(r::withReplyMarkup)
                            .defaultIfEmpty(r));
            case VENUE -> {
                var venue = m.venue().orElseThrow();
                yield Mono.just(InputBotInlineMessageMediaVenue.builder()
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
            }
        };
    }

    public static KeyboardButton.RequestPeer createRequestPeer(RequestPeerType data) {
        return switch (data.identifier()) {
            case RequestPeerTypeBroadcast.ID -> new KeyboardButton.RequestChannel((RequestPeerTypeBroadcast) data);
            case RequestPeerTypeChat.ID -> new KeyboardButton.RequestChat((RequestPeerTypeChat) data);
            case RequestPeerTypeUser.ID -> new KeyboardButton.RequestUser((RequestPeerTypeUser) data);
            default -> throw new IllegalArgumentException("Unknown RequestPeerType type: " + data);
        };
    }
}
