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

import java.util.Objects;

abstract class BaseEntityParser implements EntityParser {

    final String str;
    final StringBuilder striped;

    int cursor;
    int offset; // offset without markup chars
    EntityToken prev = null;

    BaseEntityParser(String source) {
        this.str = Objects.requireNonNull(source);
        this.striped = new StringBuilder(source.length());
    }

    @Override
    public String source() {
        return str;
    }

    @Override
    public String striped() {
        if (cursor < str.length()) {
            throw new IllegalStateException("Parsing has not completed yet.");
        }

        return striped.toString();
    }

    @Override
    public abstract EntityToken nextToken();
}
