package telegram4j.core.util;

import reactor.util.annotation.Nullable;
import telegram4j.core.object.MessageEntity;

/** Markup token interface with base information. */
public interface EntityToken {

    /** A token is indicating the EOF. */
    EntityToken UNKNOWN = create(MessageEntity.Type.UNKNOWN, -1, null);

    /**
     * Create a default immutable implementation of {@link EntityToken}.
     *
     * @apiNote The token value ({@code arg}) must be present only on <i>begin</i> token for correct parsing.
     *
     * @param type The {@link MessageEntity.Type} of token.
     * @param offset The offset in the cleared text.
     * @param arg The optional argument of token.
     * @return The immutable {@link EntityToken} from given parameters.
     */
    static EntityToken create(MessageEntity.Type type, int offset, @Nullable String arg) {
        return new EntityTokenImpl(type, offset, arg);
    }

    /**
     * Gets a type of token.
     *
     * @return The {@link MessageEntity.Type message entity type} of token.
     */
    MessageEntity.Type type();

    /**
     * Gets an offset position of token according cleared text length.
     *
     * @return An offset position in the cleared text.
     */
    int offset();

    /**
     * Gets the string representation of token value, if present.
     *
     * @return The string representation of token value, if present.
     */
    @Nullable
    String arg();
}
