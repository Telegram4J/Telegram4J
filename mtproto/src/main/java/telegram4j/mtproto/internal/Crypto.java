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
package telegram4j.mtproto.internal;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.concurrent.FastThreadLocal;

import java.security.MessageDigest;

public class Crypto {
    private Crypto() {}

    public static final FastThreadLocal<MessageDigest> SHA256;
    public static final FastThreadLocal<MessageDigest> SHA1;

    static {
        SHA256 = new FastThreadLocal<>() {
            @Override
            protected MessageDigest initialValue() throws Exception {
                return MessageDigest.getInstance("SHA-256");
            }
        };

        SHA1 = new FastThreadLocal<>() {
            @Override
            protected MessageDigest initialValue() throws Exception {
                return MessageDigest.getInstance("SHA-1");
            }
        };
    }

    public static ByteBuf sha256Digest(ByteBuf first) {
        var sha256 = SHA256.get();
        sha256.reset();
        sha256.update(first.nioBuffer());
        return Unpooled.wrappedBuffer(sha256.digest());
    }

    public static ByteBuf sha256Digest(ByteBuf first, ByteBuf second) {
        var sha256 = SHA256.get();
        sha256.reset();
        sha256.update(first.nioBuffer());
        sha256.update(second.nioBuffer());
        return Unpooled.wrappedBuffer(sha256.digest());
    }

    public static ByteBuf sha1Digest(ByteBuf buf) {
        var sha1 = SHA1.get();
        sha1.reset();
        sha1.update(buf.nioBuffer());
        return Unpooled.wrappedBuffer(sha1.digest());
    }

    public static ByteBuf sha1Digest(ByteBuf first, ByteBuf second) {
        var sha1 = SHA1.get();
        sha1.reset();
        sha1.update(first.nioBuffer());
        sha1.update(second.nioBuffer());
        return Unpooled.wrappedBuffer(sha1.digest());
    }

    public static ByteBuf sha1Digest(ByteBuf first, ByteBuf second, ByteBuf third) {
        var sha1 = SHA1.get();
        sha1.reset();
        sha1.update(first.nioBuffer());
        sha1.update(second.nioBuffer());
        sha1.update(third.nioBuffer());
        return Unpooled.wrappedBuffer(sha1.digest());
    }
}
