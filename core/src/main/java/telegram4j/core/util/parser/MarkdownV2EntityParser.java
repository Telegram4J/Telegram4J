package telegram4j.core.util.parser;

import telegram4j.core.object.MessageEntity;

import java.util.regex.Matcher;

import static telegram4j.core.util.parser.EntityParserSupport.USER_LINK_ID_PATTERN;

class MarkdownV2EntityParser extends BaseEntityParser {

    MarkdownV2EntityParser(String source) {
        super(source);
    }

    @Override
    public EntityToken nextToken() {
        if (cursor >= str.length()) {
            return EntityToken.UNKNOWN;
        }

        for (; cursor < str.length(); cursor++) {
            char c = str.charAt(cursor);
            if (c == '\\') {
                striped.append(c);
                continue;
            }

            int length = 1;
            MessageEntity.Type type = MessageEntity.Type.UNKNOWN;
            String arg = null;
            switch (c) {
                case '*':
                    if (cursor + 1 < str.length() && str.charAt(cursor + 1) == '*') {
                        type = MessageEntity.Type.BOLD;
                        length = 2;
                    }
                    break;
                case '~':
                    if (cursor + 1 < str.length() && str.charAt(cursor + 1) == '~') {
                        type = MessageEntity.Type.STRIKETHROUGH;
                        length = 2;
                    }
                    break;
                case '_':
                    if (cursor + 1 < str.length() && str.charAt(cursor + 1) == '_') {
                        type = MessageEntity.Type.UNDERLINE;
                        length = 2;
                    } else {
                        type = MessageEntity.Type.ITALIC;
                    }
                    break;
                case ']':
                    if (prev.type() == MessageEntity.Type.TEXT_URL ||
                            prev.type() == MessageEntity.Type.MENTION_NAME) {
                        type = prev.type();
                        length = str.indexOf(')', cursor) + 1 - cursor;
                    }
                    break;
                case '[':
                    int endPos = -1;
                    int endText = -1;
                    int beginUrl = -1;
                    for (int i = cursor + 1; i < str.length(); i++) {
                        char n = str.charAt(i);
                        if (n == ')') {
                            endPos = i;
                            break;
                        } else if (n == '(') {
                            beginUrl = i;
                        } else if (n == ']') {
                            endText = i;
                        }
                    }

                    if (endText != -1 && beginUrl != -1 && endPos != -1) {
                        arg = str.substring(beginUrl + 1, endPos);

                        Matcher mentionName = USER_LINK_ID_PATTERN.matcher(arg);
                        if (mentionName.matches()) {
                            type = MessageEntity.Type.MENTION_NAME;
                            arg = mentionName.group(1);
                        } else {
                            type = MessageEntity.Type.TEXT_URL;
                        }
                    }

                    break;
                case '`':
                    if (cursor + 2 < str.length() &&
                            str.charAt(cursor + 1) == '`' &&
                            str.charAt(cursor + 2) == '`') {

                        int endArg = -1;
                        for (int i = cursor + 1; i < str.length(); i++) {
                            if (Character.isWhitespace(str.charAt(i))) {
                                endArg = i;
                                break;
                            }
                        }

                        type = MessageEntity.Type.PRE;
                        length = 3;
                        if (endArg != -1) {
                            arg = str.substring(cursor + 3, endArg);
                            length += arg.length();
                        }
                    } else {
                        type = MessageEntity.Type.CODE;
                    }

                    break;
                case '|':
                    if (cursor + 1 < str.length() && str.charAt(cursor + 1) == '|') {
                        type = MessageEntity.Type.SPOILER;
                        length = 2;
                    }
                    break;
            }

            if (type == MessageEntity.Type.UNKNOWN) {
                striped.append(c);
            } else {
                // skip identity tokens chars
                if (length == 1) {
                    while (cursor + 1 < str.length() && c == str.charAt(cursor + 1)) {
                        striped.append(c);
                        cursor += 1;
                    }
                }

                EntityTokenImpl t = new EntityTokenImpl(type, cursor - offset, arg);

                cursor += length;
                prev = t;
                offset += length;
                return t;
            }
        }
        return EntityToken.UNKNOWN;
    }
}
