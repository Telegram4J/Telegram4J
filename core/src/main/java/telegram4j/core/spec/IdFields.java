package telegram4j.core.spec;

import org.immutables.value.Value;
import telegram4j.tl.*;

import java.util.Optional;

/**
 * Optimized representation of id types.
 */
@Value.Enclosing
@FieldsStyle
public final class IdFields {

    private IdFields() {}

    /** Simplified representation of {@link InputMessage}. */
    @Value.Immutable(builder = false)
    public interface MessageId extends Spec {

        static MessageId from(InputMessage inputMessage) {
            switch (inputMessage.identifier()) {
                case InputMessageID.ID: return of(((InputMessageID) inputMessage).id());
                case InputMessagePinned.ID: return pinned();
                case InputMessageReplyTo.ID: return replyTo(((InputMessageReplyTo) inputMessage).id());
                case InputMessageCallbackQuery.ID:
                    InputMessageCallbackQuery callbackQuery = (InputMessageCallbackQuery) inputMessage;
                    return callbackQuery(callbackQuery.id(), callbackQuery.queryId());
                default: throw new IllegalArgumentException("Unknown input message type: " + inputMessage);
            }
        }

        static MessageId of(int id) {
            return ImmutableIdFields.MessageId.of(Type.DEFAULT).withId(id);
        }

        static MessageId replyTo(int id) {
            return ImmutableIdFields.MessageId.of(Type.REPLY_TO).withId(id);
        }

        static MessageId pinned() {
            return ImmutableIdFields.MessageId.of(Type.PINNED);
        }

        static MessageId callbackQuery(int id, long queryId) {
            return ImmutableIdFields.MessageId.of(Type.CALLBACK_QUERY)
                    .withId(id)
                    .withQueryId(queryId);
        }

        Type type();

        Optional<Integer> id();

        Optional<Long> queryId();

        default InputMessage asInputMessage() {
            switch (type()) {
                case PINNED: return InputMessagePinned.instance();
                case DEFAULT: return ImmutableInputMessageID.of(id().orElseThrow());
                case REPLY_TO: return ImmutableInputMessageReplyTo.of(id().orElseThrow());
                case CALLBACK_QUERY: return ImmutableInputMessageCallbackQuery.of(id().orElseThrow(), queryId().orElseThrow());
                default: throw new IllegalStateException();
            }
        }

        enum Type {
            DEFAULT,
            REPLY_TO,
            PINNED,
            CALLBACK_QUERY
        }
    }

    @Value.Immutable(builder = false)
    public interface StickerSetId extends Spec {

        static StickerSetId from(InputStickerSet inputStickerSet) {
            switch (inputStickerSet.identifier()) {
                case InputStickerSetAnimatedEmoji.ID: return animatedEmoji();
                case InputStickerSetAnimatedEmojiAnimations.ID: return animatedEmojiAnimations();
                case InputStickerSetDice.ID: return dice(((InputStickerSetDice) inputStickerSet).emoticon());
                case InputStickerSetID.ID:
                    InputStickerSetID cast = (InputStickerSetID) inputStickerSet;
                    return of(cast.id(), cast.accessHash());
                case InputStickerSetShortName.ID: return shortName(((InputStickerSetShortName) inputStickerSet).shortName());
                default: throw new IllegalArgumentException("Unknown input sticker set type: " + inputStickerSet);
            }
        }

        static StickerSetId of(long id, long accessHash) {
            return ImmutableIdFields.StickerSetId.of(Type.ID)
                    .withId(id)
                    .withAccessHash(accessHash);
        }

        static StickerSetId animatedEmoji() {
            return ImmutableIdFields.StickerSetId.of(Type.ANIMATED_EMOJI);
        }

        static StickerSetId animatedEmojiAnimations() {
            return ImmutableIdFields.StickerSetId.of(Type.ANIMATED_EMOJI_ANIMATIONS);
        }

        static StickerSetId dice(String emoticon) {
            return ImmutableIdFields.StickerSetId.of(Type.DICE)
                    .withEmoticon(emoticon);
        }

        static StickerSetId shortName(String shortName) {
            return ImmutableIdFields.StickerSetId.of(Type.SHORT_NAME)
                    .withShortName(shortName);
        }

        Type type();

        Optional<Long> id();

        Optional<Long> accessHash();

        Optional<String> emoticon();

        Optional<String> shortName();

        default InputStickerSet asInputStickerSet() {
            switch (type()) {
                case ANIMATED_EMOJI: return InputStickerSetAnimatedEmoji.instance();
                case ANIMATED_EMOJI_ANIMATIONS: return InputStickerSetAnimatedEmojiAnimations.instance();
                case DICE: return ImmutableInputStickerSetDice.of(emoticon().orElseThrow());
                case ID: return ImmutableInputStickerSetID.of(id().orElseThrow(), accessHash().orElseThrow());
                case SHORT_NAME: return ImmutableInputStickerSetShortName.of(shortName().orElseThrow());
                default: throw new IllegalStateException();
            }
        }

        enum Type {
            ANIMATED_EMOJI,
            ANIMATED_EMOJI_ANIMATIONS,
            DICE,
            ID,
            SHORT_NAME
        }
    }
}
