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

import telegram4j.core.util.BitFlag;

import java.util.EnumSet;
import java.util.Set;

import static telegram4j.tl.ChatAdminRights.*;

/** Enumeration of {@link telegram4j.tl.ChatAdminRights} bit-flags, that can be used in {@link EnumSet} */
public enum AdminRight implements BitFlag {

    /** Allows to modify the description of the channel/supergroup. */
    CHANGE_INFO(CHANGE_INFO_POS),

    /** Allows to post messages in the channel. */
    POST_MESSAGES(POST_MESSAGES_POS),

    /** Allows to also edit messages from other admins in the channel. */
    EDIT_MESSAGES(EDIT_MESSAGES_POS),

    /** Allows to also delete messages from other admins in the channel. */
    DELETE_MESSAGES(DELETE_MESSAGES_POS),

    /** Allows to ban users from the channel/supergroup */
    BAN_USERS(BAN_USERS_POS),

    /** Allows to invite users in the channel/supergroup. */
    INVITE_USERS(INVITE_USERS_POS),

    /** Allows to pin messages in the channel/supergroup. */
    PIN_MESSAGES(PIN_MESSAGES_POS),

    /** Allows to add other admins with the same (or more limited) permissions in the channel/supergroup. */
    ADD_ADMINS(ADD_ADMINS_POS),

    /** Allows the admin to remain anonymous. */
    ANONYMOUS(ANONYMOUS_POS),

    /** Allows to change group call/livestream settings. */
    MANAGE_CALL(MANAGE_CALL_POS),

    /** Set this flag if none of the other flags are set, but you still want the user to be an admin. */
    OTHER(OTHER_POS),

    /** Allows to create, edit and delete supergroup topics. */
    MANAGE_TOPICS(MANAGE_TOPICS_POS);

    private final byte position;

    AdminRight(byte position) {
        this.position = position;
    }

    @Override
    public byte position() {
        return position;
    }

    /**
     * Computes {@link EnumSet} from raw {@link telegram4j.tl.ChatAdminRights} data.
     *
     * @param chatAdminRights The chat admin rights data.
     * @return The {@link EnumSet} of the {@link telegram4j.tl.ChatAdminRights} flags.
     */
    public static Set<AdminRight> of(telegram4j.tl.ChatAdminRights chatAdminRights) {
        var set = EnumSet.allOf(AdminRight.class);
        int flags = chatAdminRights.flags();
        set.removeIf(value -> (flags & value.mask()) == 0);
        return set;
    }
}
