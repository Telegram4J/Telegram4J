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
            CALLBACK_QUERY;
        }
    }
}
