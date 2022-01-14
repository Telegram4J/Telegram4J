package telegram4j.core.util;

import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import telegram4j.core.object.MessageEntity.Type;
import telegram4j.tl.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EntityParser {

    private static final String markdownV2 = "_*[]()~`";
    private static final Pattern USER_LINK_ID_PATTERN = Pattern.compile("^tg://user\\?id=(\\d{1,19})$", Pattern.CASE_INSENSITIVE);

    public static Tuple2<String, List<MessageEntity>> parse(String str, Mode mode) {
        switch (mode) {
            case MARKDOWN_V2: return parseMarkdownV2(str);
            case HTML: return parseHtml(str);
            default: throw new IllegalStateException();
        }
    }

    private static Tuple2<String, List<MessageEntity>> parseMarkdownV2(String str) {

        List<MessageEntity> entities = new ArrayList<>();
        StringBuilder res = new StringBuilder();
        Deque<Token> tokens = new ArrayDeque<>();
        int u = 0;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '\\') {
                u++;
                res.append(str.charAt(i++));
                continue;
            }

            String dels = markdownV2;
            if (!tokens.isEmpty()) {
                switch (tokens.getLast().type) {
                    case CODE:
                    case PRE:
                        dels = "`";
                        break;
                }
            }

            if (dels.indexOf(c) == -1) {
                u++;
                res.append(c);
                continue;
            }

            boolean closeEntity = false;
            if (!tokens.isEmpty()) {
                switch (tokens.getLast().type) {
                    case BOLD:
                        closeEntity = c == '*';
                        break;
                    case ITALIC:
                        closeEntity = c == '_' && i + 1 < str.length() && str.charAt(i + 1) != '_';
                        break;
                    case CODE:
                        closeEntity = c == '`';
                        break;
                    case PRE:
                        closeEntity = c == '`' && i + 2 < str.length() &&
                                str.charAt(i + 1) == '`' && str.charAt(i + 2) == '`';
                        break;
                    case TEXT_URL:
                        closeEntity = c == ']';
                        break;
                    case UNDERLINE:
                        closeEntity = c == '_' && i + 1 < str.length() && str.charAt(i + 1) == '_';
                        break;
                    case STRIKETHROUGH:
                        closeEntity = c == '~';
                        break;
                }
            }

            if (!closeEntity) {
                Type curr;
                String arg = "";
                long userId = 0;

                switch (c) {
                    case '*':
                        curr = Type.BOLD;
                        break;
                    case '_':
                        if (i + 1 < str.length() && str.charAt(i + 1) == '_') {
                            curr = Type.UNDERLINE;
                            i++;
                        } else {
                            curr = Type.ITALIC;
                        }
                        break;
                    case '`':
                        if (i + 2 < str.length() && str.charAt(i + 1) == '`' && str.charAt(i + 2) == '`') {
                            curr = Type.PRE;
                            i += 3;

                            int languageEnd = i;
                            while (!Character.isWhitespace(str.charAt(languageEnd)) && str.charAt(languageEnd) != '`') {
                                languageEnd++;
                            }

                            if (i != languageEnd) {
                                arg = str.substring(i, languageEnd);
                                i = languageEnd;
                            }

                            if (str.charAt(i) == '\n' || str.charAt(i) == '\r') {
                                if ((str.charAt(i + 1) == '\n' || str.charAt(i + 1) == '\r') && str.charAt(i) != str.charAt(i + 1)) {
                                    i += 2;
                                } else {
                                    i++;
                                }
                            }

                            i--;
                        } else {
                            curr = Type.CODE;
                        }
                        break;
                    case '~':
                        curr = Type.STRIKETHROUGH;
                        break;
                    case ']':
                    case '[':
                        curr = Type.TEXT_URL;
                        break;
                    default:
                        throw new IllegalStateException();
                }

                tokens.add(new Token(curr, arg, userId, u, res.length()));
            } else {
                Token t = tokens.removeLast();
                String arg = t.arg;
                long userId = t.userId;
                int strPos = t.strPos;

                switch (t.type) {
                    case PRE:
                        i += 2;
                        break;
                    case UNDERLINE:
                        i++;
                        break;
                    case TEXT_URL:
                        StringBuilder url = new StringBuilder();
                        if (str.charAt(i + 1) != '(') {
                            url.append(res.substring(strPos));
                        } else {
                            i += 2;
                            int urlBeginPos = i;
                            while (i < str.length() && str.charAt(i) != ')') {
                                if (str.charAt(i) == '\\') {
                                    url.append(str.charAt(i + 1));
                                    i += 2;
                                    continue;
                                }
                                url.append(str.charAt(i++));
                            }
                            if (str.charAt(i) != ')') {
                                throw new IllegalStateException("Can't find end of a URL at " + urlBeginPos);
                            }
                        }

                        userId = parseLinkUserId(url.toString());
                        if (userId == -1) {
                            // TODO: check link?
                            arg = url.toString();
                        }

                        break;
                }

                int length = u - t.offset;
                int offset = t.offset;

                switch (t.type) {
                    case BOLD:
                        entities.add(ImmutableMessageEntityBold.of(offset, length));
                        break;
                    case ITALIC:
                        entities.add(ImmutableMessageEntityItalic.of(offset, length));
                        break;
                    case UNDERLINE:
                        entities.add(ImmutableMessageEntityUnderline.of(offset, length));
                        break;
                    case CODE:
                        entities.add(ImmutableMessageEntityCode.of(offset, length));
                        break;
                    case PRE:
                        entities.add(ImmutableMessageEntityPre.of(offset, length, arg));
                        break;
                    case STRIKETHROUGH:
                        entities.add(ImmutableMessageEntityStrike.of(offset, length));
                        break;
                    case TEXT_URL:
                        if (userId != -1) {
                            entities.add(ImmutableMessageEntityMentionName.of(offset, length, userId));
                        } else {
                            entities.add(ImmutableMessageEntityTextUrl.of(offset, length, arg));
                        }

                        break;
                }
            }
        }

        if (!tokens.isEmpty()) {
            throw new IllegalArgumentException("Failed to find end of " + tokens.getLast().type + " entity.");
        }

        return Tuples.of(res.toString(), entities);
    }

    private static Tuple2<String, List<MessageEntity>> parseHtml(String str) {
        // TODO: implement
        throw new UnsupportedOperationException();
    }

    private static long parseLinkUserId(String url) {
        Matcher m = USER_LINK_ID_PATTERN.matcher(url);
        if (!m.matches()) {
            return -1;
        }

        return Long.parseLong(m.group(1));
    }

    private static class Token {
        private final Type type;
        private final String arg;
        private final long userId;
        private final int offset;
        private final int strPos;

        private Token(Type type, String arg, long userId, int offset, int strPos) {
            this.type = type;
            this.arg = arg;
            this.userId = userId;
            this.offset = offset;
            this.strPos = strPos;
        }
    }

    public enum Mode {
        MARKDOWN_V2,
        HTML
    }
}
