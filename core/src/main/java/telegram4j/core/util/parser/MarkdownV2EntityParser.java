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

class MarkdownV2EntityParser extends BaseEntityParser {
    // TODO: implement CUSTOM_EMOJI parsing

    int urlEnd = -1; // cached position of ')' in text url

    MarkdownV2EntityParser(String source) {
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

            int length = 1;
            MessageEntity.Type type = null;
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
                        length = urlEnd - cursor;
                    }
                    break;
                case '[':
                    int endPos = -1;
                    int endText = -1;
                    int beginUrl = -1;
                    StringBuilder url = new StringBuilder();
                    for (int i = cursor + 1; i < str.length(); i++) {
                        char n = str.charAt(i);
                        if (n == '\\' && i + 1 < str.length()) {
                            i++;
                            continue;
                        }

                        if (n == ')') {
                            endPos = i;
                            break;
                        } else if (n == '(') {
                            beginUrl = i;
                        } else if (n == ']') {
                            endText = i;
                        } else if (beginUrl != -1) {
                            url.append(str.charAt(i));
                        }
                    }

                    if (endText != -1 && beginUrl != -1 && endPos != -1) {
                        urlEnd = endPos + 1;

                        Matcher mentionName = USER_LINK_ID_PATTERN.matcher(url);
                        if (mentionName.matches()) {
                            type = MessageEntity.Type.MENTION_NAME;
                            arg = mentionName.group(1);
                        } else {
                            type = MessageEntity.Type.TEXT_URL;
                            arg = url.toString();
                        }
                    }

                    break;
                case '`':
                    if (cursor + 2 < str.length() &&
                            str.charAt(cursor + 1) == '`' &&
                            str.charAt(cursor + 2) == '`') {

                        int endArg = -1;
                        for (int i = cursor + 1; i < str.length(); i++) {
                            if (str.charAt(i) == '\n') {
                                endArg = i;
                                break;
                            }
                        }

                        if (endArg != -1) {
                            type = MessageEntity.Type.PRE;
                            length = 3;

                            if (cursor + 3 != endArg) {
                                arg = str.substring(cursor + 3, endArg).trim();
                                length += arg.length();
                            }
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

            if (type == null) {
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
        return null;
    }
}
