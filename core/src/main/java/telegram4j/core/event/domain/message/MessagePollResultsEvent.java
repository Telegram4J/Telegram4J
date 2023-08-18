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

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.media.Poll;
import telegram4j.core.object.media.PollResults;

import java.util.Optional;

/** Event of poll results. */
public final class MessagePollResultsEvent extends MessageEvent{

    private final @Nullable Poll poll;
    private final PollResults results;

    public MessagePollResultsEvent(MTProtoTelegramClient client, @Nullable Poll poll, PollResults results) {
        super(client);
        this.poll = poll;
        this.results = results;
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
     * Gets results of poll.
     *
     * @return The results of poll.
     */
    public PollResults getResults() {
        return results;
    }

    @Override
    public String toString() {
        return "MessagePollResultsEvent{" +
                "poll=" + poll +
                ", results=" + results +
                '}';
    }
}
