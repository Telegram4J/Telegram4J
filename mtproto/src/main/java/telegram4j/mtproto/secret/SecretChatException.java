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
package telegram4j.mtproto.secret;

import telegram4j.mtproto.MTProtoException;

import java.io.Serial;

/**
 * Thrown when an end-to-end encrypted message can not be trusted.
 *
 * <p> Every cause of this exception is a protocol violation by the other side and never a
 * recoverable condition: the message must be dropped, and repeated occurrences are a reason
 * to discard the secret chat.
 */
public final class SecretChatException extends MTProtoException {
    @Serial
    private static final long serialVersionUID = 7532960311487431493L;

    public SecretChatException() {
    }

    public SecretChatException(Throwable cause) {
        super(cause);
    }

    public SecretChatException(String message) {
        super(message);
    }
}
