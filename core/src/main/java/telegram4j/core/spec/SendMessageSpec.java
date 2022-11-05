package telegram4j.core.spec;

import reactor.util.annotation.Nullable;
import telegram4j.core.object.Message;
import telegram4j.core.spec.markup.ReplyMarkupSpec;
import telegram4j.core.spec.media.InputMediaSpec;
import telegram4j.core.util.BitFlag;
import telegram4j.core.util.Id;
import telegram4j.core.util.ImmutableEnumSet;
import telegram4j.core.util.PeerId;
import telegram4j.core.util.parser.EntityParserFactory;
import telegram4j.tl.request.messages.SendMessage;

import java.time.Instant;
import java.util.*;

public final class SendMessageSpec {
    private final ImmutableEnumSet<Flag> flags;
    @Nullable
    private final Integer replyToMessageId;
    @Nullable
    private final InputMediaSpec media;
    private final String message;
    @Nullable
    private final EntityParserFactory parser;
    @Nullable
    private final ReplyMarkupSpec replyMarkup;
    @Nullable
    private final Instant scheduleTimestamp;
    @Nullable
    private final PeerId sendAs;
    @Nullable
    private final Integer topMessageId;

    private SendMessageSpec(String message) {
        this.message = Objects.requireNonNull(message);
        this.flags = ImmutableEnumSet.of(Flag.class, 0);
        this.replyToMessageId = 0;
        this.parser = null;
        this.replyMarkup = null;
        this.scheduleTimestamp = null;
        this.sendAs = null;
        this.media = null;
        this.topMessageId = null;
    }

    private SendMessageSpec(Builder builder) {
        this.flags = ImmutableEnumSet.of(Flag.class, builder.flags);
        this.replyToMessageId = builder.replyToMessageId;
        this.message = builder.message;
        this.parser = builder.parser;
        this.replyMarkup = builder.replyMarkup;
        this.scheduleTimestamp = builder.scheduleTimestamp;
        this.sendAs = builder.sendAs;
        this.media = builder.media;
        this.topMessageId = builder.topMessageId;
    }

    private SendMessageSpec(ImmutableEnumSet<Flag> flags, @Nullable Integer replyToMessageId, String message,
                            @Nullable EntityParserFactory parser, @Nullable ReplyMarkupSpec replyMarkup,
                            @Nullable Instant scheduleTimestamp, @Nullable PeerId sendAs,
                            @Nullable InputMediaSpec media, @Nullable Integer topMessageId) {
        this.flags = flags;
        this.replyToMessageId = replyToMessageId;
        this.message = message;
        this.parser = parser;
        this.replyMarkup = replyMarkup;
        this.scheduleTimestamp = scheduleTimestamp;
        this.sendAs = sendAs;
        this.media = media;
        this.topMessageId = topMessageId;
    }

    public ImmutableEnumSet<Flag> flags() {
        return flags;
    }

    public Optional<Integer> replyToMessageId() {
        return Optional.ofNullable(replyToMessageId);
    }

    public String message() {
        return message;
    }

    public Optional<EntityParserFactory> parser() {
        return Optional.ofNullable(parser);
    }

    public Optional<ReplyMarkupSpec> replyMarkup() {
        return Optional.ofNullable(replyMarkup);
    }

    public Optional<Instant> scheduleTimestamp() {
        return Optional.ofNullable(scheduleTimestamp);
    }

    public Optional<PeerId> sendAs() {
        return Optional.ofNullable(sendAs);
    }

    public Optional<InputMediaSpec> media() {
        return Optional.ofNullable(media);
    }

    public Optional<Integer> getTopMessageId() {
        return Optional.ofNullable(topMessageId);
    }

    private SendMessageSpec withFlags(Iterable<Flag> value) {
        Objects.requireNonNull(value);
        if (flags.equals(value)) return this;
        var newValue = ImmutableEnumSet.of(Flag.class, value);
        return new SendMessageSpec(newValue, replyToMessageId, message, parser,
                replyMarkup, scheduleTimestamp, sendAs, media, topMessageId);
    }

    public SendMessageSpec withReplyTo(@Nullable Message value) {
        return withReplyToMessageId(value != null ? value.getId() : null);
    }

    public SendMessageSpec withReplyToMessageId(@Nullable Integer value) {
        if (Objects.equals(replyToMessageId, value)) return this;
        return new SendMessageSpec(flags, value, message, parser,
                replyMarkup, scheduleTimestamp, sendAs, media, topMessageId);
    }

    public SendMessageSpec withReplyToMessageId(Optional<Integer> opt) {
        return withReplyToMessageId(opt.orElse(null));
    }

    public SendMessageSpec withMessage(String value) {
        Objects.requireNonNull(value);
        if (message.equals(value)) return this;
        return new SendMessageSpec(flags, replyToMessageId, value, parser,
                replyMarkup, scheduleTimestamp, sendAs, media, topMessageId);
    }

    public SendMessageSpec withParser(@Nullable EntityParserFactory value) {
        if (parser == value) return this;
        return new SendMessageSpec(flags, replyToMessageId, message, value,
                replyMarkup, scheduleTimestamp, sendAs, media, topMessageId);
    }

    public SendMessageSpec withParser(Optional<? extends EntityParserFactory> opt) {
        return withParser(opt.orElse(null));
    }

    public SendMessageSpec withReplyMarkup(@Nullable ReplyMarkupSpec value) {
        if (replyMarkup == value) return this;
        return new SendMessageSpec(flags, replyToMessageId, message, parser,
                value, scheduleTimestamp, sendAs, media, topMessageId);
    }

    public SendMessageSpec withReplyMarkup(Optional<ReplyMarkupSpec> opt) {
        return withReplyMarkup(opt.orElse(null));
    }

    public SendMessageSpec withScheduleTimestamp(@Nullable Instant value) {
        if (scheduleTimestamp == value) return this;
        return new SendMessageSpec(flags, replyToMessageId, message, parser,
                replyMarkup, value, sendAs, media, topMessageId);
    }

    public SendMessageSpec withScheduleTimestamp(Optional<Instant> opt) {
        return withScheduleTimestamp(opt.orElse(null));
    }

    public SendMessageSpec withSendAs(@Nullable PeerId value) {
        if (sendAs == value) return this;
        return new SendMessageSpec(flags, replyToMessageId, message, parser,
                replyMarkup, scheduleTimestamp, value, media, topMessageId);
    }

    public SendMessageSpec withSendAs(@Nullable String value) {
        return withSendAs(value != null ? PeerId.of(value) : null);
    }

    public SendMessageSpec withSendAs(@Nullable Id value) {
        return withSendAs(value != null ? PeerId.of(value) : null);
    }

    public SendMessageSpec withSendAs(Optional<PeerId> opt) {
        return withSendAs(opt.orElse(null));
    }

    public SendMessageSpec withMedia(@Nullable InputMediaSpec value) {
        if (media == value) return this;
        return new SendMessageSpec(flags, replyToMessageId, message, parser,
                replyMarkup, scheduleTimestamp, sendAs, value, topMessageId);
    }

    public SendMessageSpec withMedia(Optional<? extends InputMediaSpec> opt) {
        return withMedia(opt.orElse(null));
    }

    public SendMessageSpec withTopMessage(@Nullable Message value) {
        return withTopMessageId(value != null ? value.getId() : null);
    }

    public SendMessageSpec withTopMessageId(@Nullable Integer value) {
        if (Objects.equals(topMessageId, value)) return this;
        return new SendMessageSpec(flags, replyToMessageId, message, parser,
                replyMarkup, scheduleTimestamp, sendAs, media, value);
    }

    public SendMessageSpec withTopMessageId(Optional<Integer> opt) {
        return withTopMessageId(opt.orElse(null));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SendMessageSpec)) return false;
        SendMessageSpec other = (SendMessageSpec) o;
        return flags.equals(other.flags)
                && Objects.equals(replyToMessageId, other.replyToMessageId)
                && message.equals(other.message)
                && Objects.equals(parser, other.parser)
                && Objects.equals(replyMarkup, other.replyMarkup)
                && Objects.equals(scheduleTimestamp, other.scheduleTimestamp)
                && Objects.equals(sendAs, other.sendAs)
                && Objects.equals(media, other.media)
                && Objects.equals(topMessageId, other.topMessageId);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + flags.hashCode();
        h += (h << 5) + Objects.hashCode(replyToMessageId);
        h += (h << 5) + message.hashCode();
        h += (h << 5) + Objects.hashCode(parser);
        h += (h << 5) + Objects.hashCode(replyMarkup);
        h += (h << 5) + Objects.hashCode(scheduleTimestamp);
        h += (h << 5) + Objects.hashCode(sendAs);
        h += (h << 5) + Objects.hashCode(media);
        h += (h << 5) + Objects.hashCode(topMessageId);
        return h;
    }

    @Override
    public String toString() {
        return "SendMessageSpec{" +
                "flags=" + flags +
                ", replyToMessageId=" + replyToMessageId +
                ", media=" + media +
                ", message='" + message + '\'' +
                ", parser=" + parser +
                ", replyMarkup=" + replyMarkup +
                ", scheduleTimestamp=" + scheduleTimestamp +
                ", sendAs=" + sendAs +
                ", topMessageId=" + topMessageId +
                '}';
    }

    public static SendMessageSpec of(String message) {
        return new SendMessageSpec(message);
    }

    public static Builder builder() {
        return new SendMessageSpec.Builder();
    }

    public static final class Builder {
        private Set<Flag> flags = EnumSet.noneOf(Flag.class);
        private Integer replyToMessageId;
        private String message;
        private EntityParserFactory parser;
        private ReplyMarkupSpec replyMarkup;
        private Instant scheduleTimestamp;
        private PeerId sendAs;
        private InputMediaSpec media;
        private Integer topMessageId;

        private Builder() {
        }

        public Builder from(SendMessageSpec instance) {
            flags(instance.flags);
            message(instance.message);
            replyToMessageId(instance.replyToMessageId);
            parser(instance.parser);
            replyMarkup(instance.replyMarkup);
            scheduleTimestamp(instance.scheduleTimestamp);
            sendAs(instance.sendAs);
            media(instance.media);
            topMessageId(instance.topMessageId);
            return this;
        }

        public Builder flags(Set<Flag> flags) {
            this.flags = EnumSet.copyOf(flags);
            return this;
        }

        public Builder addFlag(Flag flag) {
            flags.add(flag);
            return this;
        }

        public Builder addFlags(Flag... flags) {
            Collections.addAll(this.flags, flags);
            return this;
        }

        public Builder addFlags(Iterable<Flag> flags) {
            for (Flag flag : flags) {
                this.flags.add(flag);
            }
            return this;
        }

        public Builder replyTo(@Nullable Message message) {
            this.replyToMessageId = message != null ? message.getId() : null;
            return this;
        }

        public Builder replyToMessageId(@Nullable Integer replyToMessageId) {
            this.replyToMessageId = replyToMessageId;
            return this;
        }

        public Builder replyToMessageId(Optional<Integer> replyToMessageId) {
            this.replyToMessageId = replyToMessageId.orElse(null);
            return this;
        }

        public Builder message(String message) {
            this.message = Objects.requireNonNull(message);
            return this;
        }

        public Builder parser(@Nullable EntityParserFactory parser) {
            this.parser = parser;
            return this;
        }

        public Builder parser(Optional<? extends EntityParserFactory> parser) {
            this.parser = parser.orElse(null);
            return this;
        }

        public Builder replyMarkup(@Nullable ReplyMarkupSpec replyMarkup) {
            this.replyMarkup = replyMarkup;
            return this;
        }

        public Builder replyMarkup(Optional<ReplyMarkupSpec> replyMarkup) {
            this.replyMarkup = replyMarkup.orElse(null);
            return this;
        }

        public Builder scheduleTimestamp(@Nullable Instant scheduleTimestamp) {
            this.scheduleTimestamp = scheduleTimestamp;
            return this;
        }

        public Builder scheduleTimestamp(Optional<Instant> scheduleTimestamp) {
            this.scheduleTimestamp = scheduleTimestamp.orElse(null);
            return this;
        }

        public Builder sendAs(@Nullable Id sendAs) {
            this.sendAs = sendAs != null ? PeerId.of(sendAs) : null;
            return this;
        }

        public Builder sendAs(@Nullable String sendAs) {
            this.sendAs = sendAs != null ? PeerId.of(sendAs) : null;
            return this;
        }

        public Builder sendAs(@Nullable PeerId sendAs) {
            this.sendAs = sendAs;
            return this;
        }

        public Builder sendAs(Optional<PeerId> sendAs) {
            this.sendAs = sendAs.orElse(null);
            return this;
        }

        public Builder media(@Nullable InputMediaSpec media) {
            this.media = media;
            return this;
        }

        public Builder media(Optional<? extends InputMediaSpec> media) {
            this.media = media.orElse(null);
            return this;
        }

        public Builder topMessage(@Nullable Message message) {
            return topMessageId(message != null ? message.getId() : null);
        }

        public Builder topMessageId(@Nullable Integer topMessageId) {
            this.topMessageId = topMessageId;
            return this;
        }

        public Builder topMessageId(Optional<Integer> opt) {
            return topMessageId(opt.orElse(null));
        }

        public SendMessageSpec build() {
            if (message == null) {
                throw new IllegalStateException("Cannot build SendMessageSpec, 'message' attribute is not set");
            }
            return new SendMessageSpec(this);
        }
    }

    public enum Flag implements BitFlag {
        NO_WEBPAGE(SendMessage.NO_WEBPAGE_POS), // ignored with present 'media' attribute
        SILENT(SendMessage.SILENT_POS),
        BACKGROUND(SendMessage.BACKGROUND_POS),
        CLEAR_DRAFT(SendMessage.CLEAR_DRAFT_POS),
        NO_FORWARDS(SendMessage.NOFORWARDS_POS),
        UPDATE_STICKERS_ORDER(SendMessage.UPDATE_STICKERSETS_ORDER_POS);

        private final byte position;

        Flag(byte position) {
            this.position = position;
        }

        @Override
        public byte position() {
            return position;
        }
    }
}
