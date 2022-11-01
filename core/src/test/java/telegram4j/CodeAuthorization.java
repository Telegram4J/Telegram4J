package telegram4j;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.retry.Retry;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.mtproto.RpcException;
import telegram4j.tl.ImmutableBaseInputCheckPasswordSRP;
import telegram4j.tl.ImmutableCodeSettings;
import telegram4j.tl.PasswordKdfAlgoSHA256SHA256PBKDF2HMACSHA512iter100000SHA256ModPow;
import telegram4j.tl.auth.Authorization;
import telegram4j.tl.auth.BaseAuthorization;
import telegram4j.tl.auth.CodeType;
import telegram4j.tl.auth.SentCode;
import telegram4j.tl.request.auth.SignIn;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

import static telegram4j.mtproto.util.CryptoUtil.*;

/**
 * Simple implementation of user auth flow, which
 * can only handle SMS and APP codes.
 */
public class CodeAuthorization {

    static final String delimiter = "=".repeat(32);

    CodeAuthorization(MTProtoTelegramClient client) {
        this.client = client;
    }

    SentCode currentCode;
    String phoneNumber;
    boolean first2fa = true;

    final MTProtoTelegramClient client;
    final Sinks.Many<State> state = Sinks.many().replay()
            .latestOrDefault(State.SEND_CODE);
    final Scanner sc = new Scanner(System.in);

    enum State {
        SEND_CODE,
        AWAIT_CODE,
        RESEND_CODE,
        CANCEL_CODE,
        SIGN_IN
    }

    static String readPhoneNumber(Scanner sc) {
        String phoneNumber = sc.nextLine();
        if (phoneNumber.startsWith("+"))
            phoneNumber = phoneNumber.substring(1);
        phoneNumber = phoneNumber.replaceAll(" ", "");
        return phoneNumber;
    }

    Publisher<?> begin() {
        return state.asFlux()
                .flatMap(s -> {
                    switch (s) {
                        case SEND_CODE:
                            synchronized (System.out) {
                                System.out.println(delimiter);
                                System.out.print("Write your phone number: ");
                            }

                            phoneNumber = readPhoneNumber(sc);
                            int apiId = client.getAuthResources().getApiId();
                            String apiHash = client.getAuthResources().getApiHash();

                            return client.getServiceHolder().getAuthService()
                                    .sendCode(phoneNumber, apiId, apiHash, ImmutableCodeSettings.of())
                                    .flatMapMany(this::applyCode);
                        case RESEND_CODE:
                            return client.getServiceHolder().getAuthService()
                                    .resendCode(phoneNumber, currentCode.phoneCodeHash())
                                    .flatMapMany(this::applyCode);
                        case AWAIT_CODE:
                            synchronized (System.out) {
                                System.out.println(delimiter);
                                System.out.println("Invalid phone code, please write it again");
                            }
                            return applyCode(currentCode);
                        case CANCEL_CODE:
                            System.out.println("Good buy, " + System.getProperty("user.name"));
                            state.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);
                            return client.disconnect();
                        case SIGN_IN:
                            state.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);
                            return Mono.empty();
                        default:
                            return Flux.error(new IllegalStateException());
                    }
                })
                .then();
    }

    public static Publisher<?> authorize(MTProtoTelegramClient client) {
        return Flux.defer(() -> Flux.from(new CodeAuthorization(client).begin()));
    }

    Publisher<?> applyCode(SentCode scode) {
        boolean resend = currentCode != null;
        currentCode = scode;

        Instant sendTimestamp = Instant.now();
        Integer t = scode.timeout();
        synchronized (System.out) {
            System.out.println(delimiter);
            StringJoiner j = new StringJoiner(", ");
            j.add("code type: " + scode.type());
            if (t != null) {
                j.add("expires at: " + sendTimestamp.plusSeconds(t));
            }
            CodeType nextType = scode.nextType();
            if (nextType != null) {
                j.add("next code type: " + nextType);
            }
            System.out.println(j);
            System.out.print((resend ? "New code" : "Code") + " has been sent, write it: ");
            j.add((resend ? "New" : "Sent") + " code: ");
        }

        String code = sc.nextLine();
        boolean expired = t != null && sendTimestamp.plusSeconds(t).isAfter(Instant.now());
        if (expired || code.equalsIgnoreCase("resend")) {
            if (expired) {
                synchronized (System.out) {
                    System.out.println(delimiter);
                    System.out.println("Code has expired... Sending a new one");
                }
            }

            state.emitNext(State.RESEND_CODE, Sinks.EmitFailureHandler.FAIL_FAST);
            return Mono.empty();
        }

        if (code.equalsIgnoreCase("cancel")) {
            return client.getServiceHolder().getAuthService()
                    .cancelCode(phoneNumber, scode.phoneCodeHash())
                    .doOnNext(b -> {
                        synchronized (System.out) {
                            System.out.println(delimiter);
                            if (b) {
                                System.out.println("Phone code successfully canceled");
                            } else {
                                System.out.println("Failed to cancel phone code");
                            }
                        }

                        state.emitNext(State.CANCEL_CODE, Sinks.EmitFailureHandler.FAIL_FAST);
                    });
        }

        return client.getServiceHolder().getAuthService()
                .signIn(SignIn.builder()
                        .phoneNumber(phoneNumber)
                        .phoneCode(code)
                        .phoneCodeHash(scode.phoneCodeHash())
                        .build())
                .onErrorResume(RpcException.isErrorMessage("PHONE_CODE_INVALID"), e -> Mono.fromRunnable(() ->
                        state.emitNext(State.AWAIT_CODE, Sinks.EmitFailureHandler.FAIL_FAST)))
                .onErrorResume(RpcException.isErrorMessage("SESSION_PASSWORD_NEEDED"), e -> begin2FA()
                        .retryWhen(Retry.indefinitely()
                                .filter(RpcException.isErrorMessage("PASSWORD_HASH_INVALID"))))
                .cast(BaseAuthorization.class)
                .doOnNext(a -> {
                    synchronized (System.out) {
                        System.out.println(delimiter);
                        System.out.println("Authorization is successful: " + a);
                    }

                    state.emitNext(State.SIGN_IN, Sinks.EmitFailureHandler.FAIL_FAST);
                });
    }

    // Ported version of https://gist.github.com/andrew-ld/524332536dbc8c525ed80d281855a0d4 and
    // https://github.com/DrKLO/Telegram/blob/abb896635f849a93968a2ba35a944c91b4978be4/TMessagesProj/src/main/java/org/telegram/messenger/SRPHelper.java#L29
    Mono<Authorization> begin2FA() {
        return client.getServiceHolder().getAccountService().getPassword().flatMap(pswrd -> {
            if (!pswrd.hasPassword()) {
                return Mono.error(new IllegalStateException("?".repeat(1 << 4)));
            }

            var currentAlgo = pswrd.currentAlgo();
            if (!(currentAlgo instanceof PasswordKdfAlgoSHA256SHA256PBKDF2HMACSHA512iter100000SHA256ModPow)) {
                return Mono.error(new IllegalStateException("Unexpected type of current algorithm: " + currentAlgo));
            }

            synchronized (System.out) {
                System.out.println(delimiter);
                if (first2fa) {
                    first2fa = false;
                    System.out.print("The account is protected by 2FA, please write password");
                } else {
                    System.out.print("Invalid password, please write it again");
                }
                String hint = pswrd.hint();
                if (hint != null) {
                    System.out.print(" (Hint \"" + hint + "\")");
                }
                System.out.print(": ");
            }

            var password = sc.nextLine();
            var algo = (PasswordKdfAlgoSHA256SHA256PBKDF2HMACSHA512iter100000SHA256ModPow) currentAlgo;

            var g = BigInteger.valueOf(algo.g());
            var gBytes = toBytesPadded(g);
            var pBytes = algo.p();

            var salt1 = algo.salt1();
            var salt2 = algo.salt2();

            var k = fromByteBuf(sha256Digest(pBytes, gBytes));
            var p = fromByteBuf(pBytes.retain());

            var hash1 = sha256Digest(salt1, Unpooled.wrappedBuffer(password.getBytes(StandardCharsets.UTF_8)), salt1);
            var hash2 = sha256Digest(salt2, hash1, salt2);
            var hash3 = pbkdf2HmacSha512Iter100000(hash2, salt1);

            var x = fromByteBuf(sha256Digest(salt2, hash3, salt2));

            random.setSeed(toByteArray(pswrd.secureRandom()));

            var a = random2048Number();
            var gA = g.modPow(a, p);
            var gABytes = toBytesPadded(gA);

            var srpB = pswrd.srpB();
            Objects.requireNonNull(srpB);
            var b = fromByteBuf(srpB);
            var bBytes = toBytesPadded(b);

            var u = fromByteBuf(sha256Digest(gABytes, bBytes));

            var bkgx = b.subtract(k.multiply(g.modPow(x, p)).mod(p));
            if (bkgx.compareTo(BigInteger.ZERO) < 0) {
                bkgx = bkgx.add(p);
            }

            var s = bkgx.modPow(a.add(u.multiply(x)), p);
            var sBytes = toBytesPadded(s);
            var kBytes = sha256Digest(sBytes);

            // TODO: checks

            var m1 = sha256Digest(
                    xor(sha256Digest(pBytes), sha256Digest(gBytes)),
                    sha256Digest(salt1),
                    sha256Digest(salt2),
                    gABytes, bBytes, kBytes
            );

            long srpId = Objects.requireNonNull(pswrd.srpId());
            var icpsrp = ImmutableBaseInputCheckPasswordSRP.of(srpId, gABytes, m1);

            return client.getServiceHolder().getAuthService()
                    .checkPassword(icpsrp);
        });
    }

    // > the numbers must be used in big-endian form, padded to 2048 bits
    public static ByteBuf toBytesPadded(BigInteger value) {
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
        int keyLength = keyLengthInBit/8;
        var key = new byte[keyLength];
        try {
            int hlen = prf.getMacLength();
            int intL = (keyLength + hlen - 1)/hlen; // ceiling
            int intR = keyLength - (intL - 1)*hlen; // residue
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
                    SecretKey sk = (SecretKey)obj;
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
                    System.arraycopy(ti, 0, key, (i-1)*hlen, intR);
                } else {
                    System.arraycopy(ti, 0, key, (i-1)*hlen, hlen);
                }
            }
        } catch (GeneralSecurityException gse) {
            throw new RuntimeException("Error deriving PBKDF2 keys", gse);
        }
        return key;
    }
}
