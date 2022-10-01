package telegram4j.core.spec;

import reactor.util.annotation.Nullable;
import telegram4j.core.object.BitFlag;
import telegram4j.core.util.ImmutableEnumSet;
import telegram4j.core.util.PeerId;
import telegram4j.tl.api.TlEncodingUtil;
import telegram4j.tl.request.messages.ForwardMessages;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class ForwardMessagesSpec {
    private final ImmutableEnumSet<Flag> flags;
    private final List<Integer> ids;
    @Nullable
    private final Instant scheduleTimestamp;
    @Nullable
    private final PeerId sendAs;

    private ForwardMessagesSpec(Iterable<Integer> ids) {
        this.ids = TlEncodingUtil.copyList(ids);
        this.flags = ImmutableEnumSet.of(Flag.class, 0);
        this.scheduleTimestamp = null;
        this.sendAs = null;
    }

    private ForwardMessagesSpec(Builder builder) {
        this.flags = ImmutableEnumSet.of(Flag.class, builder.flags);
        this.ids = List.copyOf(builder.ids);
        this.scheduleTimestamp = builder.scheduleTimestamp;
        this.sendAs = builder.sendAs;
    }

    private ForwardMessagesSpec(ImmutableEnumSet<Flag> flags, List<Integer> ids,
                                @Nullable Instant scheduleTimestamp, @Nullable PeerId sendAs) {
        this.flags = flags;
        this.ids = ids;
        this.scheduleTimestamp = scheduleTimestamp;
        this.sendAs = sendAs;
    }

    public ImmutableEnumSet<Flag> flags() {
        return flags;
    }

    public List<Integer> ids() {
        return ids;
    }

    public Optional<Instant> scheduleTimestamp() {
        return Optional.ofNullable(scheduleTimestamp);
    }

    public Optional<PeerId> sendAs() {
        return Optional.ofNullable(sendAs);
    }

    private ForwardMessagesSpec withFlags(Set<Flag> value) {
        Objects.requireNonNull(value);
        if (flags.equals(value)) return this;
        var newValue = ImmutableEnumSet.of(Flag.class, value);
        return new ForwardMessagesSpec(newValue, ids, scheduleTimestamp, sendAs);
    }

    private ForwardMessagesSpec withIds(Iterable<Integer> value) {
        Objects.requireNonNull(value);
        if (ids == value) return this;
        var newValue = TlEncodingUtil.copyList(value);
        return new ForwardMessagesSpec(flags, newValue, scheduleTimestamp, sendAs);
    }

    private ForwardMessagesSpec withIds(int... value) {
        var newValue = Arrays.stream(value)
                .boxed()
                .collect(Collectors.toUnmodifiableList());
        if (ids == newValue) return this;
        return new ForwardMessagesSpec(flags, newValue, scheduleTimestamp, sendAs);
    }

    public ForwardMessagesSpec withScheduleTimestamp(@Nullable Instant value) {
        if (scheduleTimestamp == value) return this;
        return new ForwardMessagesSpec(flags, ids, value, sendAs);
    }

    public ForwardMessagesSpec withScheduleTimestamp(Optional<Instant> opt) {
        return withScheduleTimestamp(opt.orElse(null));
    }

    public ForwardMessagesSpec withSendAs(@Nullable PeerId value) {
        if (sendAs == value) return this;
        return new ForwardMessagesSpec(flags, ids, scheduleTimestamp, value);
    }

    public ForwardMessagesSpec withSendAs(Optional<PeerId> opt) {
        return withSendAs(opt.orElse(null));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ForwardMessagesSpec)) return false;
        ForwardMessagesSpec other = (ForwardMessagesSpec) o;
        return flags.equals(other.flags)
                && ids.equals(other.ids)
                && Objects.equals(scheduleTimestamp, other.scheduleTimestamp)
                && Objects.equals(sendAs, other.sendAs);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + flags.hashCode();
        h += (h << 5) + Objects.hashCode(ids);
        h += (h << 5) + Objects.hashCode(scheduleTimestamp);
        h += (h << 5) + Objects.hashCode(sendAs);
        return h;
    }

    @Override
    public String toString() {
        return "ForwardMessagesSpec{" +
                "flags=" + flags +
                ", ids=" + ids +
                ", scheduleTimestamp=" + scheduleTimestamp +
                ", sendAs=" + sendAs +
                '}';
    }

    public static ForwardMessagesSpec of(Iterable<Integer> ids) {
        return new ForwardMessagesSpec(ids);
    }

    public static Builder builder() {
        return new ForwardMessagesSpec.Builder();
    }

    public static final class Builder {
        private EnumSet<Flag> flags;
        private List<Integer> ids;
        @Nullable
        private Instant scheduleTimestamp;
        @Nullable
        private PeerId sendAs;

        private Builder() {
        }

        public Builder from(ForwardMessagesSpec instance) {
            flags(instance.flags);
            ids(instance.ids);
            scheduleTimestamp(instance.scheduleTimestamp);
            sendAs(instance.sendAs);
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

        public Builder ids(Iterable<Integer> ids) {
            this.ids = StreamSupport.stream(ids.spliterator(), false)
                    .collect(Collectors.toList());
            return this;
        }

        public Builder addId(int id) {
            if (ids == null) ids = new ArrayList<>();
            ids.add(id);
            return this;
        }

        public Builder addIds(int... ids) {
            if (this.ids == null) {
                this.ids = Arrays.stream(ids)
                        .boxed()
                        .collect(Collectors.toList());
            } else {
                for (int id : ids) {
                    this.ids.add(id);
                }
            }
            return this;
        }

        public Builder addIds(Iterable<Integer> ids) {
            if (this.ids == null) {
                this.ids = StreamSupport.stream(ids.spliterator(), false)
                        .collect(Collectors.toList());
            } else {
                for (int id : ids) {
                    this.ids.add(id);
                }
            }
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

        public Builder sendAs(@Nullable PeerId sendAs) {
            this.sendAs = sendAs;
            return this;
        }

        public Builder sendAs(Optional<PeerId> sendAs) {
            this.sendAs = sendAs.orElse(null);
            return this;
        }

        public ForwardMessagesSpec build() {
            if (ids == null) {
                throw new IllegalStateException("Cannot build ForwardMessagesSpec, 'ids' attribute is not set");
            }
            return new ForwardMessagesSpec(this);
        }
    }

    public enum Flag implements BitFlag {
        SILENT(ForwardMessages.SILENT_POS),
        BACKGROUND(ForwardMessages.BACKGROUND_POS),
        MY_SCORE(ForwardMessages.WITH_MY_SCORE_POS),
        DROP_AUTHOR(ForwardMessages.DROP_AUTHOR_POS),
        DROP_MEDIA_CAPTIONS(ForwardMessages.DROP_MEDIA_CAPTIONS_POS),
        NO_FORWARDS(ForwardMessages.NOFORWARDS_POS);

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
