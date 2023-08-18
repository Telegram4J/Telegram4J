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

import telegram4j.core.object.chat.Channel;
import telegram4j.core.retriever.EntityRetriever;
import telegram4j.core.util.PeerId;
import telegram4j.tl.RestrictionReason;

import java.util.List;
import java.util.Optional;

/** Interface for {@link User} and {@link Channel} peers which can have username. */
public sealed interface MentionablePeer extends PeerEntity, Restrictable
        permits User, Channel {

    /**
     * Gets username of this user in format <b>username</b>, if present.
     * Can be used in the {@link EntityRetriever#resolvePeer(PeerId)}
     *
     * @return The username of this user, if present.
     */
    Optional<String> getUsername();

    // TODO: map bit-flags?
    /**
     * Gets list of another associated to this peer usernames, if present, otherwise will return empty list.
     *
     * @return The list of another associated to this peer usernames, if present, otherwise will return empty list.
     */
    List<telegram4j.tl.Username> getUsernames();

    /**
     * Gets the low quality peer photo, if present.
     *
     * @return The {@link ProfilePhoto photo} of peer, if present.
     */
    Optional<ProfilePhoto> getMinPhoto();

    /**
     * Gets the peer photo, if present
     * and if detailed information about peer is available.
     *
     * @return The {@link Photo photo} of peer, if present.
     */
    Optional<Photo> getPhoto();

    /**
     * Gets text of channel description or user about field, if present and
     * if detailed information about peer is available.
     *
     * @return The text of channel description or user about field, if present.
     */
    Optional<String> getAbout();

    /**
     * Gets list of reasons for why access to this peer must be restricted, if present.
     *
     * @return The list of reasons for why access to this peer must be restricted, if present.
     */
    @Override
    List<RestrictionReason> getRestrictionReasons();
}
