package telegram4j.core.util.parser;

import telegram4j.core.object.MessageEntity;

import java.util.regex.Matcher;

import static telegram4j.core.util.parser.EntityParserSupport.USER_LINK_ID_PATTERN;

public class HtmlEntityParser implements EntityParser {

    final StringBuilder striped;
    final String str;

    int cursor;
    int offset; // offset without markup chars
    EntityToken prev = EntityToken.UNKNOWN;

    HtmlEntityParser(String str) {
        this.striped = new StringBuilder(str.length());
        this.str = str;
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
    public EntityToken nextToken() {
        if (cursor >= str.length()) {
            return EntityToken.UNKNOWN;
        }

        for (; cursor < str.length(); cursor++) {
            char c = str.charAt(cursor);
            if (c != '<') {
                striped.append(c);
                continue;
            }

            int length = 3;
            MessageEntity.Type type = MessageEntity.Type.UNKNOWN;
            String arg = null;

            int endPos = -1;
            int argBegin = -1;
            int tokenEnd = -1;
            for (int i = cursor + 1; i < str.length(); i++) {
                char n = str.charAt(i);
                if (n == '>') {
                    endPos = i;
                    break;
                } else if (Character.isWhitespace(n)) {
                    if (tokenEnd == -1) {
                        tokenEnd = i;
                    }

                    if (argBegin == -1) {
                        argBegin = i;
                    }
                } else if (n == '<') { // invalid token
                    break;
                }
            }

            if (endPos == -1) {
                break;
            }

            tokenEnd = tokenEnd != -1 ? tokenEnd : endPos;
            int coffset = 1;
            char n = str.charAt(cursor + 1);
            if (n == '/') {
                n = str.charAt(cursor + 2);
                coffset = 2;
                length += 1;
            }

            n = Character.toLowerCase(n);

            switch (n) {
                case 'b':
                    type = MessageEntity.Type.BOLD;
                    break;
                case 'i':
                    type = MessageEntity.Type.ITALIC;
                    break;
                case 's':
                    if (cursor + coffset + 7 < str.length() && str.substring(cursor + coffset, tokenEnd)
                            .equalsIgnoreCase("spoiler")) {
                        type = MessageEntity.Type.SPOILER;
                        length += 6;
                    } else {
                        type = MessageEntity.Type.STRIKETHROUGH;
                    }
                    break;
                case 'a':
                    type = MessageEntity.Type.TEXT_URL;
                    String href;
                    if (argBegin != -1 && (href = str.substring(argBegin, endPos)).contains("=")) {
                        String[] parts = href.split("=", 2);
                        if (parts[0].trim().equalsIgnoreCase("href")) {
                            arg = unqoute(parts[1]);
                            Matcher userIdMatcher = USER_LINK_ID_PATTERN.matcher(arg);
                            if (userIdMatcher.matches()) {
                                type = MessageEntity.Type.MENTION_NAME;
                                arg = userIdMatcher.group(1);
                            }

                            length += href.length();
                        } else {
                            type = MessageEntity.Type.UNKNOWN;
                        }
                    } else { // end of url/mention
                        if (prev.type() != MessageEntity.Type.TEXT_URL && prev.type() != MessageEntity.Type.MENTION_NAME) {
                            type = MessageEntity.Type.UNKNOWN;
                        } else {
                            type = prev.type();
                        }
                    }
                    break;
                case 'u':
                    type = MessageEntity.Type.UNDERLINE;
                    break;
                case 'p':
                    if (cursor + coffset + 3 < str.length() && str.substring(cursor + coffset, tokenEnd)
                            .equalsIgnoreCase("pre")) {
                        type = MessageEntity.Type.PRE;
                        length += 2;

                        String language;
                        if (argBegin != -1 && (language = str.substring(argBegin, endPos)).contains("=")) {
                            String[] parts = language.split("=", 2);
                            if (parts[0].trim().equalsIgnoreCase("language")) {
                                arg = unqoute(parts[1]);
                                length += language.length();
                            } else {
                                type = MessageEntity.Type.UNKNOWN;
                            }
                        }
                    }

                    break;
                case 'c':
                    type = MessageEntity.Type.CODE;
                    if (cursor + coffset + 4 < str.length() && str.substring(cursor + coffset, tokenEnd)
                            .equalsIgnoreCase("code")) {
                        length += 3;
                    }

                    break;
            }

            if (type == MessageEntity.Type.UNKNOWN) {
                striped.append(c);
            } else {
                EntityTokenImpl t = new EntityTokenImpl(type, cursor - offset, arg);

                cursor += length;
                prev = t;
                offset += length;
                return t;
            }
        }
        return EntityToken.UNKNOWN;
    }

    private String unqoute(String part) {
        char f = part.charAt(0);
        char e = part.charAt(part.length() - 1);
        // The parser is very loyal to the markup, so the lines with beginning " and end ' chats will be valid
        if ((f == '\'' || f == '"') && (e == '\'' || e == '"')) {
            return part.substring(1, part.length() - 1);
        }
        return part;
    }
}
