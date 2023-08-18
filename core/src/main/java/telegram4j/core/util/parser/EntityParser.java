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
package telegram4j.core.util.parser;

import reactor.util.annotation.Nullable;

/**
 * Interface of <a href="https://core.telegram.org/type/MessageEntity">message entity</a> parser
 * that can parse markup token and strip original text.
 * Parsed and collected tokens must be sorted according to
 * the {@link EntityToken#type() type} of token and the distance to the nearest similar token
 * in <b>ascending</b> order.
 */
public interface EntityParser {

    /**
     * Gets the original text.
     *
     * @return The original text.
     */
    String source();

    /**
     * Gets the text cleared from markup.
     *
     * @throws IllegalStateException if the analysis is not finished yet
     * @return The text cleared from markup.
     */
    String striped();

    /**
     * Finds the next markup token.
     *
     * @return The next token of markup or {@code null} indicating end of input.
     */
    @Nullable
    EntityToken nextToken();
}
