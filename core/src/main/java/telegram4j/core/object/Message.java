package telegram4j.core.object;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import reactor.util.function.Tuples;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.action.MessageAction;
import telegram4j.core.object.markup.ReplyMarkup;
import telegram4j.core.object.media.MessageMedia;
import telegram4j.core.spec.EditMessageSpec;
import telegram4j.core.util.EntityFactory;
import telegram4j.core.util.EntityParserSupport;
import telegram4j.tl.*;
import telegram4j.tl.messages.AffectedMessages;
import telegram4j.tl.request.messages.EditMessage;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static reactor.function.TupleUtils.function;
import static telegram4j.mtproto.util.TlEntityUtil.unmapEmpty;

/**
 * Representation for default and service messages.
 */
public final class Message implements TelegramObject {

    private final MTProtoTelegramClient client;
    @Nullable
    private final BaseMessage baseData;
    @Nullable
    private final MessageService serviceData;
    private final Id resolvedChatId;

    public Message(MTProtoTelegramClient client, MessageService serviceData, Id resolvedChatId) {
        this.client = Objects.requireNonNull(client, "client");
        this.resolvedChatId = Objects.requireNonNull(resolvedChatId, "resolvedChatId");
        this.baseData = null;
        this.serviceData = Objects.requireNonNull(serviceData, "serviceData");
    }

    public Message(MTProtoTelegramClient client, BaseMessage baseData, Id resolvedChatId) {
        this.client = Objects.requireNonNull(client, "client");
        this.baseData = Objects.requireNonNull(baseData, "baseData");
        this.resolvedChatId = Objects.requireNonNull(resolvedChatId, "resolvedChatId");
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
     * Computes {@link EnumSet} of the message flags.
     *
     * @return The {@link EnumSet} of the message flags.
     */
    public EnumSet<Flag> getFlags() {
        return Flag.of(getBaseData());
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
     * Gets id of the chat, where message was sent.
     *
     * @return The {@link Id} of the chat, where message was sent.
     */
    public Id getChatId() {
        return resolvedChatId;
    }

    /**
     * Gets header of the reply information, if present.
     *
     * @return The header of the reply information, if present.
     */
    public Optional<MessageReplyHeader> getReplyTo() {
        return Optional.ofNullable(getBaseData().replyTo()).map(d -> new MessageReplyHeader(client, d));
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
     * <p>
     * For getting auto-delete timestamp you can use following pattern:
     * <pre>
     *   Message message = ...;
     *
     *   Instant deleteTimestamp = message.getAutoDeleteDuration()
     *     .map(message.createTimestamp()::plus)
     *     .orElse(Instant.MIN);
     * </pre>
     *
     * @return The {@link Duration} of the message Time-To-Live, if present.
     */
    public Optional<Duration> getAutoDeleteDuration() {
        return Optional.ofNullable(getBaseData().ttlPeriod()).map(Duration::ofSeconds);
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
     * Gets id of an inline bot that generated this message, if message it's not service and id present.
     *
     * @return The {@link Id} of an inline bot that generated this message, if message it's not service and id present.
     */
    public Optional<Id> getViaBotId() {
        return Optional.ofNullable(baseData)
                .map(BaseMessage::viaBotId)
                .map(l -> Id.ofUser(l, null));
    }

    /**
     * Gets text of message, if it's not service.
     *
     * @return The raw text of message, if it's not service.
     */
    public Optional<String> getMessage() {
        return Optional.ofNullable(baseData).map(BaseMessage::message);
    }

    /**
     * Gets media of message, if message is not service and data present.
     *
     * @return The media of message, if message is not service and data present.
     */
    public Optional<MessageMedia> getMedia() {
        return Optional.ofNullable(baseData)
                .map(BaseMessage::media)
                .map(d -> EntityFactory.createMessageMedia(client, d,
                        getId(), getChatIdAsPeer()));
    }

    /**
     * Gets bot reply markup (e.g. keyboard), if message is not service and data present.
     *
     * @return The bot reply markup, if message is not service and data present.
     */
    public Optional<ReplyMarkup> getReplyMarkup() {
        return Optional.ofNullable(baseData)
                .map(BaseMessage::replyMarkup)
                .map(d -> EntityFactory.createReplyMarkup(client, d));
    }

    /**
     * Gets markup entities for {@link #getMessage() text}, if message is not service and data present.
     *
     * @return The {@link List} of markup entities for {@link #getMessage() text}, if message is not service and data present.
     */
    public Optional<List<MessageEntity>> getEntities() {
        return Optional.ofNullable(baseData)
                .map(BaseMessage::entities)
                .map(list -> list.stream()
                        .map(e -> new MessageEntity(client, e, baseData.message()))
                        .collect(Collectors.toList()));
    }

    /**
     * Gets views count of channel post, if message is not service and data present.
     *
     * @return The views count of channel post, if message is not service and data present.
     */
    public Optional<Integer> getViews() {
        return Optional.ofNullable(baseData).map(BaseMessage::views);
    }

    /**
     * Gets forwards count, if message is not service and data present.
     *
     * @return The forwards count of message, if it's not service and data present.
     */
    public Optional<Integer> getForwards() {
        return Optional.ofNullable(baseData).map(BaseMessage::forwards);
    }

    /**
     * Gets reply information, if message is not service and data present.
     *
     * @return The {@link MessageReplies} of message, if it's not service and data present.
     */
    public Optional<MessageReplies> getReplies() {
        return Optional.ofNullable(baseData)
                .map(BaseMessage::replies)
                .map(d -> new MessageReplies(client, d));
    }

    /**
     * Gets timestamp of the last message editing, if message is not service and data present.
     *
     * @return The {@link Instant} of the last message editing, if message is not service and data present.
     */
    public Optional<Instant> getEditTimestamp() {
        return Optional.ofNullable(baseData)
                .map(BaseMessage::editDate)
                .map(Instant::ofEpochSecond);
    }

    /**
     * Gets display name of the channel post's author, if message is not service and name present.
     *
     * @return The display name of the channel post's author, if message is not service and name present.
     */
    public Optional<String> getPostAuthor() {
        return Optional.ofNullable(baseData).map(BaseMessage::postAuthor);
    }

    /**
     * Gets id of the group of multimedia/album, if present.
     *
     * @return The id of the group of multimedia/album, if message is not service and id present.
     */
    public Optional<Long> getGroupedId() {
        return Optional.ofNullable(baseData).map(BaseMessage::groupedId);
    }

    /**
     * Gets {@link List} of {@link RestrictionReason} for why access to this message must be restricted,
     * if message is not service and list present.
     *
     * @return The {@link List} of the {@link RestrictionReason}, if message is not service and list present.
     */
    public Optional<List<RestrictionReason>> getRestrictionReason() {
        return Optional.ofNullable(baseData)
                .map(BaseMessage::restrictionReason)
                .map(list -> list.stream()
                        .map(d -> new RestrictionReason(client, d))
                        .collect(Collectors.toList()));
    }

    // MessageService fields

    /**
     * Gets action about which the service message notifies, if message is service.
     *
     * @return The service message action, if message is service.
     */
    public Optional<MessageAction> getAction() {
        return Optional.ofNullable(serviceData)
                .map(e -> unmapEmpty(e.action(), telegram4j.tl.MessageAction.class))
                .map(e -> EntityFactory.createMessageAction(client, e,
                        getChatIdAsPeer(), getId()));
    }

    // Interaction methods

    /**
     * Requests to edit this message by specified edit specification.
     *
     * @param spec an immutable object that specifies how to edit the message.
     * @return A {@link Mono} emitting on successful completion updated message.
     */
    public Mono<Message> edit(EditMessageSpec spec) {
        return Mono.defer(() -> {
            var parsed = spec.parser()
                    .or(() -> client.getMtProtoResources().getDefaultEntityParser())
                    .flatMap(parser -> spec.message().map(s -> EntityParserSupport.parse(client, parser.apply(s.trim()))))
                    .orElseGet(() -> Mono.just(Tuples.of(spec.message().orElse(""), List.of())));

            var replyMarkup = Mono.justOrEmpty(spec.replyMarkup())
                    .flatMap(r -> r.asData(client));

            var media = Mono.justOrEmpty(spec.media())
                    .flatMap(r -> r.asData(client));

            return parsed.map(function((txt, ent) -> EditMessage.builder()
                            .message(txt.isEmpty() ? null : txt)
                            .entities(ent.isEmpty() ? null : ent)
                            .noWebpage(spec.noWebpage())
                            .id(getId())
                            .scheduleDate(spec.scheduleTimestamp()
                                    .map(Instant::getEpochSecond)
                                    .map(Math::toIntExact)
                                    .orElse(null))
                            .peer(getChatIdAsPeer())))
                    .flatMap(builder -> replyMarkup.doOnNext(builder::replyMarkup)
                            .then(media.doOnNext(builder::media))
                            .then(Mono.fromSupplier(builder::build)))
                    .flatMap(editMessage -> client.getServiceHolder()
                            .getMessageService().editMessage(editMessage))
                    .map(e -> EntityFactory.createMessage(client, e, resolvedChatId));
        });
    }

    /**
     * Requests to delete this message.
     *
     * @param revoke Whether to delete messages for all participants of the chat.
     * @return A {@link Mono} emitting on successful completion updated message.
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

    // Private methods

    private InputPeer getChatIdAsPeer() {
        switch (getChatId().getType()) {
            case CHAT: return ImmutableInputPeerChat.of(getChatId().asLong());
            case CHANNEL: return ImmutableInputPeerChannel.of(getChatId().asLong(), getChatId().getAccessHash().orElseThrow());
            case USER: return ImmutableInputPeerUser.of(getChatId().asLong(), getChatId().getAccessHash().orElseThrow());
            default: throw new IllegalArgumentException("Unknown peer type: " + getChatId().getType());
        }
    }

    private BaseMessageFields getBaseData() {
        return baseData != null ? baseData : Objects.requireNonNull(serviceData);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message that = (Message) o;
        return getBaseData().id() == that.getBaseData().id();
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(getBaseData().id());
    }

    @Override
    public String toString() {
        return "Message{data=" + getBaseData() + "}";
    }

    /** Available flag types of message. */
    public enum Flag {

        // BaseMessage & ServiceMessage flags

        /** Is this an outgoing message. */
        OUT(1),

        /** Whether we were <a href="https://core.telegram.org/api/mentions">mentioned</a> in this message. */
        MENTIONED(4),

        /** Whether there are unread media attachments in this message. */
        MEDIA_UNREAD(5),

        /** Whether this is a silent message (no notification triggered). */
        SILENT(13),

        /** Whether this is a channel post. */
        POST(14),

        /** Whether this is a <a href="https://core.telegram.org/api/scheduled-messages">scheduled message</a>. */
        FROM_SCHEDULED(18),

        /** This is a legacy message: it has to be refetched with the new layer. */
        LEGACY(19),

        /** Whether the message should be shown as not modified to the user, even if an edit date is present. */
        EDIT_HIDE(21),

        /** Whether this message is <a href="https://core.telegram.org/api/pin">pinned</a>. */
        PINNED(24);

        private final int value;
        private final int flag;

        Flag(int value) {
            this.value = value;
            this.flag = 1 << value;
        }

        /**
         * Gets flag position, used in the {@link #getFlag()} as {@code 1 << position}.
         *
         * @return The flag shift position.
         */
        public int getValue() {
            return value;
        }

        /**
         * Gets bit-mask for flag.
         *
         * @return The bit-mask for flag.
         */
        public int getFlag() {
            return flag;
        }

        /**
         * Computes {@link EnumSet} from raw message data.
         *
         * @param data The message data.
         * @return The {@link EnumSet} of the message flags.
         */
        public static EnumSet<Flag> of(telegram4j.tl.Message data) {
            EnumSet<Flag> set = EnumSet.noneOf(Flag.class);
            if (data instanceof MessageEmpty) {
                return set;
            }

            int flags = data.flags();
            for (Flag value : values()) {
                if ((flags & value.flag) != 0) {
                    set.add(value);
                }
            }
            return set;
        }
    }
}
