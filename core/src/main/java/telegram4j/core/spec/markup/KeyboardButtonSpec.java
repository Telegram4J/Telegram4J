package telegram4j.core.spec.markup;

import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.markup.KeyboardButton;
import telegram4j.core.spec.Spec;
import telegram4j.tl.*;

public interface KeyboardButtonSpec extends Spec {

    static KeyboardButtonSpec from(KeyboardButton object) {
        switch (object.getType()) {
            // Inline buttons
            case URL_AUTH:
                return InlineButtonSpecDef.urlAuth(object.getText(), object.isRequestWriteAccess().orElse(false),
                        object.getForwardText().orElse(null),
                        object.getUrl().orElseThrow(), object.getBotId().orElseThrow());
            case BUY: return InlineButtonSpecDef.buy(object.getText());
            case CALLBACK:
                return InlineButtonSpecDef.callback(object.getText(), object.isRequiresPassword().orElse(false),
                        object.getData().orElseThrow());
            case GAME: return InlineButtonSpecDef.game(object.getText());
            case SWITCH_INLINE:
                return InlineButtonSpecDef.switchInline(object.getText(),
                        object.isSamePeer().orElse(false), object.getQuery().orElseThrow());
            case URL: return InlineButtonSpecDef.url(object.getText(), object.getUrl().orElseThrow());
            case USER_PROFILE: return InlineButtonSpecDef.userProfile(object.getText(), object.getUserId().orElseThrow());
            // Reply buttons
            case REQUEST_GEO_LOCATION:
            case REQUEST_PHONE:
            case DEFAULT: return ReplyButtonSpec.of(object.getType(), object.getText());
            case REQUEST_POLL: return ReplyButtonSpecDef.requestPoll(object.getText(), object.isQuiz().orElse(false));
            default: throw new IllegalStateException();
        }
    }

    KeyboardButton.Type type();

    String text();

    default Mono<telegram4j.tl.KeyboardButton> asData(MTProtoTelegramClient client) {
        return Mono.defer(() -> {
            switch (type()) {
                case URL_AUTH: {
                    InlineButtonSpecDef s = (InlineButtonSpecDef) this;
                    return client.asInputUser(s.userId().orElseThrow())
                            .map(id -> ImmutableInputKeyboardButtonUrlAuth.of(text(), s.url().orElseThrow(), id)
                                    .withFwdText(s.forwardText().orElse(null))
                                    .withRequestWriteAccess(s.requestWriteAccess().orElseThrow()));
                }
                case DEFAULT: return Mono.just(ImmutableBaseKeyboardButton.of(text()));
                case BUY: return Mono.just(ImmutableKeyboardButtonBuy.of(text()));
                case URL: {
                    InlineButtonSpecDef s = (InlineButtonSpecDef) this;
                    return Mono.just(ImmutableKeyboardButtonUrl.of(text(), s.url().orElseThrow()));
                }
                case USER_PROFILE: {
                    InlineButtonSpecDef s = (InlineButtonSpecDef) this;
                    return client.asInputUser(s.userId().orElseThrow())
                            .map(id -> ImmutableInputKeyboardButtonUserProfile.of(text(), id));
                }
                case SWITCH_INLINE: {
                    InlineButtonSpecDef s = (InlineButtonSpecDef) this;
                    return Mono.just(ImmutableKeyboardButtonSwitchInline.of(text(), s.query().orElseThrow())
                            .withSamePeer(s.samePeer().orElseThrow()));
                }
                case REQUEST_POLL: {
                    ReplyButtonSpecDef s = (ReplyButtonSpecDef) this;
                    return Mono.just(ImmutableKeyboardButtonRequestPoll.of(text())
                            .withQuiz(s.quiz().orElse(null)));
                }
                case REQUEST_PHONE: return Mono.just(ImmutableKeyboardButtonRequestPhone.of(text()));
                case REQUEST_GEO_LOCATION: return Mono.just(ImmutableKeyboardButtonRequestGeoLocation.of(text()));
                case GAME: return Mono.from(Mono.just(ImmutableKeyboardButtonGame.of(text())));
                case CALLBACK: {
                    InlineButtonSpecDef s = (InlineButtonSpecDef) this;
                    return Mono.just(ImmutableKeyboardButtonCallback.of(text())
                            .withData(s.data().orElseThrow())
                            .withRequiresPassword(s.requiresPassword().orElse(false)));
                }
                default: return Mono.error(new IllegalStateException());
            }
        });
    }
}
