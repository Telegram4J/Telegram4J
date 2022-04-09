package telegram4j.core.spec.markup;

import org.immutables.value.Value;
import reactor.util.annotation.Nullable;
import telegram4j.core.object.markup.KeyboardButton;
import telegram4j.core.util.Id;

import java.util.Optional;

@Value.Immutable
abstract class InlineButtonSpecDef implements KeyboardButtonSpec {

    public static InlineButtonSpec buy(String text) {
        return InlineButtonSpec.of(KeyboardButton.Type.BUY, text);
    }

    public static InlineButtonSpec callback(String text, boolean requiresPassword, byte[] data) {
        return InlineButtonSpec.of(KeyboardButton.Type.CALLBACK, text)
                .withRequiresPassword(requiresPassword)
                .withData(data);
    }

    public static InlineButtonSpec userProfile(String text, Id userId) {
        return InlineButtonSpec.of(KeyboardButton.Type.USER_PROFILE, text)
                .withUserId(userId);
    }

    public static InlineButtonSpec urlAuth(String text, boolean requestWriteAccess,
                                           @Nullable String forwardText, String url, Id botId) {
        return InlineButtonSpec.of(KeyboardButton.Type.URL_AUTH, text)
                .withRequestWriteAccess(requestWriteAccess)
                .withForwardText(Optional.ofNullable(forwardText))
                .withUrl(url)
                .withUserId(botId);
    }

    public static InlineButtonSpec game(String text) {
        return InlineButtonSpec.of(KeyboardButton.Type.GAME, text);
    }

    public static InlineButtonSpec switchInline(String text, boolean samePeer, String query) {
        return InlineButtonSpec.of(KeyboardButton.Type.SWITCH_INLINE, text)
                .withSamePeer(samePeer)
                .withQuery(query);
    }

    public static InlineButtonSpec url(String text, String url) {
        return InlineButtonSpec.of(KeyboardButton.Type.URL, text)
                .withUrl(url);
    }

    public abstract Optional<byte[]> data();

    public abstract Optional<String> query();

    public abstract Optional<String> url();

    public abstract Optional<String> forwardText();

    public abstract Optional<Boolean> requestWriteAccess();

    public abstract Optional<Boolean> requiresPassword();

    public abstract Optional<Boolean> samePeer();

    public abstract Optional<Id> userId();
}
