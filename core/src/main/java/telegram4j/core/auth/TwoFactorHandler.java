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
package telegram4j.core.auth;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;
import telegram4j.core.auth.AuthorizationHandler.Resources;
import telegram4j.mtproto.DcId;
import telegram4j.mtproto.RpcException;
import telegram4j.mtproto.internal.Preconditions;
import telegram4j.mtproto.util.CryptoUtil;
import telegram4j.tl.BaseInputCheckPasswordSRP;
import telegram4j.tl.ImmutableBaseInputCheckPasswordSRP;
import telegram4j.tl.PasswordKdfAlgoSHA256SHA256PBKDF2HMACSHA512iter100000SHA256ModPow;
import telegram4j.tl.account.Password;
import telegram4j.tl.auth.Authorization;
import telegram4j.tl.request.account.GetPassword;
import telegram4j.tl.request.auth.ImmutableCheckPassword;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

import static telegram4j.mtproto.util.CryptoUtil.*;

// Ported version of https://gist.github.com/andrew-ld/524332536dbc8c525ed80d281855a0d4 and
// https://github.com/DrKLO/Telegram/blob/abb896635f849a93968a2ba35a944c91b4978be4/TMessagesProj/src/main/java/org/telegram/messenger/SRPHelper.java#L29
/**
 * Base implementation of 2FA password handler.
 *
 * @apiNote This class not intended to be shared, as it not thread-safe.
 */
public class TwoFactorHandler {
    protected static final Logger log = Loggers.getLogger(TwoFactorHandler.class);

    protected final Resources res;
    protected final Callback callback;
    protected final Context ctx;

    public TwoFactorHandler(Resources res, Callback callback, String id) {
        this.res = Objects.requireNonNull(res);
        this.callback = Objects.requireNonNull(callback);
        this.ctx = new Context(id);
    }

    public Mono<Authorization> process() {
        return getPassword().flatMap(srp -> {
                    ctx.init(srp);

                    return callback.on2FAPassword(res, ctx)
                            .flatMap(password -> {
                                ctx.password = password;

                                var srpInput = generateSRP(ctx);

                                return checkPassword(srpInput);
                            });
                })
                .onErrorResume(RpcException.isErrorMessage("PASSWORD_HASH_INVALID"), e ->
                        callback.on2FAPasswordInvalid(res, ctx)
                                .flatMap(actionType -> switch (actionType) {
                                    case RETRY -> process();
                                    case STOP -> this.<Authorization>cancel();
                                }));
    }

    /**
     * Interface for controlling 2FA flow.
     *
     * @implSpec It's preferable to make the implementation ready for sharing.
     */
    public interface Callback {

        /**
         * Reads required 2FA password from input.
         *
         * @param res An auth flow resources.
         * @param ctx Current context of 2FA. Do not cache this value.
         * @return A {@link Mono} emitting on successful completion 2FA password.
         * Any emitted errors or empty signals will terminate auth flow.
         */
        Mono<String> on2FAPassword(Resources res, Context ctx);

        /**
         * Handles errors for invalid 2FA password.
         *
         * @param res An auth flow resources.
         * @param ctx Current context of 2FA. Do not cache this value.
         * @return A {@link Mono} emitting on successful completion action to do.
         * Any emitted errors or empty signals will terminate auth flow.
         */
        default Mono<ActionType> on2FAPasswordInvalid(Resources res, Context ctx) {
            return Mono.fromSupplier(() -> {
                ctx.log("Specified 2FA password is invalid, retrying...");
                return ActionType.RETRY;
            });
        }

        /** Enumeration that represents action for handling errors. */
        enum ActionType {
            /** Retry current auth step, like a resending of code or re-checking 2FA password. */
            RETRY,

            /** Cancel and stop current auth. */
            STOP
        }
    }

    /**
     * Context available when entering a 2FA password.
     *
     * @apiNote Class not intended to be cached when passed to the callback methods, as it
     * not thread-safe and mutable.
     */
    public static class Context extends AuthContext {

        protected String password;
        protected Password srp;
        // Terrible class name.
        protected PasswordKdfAlgoSHA256SHA256PBKDF2HMACSHA512iter100000SHA256ModPow algo;

        protected final MessageDigest sha256 = CryptoUtil.createDigest("SHA-256");

        protected Context(String id) {
            super(id);
        }

        protected ByteBuf sha256Digest(ByteBuf first) {
            sha256.reset();
            sha256.update(first.nioBuffer());
            return Unpooled.wrappedBuffer(sha256.digest());
        }

        protected ByteBuf sha256Digest(ByteBuf... bufs) {
            sha256.reset();
            for (ByteBuf b : bufs) {
                sha256.update(b.nioBuffer());
            }
            return Unpooled.wrappedBuffer(sha256.digest());
        }

        protected void init(Password srp) {
            if (!srp.hasPassword()) {
                throw new UnsupportedOperationException("No 2FA password");
            }

            var currentAlgo = srp.currentAlgo();
            if (!(currentAlgo instanceof PasswordKdfAlgoSHA256SHA256PBKDF2HMACSHA512iter100000SHA256ModPow algo)) {
                throw new IllegalStateException("Unexpected type of current algorithm: " + currentAlgo);
            }

            this.srp = srp;
            this.algo = algo;
        }

        @Override
        protected void log0(String message) {
            log.info(message);
        }

        // Public API
        // =============

        /** {@return A current SRP parameters} */
        public final Password srp() {
            Password srp = this.srp;
            Preconditions.requireState(srp != null, "srp has not initialized yet");
            return srp;
        }
    }

    // Implementation code
    // ======================

    protected Mono<Password> getPassword() {
        return res.clientGroup().send(DcId.main(), GetPassword.instance());
    }

    protected <T> Mono<T> cancel() {
        return Mono.empty();
    }

    protected BaseInputCheckPasswordSRP generateSRP(Context ctx) {
        var g = BigInteger.valueOf(ctx.algo.g());
        var gBytes = toBytesPadded(g);
        var pBytes = ctx.algo.p();

        var salt1 = ctx.algo.salt1();
        var salt2 = ctx.algo.salt2();

        var k = fromByteBuf(ctx.sha256Digest(pBytes, gBytes));
        var p = fromByteBuf(pBytes.retain());

        var hash1 = ctx.sha256Digest(salt1, Unpooled.wrappedBuffer(ctx.password.getBytes(StandardCharsets.UTF_8)), salt1);
        var hash2 = ctx.sha256Digest(salt2, hash1, salt2);
        var hash3 = pbkdf2HmacSha512Iter100000(hash2, salt1);

        var x = fromByteBuf(ctx.sha256Digest(salt2, hash3, salt2));

        random.setSeed(toByteArray(ctx.srp.secureRandom()));

        var a = random2048Number();
        var gA = g.modPow(a, p);
        var gABytes = toBytesPadded(gA);

        var srpB = ctx.srp.srpB();
        Objects.requireNonNull(srpB);
        var b = fromByteBuf(srpB);
        var bBytes = toBytesPadded(b);

        var u = fromByteBuf(ctx.sha256Digest(gABytes, bBytes));

        var bkgx = b.subtract(k.multiply(g.modPow(x, p)).mod(p));
        if (bkgx.compareTo(BigInteger.ZERO) < 0) {
            bkgx = bkgx.add(p);
        }

        var s = bkgx.modPow(a.add(u.multiply(x)), p);
        var sBytes = toBytesPadded(s);
        var kBytes = ctx.sha256Digest(sBytes);

        // TODO: checks

        var m1 = ctx.sha256Digest(
                xor(ctx.sha256Digest(pBytes), ctx.sha256Digest(gBytes)),
                ctx.sha256Digest(salt1),
                ctx.sha256Digest(salt2),
                gABytes, bBytes, kBytes
        );

        long srpId = Objects.requireNonNull(ctx.srp.srpId());
        return ImmutableBaseInputCheckPasswordSRP.of(srpId, gABytes, m1);
    }

    protected Mono<Authorization> checkPassword(BaseInputCheckPasswordSRP inputCheckPasswordSRP) {
        return res.clientGroup().send(DcId.main(), ImmutableCheckPassword.of(inputCheckPasswordSRP));
    }

    // Implementation details
    // =========================

    // > the numbers must be used in big-endian form, padded to 2048 bits
    // Copied and adapted from https://github.com/DrKLO/Telegram/blob/dfd74f809e97d1ecad9672fc7388cb0223a95dfc/TMessagesProj/src/main/java/org/telegram/messenger/SRPHelper.java#L9
    static ByteBuf toBytesPadded(BigInteger value) {
        var bytes = value.toByteArray();
        if (bytes.length > 256) {
            var correctedAuth = new byte[256];
            System.arraycopy(bytes, 1, correctedAuth, 0, 256);
            return Unpooled.wrappedBuffer(correctedAuth);
        } else if (bytes.length < 256) {
            var correctedAuth = new byte[256];
            System.arraycopy(bytes, 0, correctedAuth, 256 - bytes.length, bytes.length);
            for (int a = 0; a < 256 - bytes.length; a++) {
                correctedAuth[a] = 0;
            }
            return Unpooled.wrappedBuffer(correctedAuth);
        }
        return Unpooled.wrappedBuffer(bytes);
    }

    static BigInteger random2048Number() {
        var b = new byte[2048 / 8];
        random.nextBytes(b);
        return fromByteArray(b);
    }

    static ByteBuf pbkdf2HmacSha512Iter100000(ByteBuf password, ByteBuf salt) {
        try {
            var saltBytes = ByteBufUtil.getBytes(salt);
            var passwdBytes = ByteBufUtil.getBytes(password);
            Mac prf = Mac.getInstance("HmacSHA512");
            return Unpooled.wrappedBuffer(deriveKey(prf, passwdBytes, saltBytes, 100000, 512));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    // copied from com.sun.crypto.provider.PBKDF2KeyImpl
    // because the public interface does not allow to work directly with byte arrays
    static byte[] deriveKey(Mac prf, byte[] password, byte[] salt, int iterCount, int keyLengthInBit) {
        int keyLength = keyLengthInBit / 8;
        var key = new byte[keyLength];
        try {
            int hlen = prf.getMacLength();
            int intL = (keyLength + hlen - 1) / hlen; // ceiling
            int intR = keyLength - (intL - 1) * hlen; // residue
            var ui = new byte[hlen];
            var ti = new byte[hlen];
            // SecretKeySpec cannot be used, since password can be empty here.
            SecretKey macKey = new SecretKey() {
                @Override
                public String getAlgorithm() {
                    return prf.getAlgorithm();
                }

                @Override
                public String getFormat() {
                    return "RAW";
                }

                @Override
                public byte[] getEncoded() {
                    return password.clone();
                }

                @Override
                public int hashCode() {
                    return Arrays.hashCode(password) * 41 +
                            prf.getAlgorithm().toLowerCase(Locale.ENGLISH).hashCode();
                }

                @Override
                public boolean equals(Object obj) {
                    if (this == obj) return true;
                    if (this.getClass() != obj.getClass()) return false;
                    SecretKey sk = (SecretKey) obj;
                    return prf.getAlgorithm().equalsIgnoreCase(sk.getAlgorithm()) &&
                            MessageDigest.isEqual(password, sk.getEncoded());
                }
            };
            prf.init(macKey);

            var ibytes = new byte[4];
            for (int i = 1; i <= intL; i++) {
                prf.update(salt);
                ibytes[3] = (byte) i;
                ibytes[2] = (byte) ((i >> 8) & 0xff);
                ibytes[1] = (byte) ((i >> 16) & 0xff);
                ibytes[0] = (byte) ((i >> 24) & 0xff);
                prf.update(ibytes);
                prf.doFinal(ui, 0);
                System.arraycopy(ui, 0, ti, 0, ui.length);

                for (int j = 2; j <= iterCount; j++) {
                    prf.update(ui);
                    prf.doFinal(ui, 0);
                    // XOR the intermediate Ui's together.
                    for (int k = 0; k < ui.length; k++) {
                        ti[k] ^= ui[k];
                    }
                }
                if (i == intL) {
                    System.arraycopy(ti, 0, key, (i - 1) * hlen, intR);
                } else {
                    System.arraycopy(ti, 0, key, (i - 1) * hlen, hlen);
                }
            }
        } catch (GeneralSecurityException gse) {
            throw new RuntimeException("Error deriving PBKDF2 keys", gse);
        }
        return key;
    }
}
