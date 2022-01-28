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

import static telegram4j.mtproto.util.TlEntityUtil.unmapEmpty;

public class Message implements TelegramObject {

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

    public boolean isService() {
        return serviceData != null;
    }

    public EnumSet<Flag> getFlags() {
        return baseData != null ? Flag.of(baseData) : Flag.of(Objects.requireNonNull(serviceData));
    }

    public int getId() {
        return getBaseData().id();
    }

    public Optional<Id> getAuthorId() {
        return Optional.ofNullable(getBaseData().fromId()).map(Id::of);
    }

    public Id getChatId() {
        return resolvedChatId;
    }

    public Optional<MessageReplyHeader> getReplyTo() {
        return Optional.ofNullable(getBaseData().replyTo()).map(d -> new MessageReplyHeader(client, d));
    }

    public Instant getCreateTimestamp() {
        return Instant.ofEpochSecond(getBaseData().date());
    }

    public Optional<Duration> getAutoDeleteDuration() {
        return Optional.ofNullable(getBaseData().ttlPeriod()).map(Duration::ofSeconds);
    }

    // BaseMessage fields

    public Optional<MessageForwardHeader> getForwardedFrom() {
        return Optional.ofNullable(baseData)
                .map(BaseMessage::fwdFrom)
                .map(d -> new MessageForwardHeader(client, d));
    }

    public Optional<Long> getViaBotId() {
        return Optional.ofNullable(baseData).map(BaseMessage::viaBotId);
    }

    public Optional<String> getMessage() {
        return Optional.ofNullable(baseData).map(BaseMessage::message);
    }

    public Optional<MessageMedia> getMedia() {
        return Optional.ofNullable(baseData)
                .map(BaseMessage::media)
                .map(d -> EntityFactory.createMessageMedia(client, d, getId()));
    }

    public Optional<ReplyMarkup> getReplyMarkup() {
        return Optional.ofNullable(baseData)
                .map(BaseMessage::replyMarkup)
                .map(d -> EntityFactory.createReplyMarkup(client, d));
    }

    public Optional<List<MessageEntity>> getEntities() {
        return Optional.ofNullable(baseData)
                .map(BaseMessage::entities)
                .map(list -> list.stream()
                        .map(e -> new MessageEntity(client, e, baseData.message()))
                        .collect(Collectors.toList()));
    }

    public Optional<Integer> getViews() {
        return Optional.ofNullable(baseData).map(BaseMessage::views);
    }

    public Optional<Integer> getForwards() {
        return Optional.ofNullable(baseData).map(BaseMessage::forwards);
    }

    public Optional<MessageReplies> getReplies() {
        return Optional.ofNullable(baseData)
                .map(BaseMessage::replies)
                .map(d -> new MessageReplies(client, d));
    }

    public Optional<Instant> getEditTimestamp() {
        return Optional.ofNullable(baseData)
                .map(BaseMessage::editDate)
                .map(Instant::ofEpochSecond);
    }

    public Optional<String> getPostAuthor() {
        return Optional.ofNullable(baseData).map(BaseMessage::postAuthor);
    }

    public Optional<Long> getGroupedId() {
        return Optional.ofNullable(baseData).map(BaseMessage::groupedId);
    }

    public Optional<List<RestrictionReason>> getRestrictionReason() {
        return Optional.ofNullable(baseData)
                .map(BaseMessage::restrictionReason)
                .map(list -> list.stream()
                        .map(d -> new RestrictionReason(client, d))
                        .collect(Collectors.toList()));
    }

    // MessageService fields

    public Optional<MessageAction> getAction() {
        return Optional.ofNullable(serviceData)
                .map(e -> unmapEmpty(e.action(), telegram4j.tl.MessageAction.class))
                .map(e -> EntityFactory.createMessageAction(client, e,
                        getChatIdAsPeer(), getId()));
    }

    // Interaction methods

    public Mono<Message> edit(EditMessageSpec spec) {
        return Mono.defer(() -> {
            var text = spec.parser()
                    .or(() -> client.getMtProtoResources().getDefaultEntityParser())
                    .flatMap(parser -> spec.message().map(s -> EntityParserSupport.parse(parser.apply(s))))
                    .or(() -> spec.message().map(s -> Tuples.of(s, List.of())))
                    .orElse(null);

            return client.getServiceHolder().getMessageService()
                    .editMessage(EditMessage.builder()
                            .message(text != null ? text.getT1() : null)
                            .entities(text != null ? text.getT2() : null)
                            .noWebpage(spec.noWebpage())
                            .id(getId())
                            .scheduleDate(spec.scheduleTimestamp()
                                    .map(Instant::getEpochSecond)
                                    .map(Math::toIntExact)
                                    .orElse(null))
                            .replyMarkup(spec.replyMarkup().orElse(null))
                            .media(spec.media().orElse(null))
                            .peer(getChatIdAsPeer())
                            .build())
                    .map(e -> EntityFactory.createMessage(client, e, resolvedChatId));
        });
    }

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
        Message message = (Message) o;
        return getBaseData().equals(message.getBaseData()) && resolvedChatId.equals(message.resolvedChatId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getBaseData(), resolvedChatId);
    }

    @Override
    public String toString() {
        return "Message{" +
                "data=" + getBaseData() +
                ", resolvedChatId=" + resolvedChatId +
                "}";
    }

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

        public int getValue() {
            return value;
        }

        public int getFlag() {
            return flag;
        }

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
