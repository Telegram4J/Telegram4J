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
package telegram4j.core.event.domain.inline;

import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import reactor.util.function.Tuples;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.User;
import telegram4j.core.spec.EditMessageSpec;
import telegram4j.core.util.InlineMessageId;
import telegram4j.core.util.parser.EntityParserSupport;
import telegram4j.tl.request.messages.EditInlineBotMessage;

import java.util.List;

import static reactor.function.TupleUtils.function;

/**
 * Event of ordinary inline button triggered from the inline query.
 */
public final class InlineCallbackQueryEvent extends CallbackEvent {

    private final InlineMessageId messageId;

    public InlineCallbackQueryEvent(MTProtoTelegramClient client, long queryId,
                                    User user, long chatInstance, @Nullable ByteBuf data,
                                    @Nullable String gameShortName, InlineMessageId messageId) {
        super(client, queryId, user, chatInstance, data, gameShortName);
        this.messageId = messageId;
    }

    /**
     * Gets id of this inline message.
     *
     * @return The id of this inline message.
     */
    public InlineMessageId getMessageId() {
        return messageId;
    }

    /**
     * Request to edit inline message with given specification.
     * An attribute {@link EditMessageSpec#scheduleTimestamp()} will be ignored.
     *
     * @param spec The specification for editing inline message.
     * @return A {@link Mono} which emitting on completion boolean which displays completion state.
     */
    public Mono<Boolean> edit(EditMessageSpec spec) {
        return Mono.defer(() -> {
            var parsed = spec.parser()
                    .or(() -> client.getMtProtoResources().getDefaultEntityParser())
                    .flatMap(parser -> spec.message().map(s -> EntityParserSupport.parse(client, parser.apply(s.trim()))))
                    .orElseGet(() -> Mono.just(Tuples.of(spec.message().orElse(""), List.of())));

            var replyMarkup = Mono.justOrEmpty(spec.replyMarkup())
                    .flatMap(r -> r.asData(client));

            var media = Mono.justOrEmpty(spec.media())
                    .flatMap(r -> r.resolve(client));

            return Mono.fromSupplier(() -> EditInlineBotMessage.builder()
                            .noWebpage(spec.noWebpage())
                            .id(messageId.asData()))
                    .flatMap(builder -> parsed.map(function((txt, ent) -> builder
                                    .message(txt.isEmpty() ? null : txt)
                                    .entities(ent.isEmpty() ? null : ent))))
                    .flatMap(builder -> replyMarkup
                            .map(builder::replyMarkup)
                            .defaultIfEmpty(builder))
                    .flatMap(builder -> media
                            .map(builder::media)
                            .defaultIfEmpty(builder))
                    .flatMap(builder -> client.getServiceHolder()
                            .getChatService().editInlineBotMessage(builder.build()));
        });
    }

    @Override
    public String toString() {
        return "InlineCallbackQueryEvent{" +
                "messageId=" + messageId +
                "} " + super.toString();
    }
}
