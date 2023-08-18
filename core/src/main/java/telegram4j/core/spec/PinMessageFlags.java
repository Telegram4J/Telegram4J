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
package telegram4j.core.spec;

import telegram4j.core.util.BitFlag;
import telegram4j.tl.request.messages.UpdatePinnedMessage;

import static telegram4j.tl.request.messages.UpdatePinnedMessage.*;

/** An enumeration of {@link UpdatePinnedMessage} bit-flags. */
public enum PinMessageFlags implements BitFlag {
    /** Whether need to pin message silently, without triggering a notification. */
    SILENT(SILENT_POS),

    /** Whether message should be unpinned. */
    UNPIN(UNPIN_POS),

    /** Whether message should only be pinned for the <i>current</i> user in private messages. */
    PM_ONE_SIDE(PM_ONESIDE_POS);

    private final byte position;

    PinMessageFlags(byte position) {
        this.position = position;
    }

    @Override
    public byte position() {
        return position;
    }
}
