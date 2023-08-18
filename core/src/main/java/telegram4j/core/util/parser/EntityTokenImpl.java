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
import telegram4j.core.object.MessageEntity;

class EntityTokenImpl implements EntityToken {
    private final MessageEntity.Type type;
    private final int offset;
    @Nullable
    private final String arg;

    EntityTokenImpl(MessageEntity.Type type, int offset, @Nullable String arg) {
        this.type = type;
        this.offset = offset;
        this.arg = arg;
    }

    @Override
    public MessageEntity.Type type() {
        return type;
    }

    @Override
    public int position() {
        return offset;
    }

    @Nullable
    @Override
    public String arg() {
        return arg;
    }

    @Override
    public String toString() {
        return "EntityTokenImpl{" +
                "type=" + type +
                ", offset=" + offset +
                ", arg='" + arg + '\'' +
                '}';
    }
}
