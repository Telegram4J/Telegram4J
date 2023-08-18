/*
 * Copyright 2023 Telegram4J
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package telegram4j.core.spec.markup;

import reactor.util.annotation.Nullable;
import telegram4j.core.internal.MappingUtil;
import telegram4j.core.object.chat.AdminRight;
import telegram4j.core.util.ImmutableEnumSet;
import telegram4j.tl.ImmutableChatAdminRights;
import telegram4j.tl.ImmutableRequestPeerTypeBroadcast;
import telegram4j.tl.RequestPeerType;
import telegram4j.tl.RequestPeerTypeBroadcast;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;

public final class RequestChannelSpec implements RequestPeerSpec {
    // TODO: make singleton?

    private final boolean ownedByUser;
    @Nullable
    private final Boolean hasUsername;
    @Nullable
    private final ImmutableEnumSet<AdminRight> userAdminRights;
    @Nullable
    private final ImmutableEnumSet<AdminRight> botAdminRights;

    RequestChannelSpec(boolean ownedByUser, @Nullable Boolean hasUsername,
                       @Nullable Iterable<AdminRight> userAdminRights,
                       @Nullable Iterable<AdminRight> botAdminRights) {
        this.ownedByUser = ownedByUser;
        this.hasUsername = hasUsername;
        this.userAdminRights = userAdminRights != null ? ImmutableEnumSet.of(AdminRight.class, userAdminRights) : null;
        this.botAdminRights = botAdminRights != null ? ImmutableEnumSet.of(AdminRight.class, botAdminRights) : null;
    }

    RequestChannelSpec(boolean ownedByUser, @Nullable Boolean hasUsername,
                       @Nullable ImmutableEnumSet<AdminRight> userAdminRights,
                       @Nullable ImmutableEnumSet<AdminRight> botAdminRights) {
        this.ownedByUser = ownedByUser;
        this.hasUsername = hasUsername;
        this.userAdminRights = userAdminRights;
        this.botAdminRights = botAdminRights;
    }

    public boolean ownedByUser() {
        return ownedByUser;
    }

    public Optional<Boolean> hasUsername() {
        return Optional.ofNullable(hasUsername);
    }

    public Optional<ImmutableEnumSet<AdminRight>> userAdminRights() {
        return Optional.ofNullable(userAdminRights);
    }

    public Optional<ImmutableEnumSet<AdminRight>> botAdminRights() {
        return Optional.ofNullable(botAdminRights);
    }

    public static RequestChannelSpec of(boolean ownedByUser, @Nullable Boolean hasUsername,
                                        @Nullable Iterable<AdminRight> userAdminRight,
                                        @Nullable Iterable<AdminRight> botAdminRight) {
        return new RequestChannelSpec(ownedByUser, hasUsername, userAdminRight, botAdminRight);
    }

    public static RequestChannelSpec of() {
        return new RequestChannelSpec(false, null, null, null);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public ImmutableRequestPeerTypeBroadcast resolve() {
        return RequestPeerTypeBroadcast.builder()
                .creator(ownedByUser)
                .hasUsername(hasUsername)
                .userAdminRights(userAdminRights != null ? ImmutableChatAdminRights.of(userAdminRights.getValue()) : null)
                .botAdminRights(botAdminRights != null ? ImmutableChatAdminRights.of(botAdminRights.getValue()) : null)
                .build();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof RequestChannelSpec that)) return false;
        return ownedByUser == that.ownedByUser &&
                Objects.equals(hasUsername, that.hasUsername) &&
                Objects.equals(userAdminRights, that.userAdminRights) &&
                Objects.equals(botAdminRights, that.botAdminRights);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Boolean.hashCode(ownedByUser);
        h += (h << 5) + Objects.hashCode(hasUsername);
        h += (h << 5) + Objects.hashCode(userAdminRights);
        h += (h << 5) + Objects.hashCode(botAdminRights);
        return h;
    }

    @Override
    public String toString() {
        return "RequestChannelSpec{" +
                "ownedByUser=" + ownedByUser +
                ", hasUsername=" + hasUsername +
                ", userAdminRights=" + userAdminRights +
                ", botAdminRights=" + botAdminRights +
                '}';
    }

    public static class Builder {
        private boolean ownedByUser;
        @Nullable
        private Boolean hasUsername;
        @Nullable
        private EnumSet<AdminRight> userAdminRights;
        @Nullable
        private EnumSet<AdminRight> botAdminRights;

        private Builder() {}

        public Builder ownedByUser(boolean ownedByUser) {
            this.ownedByUser = ownedByUser;
            return this;
        }

        public Builder hasUsername(Optional<Boolean> opt) {
            return hasUsername(opt.orElse(null));
        }

        public Builder hasUsername(@Nullable Boolean hasUsername) {
            this.hasUsername = hasUsername;
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

        public RequestChannelSpec build() {
            return new RequestChannelSpec(ownedByUser, hasUsername, userAdminRights, botAdminRights);
        }
    }
}
