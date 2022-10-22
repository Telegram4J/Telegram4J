package telegram4j.core.spec.markup;

import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.internal.MappingUtil;
import telegram4j.core.object.markup.KeyboardButton;
import telegram4j.core.util.Id;
import telegram4j.tl.*;

public interface KeyboardButtonSpec {

    static KeyboardButtonSpec from(KeyboardButton object) {
        switch (object.getType()) {
            // Inline buttons
            case URL_AUTH:
                return InlineButtonSpec.urlAuth(object.getText(), object.isRequestWriteAccess(),
                        object.getForwardText().orElse(null),
                        object.getUrl().orElseThrow(), object.getBotId().orElseThrow());
            case BUY: return InlineButtonSpec.buy(object.getText());
            case CALLBACK:
                return InlineButtonSpec.callback(object.getText(), object.isRequiresPassword(),
                        object.getData().orElseThrow());
            case GAME: return InlineButtonSpec.game(object.getText());
            case SWITCH_INLINE:
                return InlineButtonSpec.switchInline(object.getText(),
                        object.isSamePeer(), object.getQuery().orElseThrow());
            case URL: return InlineButtonSpec.url(object.getText(), object.getUrl().orElseThrow());
            case USER_PROFILE: return InlineButtonSpec.userProfile(object.getText(), object.getUserId().orElseThrow());
            // Reply buttons
            case REQUEST_GEO_LOCATION:
            case REQUEST_PHONE:
            case DEFAULT:
                return new ReplyButtonSpec(object.getType(), object.getText());
            case REQUEST_POLL: return ReplyButtonSpec.requestPoll(object.getText(), object.isQuiz().orElse(null));
            default: throw new IllegalStateException();
        }
    }

    KeyboardButton.Type type();

    String text();

    default Mono<telegram4j.tl.KeyboardButton> asData(MTProtoTelegramClient client) {
        return Mono.defer(() -> {
            switch (type()) {
                case URL_AUTH: {
                    InlineButtonSpec s = (InlineButtonSpec) this;
                    Id userId = s.userId().orElseThrow();
                    return client.asInputUser(userId)
                            .switchIfEmpty(MappingUtil.unresolvedPeer(userId))
                            .map(id -> ImmutableInputKeyboardButtonUrlAuth.builder()
                                    .text(text())
                                    .fwdText(s.forwardText().orElse(null))
                                    .requestWriteAccess(s.requestWriteAccess())
                                    .bot(id)
                                    .url(s.url().orElseThrow())
                                    .build());
                }
                case DEFAULT: return Mono.just(ImmutableBaseKeyboardButton.of(text()));
                case BUY: return Mono.just(ImmutableKeyboardButtonBuy.of(text()));
                case URL: {
                    InlineButtonSpec s = (InlineButtonSpec) this;
                    return Mono.just(ImmutableKeyboardButtonUrl.of(text(), s.url().orElseThrow()));
                }
                case USER_PROFILE: {
                    InlineButtonSpec s = (InlineButtonSpec) this;
                    Id userId = s.userId().orElseThrow();
                    return client.asInputUser(userId)
                            .switchIfEmpty(MappingUtil.unresolvedPeer(userId))
                            .map(id -> ImmutableInputKeyboardButtonUserProfile.of(text(), id));
                }
                case SWITCH_INLINE: {
                    InlineButtonSpec s = (InlineButtonSpec) this;
                    return Mono.just(ImmutableKeyboardButtonSwitchInline.of(
                            s.samePeer() ? ImmutableKeyboardButtonSwitchInline.SAME_PEER_MASK : 0,
                            text(), s.query().orElseThrow()));
                }
                case REQUEST_POLL: {
                    ReplyButtonSpec s = (ReplyButtonSpec) this;
                    return Mono.just(ImmutableKeyboardButtonRequestPoll.of(text())
                            .withQuiz(s.quiz().orElse(null)));
                }
                case REQUEST_PHONE: return Mono.just(ImmutableKeyboardButtonRequestPhone.of(text()));
                case REQUEST_GEO_LOCATION: return Mono.just(ImmutableKeyboardButtonRequestGeoLocation.of(text()));
                case GAME: return Mono.from(Mono.just(ImmutableKeyboardButtonGame.of(text())));
                case CALLBACK: {
                    InlineButtonSpec s = (InlineButtonSpec) this;
                    return Mono.just(ImmutableKeyboardButtonCallback.of(
                            s.requiresPassword() ? ImmutableKeyboardButtonCallback.REQUIRES_PASSWORD_MASK : 0,
                            text(), s.data().orElseThrow()));
                }
                // TODO: implement web view
                default: return Mono.error(new IllegalStateException("Unexpected button type: " + type()));
            }
        });
    }
}
