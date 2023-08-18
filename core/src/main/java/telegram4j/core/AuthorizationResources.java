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

import java.util.Objects;
import java.util.Optional;

/** Settings of user and bot auth resources. */
public final class AuthorizationResources {
    private final int apiId;
    private final String apiHash;
    @Nullable
    private final String botAuthToken;

    AuthorizationResources(int apiId, String apiHash) {
        this.apiId = apiId;
        this.apiHash = Objects.requireNonNull(apiHash);
        this.botAuthToken = null;
    }

    AuthorizationResources(int apiId, String apiHash, String botAuthToken) {
        this.apiId = apiId;
        this.apiHash = Objects.requireNonNull(apiHash);
        this.botAuthToken = Objects.requireNonNull(botAuthToken);
    }

    /**
     * Gets an <a href="https://core.telegram.org/api/obtaining_api_id#obtaining-api-id">api id</a> of application.
     *
     * @return An api id of application.
     */
    public int getApiId() {
        return apiId;
    }

    /**
     * Gets an <a href="https://core.telegram.org/api/obtaining_api_id#obtaining-api-id">api hash</a> of application.
     *
     * @return An api hash of application.
     */
    public String getApiHash() {
        return apiHash;
    }

    /**
     * Gets a bot auth token.
     *
     * @return A bot auth token.
     */
    public Optional<String> getBotAuthToken() {
        return Optional.ofNullable(botAuthToken);
    }

    /**
     * Gets whether <a href="https://core.telegram.org/bots">bot authorization</a> is used for connection,
     * otherwise <a href="https://core.telegram.org/api/auth">user authorization</a> must be implemented.
     *
     * @return {@code true} if it's bot authorization form.
     */
    public boolean isBot() {
        return botAuthToken != null;
    }
}
