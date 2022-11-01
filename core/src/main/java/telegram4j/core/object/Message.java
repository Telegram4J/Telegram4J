package telegram4j.core.object;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import reactor.util.function.Tuples;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.internal.EntityFactory;
import telegram4j.core.internal.MappingUtil;
import telegram4j.core.object.chat.Channel;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.object.markup.ReplyMarkup;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.spec.EditMessageSpec;
import telegram4j.core.spec.PinMessageFlags;
import telegram4j.core.util.BitFlag;
import telegram4j.core.util.Id;
import telegram4j.core.util.parser.EntityParserSupport;
import telegram4j.tl.*;
import telegram4j.tl.messages.AffectedMessages;
import telegram4j.tl.request.messages.EditMessage;
import telegram4j.tl.request.messages.UpdatePinnedMessage;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static reactor.function.TupleUtils.function;
import static telegram4j.mtproto.util.TlEntityUtil.unmapEmpty;
import static telegram4j.tl.BaseMessage.*;

/**
 * Representation for default and service messages.
 */
public final class Message implements TelegramObject {

    private final MTProtoTelegramClient client;
    @Nullable
    private final BaseMessage baseData;
    @Nullable
    private final MessageService serviceData;
    private final Id resolvedChatId; // possibly chat id with access hash

    public Message(MTProtoTelegramClient client, MessageService serviceData, Id resolvedChatId) {
        this.client = Objects.requireNonNull(client);
        this.resolvedChatId = Objects.requireNonNull(resolvedChatId);
        this.baseData = null;
        this.serviceData = Objects.requireNonNull(serviceData);
    }

    public Message(MTProtoTelegramClient client, BaseMessage baseData, Id resolvedChatId) {
        this.client = Objects.requireNonNull(client);
        this.baseData = Objects.requireNonNull(baseData);
        this.resolvedChatId = Objects.requireNonNull(resolvedChatId);
        this.serviceData = null;
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    /**
     * Gets whether message is service notification.
     *
     * @return {@code true} if message is service notification.
     */
    public boolean isService() {
        return serviceData != null;
    }

    /**
     * Computes a mutable {@link Set} of the message flags.
     *
     * @return The mutable {@link Set} of the message flags.
     */
    public Set<Flag> getFlags() {
        return Flag.of0(getBaseData());
    }

    /**
     * Gets the <b>incremental</b> id of message for <i>current</i> user if message from DM or group chat or for channel.
     *
     * @return The id of message.
     */
    public int getId() {
        return getBaseData().id();
    }

    /**
     * Gets id of the message author, if present.
     *
     * @return The {@link Id} of the message author, if present.
     */
    public Optional<Id> getAuthorId() {
        return Optional.ofNullable(getBaseData().fromId()).map(Id::of);
    }

    /**
     * Requests to retrieve author of this message.
     *
     * @return An {@link Mono} emitting on successful completion the {@link User} or {@link Channel}.
     */
    public Mono<MentionablePeer> getAuthor() {
        return getAuthor(MappingUtil.IDENTITY_RETRIEVER);
    }

    /**
     * Requests to retrieve author of this message using specified retrieval strategy.
     *
     * @param strategy The strategy to apply.
     * @return An {@link Mono} emitting on successful completion the {@link User} or {@link Channel}.
     */
    public Mono<MentionablePeer> getAuthor(EntityRetrievalStrategy strategy) {
        return Mono.justOrEmpty(getAuthorId())
                .flatMap(id -> {
                    var retriever = client.withRetrievalStrategy(strategy);
                    switch (id.getType()) {
                        case CHANNEL: return retriever.getChatById(id);
                        case USER: return retriever.getUserById(id);
                        default: return Mono.error(new IllegalStateException());
                    }
                })
                .cast(MentionablePeer.class);
    }

    /**
     * Gets id of the chat, where message was sent.
     *
     * @return The {@link Id} of the chat, where message was sent.
     */
    public Id getChatId() {
        return resolvedChatId;
    }

    /**
     * Requests to retrieve chat where message was sent using specified retrieval strategy.
     *
     * @param strategy The strategy to apply.
     * @return An {@link Mono} emitting on successful completion the {@link Chat chat}.
     */
    public Mono<Chat> getChat(EntityRetrievalStrategy strategy) {
        return client.withRetrievalStrategy(strategy).getChatById(resolvedChatId);
    }

    /**
     * Requests to retrieve chat where message was sent.
     *
     * @return An {@link Mono} emitting on successful completion the {@link Chat chat}.
     */
    public Mono<Chat> getChat() {
        return client.getChatById(resolvedChatId);
    }

    /**
     * Gets header of the reply information, if present.
     *
     * @return The header of the reply information, if present.
     */
    public Optional<MessageReplyHeader> getReplyTo() {
        return Optional.ofNullable(getBaseData().replyTo())
                .map(d -> new MessageReplyHeader(client, d, resolvedChatId));
    }

    /**
     * Gets timestamp of the message creation.
     *
     * @return The timestamp of the message creation.
     */
    public Instant getCreateTimestamp() {
        return Instant.ofEpochSecond(getBaseData().date());
    }

    /**
     * Gets {@link Duration} of the message Time-To-Live, if present.
     *
     * @return The {@link Duration} of the message Time-To-Live, if present.
     */
    public Optional<Duration> getAutoDeleteDuration() {
        return Optional.ofNullable(getBaseData().ttlPeriod()).map(Duration::ofSeconds);
    }

    /**
     * Gets timestamp of the message auto-deletion, if present.
     *
     * @see #getAutoDeleteDuration()
     * @return The timestamp of the message auto-deletion, if present.
     */
    public Optional<Instant> getAutoDeleteTimestamp() {
        return getAutoDeleteDuration()
                .map(getCreateTimestamp()::plus);
    }

    // BaseMessage fields

    /**
     * Gets header of forward information, if message is not service and data present.
     *
     * @return The header of forward information, if message is not service and data present.
     */
    public Optional<MessageForwardHeader> getForwardedFrom() {
        return Optional.ofNullable(baseData)
                .map(BaseMessage::fwdFrom)
                .map(d -> new MessageForwardHeader(client, d));
    }

    /**
     * Gets id of an inline bot that generated this message, if present.
     *
     * @return The {@link Id} of an inline bot that generated this message, if present.
     */
    public Optional<Id> getViaBotId() {
        return Optional.ofNullable(baseData)
                .map(BaseMessage::viaBotId)
                .map(l -> Id.ofUser(l, null));
    }

    /**
     * Requests to retrieve inline bot that generated this message.
     *
     * @return An {@link Mono} emitting on successful completion the {@link User bot}.
     */
    public Mono<User> getViaBot() {
        return getViaBot(MappingUtil.IDENTITY_RETRIEVER);
    }

    /**
     * Requests to retrieve inline bot that generated this message using specified retrieval strategy.
     *
     * @param strategy The strategy to apply.
     * @return An {@link Mono} emitting on successful completion the {@link User bot}.
     */
    public Mono<User> getViaBot(EntityRetrievalStrategy strategy) {
        return Mono.justOrEmpty(getViaBotId())
                .flatMap(id -> client.withRetrievalStrategy(strategy).getUserById(id));
    }

    /**
     * Gets text of message, otherwise will return empty string.
     *
     * @return The raw text of message, or empty string.
     */
    public String getContent() {
        return baseData != null ? baseData.message() : "";
    }

    /**
     * Gets media of message, if message is not service and data present.
     *
     * @return The media of message, if present.
     */
    public Optional<MessageMedia> getMedia() {
        return Optional.ofNullable(baseData)
                .map(BaseMessage::media)
                .map(d -> EntityFactory.createMessageMedia(client, d,
                        getId(), client.asResolvedInputPeer(resolvedChatId)));
    }

    /**
     * Gets bot reply markup (e.g. keyboard), if present.
     *
     * @return The bot reply markup, if present.
     */
    public Optional<ReplyMarkup> getReplyMarkup() {
        return Optional.ofNullable(baseData)
                .map(BaseMessage::replyMarkup)
                .map(d -> new ReplyMarkup(client, d));
    }

    /**
     * Gets markup entities for {@link #getContent() text}, if message is not service and data present.
     *
     * @return The {@link List} of markup entities for {@link #getContent() text}, if present otherwise empty list.
     */
    public List<MessageEntity> getEntities() {
        return Optional.ofNullable(baseData)
                .map(BaseMessage::entities)
                .map(list -> {
                    var peer = client.asResolvedInputPeer(resolvedChatId);
                    return list.stream()
                            .map(e -> new MessageEntity(client, e, baseData.message(), getId(), peer))
                            .collect(Collectors.toList());
                })
                .orElse(List.of());
    }

    /**
     * Gets views count of channel post, if message is not service and data present.
     *
     * @return The views count of channel post, if present.
     */
    public Optional<Integer> getViews() {
        return Optional.ofNullable(baseData).map(BaseMessage::views);
    }

    /**
     * Gets forwards count, if message is not service and data present.
     *
     * @return The forwards count of message, if present.
     */
    public Optional<Integer> getForwards() {
        return Optional.ofNullable(baseData).map(BaseMessage::forwards);
    }

    /**
     * Gets information about message thread, if message is not service and data present.
     *
     * @return The {@link MessageReplies} of message, if present.
     */
    public Optional<MessageReplies> getReplies() {
        return Optional.ofNullable(baseData)
                .map(BaseMessage::replies)
                .map(d -> new MessageReplies(client, d));
    }

    /**
     * Gets timestamp of the last message editing, if message is not service and data present.
     *
     * @return The {@link Instant} of the last message editing, if present.
     */
    public Optional<Instant> getEditTimestamp() {
        return Optional.ofNullable(baseData)
                .map(BaseMessage::editDate)
                .map(Instant::ofEpochSecond);
    }

    /**
     * Gets display name of the channel post's author, if message is not service and name present.
     *
     * @return The display name of the channel post's author, if present.
     */
    public Optional<String> getPostAuthor() {
        return Optional.ofNullable(baseData).map(BaseMessage::postAuthor);
    }

    /**
     * Gets id of the group of multimedia/album, if message is not service and id present
     *
     * @return The id of the group of multimedia/album, if present.
     */
    public Optional<Long> getGroupedId() {
        return Optional.ofNullable(baseData).map(BaseMessage::groupedId);
    }

    /**
     * Gets immutable list of {@link RestrictionReason} for why access to this message must be restricted,
     * if message is not service and list present.
     *
     * @return The immutable list of the {@link RestrictionReason}, if present otherwise empty list.
     */
    public List<RestrictionReason> getRestrictionReason() {
        return Optional.ofNullable(baseData)
                .map(BaseMessage::restrictionReason)
                .orElse(List.of());
    }

    /**
     * Gets information about reactions, if message is not service and id present
     *
     * @return The information about reactions, if present.
     */
    public Optional<MessageReactions> getReactions() {
        return Optional.ofNullable(baseData)
                .map(BaseMessage::reactions)
                .map(d -> new MessageReactions(client, d));
    }

    // MessageService fields

    /**
     * Gets action about which the service message notifies, if message is service.
     *
     * @return The service message action, if present.
     */
    public Optional<MessageAction> getAction() {
        return Optional.ofNullable(serviceData)
                .map(e -> unmapEmpty(e.action(), telegram4j.tl.MessageAction.class))
                .map(e -> EntityFactory.createMessageAction(client, e,
                        // TODO: possible bug, access_hash can be absent
                        client.asResolvedInputPeer(resolvedChatId), getId()));
    }

    // Interaction methods

    /**
     * Requests to edit this message by specified edit specification.
     *
     * @param spec an immutable object that specifies how to edit the message.
     * @return A {@link Mono} emitting on successful completion updated message.
     */
    public Mono<Message> edit(EditMessageSpec spec) {
        return client.asInputPeer(resolvedChatId).switchIfEmpty(MappingUtil.unresolvedPeer(resolvedChatId)).flatMap(peer -> {
            var parsed = spec.parser()
                    .or(() -> client.getMtProtoResources().getDefaultEntityParser())
                    .flatMap(parser -> spec.message().map(s -> EntityParserSupport.parse(client, parser.apply(s.trim()))))
                    .orElseGet(() -> Mono.just(Tuples.of(spec.message().orElse(""), List.of())));

            var replyMarkup = Mono.justOrEmpty(spec.replyMarkup())
                    .flatMap(r -> r.asData(client));

            var media = Mono.justOrEmpty(spec.media())
                    .flatMap(r -> r.asData(client));

            var chatId = Id.of(peer, client.getSelfId());

            return parsed.map(function((txt, ent) -> EditMessage.builder()
                            .message(txt.isEmpty() ? null : txt)
                            .entities(ent.isEmpty() ? null : ent)
                            .noWebpage(spec.noWebpage())
                            .id(getId())
                            .scheduleDate(spec.scheduleTimestamp()
                                    .map(Instant::getEpochSecond)
                                    .map(Math::toIntExact)
                                    .orElse(null))
                            .peer(peer)))
                    .flatMap(builder -> replyMarkup.doOnNext(builder::replyMarkup)
                            .then(media.doOnNext(builder::media))
                            .then(Mono.fromSupplier(builder::build)))
                    .flatMap(request -> client.getServiceHolder()
                            .getChatService().editMessage(request))
                    .map(e -> EntityFactory.createMessage(client, e, chatId))
                    // maybe we edited the poll, so we need to request a fresh message
                    .switchIfEmpty(client.withRetrievalStrategy(EntityRetrievalStrategy.RPC)
                            .getMessagesById(chatId, List.of(ImmutableInputMessageID.of(getId())))
                            .map(c -> c.getMessages().get(0)));
        });
    }

    /**
     * Requests to delete this message.
     *
     * @param revoke Whether to delete messages for all participants of the chat. Will be ignored for channel messages.
     * @return A {@link Mono} emitting on successful completion range of message updates.
     */
    public Mono<AffectedMessages> delete(boolean revoke) {
        return Mono.defer(() -> {
            switch (resolvedChatId.getType()) {
                case CHAT:
                case USER: return client.deleteMessages(revoke, List.of(getId()));
                case CHANNEL: return client.deleteChannelMessages(resolvedChatId, List.of(getId()));
                default: throw new IllegalStateException();
            }
        });
    }

    /**
     * Requests to pin/unpin this message by specified parameters.
     *
     * @param flags The request flags.
     * @return A {@link Mono} emitting on successful completion nothing.
     */
    public Mono<Void> pin(Set<PinMessageFlags> flags) {
        return client.asInputPeer(resolvedChatId)
                .switchIfEmpty(MappingUtil.unresolvedPeer(resolvedChatId))
                .flatMap(peer -> client.getServiceHolder().getChatService()
                        .updatePinnedMessage(UpdatePinnedMessage.builder()
                                .peer(peer)
                                .id(getId())
                                .flags(MappingUtil.getMaskValue(flags))
                                .build()));
    }

    // Private methods

    private BaseMessageFields getBaseData() {
        return baseData != null ? baseData : Objects.requireNonNull(serviceData);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message that = (Message) o;
        return resolvedChatId.equals(that.resolvedChatId) &&
                getId() == that.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(resolvedChatId, getId());
    }

    @Override
    public String toString() {
        return "Message{data=" + getBaseData() + "}";
    }

    /** Available flag types of message. */
    public enum Flag implements BitFlag {

        // it's checked common flags:
        // https://github.com/telegramdesktop/tdesktop/blob/7a61693034b260084b2ce99463e394526c279df3/Telegram/SourceFiles/codegen/scheme/codegen_scheme.py#L39

        /** Is this an outgoing message. */
        OUT(OUT_POS),

        /** Whether we were <a href="https://core.telegram.org/api/mentions">mentioned</a> in this message. */
        MENTIONED(MENTIONED_POS),

        /** Whether there are unread media attachments in this message. */
        MEDIA_UNREAD(MEDIA_UNREAD_POS),

        /** Whether this is a silent message (no notification triggered). */
        SILENT(SILENT_POS),

        /** Whether this is a channel post. */
        POST(POST_POS),

        /** Whether this is a <a href="https://core.telegram.org/api/scheduled-messages">scheduled message</a>. */
        FROM_SCHEDULED(FROM_SCHEDULED_POS),

        /** This is a legacy message: it has to be refetched with the new layer. */
        LEGACY(LEGACY_POS),

        /**
         * Whether this message is <a href="https://telegram.org/blog/protected-content-delete-by-date-and-more">protected</a>
         * and thus cannot be forwarded.
         */
        NO_FORWARDS(NOFORWARDS_POS),

        /** Whether the message should be shown as not modified to the user, even if an edit date is present. */
        EDIT_HIDE(EDIT_HIDE_POS),

        /** Whether this message is <a href="https://core.telegram.org/api/pin">pinned</a>. */
        PINNED(PINNED_POS);

        private final byte position;

        Flag(byte position) {
            this.position = position;
        }

        @Override
        public byte position() {
            return position;
        }

        /**
         * Computes {@link Set} from raw message service data.
         *
         * @param data The message data.
         * @return The {@link Set} of the message service flags.
         */
        public static Set<Flag> of(telegram4j.tl.MessageService data) {
            return of0(data);
        }

        /**
         * Computes {@link Set} from raw message data.
         *
         * @param data The message service data.
         * @return The {@link Set} of the message flags.
         */
        public static Set<Flag> of(telegram4j.tl.BaseMessage data) {
            return of0(data);
        }

        private static Set<Flag> of0(telegram4j.tl.BaseMessageFields data) {
            var set = EnumSet.allOf(Flag.class);
            int flags = data.flags();
            set.removeIf(value -> (flags & value.mask()) == 0);
            return set;
        }
    }
}
