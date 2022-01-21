package telegram4j.core.spec;

import org.immutables.value.Value;
import reactor.util.annotation.Nullable;
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

        static ReplyMarkupSpec from(telegram4j.tl.ReplyMarkup data) {
            switch (data.identifier()) {
                case ReplyInlineMarkup.ID:
                    ReplyInlineMarkup replyInlineMarkup = (ReplyInlineMarkup) data;

                    return inlineKeyboard(replyInlineMarkup.rows().stream()
                            .map(d -> d.buttons().stream()
                                    .map(KeyboardButtonSpec::from)
                                    .collect(Collectors.toList()))
                            .collect(Collectors.toList()));
                case ReplyKeyboardForceReply.ID: {
                    ReplyKeyboardForceReply data0 = (ReplyKeyboardForceReply) data;

                    return forceReplyKeyboard(data0.singleUse(), data0.selective());
                }
                case ReplyKeyboardHide.ID: return hideKeyboard(((ReplyKeyboardHide) data).selective());
                case ReplyKeyboardMarkup.ID: {
                    ReplyKeyboardMarkup data0 = (ReplyKeyboardMarkup) data;

                    return keyboard(data0.resize(), data0.singleUse(), data0.selective(),
                            data0.placeholder(), data0.rows().stream()
                                    .map(d -> d.buttons().stream()
                                            .map(KeyboardButtonSpec::from)
                                            .collect(Collectors.toList()))
                                    .collect(Collectors.toList()));
                }
                default:
                    throw new IllegalArgumentException("Unknown reply markup type: " + data);
            }
        }

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
                    .withPlaceholder(Optional.ofNullable(placeholder)) // todo: why?
                    .withRows(rows);
        }

        ReplyMarkup.Type type();

        Optional<List<List<KeyboardButtonSpec>>> rows();

        Optional<Boolean> singleUse();

        Optional<Boolean> selective();

        Optional<Boolean> resize();

        Optional<String> placeholder();

        default telegram4j.tl.ReplyMarkup asData() {
            switch (type()) {
                case DEFAULT:
                    return ReplyKeyboardMarkup.builder()
                            .selective(selective().orElseThrow())
                            .singleUse(singleUse().orElseThrow())
                            .resize(resize().orElseThrow())
                            .placeholder(placeholder().orElse(null))
                            .rows(rows().orElseThrow().stream()
                                    .map(d -> KeyboardButtonRow.builder()
                                            .buttons(d.stream()
                                                    .map(KeyboardButtonSpec::asData)
                                                    .collect(Collectors.toList()))
                                            .build())
                                    .collect(Collectors.toList()))
                            .build();
                case HIDE:
                    return ReplyKeyboardHide.builder()
                            .selective(selective().orElseThrow())
                            .build();
                case FORCE:
                    return ReplyKeyboardForceReply.builder()
                            .selective(selective().orElseThrow())
                            .singleUse(singleUse().orElseThrow())
                            .placeholder(placeholder().orElse(null))
                            .build();
                case INLINE:
                    return ReplyInlineMarkup.builder()
                            .rows(rows().orElseThrow().stream()
                                    .map(d -> KeyboardButtonRow.builder()
                                            .buttons(d.stream()
                                                    .map(KeyboardButtonSpec::asData)
                                                    .collect(Collectors.toList()))
                                            .build())
                                    .collect(Collectors.toList()))
                            .build();
                default: throw new IllegalStateException();
            }
        }
    }

    @Value.Immutable(builder = false)
    public interface KeyboardButtonSpec extends Spec {

        static KeyboardButtonSpec from(telegram4j.tl.KeyboardButton data) {
            switch (data.identifier()) {
                case BaseKeyboardButton.ID: return text(data.text());
                case InputKeyboardButtonUrlAuth.ID: {
                    InputKeyboardButtonUrlAuth data0 = (InputKeyboardButtonUrlAuth) data;

                    return inputUrlAuth(data0.text(), data0.requestWriteAccess(), data0.fwdText(),
                            data0.url(), data0.bot());
                }
                case InputKeyboardButtonUserProfile.ID: {
                    InputKeyboardButtonUserProfile data0 = (InputKeyboardButtonUserProfile) data;

                    return inputUserProfile(data0.text(), data0.userId());
                }
                case KeyboardButtonBuy.ID: return buy(data.text());
                case KeyboardButtonCallback.ID: {
                    KeyboardButtonCallback data0 = (KeyboardButtonCallback) data;

                    return callback(data0.text(), data0.requiresPassword(), data0.data());
                }
                case KeyboardButtonGame.ID: return game(data.text());
                case KeyboardButtonRequestGeoLocation.ID: return requestGeoLocation(data.text());
                case KeyboardButtonRequestPhone.ID: return requestPhone(data.text());
                case KeyboardButtonRequestPoll.ID: {
                    KeyboardButtonRequestPoll data0 = (KeyboardButtonRequestPoll) data;

                    return requestPoll(data0.text(), data0.quiz());
                }
                case KeyboardButtonSwitchInline.ID: {
                    KeyboardButtonSwitchInline data0 = (KeyboardButtonSwitchInline) data;

                    return switchInline(data0.text(), data0.samePeer(), data0.query());
                }
                case KeyboardButtonUrl.ID: {
                    KeyboardButtonUrl data0 = (KeyboardButtonUrl) data;

                    return url(data0.text(), data0.url());
                }
                case KeyboardButtonUrlAuth.ID: {
                    KeyboardButtonUrlAuth data0 = (KeyboardButtonUrlAuth) data;

                    return urlAuth(data0.text(), data0.fwdText(), data0.url(), data0.buttonId());
                }
                case KeyboardButtonUserProfile.ID: {
                    KeyboardButtonUserProfile data0 = (KeyboardButtonUserProfile) data;

                    return userProfile(data0.text(), data0.userId());
                }
                default:
                    throw new IllegalArgumentException("Unknown keyboard button type: " + data);
            }
        }

        static KeyboardButtonSpec userProfile(String text, long userId) {
            return ImmutableMessageFields.KeyboardButtonSpec.of(KeyboardButton.Type.USER_PROFILE, text)
                    .withUserId(ImmutableBaseInputUser.of(userId, -1)); // TODO
        }

        static KeyboardButtonSpec text(String text) {
            return ImmutableMessageFields.KeyboardButtonSpec.of(KeyboardButton.Type.DEFAULT, text);
        }

        static KeyboardButtonSpec inputUrlAuth(String text, boolean requestWriteAccess,
                                               @Nullable String forwardText, String url, InputUser botId) {
            return ImmutableMessageFields.KeyboardButtonSpec.of(KeyboardButton.Type.INPUT_URH_AUTH, text)
                    .withRequestWriteAccess(requestWriteAccess)
                    .withForwardText(Optional.ofNullable(forwardText))
                    .withUrl(url)
                    .withBotId(botId);
        }

        static KeyboardButtonSpec inputUserProfile(String text, InputUser userId) {
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

        Optional<InputUser> botId();

        Optional<InputUser> userId();

        default telegram4j.tl.KeyboardButton asData() {
            switch (type()) {
                case INPUT_URH_AUTH:
                    return ImmutableInputKeyboardButtonUrlAuth.of(text(), url().orElseThrow(), botId().orElseThrow())
                            .withFwdText(forwardText().orElse(null))
                            .withRequestWriteAccess(requestWriteAccess().orElseThrow());
                case URL_AUTH: return ImmutableKeyboardButtonUrlAuth.of(text(), url().orElseThrow(), buttonId().orElseThrow())
                        .withFwdText(forwardText().orElse(null));
                case DEFAULT: return ImmutableBaseKeyboardButton.of(text());
                case BUY: return ImmutableKeyboardButtonBuy.of(text());
                case URL: return ImmutableKeyboardButtonUrl.of(text(), url().orElseThrow());
                case INPUT_USER_PROFILE: return ImmutableInputKeyboardButtonUserProfile.of(text(), userId().orElseThrow());
                case USER_PROFILE: return ImmutableKeyboardButtonUserProfile.of(text(),
                        ((BaseInputUser) userId().orElseThrow()).userId());
                case SWITCH_INLINE: return ImmutableKeyboardButtonSwitchInline.of(text(), query().orElseThrow())
                        .withSamePeer(samePeer().orElseThrow());
                case REQUEST_POLL: return ImmutableKeyboardButtonRequestPoll.of(text())
                        .withQuiz(quiz().orElse(null));
                case REQUEST_PHONE: return ImmutableKeyboardButtonRequestPhone.of(text());
                case REQUEST_GEO_LOCATION: return ImmutableKeyboardButtonRequestGeoLocation.of(text());
                case GAME: return ImmutableKeyboardButtonGame.of(text());
                case CALLBACK: return ImmutableKeyboardButtonCallback.of(text(), data().orElseThrow());
                default: throw new IllegalStateException();
            }
        }
    }
}
