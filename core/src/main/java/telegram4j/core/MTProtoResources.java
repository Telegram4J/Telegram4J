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
package telegram4j.core;

import reactor.util.annotation.Nullable;
import telegram4j.core.event.EventDispatcher;
import telegram4j.core.util.parser.EntityParserFactory;
import telegram4j.mtproto.store.StoreLayout;

import java.util.Objects;
import java.util.Optional;

/** Shared MTProto telegram client resources. */
public final class MTProtoResources {
    private final StoreLayout storeLayout;
    private final EventDispatcher eventDispatcher;
    @Nullable
    private final EntityParserFactory defaultEntityParser;

    MTProtoResources(StoreLayout storeLayout, EventDispatcher eventDispatcher,
                     @Nullable EntityParserFactory defaultEntityParser) {
        this.storeLayout = Objects.requireNonNull(storeLayout);
        this.eventDispatcher = Objects.requireNonNull(eventDispatcher);
        this.defaultEntityParser = defaultEntityParser;
    }

    /**
     * Gets the global entity storage.
     *
     * @return The {@link StoreLayout} entity storage.
     */
    public StoreLayout getStoreLayout() {
        return storeLayout;
    }

    /**
     * Gets the event dispatcher which distributes updates to subscribers.
     *
     * @return The {@link EventDispatcher} event dispatcher.
     */
    public EventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }

    /**
     * Gets the default factory of message entity parser, used if
     * a spec doesn't set own parser, if present.
     *
     * @return The factory of message entity parser, if present.
     */
    public Optional<EntityParserFactory> getDefaultEntityParser() {
        return Optional.ofNullable(defaultEntityParser);
    }
}
