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
package telegram4j.mtproto.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/** Utility class with set of most-used cryptography and bytebuf methods. */
public final class CryptoUtil {

    private CryptoUtil() {
    }

    public static final SecureRandom random;

    static {
        SecureRandom possibleStrong;
        try {
            possibleStrong = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            possibleStrong = new SecureRandom();
        }

        random = possibleStrong;
    }

    public static BigInteger fromByteArray(byte[] data) {
        return new BigInteger(1, data);
    }

    public static BigInteger fromByteBuf(ByteBuf data) {
        return new BigInteger(1, toByteArray(data));
    }

    public static ByteBuf toByteBuf(BigInteger val) {
        byte[] res = val.toByteArray();
        if (res[0] == 0) {
            return Unpooled.wrappedBuffer(res, 1, res.length - 1);
        }
        return Unpooled.wrappedBuffer(res);
    }

    public static byte[] toByteArray(ByteBuf buf) {
        try {
            return ByteBufUtil.getBytes(buf);
        } finally {
            ReferenceCountUtil.safeRelease(buf);
        }
    }

    public static MessageDigest createDigest(String alg) {
        try {
            return MessageDigest.getInstance(alg);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static ByteBuf xor(ByteBuf a, ByteBuf b) {
        ByteBuf res = Unpooled.buffer(a.readableBytes());
        for (int i = 0, n = a.readableBytes(); i < n; i++) {
            res.writeByte((byte) (a.getByte(i) ^ b.getByte(i)));
        }
        return res;
    }

    // TODO: can be done with System.arraycopy()
    public static ByteBuf reverse(ByteBuf buf) {
        ByteBuf result = buf.alloc().buffer(buf.readableBytes());
        for (int i = buf.writerIndex() - 1; i >= buf.readerIndex(); i--) {
            result.writeByte(buf.getByte(i));
        }
        return result;
    }
}
