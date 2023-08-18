/*
 * Copyright 2023 Telegram4J
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package telegram4j.core.util.parser;

import telegram4j.core.object.MessageEntity;

import java.util.regex.Matcher;

import static telegram4j.core.util.parser.EntityParserSupport.USER_LINK_ID_PATTERN;

class HtmlEntityParser extends BaseEntityParser {
    // TODO: implement CUSTOM_EMOJI parsing

    boolean prevOpen;

    HtmlEntityParser(String source) {
        super(source);
    }

    @Override
    public EntityToken nextToken() {
        if (cursor >= str.length()) {
            return null;
        }

        for (; cursor < str.length(); cursor++) {
            char c = str.charAt(cursor);
            if (cursor - 1 >= 0 && str.charAt(cursor - 1) == '\\') {
                striped.append(c);
                continue;
            }

            if (c != '<' || cursor + 1 < str.length() &&
                    str.charAt(cursor + 1) == '/' && !prevOpen) {
                striped.append(c);
                continue;
            }

            int length = 3;
            MessageEntity.Type type = null;
            String arg = null;

            int endPos = -1;
            int argBegin = -1;
            int tokenEnd = -1;
            for (int i = cursor + 1; i < str.length(); i++) {
                char n = str.charAt(i);
                if (i - 1 >= 0 && str.charAt(i - 1) == '\\') {
                    continue;
                }

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
                }
            }

            if (endPos == -1) {
                break;
            }

            tokenEnd = tokenEnd != -1 ? tokenEnd : endPos;
            int coffset = 1;
            char n = str.charAt(cursor + 1);
            boolean open = n != '/';
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
                    if (argBegin != -1 && open && (href = str.substring(argBegin, endPos)).contains("=")) {
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
                            type = null;
                        }
                    } else { // end of url/mention
                        if (prev.type() != MessageEntity.Type.TEXT_URL &&
                            prev.type() != MessageEntity.Type.MENTION_NAME) {
                            // incorrect tag markup
                            type = null;
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
                        if (argBegin != -1 && open && (language = str.substring(argBegin, endPos)).contains("=")) {
                            String[] parts = language.split("=", 2);
                            if (parts[0].trim().equalsIgnoreCase("language")) {
                                arg = unqoute(parts[1]);
                                length += language.length();
                            } else {
                                type = null;
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

            if (type == null) {
                striped.append(c);
            } else {
                EntityTokenImpl t = new EntityTokenImpl(type, cursor - offset, arg);

                cursor += length;
                prev = t;
                prevOpen = open;
                offset += length;
                return t;
            }
        }
        return null;
    }

    private String unqoute(String part) {
        char f = part.charAt(0);
        char e = part.charAt(part.length() - 1);
        if ((f == '\'' || f == '"') && e == f) {
            return part.substring(1, part.length() - 1);
        }
        return part;
    }
}
