package telegram4j.core.spec;

import org.immutables.value.Value;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Id;
import telegram4j.core.object.markup.KeyboardButton;
import telegram4j.core.object.markup.ReplyMarkup;
import telegram4j.tl.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@FieldsStyle
@Value.Enclosing
public final class MessageFields {

    private MessageFields() {
    }

    @Value.Immutable(builder = false)
    public interface ReplyMarkupSpec extends Spec {

        static MessageFields.ReplyMarkupSpec from(ReplyMarkup object) {
            var spec = ImmutableMessageFields.ReplyMarkupSpec.of(object.getType());
            switch (object.getType()) {
                case KEYBOARD: {
                    var c = (telegram4j.core.object.markup.ReplyKeyboardMarkup) object;
                    spec = spec.withResize(c.isResize())
                            .withSingleUse(c.isSingleUse())
                            .withSelective(c.isSelective())
                            .withPlaceholder(c.getPlaceholder())
                            .withRows(c.getRows().stream()
                                    .map(r -> r.stream()
                                            .map(KeyboardButtonSpec::from)
                                            .collect(Collectors.toList()))
                                    .collect(Collectors.toList()));
                    break;
                }
                case INLINE: {
                    var c = (telegram4j.core.object.markup.ReplyInlineMarkup) object;
                    spec = spec.withRows(c.getRows().stream()
                            .map(r -> r.stream()
                                    .map(KeyboardButtonSpec::from)
                                    .collect(Collectors.toList()))
                            .collect(Collectors.toList()));
                    break;
                }
                case FORCE: {
                    var c = (telegram4j.core.object.markup.ReplyKeyboardForceReply) object;
                    spec = spec.withSingleUse(c.isSingleUse())
                            .withSelective(c.isSelective())
                            .withPlaceholder(c.getPlaceholder());
                    break;
                }
                case HIDE: {
                    var c = (telegram4j.core.object.markup.ReplyKeyboardHide) object;
                    spec = spec.withSelective(c.isSelective());
                    break;
                }
            }

            return spec;
        }

        static MessageFields.ReplyMarkupSpec inlineKeyboard(List<List<? extends InlineButtonSpec>> rows) {
            return ImmutableMessageFields.ReplyMarkupSpec.of(ReplyMarkup.Type.INLINE)
                    .withRows(rows);
        }

        static MessageFields.ReplyMarkupSpec forceReplyKeyboard(boolean singleUse, boolean selective) {
            return ImmutableMessageFields.ReplyMarkupSpec.of(ReplyMarkup.Type.FORCE)
                    .withSingleUse(singleUse)
                    .withSelective(selective);
        }

        static MessageFields.ReplyMarkupSpec hideKeyboard(boolean selective) {
            return ImmutableMessageFields.ReplyMarkupSpec.of(ReplyMarkup.Type.HIDE)
                    .withSelective(selective);
        }

        static MessageFields.ReplyMarkupSpec keyboard(boolean resize, boolean singleUse, boolean selective,
                                                      @Nullable String placeholder, List<List<? extends ReplyButtonSpec>> rows) {
            return ImmutableMessageFields.ReplyMarkupSpec.of(ReplyMarkup.Type.KEYBOARD)
                    .withResize(resize)
                    .withSingleUse(singleUse)
                    .withSelective(selective)
                    .withPlaceholder(Optional.ofNullable(placeholder))
                    .withRows(rows);
        }

        ReplyMarkup.Type type();

        Optional<List<? extends List<? extends KeyboardButtonSpec>>> rows();

        Optional<Boolean> singleUse();

        Optional<Boolean> selective();

        Optional<Boolean> resize();

        Optional<String> placeholder();

        default Mono<telegram4j.tl.ReplyMarkup> asData(MTProtoTelegramClient client) {
            return Mono.defer(() -> {
                switch (type()) {
                    case KEYBOARD:
                        return Flux.fromIterable(rows().orElseThrow())
                                .flatMap(list -> Flux.fromIterable(list)
                                        .flatMap(s -> s.asData(client))
                                        .collectList()
                                        .map(l -> KeyboardButtonRow.builder().buttons(l).build()))
                                .collectList()
                                .map(rows -> ReplyKeyboardMarkup.builder()
                                        .selective(selective().orElse(false))
                                        .singleUse(singleUse().orElse(false))
                                        .resize(resize().orElse(false))
                                        .placeholder(placeholder().orElse(null))
                                        .rows(rows)
                                        .build());
                    case HIDE: return Mono.just(ReplyKeyboardHide.builder().selective(selective().orElseThrow()).build());
                    case FORCE: return Mono.just(ReplyKeyboardForceReply.builder()
                            .selective(selective().orElseThrow())
                            .singleUse(singleUse().orElseThrow())
                            .placeholder(placeholder().orElse(null))
                            .build());
                    case INLINE:
                        return Flux.fromIterable(rows().orElseThrow())
                                .flatMap(list -> Flux.fromIterable(list)
                                        .flatMap(s -> s.asData(client))
                                        .collectList()
                                        .map(l -> KeyboardButtonRow.builder().buttons(l).build()))
                                .collectList()
                                .map(rows -> ReplyInlineMarkup.builder().rows(rows).build());
                    default:
                        return Mono.error(new IllegalStateException());
                }
            });
        }
    }

    public interface KeyboardButtonSpec extends Spec {

        static KeyboardButtonSpec from(KeyboardButton object) {
            switch (object.getType()) {
                // Inline buttons
                case URL_AUTH:
                    return InlineButtonSpec.urlAuth(object.getText(), object.isRequestWriteAccess().orElse(false),
                            object.getForwardText().orElse(null),
                            object.getUrl().orElseThrow(), object.getBotId().orElseThrow());
                case BUY: InlineButtonSpec.buy(object.getText());
                case CALLBACK:
                    return InlineButtonSpec.callback(object.getText(), object.isRequiresPassword().orElse(false),
                            object.getData().orElseThrow());
                case GAME: return InlineButtonSpec.game(object.getText());
                case SWITCH_INLINE: return InlineButtonSpec.switchInline(object.getText(),
                        object.isSamePeer().orElse(false), object.getQuery().orElseThrow());
                case URL: return InlineButtonSpec.url(object.getText(), object.getUrl().orElseThrow());
                case USER_PROFILE: return InlineButtonSpec.userProfile(object.getText(), object.getUserId().orElseThrow());
                // Reply buttons
                case REQUEST_GEO_LOCATION:
                case REQUEST_PHONE:
                case DEFAULT: return ImmutableMessageFields.ReplyButtonSpec.of(object.getType(), object.getText());
                case REQUEST_POLL: return ReplyButtonSpec.requestPoll(object.getText(), object.isQuiz().orElse(false));
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
                        return client.asInputUser(s.userId().orElseThrow())
                                .map(id -> ImmutableInputKeyboardButtonUrlAuth.of(text(), s.url().orElseThrow(), id)
                                        .withFwdText(s.forwardText().orElse(null))
                                        .withRequestWriteAccess(s.requestWriteAccess().orElseThrow()));
                    }
                    case DEFAULT:
                        return Mono.just(ImmutableBaseKeyboardButton.of(text()));
                    case BUY:
                        return Mono.just(ImmutableKeyboardButtonBuy.of(text()));
                    case URL: {
                        InlineButtonSpec s = (InlineButtonSpec) this;
                        return Mono.just(ImmutableKeyboardButtonUrl.of(text(), s.url().orElseThrow()));
                    }
                    case USER_PROFILE: {
                        InlineButtonSpec s = (InlineButtonSpec) this;
                        return client.asInputUser(s.userId().orElseThrow())
                                .map(id -> ImmutableInputKeyboardButtonUserProfile.of(text(), id));
                    }
                    case SWITCH_INLINE: {
                        InlineButtonSpec s = (InlineButtonSpec) this;
                        return Mono.just(ImmutableKeyboardButtonSwitchInline.of(text(), s.query().orElseThrow())
                                .withSamePeer(s.samePeer().orElseThrow()));
                    }
                    case REQUEST_POLL: {
                        ReplyButtonSpec s = (ReplyButtonSpec) this;
                        return Mono.just(ImmutableKeyboardButtonRequestPoll.of(text())
                                .withQuiz(s.quiz().orElse(null)));
                    }
                    case REQUEST_PHONE:
                        return Mono.just(ImmutableKeyboardButtonRequestPhone.of(text()));
                    case REQUEST_GEO_LOCATION:
                        return Mono.just(ImmutableKeyboardButtonRequestGeoLocation.of(text()));
                    case GAME:
                        return Mono.from(Mono.just(ImmutableKeyboardButtonGame.of(text())));
                    case CALLBACK: {
                        InlineButtonSpec s = (InlineButtonSpec) this;
                        return Mono.just(ImmutableKeyboardButtonCallback.of(text(), s.data().orElseThrow())
                                .withRequiresPassword(s.requiresPassword().orElse(false)));
                    }
                    default:
                        return Mono.error(new IllegalStateException());
                }
            });
        }
    }

    @Value.Immutable(builder = false)
    public interface ReplyButtonSpec extends KeyboardButtonSpec {

        static ReplyButtonSpec text(String text) {
            return ImmutableMessageFields.ReplyButtonSpec.of(KeyboardButton.Type.DEFAULT, text);
        }

        static ReplyButtonSpec requestGeoLocation(String text) {
            return ImmutableMessageFields.ReplyButtonSpec.of(KeyboardButton.Type.REQUEST_GEO_LOCATION, text);
        }

        static ReplyButtonSpec requestPhone(String text) {
            return ImmutableMessageFields.ReplyButtonSpec.of(KeyboardButton.Type.REQUEST_PHONE, text);
        }

        static ReplyButtonSpec requestPoll(String text, @Nullable Boolean quiz) {
            return ImmutableMessageFields.ReplyButtonSpec.of(KeyboardButton.Type.REQUEST_POLL, text)
                    .withQuiz(Optional.ofNullable(quiz));
        }

        Optional<Boolean> quiz();
    }

    @Value.Immutable(builder = false)
    public interface InlineButtonSpec extends KeyboardButtonSpec {

        static InlineButtonSpec buy(String text) {
            return ImmutableMessageFields.InlineButtonSpec.of(KeyboardButton.Type.BUY, text);
        }

        static InlineButtonSpec callback(String text, boolean requiresPassword, byte[] data) {
            return ImmutableMessageFields.InlineButtonSpec.of(KeyboardButton.Type.CALLBACK, text)
                    .withRequiresPassword(requiresPassword)
                    .withData(data);
        }

        static InlineButtonSpec userProfile(String text, Id userId) {
            return ImmutableMessageFields.InlineButtonSpec.of(KeyboardButton.Type.USER_PROFILE, text)
                    .withUserId(userId);
        }

        static InlineButtonSpec urlAuth(String text, boolean requestWriteAccess,
                                        @Nullable String forwardText, String url, Id botId) {
            return ImmutableMessageFields.InlineButtonSpec.of(KeyboardButton.Type.URL_AUTH, text)
                    .withRequestWriteAccess(requestWriteAccess)
                    .withForwardText(Optional.ofNullable(forwardText))
                    .withUrl(url)
                    .withBotId(botId);
        }

        static InlineButtonSpec game(String text) {
            return ImmutableMessageFields.InlineButtonSpec.of(KeyboardButton.Type.GAME, text);
        }

        static InlineButtonSpec switchInline(String text, boolean samePeer, String query) {
            return ImmutableMessageFields.InlineButtonSpec.of(KeyboardButton.Type.SWITCH_INLINE, text)
                    .withSamePeer(samePeer)
                    .withQuery(query);
        }

        static InlineButtonSpec url(String text, String url) {
            return ImmutableMessageFields.InlineButtonSpec.of(KeyboardButton.Type.URL, text)
                    .withUrl(url);
        }

        Optional<byte[]> data();

        Optional<String> query();

        Optional<String> url();

        Optional<String> forwardText();

        Optional<Boolean> requestWriteAccess();

        Optional<Boolean> requiresPassword();

        Optional<Boolean> samePeer();

        Optional<Id> userId();

        Optional<Id> botId();
    }
}
