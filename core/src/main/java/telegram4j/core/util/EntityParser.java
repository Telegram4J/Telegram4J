package telegram4j.core.util;

/**
 * Interface of <a href="https://core.telegram.org/type/MessageEntity">message entity</a> parser
 * that can parse markup token and strip original text.
 * Parsed and collected tokens must be sorted according to
 * the {@link EntityToken#type() type} of token and the distance to the nearest similar token
 * in <b>ascending</b> order.
 */
public interface EntityParser {

    /**
     * Gets the text cleared from markup.
     *
     * @return The text cleared from markup.
     * @throws IllegalStateException if the analysis is not finished yet
     */
    String striped();

    /**
     * Finds the next markup token.
     *
     * @return The next token of markup or {@link EntityToken#UNKNOWN} upon completion.
     */
    EntityToken nextToken();
}
