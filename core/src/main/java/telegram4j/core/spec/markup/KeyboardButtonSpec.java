package telegram4j.core.spec.markup;

import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.internal.MappingUtil;
import telegram4j.core.object.markup.KeyboardButton;
import telegram4j.core.util.Id;
import telegram4j.tl.*;

public sealed interface KeyboardButtonSpec permits InlineButtonSpec, ReplyButtonSpec {

    static KeyboardButtonSpec from(KeyboardButton object) {
        return switch (object.getType()) {
            // Inline buttons
            case URL_AUTH -> InlineButtonSpec.urlAuth(object.getText(), object.isRequestWriteAccess(),
                    object.getForwardText().orElse(null),
                    object.getUrl().orElseThrow(), object.getBotId().orElseThrow());
            case BUY -> InlineButtonSpec.buy(object.getText());
            case CALLBACK -> InlineButtonSpec.callback(object.getText(), object.isRequiresPassword(),
                    object.getData().orElseThrow());
            case GAME -> InlineButtonSpec.game(object.getText());
            case SWITCH_INLINE -> InlineButtonSpec.switchInline(object.getText(),
                    object.isSamePeer(), object.getQuery().orElseThrow());
            case URL -> InlineButtonSpec.url(object.getText(), object.getUrl().orElseThrow());
            case USER_PROFILE -> InlineButtonSpec.userProfile(object.getText(), object.getUserId().orElseThrow());
            // Reply buttons
            case REQUEST_GEO_LOCATION, REQUEST_PHONE, DEFAULT -> new ReplyButtonSpec(object.getType(), object.getText());
            case REQUEST_POLL -> ReplyButtonSpec.requestPoll(object.getText(), object.isQuiz().orElse(null));
            case REQUEST_PEER -> ReplyButtonSpec.requestPeer(object.getText(), object.getButtonId().orElseThrow(),
                    RequestPeerSpec.from(object.getRequestPeer().orElseThrow()));
            // unsupported
            case WEB_VIEW, SIMPLE_WEB_VIEW -> throw new UnsupportedOperationException("Web view not yet supported");
        };
    }

    KeyboardButton.Type type();

    String text();

    default Mono<telegram4j.tl.KeyboardButton> asData(MTProtoTelegramClient client) {
        return Mono.defer(() -> switch (type()) {
            case URL_AUTH -> {
                var s = (InlineButtonSpec) this;
                Id userId = s.userId().orElseThrow();
                yield client.asInputUser(userId)
                        .switchIfEmpty(MappingUtil.unresolvedPeer(userId))
                        .map(id -> ImmutableInputKeyboardButtonUrlAuth.builder()
                                .text(text())
                                .fwdText(s.forwardText().orElse(null))
                                .requestWriteAccess(s.requestWriteAccess())
                                .bot(id)
                                .url(s.url().orElseThrow())
                                .build());
            }
            case DEFAULT -> Mono.just(ImmutableBaseKeyboardButton.of(text()));
            case BUY -> Mono.just(ImmutableKeyboardButtonBuy.of(text()));
            case URL -> {
                var s = (InlineButtonSpec) this;
                yield Mono.just(ImmutableKeyboardButtonUrl.of(text(), s.url().orElseThrow()));
            }
            case USER_PROFILE -> {
                var s = (InlineButtonSpec) this;
                Id userId = s.userId().orElseThrow();
                yield client.asInputUser(userId)
                        .switchIfEmpty(MappingUtil.unresolvedPeer(userId))
                        .map(id -> ImmutableInputKeyboardButtonUserProfile.of(text(), id));
            }
            case SWITCH_INLINE -> {
                var s = (InlineButtonSpec) this;
                yield Mono.just(ImmutableKeyboardButtonSwitchInline.of(
                        s.samePeer() ? ImmutableKeyboardButtonSwitchInline.SAME_PEER_MASK : 0,
                        text(), s.query().orElseThrow()));
            }
            case REQUEST_POLL -> {
                var s = (ReplyButtonSpec) this;
                yield Mono.just(ImmutableKeyboardButtonRequestPoll.of(text())
                        .withQuiz(s.quiz().orElse(null)));
            }
            case REQUEST_PHONE -> Mono.just(ImmutableKeyboardButtonRequestPhone.of(text()));
            case REQUEST_GEO_LOCATION -> Mono.just(ImmutableKeyboardButtonRequestGeoLocation.of(text()));
            case GAME -> Mono.from(Mono.just(ImmutableKeyboardButtonGame.of(text())));
            case CALLBACK -> {
                var s = (InlineButtonSpec) this;
                yield Mono.just(ImmutableKeyboardButtonCallback.of(
                        s.requiresPassword() ? ImmutableKeyboardButtonCallback.REQUIRES_PASSWORD_MASK : 0,
                        text(), s.data().orElseThrow()));
            }
            case REQUEST_PEER -> {
                var s = (ReplyButtonSpec) this;
                yield Mono.just(ImmutableKeyboardButtonRequestPeer.of(text(),
                        s.buttonId().orElseThrow(), s.requestPeer().map(RequestPeerSpec::asData).orElseThrow()));
            }
            // unsupported
            case WEB_VIEW, SIMPLE_WEB_VIEW -> throw new UnsupportedOperationException("Web view not yet supported");
        });
    }
}
