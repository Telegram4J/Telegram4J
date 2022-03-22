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

@FieldsStyle
@Value.Enclosing
public final class MessageFields {

    private MessageFields() {
    }

    @Value.Immutable(builder = false)
    public interface ReplyMarkupSpec extends Spec {

        static ReplyMarkupSpec inlineKeyboard(List<List<KeyboardButtonSpec>> rows) {
            return ImmutableMessageFields.ReplyMarkupSpec.of(ReplyMarkup.Type.INLINE)
                    .withRows(rows);
        }

        static ReplyMarkupSpec forceReplyKeyboard(boolean singleUse, boolean selective) {
            return ImmutableMessageFields.ReplyMarkupSpec.of(ReplyMarkup.Type.FORCE)
                    .withSingleUse(singleUse)
                    .withSelective(selective);
        }

        static ReplyMarkupSpec hideKeyboard(boolean selective) {
            return ImmutableMessageFields.ReplyMarkupSpec.of(ReplyMarkup.Type.HIDE)
                    .withSelective(selective);
        }

        static ReplyMarkupSpec keyboard(boolean resize, boolean singleUse, boolean selective,
                                        @Nullable String placeholder,
                                        List<List<KeyboardButtonSpec>> rows) {
            return ImmutableMessageFields.ReplyMarkupSpec.of(ReplyMarkup.Type.DEFAULT)
                    .withResize(resize)
                    .withSingleUse(singleUse)
                    .withSelective(selective)
                    .withPlaceholder(Optional.ofNullable(placeholder))
                    .withRows(rows);
        }

        ReplyMarkup.Type type();

        Optional<List<List<KeyboardButtonSpec>>> rows();

        Optional<Boolean> singleUse();

        Optional<Boolean> selective();

        Optional<Boolean> resize();

        Optional<String> placeholder();

        default Mono<telegram4j.tl.ReplyMarkup> asData(MTProtoTelegramClient client) {
            return Mono.defer(() -> {
                switch (type()) {
                    case DEFAULT:
                        return Flux.fromIterable(rows().orElseThrow())
                                .flatMap(list -> Flux.fromIterable(list)
                                        .flatMap(s -> s.asData(client))
                                        .collectList()
                                        .map(l -> KeyboardButtonRow.builder()
                                                .buttons(l)
                                                .build()))
                                .collectList()
                                .map(rows -> ReplyKeyboardMarkup.builder()
                                        .selective(selective().orElse(false))
                                        .singleUse(singleUse().orElse(false))
                                        .resize(resize().orElse(false))
                                        .placeholder(placeholder().orElse(null))
                                        .rows(rows)
                                        .build());
                    case HIDE: return Mono.just(ReplyKeyboardHide.builder().selective(selective().orElseThrow()).build());
                    case FORCE:
                        return Mono.just(ReplyKeyboardForceReply.builder()
                                .selective(selective().orElseThrow())
                                .singleUse(singleUse().orElseThrow())
                                .placeholder(placeholder().orElse(null))
                                .build());
                    case INLINE:
                        return Flux.fromIterable(rows().orElseThrow())
                                .flatMap(list -> Flux.fromIterable(list)
                                        .flatMap(s -> s.asData(client))
                                        .collectList()
                                        .map(l -> KeyboardButtonRow.builder()
                                                .buttons(l)
                                                .build()))
                                .collectList()
                                .map(rows -> ReplyInlineMarkup.builder().rows(rows).build());
                    default: return Mono.error(new IllegalStateException());
                }
            });
        }
    }

    @Value.Immutable(builder = false)
    public interface KeyboardButtonSpec extends Spec {

        static KeyboardButtonSpec userProfile(String text, Id userId) {
            return ImmutableMessageFields.KeyboardButtonSpec.of(KeyboardButton.Type.USER_PROFILE, text)
                    .withUserId(userId);
        }

        static KeyboardButtonSpec text(String text) {
            return ImmutableMessageFields.KeyboardButtonSpec.of(KeyboardButton.Type.DEFAULT, text);
        }

        static KeyboardButtonSpec inputUrlAuth(String text, boolean requestWriteAccess,
                                               @Nullable String forwardText, String url, Id botId) {
            return ImmutableMessageFields.KeyboardButtonSpec.of(KeyboardButton.Type.INPUT_URH_AUTH, text)
                    .withRequestWriteAccess(requestWriteAccess)
                    .withForwardText(Optional.ofNullable(forwardText))
                    .withUrl(url)
                    .withBotId(botId);
        }

        static KeyboardButtonSpec inputUserProfile(String text, Id userId) {
            return ImmutableMessageFields.KeyboardButtonSpec.of(KeyboardButton.Type.INPUT_USER_PROFILE, text)
                    .withUserId(userId);
        }

        static KeyboardButtonSpec buy(String text) {
            return ImmutableMessageFields.KeyboardButtonSpec.of(KeyboardButton.Type.BUY, text);
        }

        static KeyboardButtonSpec callback(String text, boolean requiresPassword, byte[] data) {
            return ImmutableMessageFields.KeyboardButtonSpec.of(KeyboardButton.Type.CALLBACK, text)
                    .withRequiresPassword(requiresPassword)
                    .withData(data);
        }

        static KeyboardButtonSpec game(String text) {
            return ImmutableMessageFields.KeyboardButtonSpec.of(KeyboardButton.Type.GAME, text);
        }

        static KeyboardButtonSpec requestGeoLocation(String text) {
            return ImmutableMessageFields.KeyboardButtonSpec.of(KeyboardButton.Type.REQUEST_GEO_LOCATION, text);
        }

        static KeyboardButtonSpec requestPhone(String text) {
            return ImmutableMessageFields.KeyboardButtonSpec.of(KeyboardButton.Type.REQUEST_PHONE, text);
        }

        static KeyboardButtonSpec requestPoll(String text, @Nullable Boolean quiz) {
            return ImmutableMessageFields.KeyboardButtonSpec.of(KeyboardButton.Type.REQUEST_POLL, text)
                    .withQuiz(Optional.ofNullable(quiz));
        }

        static KeyboardButtonSpec switchInline(String text, boolean samePeer, String query) {
            return ImmutableMessageFields.KeyboardButtonSpec.of(KeyboardButton.Type.SWITCH_INLINE, text)
                    .withSamePeer(samePeer)
                    .withQuery(query);
        }

        static KeyboardButtonSpec url(String text, String url) {
            return ImmutableMessageFields.KeyboardButtonSpec.of(KeyboardButton.Type.URL, text)
                    .withUrl(url);
        }

        static KeyboardButtonSpec urlAuth(String text, @Nullable String forwardText, String url, int buttonId) {
            return ImmutableMessageFields.KeyboardButtonSpec.of(KeyboardButton.Type.URL_AUTH, text)
                    .withForwardText(Optional.ofNullable(forwardText))
                    .withUrl(url)
                    .withButtonId(buttonId);
        }

        KeyboardButton.Type type();

        String text();

        Optional<byte[]> data();

        Optional<Boolean> quiz();

        Optional<String> query();

        Optional<String> url();

        Optional<String> forwardText();

        Optional<Integer> buttonId();

        Optional<Boolean> requestWriteAccess();

        Optional<Boolean> requiresPassword();

        Optional<Boolean> samePeer();

        Optional<Id> botId();

        Optional<Id> userId();

        default Mono<telegram4j.tl.KeyboardButton> asData(MTProtoTelegramClient client) {
            return Mono.defer(() -> {
                switch (type()) {
                    case INPUT_URH_AUTH: return client.asInputUser(userId().orElseThrow())
                            .map(id -> ImmutableInputKeyboardButtonUrlAuth.of(text(), url().orElseThrow(), id)
                                    .withFwdText(forwardText().orElse(null))
                                    .withRequestWriteAccess(requestWriteAccess().orElseThrow()));
                    case URL_AUTH: return Mono.just(ImmutableKeyboardButtonUrlAuth.of(text(), url().orElseThrow(), buttonId().orElseThrow())
                            .withFwdText(forwardText().orElse(null)));
                    case DEFAULT: return Mono.just(ImmutableBaseKeyboardButton.of(text()));
                    case BUY: return Mono.just(ImmutableKeyboardButtonBuy.of(text()));
                    case URL: return Mono.just(ImmutableKeyboardButtonUrl.of(text(), url().orElseThrow()));
                    case INPUT_USER_PROFILE: return client.asInputUser(userId().orElseThrow())
                            .map(id -> ImmutableInputKeyboardButtonUserProfile.of(text(), id));
                    case USER_PROFILE: return Mono.just(ImmutableKeyboardButtonUserProfile.of(text(), userId().orElseThrow().asLong()));
                    case SWITCH_INLINE: return Mono.just(ImmutableKeyboardButtonSwitchInline.of(text(), query().orElseThrow())
                            .withSamePeer(samePeer().orElseThrow()));
                    case REQUEST_POLL: return Mono.just(ImmutableKeyboardButtonRequestPoll.of(text())
                            .withQuiz(quiz().orElse(null)));
                    case REQUEST_PHONE: return Mono.just(ImmutableKeyboardButtonRequestPhone.of(text()));
                    case REQUEST_GEO_LOCATION: return Mono.just(ImmutableKeyboardButtonRequestGeoLocation.of(text()));
                    case GAME: return Mono.from(Mono.just(ImmutableKeyboardButtonGame.of(text())));
                    case CALLBACK: return Mono.just(ImmutableKeyboardButtonCallback.of(text(), data().orElseThrow()));
                    default: return Mono.error(new IllegalStateException());
                }
            });
        }
    }
}
