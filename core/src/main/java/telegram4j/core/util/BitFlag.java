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
package telegram4j.core.util;

/** Interface that flags enumerations must implement. */
public interface BitFlag {

    /**
     * Gets flag position, used in the {@link #mask()} as {@code 1 << position}.
     *
     * @return The flag shift position.
     */
    byte position();

    /**
     * Gets bit-mask for flag.
     *
     * @return The bit-mask for flag.
     */
    default int mask() {
        return 1 << position();
    }
}
