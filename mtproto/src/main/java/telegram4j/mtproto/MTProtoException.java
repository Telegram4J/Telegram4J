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
package telegram4j.mtproto;

import telegram4j.mtproto.auth.AuthorizationException;

import java.io.Serial;

/** General base class for all mtproto level problems. */
public sealed class MTProtoException extends RuntimeException
        permits DiscardedRpcRequestException, RpcException, TransportException, AuthorizationException {

    @Serial
    private static final long serialVersionUID = 2419676857037952979L;

    public MTProtoException() {
    }

    public MTProtoException(Throwable cause) {
        super(cause);
    }

    public MTProtoException(String message) {
        super(message);
    }
}
