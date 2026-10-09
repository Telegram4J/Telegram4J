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

import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;
import telegram4j.mtproto.store.SecretChatOutbox;
import telegram4j.tl.SecretTlInfo;

/**
 * What the end-to-end protocol fixes, rather than what this implementation chooses.
 *
 * <p> Everything here is either written into the schema or read off another client, so it holds
 * for anyone speaking the protocol and belongs to none of the classes that use it. Sizes, delays
 * and limits which are this implementation's own judgement stay with the class whose judgement
 * they are.
 */
public final class SecretChatUtils {

    /** The highest layer of the end-to-end schema this implementation can speak. */
    public static final int LAYER = SecretTlInfo.LAYER;

    /** The layer to assume for a peer which has not told us its own yet. */
    public static final int DEFAULT_LAYER = 46;

    /**
     * Length of the padding inside {@code decryptedMessageLayer}.
     *
     * <p> The schema requires at least 15 bytes, and this is what TDLib writes; matching it keeps
     * our messages from standing out by their padding alone.
     */
    public static final int RANDOM_BYTES = 31;

    /**
     * How many messages one {@code decryptedMessageActionResend} may ask for.
     *
     * <p> TDLib's {@code MAX_RESEND_COUNT}, which is the bound it enforces on requests it
     * receives; asking for more is one of the errors it answers by discarding the chat.
     */
    public static final int MAX_REQUESTED_RESENDS = 1000;

    /** How long a key may stay in use however few messages it carries. */
    public static final Duration MAX_KEY_AGE = Duration.ofDays(7);

    /** Messages a key may carry before it must be replaced, as in TDLib and MadelineProto. */
    public static final int DEFAULT_MESSAGES_PER_KEY = 100;

    /** Known maximum of UTF-16 symbols allowed in a single encrypted message in 2026 */
    public static final int MAX_ENCRYPTED_MESSAGE_SIZE = 4096;

    private SecretChatUtils() {
        throw new AssertionError();
    }

    /**
     * {@return The layer to announce on a message to the given chat}
     *
     * <p> Neither side gains from claiming a layer the other cannot read, and the value never
     * decreases over a chat's messages, which is what a peer checking it for monotonicity relies
     * on.
     */
    public static int negotiatedLayer(SecretChatState state) {
        return Math.min(LAYER, Math.max(state.layer(), DEFAULT_LAYER));
    }

    /**
     * {@return Whether the peer can still be expected to decrypt a message we kept}
     *
     * <p> An outbox holds ciphertext, so a message outlives the key it was encrypted with while
     * being unreadable to anyone once that key is two replacements old: each side keeps the
     * current key and the one before it, and nothing further back. Sending such a message would
     * not merely fail to help, it would reach the peer as a message under an unknown key, which
     * is not something a chat is expected to survive.
     *
     * <p> Which key it was is in the message itself: an encrypted body starts with the fingerprint
     * of the key it was encrypted with, which is also how an incoming one is told apart.
     */
    public static boolean stillReadable(SecretChatState state, SecretChatOutbox.Sent sent) {
        byte[] payload = sent.payload();
        if (payload.length < Long.BYTES) {
            return false;
        }

        long fingerprint = Unpooled.wrappedBuffer(payload).getLongLE(0);
        var key = state.key();
        var oldKey = state.oldKey();
        return key != null && key.id() == fingerprint
                || oldKey != null && oldKey.id() == fingerprint;
    }

    public static Properties loadFile(String name) {
        var properties = new Properties();
        var path = Path.of(name);
        if (Files.isReadable(path)) {
            try (var in = Files.newBufferedReader(path)) {
                properties.load(in);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read " + path.toAbsolutePath(), e);
            }
        }
        return properties;
    }

    public static Properties loadResource(String name) {
        var properties = new Properties();
        try (var in = SecretChatUtils.class.getResourceAsStream(name)) {
            if (in != null) {
                properties.load(in);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + name, e);
        }
        return properties;
    }

    /**
     * The encryption key as the apps lay it out, eight bytes to a row in groups of four, so it
     * can be read off against the other device without counting.
     *
     * <p> Apps show the first 32 of the 36 bytes the state carries.
     *
     * @param indent What to put before each row, which is the caller's layout rather than this
     * one's: the rows only have to line up with each other.
     */
    public static String visualisation(byte[] keyHash, String indent) {
        var out = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            if (i % 8 == 0) {
                out.append('\n').append(indent);
            } else if (i % 4 == 0) {
                out.append("  ");
            }
            out.append(String.format("%02X ", keyHash[i]));
        }
        return out.toString();
    }
}
