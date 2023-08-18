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
package telegram4j.core.event.domain.message;

import io.netty.buffer.ByteBuf;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.PeerEntity;
import telegram4j.core.object.media.Poll;
import telegram4j.core.object.media.Poll.Flag;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Event of a new vote in the poll. */
public final class MessagePollVoteEvent extends MessageEvent {
    @Nullable
    private final Poll poll;
    private final PeerEntity peer;
    private final List<ByteBuf> options;

    public MessagePollVoteEvent(MTProtoTelegramClient client, @Nullable Poll poll, PeerEntity peer, List<ByteBuf> options) {
        super(client);
        this.poll = poll;
        this.peer = peer;
        this.options = options;
    }

    /**
     * Gets poll object, if present.
     *
     * @return The poll object, if present.
     */
    public Optional<Poll> getPoll() {
        return Optional.ofNullable(poll);
    }

    /**
     * Gets voted peer.
     *
     * @return The voted peer.
     */
    public PeerEntity getPeer() {
        return peer;
    }

    /**
     * Gets the options selected by the user.
     * Can contain more than one option if poll have {@link Flag#MULTIPLE_CHOICE} flag.
     *
     * @return The list of selected options.
     */
    public List<ByteBuf> getOptions() {
        return options.stream()
                .map(ByteBuf::duplicate)
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "MessagePollVoteEvent{" +
                "poll=" + poll +
                ", peer=" + peer +
                ", options=" + options +
                '}';
    }
}
