package telegram4j.core.util.parser;

import java.util.Objects;

abstract class BaseEntityParser implements EntityParser {

    final String str;
    final StringBuilder striped;

    int cursor;
    int offset; // offset without markup chars
    EntityToken prev = null;

    BaseEntityParser(String source) {
        this.str = Objects.requireNonNull(source);
        this.striped = new StringBuilder(source.length());
    }

    @Override
    public String source() {
        return str;
    }

    @Override
    public String striped() {
        if (cursor < str.length()) {
            throw new IllegalStateException("Parsing has not completed yet.");
        }

        return striped.toString();
    }

    @Override
    public abstract EntityToken nextToken();
}
