package telegram4j.core.util.parser;

import java.util.Objects;

abstract class BaseEntityParser implements EntityParser {

    final String str;
    final StringBuilder striped;

    int cursor;
    int offset; // offset without markup chars
    EntityToken prev = EntityToken.UNKNOWN;

    BaseEntityParser(String source) {
        this.str = Objects.requireNonNull(source, "source");
        this.striped = new StringBuilder(source.length());
    }

    @Override
    public String source() {
        return str;
    }

    @Override
    public String striped() {
        if (cursor < str.length()) {
            throw new IllegalStateException("Parsing has not yet completed.");
        }

        return striped.toString();
    }

    @Override
    public abstract EntityToken nextToken();
}
