package telegram4j.core.spec.markup;

import telegram4j.core.object.markup.KeyboardButton;
import telegram4j.tl.RequestPeerType;

public sealed interface RequestPeerSpec
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

    RequestPeerType asData();
}
