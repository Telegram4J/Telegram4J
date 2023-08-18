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
package telegram4j.core.spec.markup;

import telegram4j.core.object.markup.KeyboardButton;
import telegram4j.core.spec.Spec;
import telegram4j.tl.RequestPeerType;

public sealed interface RequestPeerSpec extends Spec<RequestPeerType>
        permits RequestUserSpec, RequestChatSpec, RequestChannelSpec {

    static RequestPeerSpec from(KeyboardButton.RequestPeer requestPeer) {
        if (requestPeer instanceof KeyboardButton.RequestUser u) {
            return RequestUserSpec.of(u.isBot().orElse(null), u.isPremium().orElse(null));
        } else if (requestPeer instanceof KeyboardButton.RequestChat c) {
            return RequestChatSpec.of(
                    c.isOwnedByUser(), c.isBotParticipant(), c.hasUsername().orElseThrow(), c.isForum().orElse(null),
                    c.getUserAdminRights().orElse(null), c.getBotAdminRights().orElse(null));
        } else if (requestPeer instanceof KeyboardButton.RequestChannel c) {
            return RequestChannelSpec.of(c.isOwnedByUser(), c.hasUsername().orElse(null),
                    c.getUserAdminRights().orElse(null), c.getBotAdminRights().orElse(null));
        } else {
            throw new IllegalStateException();
        }
    }

    @Override
    RequestPeerType resolve();
}
