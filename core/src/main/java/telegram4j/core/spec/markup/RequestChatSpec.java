package telegram4j.core.spec.markup;

import reactor.util.annotation.Nullable;
import telegram4j.core.internal.MappingUtil;
import telegram4j.core.object.chat.AdminRight;
import telegram4j.core.util.ImmutableEnumSet;
import telegram4j.tl.ImmutableChatAdminRights;
import telegram4j.tl.ImmutableRequestPeerTypeChat;
import telegram4j.tl.RequestPeerTypeChat;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;

public final class RequestChatSpec implements RequestPeerSpec {
    // TODO: make singleton?

    private final boolean ownedByUser;
    private final boolean isBotParticipant;
    @Nullable
    private final Boolean hasUsername;
    @Nullable
    private final Boolean isForum;
    @Nullable
    private final ImmutableEnumSet<AdminRight> userAdminRights;
    @Nullable
    private final ImmutableEnumSet<AdminRight> botAdminRights;

    RequestChatSpec(boolean ownedByUser, boolean isBotParticipant, @Nullable Boolean hasUsername,
                    @Nullable Boolean isForum, @Nullable Iterable<AdminRight> userAdminRights,
                    @Nullable Iterable<AdminRight> botAdminRights) {
        this.ownedByUser = ownedByUser;
        this.isBotParticipant = isBotParticipant;
        this.hasUsername = hasUsername;
        this.isForum = isForum;
        this.userAdminRights = userAdminRights != null ? ImmutableEnumSet.of(AdminRight.class, userAdminRights) : null;
        this.botAdminRights = botAdminRights != null ? ImmutableEnumSet.of(AdminRight.class, botAdminRights) : null;
    }

    RequestChatSpec(boolean ownedByUser, boolean isBotParticipant, @Nullable Boolean hasUsername,
                    @Nullable Boolean isForum, @Nullable ImmutableEnumSet<AdminRight> userAdminRights,
                    @Nullable ImmutableEnumSet<AdminRight> botAdminRights) {
        this.ownedByUser = ownedByUser;
        this.isBotParticipant = isBotParticipant;
        this.hasUsername = hasUsername;
        this.isForum = isForum;
        this.userAdminRights = userAdminRights;
        this.botAdminRights = botAdminRights;
    }

    public boolean ownedByUser() {
        return ownedByUser;
    }

    public boolean isBotParticipant() {
        return isBotParticipant;
    }

    public Optional<Boolean> hasUsername() {
        return Optional.ofNullable(hasUsername);
    }

    public Optional<Boolean> isForum() {
        return Optional.ofNullable(isForum);
    }

    public Optional<ImmutableEnumSet<AdminRight>> userAdminRights() {
        return Optional.ofNullable(userAdminRights);
    }

    public Optional<ImmutableEnumSet<AdminRight>> botAdminRights() {
        return Optional.ofNullable(botAdminRights);
    }

    public static RequestChatSpec of(boolean ownedByUser, boolean isBotParticipant, @Nullable Boolean hasUsername,
                                     @Nullable Boolean isForum, @Nullable Iterable<AdminRight> userAdminRights,
                                     @Nullable Iterable<AdminRight> botAdminRights) {
        return new RequestChatSpec(ownedByUser, isBotParticipant, hasUsername, isForum, userAdminRights, botAdminRights);
    }

    public static RequestChatSpec of() {
        return new RequestChatSpec(false, false, null, null, null, null);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof RequestChatSpec that)) return false;
        return ownedByUser == that.ownedByUser && isBotParticipant == that.isBotParticipant &&
                Objects.equals(hasUsername, that.hasUsername) &&
                Objects.equals(isForum, that.isForum) &&
                Objects.equals(userAdminRights, that.userAdminRights) &&
                Objects.equals(botAdminRights, that.botAdminRights);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Boolean.hashCode(ownedByUser);
        h += (h << 5) + Boolean.hashCode(isBotParticipant);
        h += (h << 5) + Objects.hashCode(hasUsername);
        h += (h << 5) + Objects.hashCode(isForum);
        h += (h << 5) + Objects.hashCode(userAdminRights);
        h += (h << 5) + Objects.hashCode(botAdminRights);
        return h;
    }

    @Override
    public String toString() {
        return "RequestChatSpec{" +
                "ownedByUser=" + ownedByUser +
                ", isBotParticipant=" + isBotParticipant +
                ", hasUsername=" + hasUsername +
                ", isForum=" + isForum +
                ", userAdminRights=" + userAdminRights +
                ", botAdminRights=" + botAdminRights +
                '}';
    }

    @Override
    public ImmutableRequestPeerTypeChat resolve() {
        return RequestPeerTypeChat.builder()
                .creator(ownedByUser)
                .botParticipant(isBotParticipant)
                .hasUsername(hasUsername)
                .forum(isForum)
                .userAdminRights(userAdminRights != null ? ImmutableChatAdminRights.of(userAdminRights.getValue()) : null)
                .botAdminRights(botAdminRights != null ? ImmutableChatAdminRights.of(botAdminRights.getValue()) : null)
                .build();
    }

    public static class Builder {
        private boolean ownedByUser;
        private boolean isBotParticipant;
        @Nullable
        private Boolean hasUsername;
        @Nullable
        private Boolean isForum;
        @Nullable
        private EnumSet<AdminRight> userAdminRights;
        @Nullable
        private EnumSet<AdminRight> botAdminRights;

        private Builder() {}

        public Builder ownedByUser(boolean ownedByUser) {
            this.ownedByUser = ownedByUser;
            return this;
        }

        public Builder isBotParticipant(boolean isBotParticipant) {
            this.isBotParticipant = isBotParticipant;
            return this;
        }

        public Builder hasUsername(Optional<Boolean> opt) {
            return hasUsername(opt.orElse(null));
        }

        public Builder hasUsername(@Nullable Boolean hasUsername) {
            this.hasUsername = hasUsername;
            return this;
        }

        public Builder isForum(Optional<Boolean> opt) {
            return isForum(opt.orElse(null));
        }

        public Builder isForum(@Nullable Boolean isForum) {
            this.isForum = isForum;
            return this;
        }

        public Builder userAdminRights(Optional<? extends Iterable<AdminRight>> opt) {
            return userAdminRights(opt.orElse(null));
        }

        public Builder userAdminRights(@Nullable Iterable<AdminRight> iterable) {
            this.userAdminRights = iterable != null ? MappingUtil.copyAsEnumSet(AdminRight.class, iterable) : null;
            return this;
        }

        public Builder botAdminRights(Optional<? extends Iterable<AdminRight>> opt) {
            return botAdminRights(opt.orElse(null));
        }

        public Builder botAdminRights(@Nullable Iterable<AdminRight> iterable) {
            this.botAdminRights = iterable != null ? MappingUtil.copyAsEnumSet(AdminRight.class, iterable) : null;
            return this;
        }

        public RequestChatSpec build() {
            return new RequestChatSpec(ownedByUser, isBotParticipant, hasUsername, isForum,
                    userAdminRights, botAdminRights);
        }
    }
}
