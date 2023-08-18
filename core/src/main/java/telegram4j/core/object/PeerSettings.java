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
package telegram4j.core.object;

import telegram4j.core.util.BitFlag;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;

import static telegram4j.tl.PeerSettings.*;

public class PeerSettings {
    private final telegram4j.tl.PeerSettings data;

    public PeerSettings(telegram4j.tl.PeerSettings data) {
        this.data = Objects.requireNonNull(data);
    }

    public EnumSet<Flag> getFlags() {
        return Flag.of(data);
    }

    public Optional<Integer> getGeoDistance() {
        return Optional.ofNullable(data.geoDistance());
    }

    public Optional<String> getRequestChatTitle() {
        return Optional.ofNullable(data.requestChatTitle());
    }

    public Optional<Instant> getRequestChatTimestamp() {
        return Optional.ofNullable(data.requestChatDate())
                .map(Instant::ofEpochSecond);
    }

    @Override
    public String toString() {
        return "PeerSettings{" +
                "data=" + data +
                '}';
    }

    public enum Flag implements BitFlag {

        /** Whether we can still report the user for spam. */
        REPORT_SPAM(REPORT_SPAM_POS),

        /** Whether we can add the user as contact. */
        ADD_CONTACT(ADD_CONTACT_POS),

        /** Whether we can block the user. */
        BLOCK_CONTACT(BLOCK_CONTACT_POS),

        /** Whether we can share the user's contact. */
        SHARE_CONTACT(SHARE_CONTACT_POS),

        /** Whether a special exception for contacts is needed. */
        NEED_CONTACTS_EXCEPTION(NEED_CONTACTS_EXCEPTION_POS),

        /** Whether we can report a geogroup is irrelevant for this location. */
        REPORT_GEO(REPORT_GEO_POS),

        /** Whether this peer was automatically archived according to privacy settings. */
        AUTOARCHIVED(AUTOARCHIVED_POS),

        /** Whether we can invite members to a group or channel. */
        INVITE_MEMBERS(INVITE_MEMBERS_POS),

        /**
         * This flag is set if {@link #getRequestChatTitle()} and {@link #getRequestChatTimestamp()}
         * fields are set and the join request is related to a channel
         * (otherwise if only the request fields are set, the join request is related to a chat).
         *
         * @see <a href="https://core.telegram.org/api/invites#join-requests">Join Requests</a>
         */
        REQUEST_CHAT_BROADCAST(REQUEST_CHAT_BROADCAST_POS);

        private final byte position;

        Flag(byte position) {
            this.position = position;
        }

        @Override
        public byte position() {
            return position;
        }

        /**
         * Computes {@link EnumSet} from raw {@link telegram4j.tl.PeerSettings} data.
         *
         * @param data The message data.
         * @return The {@link EnumSet} of the {@link telegram4j.tl.PeerSettings} flags.
         */
        public static EnumSet<Flag> of(telegram4j.tl.PeerSettings data) {
            var set = EnumSet.allOf(Flag.class);
            int flags = data.flags();
            set.removeIf(value -> (flags & value.mask()) == 0);
            return set;
        }
    }
}
