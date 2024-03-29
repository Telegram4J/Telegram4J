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
package telegram4j.core.object.chat;

import telegram4j.core.util.BitFlag;
import telegram4j.tl.ChatBannedRights;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static telegram4j.tl.ChatBannedRights.*;

public final class ChatRestrictions {
    private final ChatBannedRights data;

    public ChatRestrictions(ChatBannedRights data) {
        this.data = Objects.requireNonNull(data);
    }

    /**
     * Gets set with disallowed actions.
     *
     * @return The enum set with disallowed actions.
     */
    public Set<Right> getRights() {
        return Right.of(data);
    }

    /**
     * Gets timestamp until which restriction is active, if present otherwise restriction is active forever.
     *
     * @return The timestamp until which restriction is active, if present otherwise restriction is active forever.
     */
    public Optional<Instant> getUntilTimestamp() {
        if (data.untilDate() == 0) {
            return Optional.empty();
        }

        // TD:
        // https://github.com/tdlib/td/blob/92f8093486f19c049de5446cc20950e641c6ade0/td/telegram/DialogParticipant.cpp#L581-#L587
        //   if user is restricted for more than 366 days or less than 30 seconds from the current time,
        //   they are considered to be restricted forever
        Instant until = Instant.ofEpochSecond(data.untilDate());
        Instant now = Instant.now();
        if (until.isBefore(now.plusSeconds(30)) ||
                until.isAfter(now.plus(366, ChronoUnit.DAYS))) {
            return Optional.empty();
        }
        return Optional.of(until);
    }

    @Override
    public String toString() {
        return "ChatRestrictions{" +
                "data=" + data +
                '}';
    }

    /** An enumeration of permissions that describes disallowed actions. */
    public enum Right implements BitFlag {

        /** Disallows to view messages in a channel/chat. */
        VIEW_MESSAGES(VIEW_MESSAGES_POS),

        /** Disallows to send messages in a channel/chat. */
        SEND_MESSAGES(SEND_MESSAGES_POS),

        /** Disallows to send any media in a channel/chat. */
        SEND_MEDIA(SEND_MEDIA_POS),

        /** Disallows to send stickers in a channel/chat. */
        SEND_STICKERS(SEND_STICKERS_POS),

        /** Disallows to send gifs in a channel/chat. */
        SEND_GIFS(SEND_GIFS_POS),

        /** Disallows to send games in a channel/chat. */
        SEND_GAMES(SEND_GAMES_POS),

        /** Disallows to use inline bots in a channel/chat. */
        SEND_INLINE(SEND_INLINE_POS),

        /** Disallows to embed links in the messages of a channel/chat. */
        EMBED_LINKS(EMBED_LINKS_POS),

        /** Disallows to send polls in a channel/chat. */
        SEND_POLLS(SEND_POLLS_POS),

        /** Disallows to change the description in a channel/chat. */
        CHANGE_INFO(CHANGE_INFO_POS),

        /** Disallows to invite users in a channel/chat. */
        INVITE_USERS(INVITE_USERS_POS),

        /** Disallows to pin messages in a channel/chat. */
        PIN_MESSAGES(PIN_MESSAGES_POS),

        /** Disallows to create, edit and delete group chat topics. */
        MANAGE_TOPICS(MANAGE_TOPICS_POS),

        SEND_PHOTOS(SEND_PHOTOS_POS),
        SEND_VIDEOS(SEND_VIDEOS_POS),
        SEND_ROUND_VIDEOS(SEND_ROUNDVIDEOS_POS),
        SEND_AUDIOS(SEND_AUDIOS_POS),
        SEND_VOICES(SEND_VOICES_POS),
        SEND_DOCS(SEND_DOCS_POS),
        SEND_PLAIN(SEND_PLAIN_POS);

        private final byte position;

        Right(byte position) {
            this.position = position;
        }

        @Override
        public byte position() {
            return position;
        }

        /**
         * Computes mutable {@link Set} from raw {@link ChatBannedRights} data.
         *
         * @param data The raw chat banned rights data.
         * @return The mutable {@link Set} of the mapped {@link ChatBannedRights} flags.
         */
        public static Set<Right> of(ChatBannedRights data) {
            var set = EnumSet.allOf(Right.class);
            int flags = data.flags();
            set.removeIf(value -> (flags & value.mask()) == 0);
            return set;
        }
    }
}
