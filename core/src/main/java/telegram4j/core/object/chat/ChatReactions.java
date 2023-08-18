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

import reactor.util.annotation.Nullable;
import telegram4j.core.object.Reaction;

import java.util.List;
import java.util.Optional;

public final class ChatReactions {
    private final boolean allowedCustom;
    @Nullable
    private final List<Reaction> allowed;

    public ChatReactions(boolean allowedCustom) {
        this.allowedCustom = allowedCustom;
        this.allowed = null;
    }

    public ChatReactions(@Nullable List<Reaction> allowed) {
        this.allowedCustom = false;
        this.allowed = allowed;
    }

    /**
     * Gets whether all custom emojis allowed.
     *
     * @return {@code true} if all custom emojis allowed,
     * otherwise {@code false} if {@link #getAllowed()} specified or custom emojis not allowed.
     */
    public boolean isAllowedCustom() {
        return allowed == null && allowedCustom;
    }

    /**
     * Gets immutable list of allowed reactions, if present.
     *
     * @return The immutable list of allowed reactions, if present.
     */
    public Optional<List<Reaction>> getAllowed() {
        return Optional.ofNullable(allowed);
    }

    @Override
    public String toString() {
        return "ChatReactions{" +
                (allowed != null ? "allowed=" + allowed : "allowedCustom=" + allowedCustom) +
                '}';
    }
}
