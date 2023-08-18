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
package telegram4j.mtproto.service;

import java.lang.annotation.*;

/**
 * Indicates service or method to compatible with different
 * account types. By default, if this annotation isn't present
 * method should consider as user-compatible.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Compatible {

    Type value();

    enum Type {
        /** Method compatible with any users. */
        USER,

        /** Method compatible with any bots. */
        BOT,

        /** Method compatible with both account types. */
        BOTH
    }
}
