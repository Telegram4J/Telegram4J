package telegram4j.mtproto;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import telegram4j.mtproto.util.CryptoUtil;
import telegram4j.tl.TlSerialUtil;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class PublicRsaKey {
    private final BigInteger modulus;
    private final BigInteger exponent;

    public static final Map<Long, PublicRsaKey> publicKeys;

    static {

        publicKeys = Map.of(
                // dc 2
                // -----BEGIN PUBLIC KEY-----
                // MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAruw2yP/BCcsJliRoW5eB
                // VBVle9dtjJw+OYED160Wybum9SXtBBLXriwt4rROd9csv0t0OHCaTmRqBcQ0J8fx
                // hN6/cpR1GWgOZRUAiQxoMnlt0R93LCX/j1dnVa/gVbCjdSxpbrfY2g2L4frzjJvd
                // l84Kd9ORYjDEAyFnEA7dD556OptgLQQ2e2iVNq8NZLYTzLp5YpOdO1doK+ttrltg
                // gTCy5SrKeLoCPPbOgGsdxJxyz5KKcZnSLj16yE5HvJQn0CNpRdENvRUXe6tBP78O
                // 39oJ8BTHp9oIjd6XWXAsp2CvK45Ol8wFXGF710w9lwCGNbmNxNYhtIkdqfsEcwR5
                // JwIDAQAB
                // -----END PUBLIC KEY-----

                0xbc35f3509f7b7a5L, create(new BigInteger("AEEC36C8FFC109CB099624685B97815415657BD76D8C9C3E398103D7A" +
                                "D16C9BBA6F525ED0412D7AE2C2DE2B44E77D72CBF4B7438709A4E646A05C43427C7F184DEBF72947519680E" +
                                "651500890C6832796DD11F772C25FF8F576755AFE055B0A3752C696EB7D8DA0D8BE1FAF38C9BDD97CE0A77D" +
                                "3916230C4032167100EDD0F9E7A3A9B602D04367B689536AF0D64B613CCBA7962939D3B57682BEB6DAE5B60" +
                                "8130B2E52ACA78BA023CF6CE806B1DC49C72CF928A7199D22E3D7AC84E47BC9427D0236945D10DBD15177BA" +
                                "B413FBF0EDFDA09F014C7A7DA088DDE9759702CA760AF2B8E4E97CC055C617BD74C3D97008635B98DC4D621" +
                                "B4891DA9FB0473047927", 16),
                        new BigInteger("010001", 16)),

                // cdn dc 121
                // -----BEGIN RSA PUBLIC KEY-----
                // MIIBCgKCAQEA4tWHcGJlElkxuxKQJwFjJaulmVHgdxNA3wgI2E8XbNnA88y51Xog
                // V5m8BEYuTSP4llXZY4ZSJW5VlFXnmsJT/hmjyeFqqTajyAW6nb9vwZX291QvqD/1
                // ZCFBy7TLvCM0lbNIEhcLMf33ZV8AetLAd+uRLF6QHosys5w0iJ7x+UbGwDxyfeic
                // 8EJJnsKaXrUOwRycMRN+V/zDySa0EYl1u1EB1MDX1/jIV1IQEbLvdBH4vsVTVEdW
                // KHlzOcFzT9qX/g8XibCPiHLJvqQb8hVibvs9NaANyClcBEt3mOucG1/46Lilkc/K
                // d4nlCcohk0jIHNp8symUzNWRPUGmTs3SPwIDAQAB
                // -----END RSA PUBLIC KEY-----

                0x995effd323b5db80L, create(new BigInteger("E2D587706265125931BB129027016325ABA59951E0771340DF0808D8" +
                                "4F176CD9C0F3CCB9D57A205799BC04462E4D23F89655D9638652256E559455E79AC253FE19A3C9E16AA936A" +
                                "3C805BA9DBF6FC195F6F7542FA83FF5642141CBB4CBBC233495B34812170B31FDF7655F007AD2C077EB912C" +
                                "5E901E8B32B39C34889EF1F946C6C03C727DE89CF042499EC29A5EB50EC11C9C31137E57FCC3C926B411897" +
                                "5BB5101D4C0D7D7F8C857521011B2EF7411F8BEC55354475628797339C1734FDA97FE0F1789B08F8872C9BE" +
                                "A41BF215626EFB3D35A00DC8295C044B7798EB9C1B5FF8E8B8A591CFCA7789E509CA219348C81CDA7CB3299" +
                                "4CCD5913D41A64ECDD23F", 16),
                        new BigInteger("010001", 16)),

                // cdn dc 201
                // -----BEGIN RSA PUBLIC KEY-----
                // MIIBCgKCAQEAug6fETVb7NkXYYu5ueZuM0pqw1heuqUrZNYomQN0lS0o7i6mAWwb
                // 1/FiscFK+y4LQSSEx+oUzXAhjmll9fmb4e7PbUiXo8MuXO0Rj3e5416DXfTiOYGW
                // XlFRV0aQzu8agy1epKwkFDidnmy7g5rJJV0q1+3eR+Jk2OEc/B6lMAOv3fBU6xhE
                // ZByN9gqc6fvkNo13PQ8JYZUSGttzLlYy76uFmvFBhRsJU+LNQ2+bsTHwafSffVYl
                // Z2boJOblvqbRWe453CzssaSWywGXOQmWvVbEe7F8q1ki/s7S8BxYWrhSLJ6bsu9V
                // ZWnIHD9vB34QF8IABPRE93mhCOHBqJxSBQIDAQAB
                // -----END RSA PUBLIC KEY-----

                0xc884b3e62d09e5c5L, create(new BigInteger("BA0E9F11355BECD917618BB9B9E66E334A6AC3585EBAA52B64D62899" +
                                "0374952D28EE2EA6016C1BD7F162B1C14AFB2E0B412484C7EA14CD70218E6965F5F99BE1EECF6D4897A3C32" +
                                "E5CED118F77B9E35E835DF4E23981965E5151574690CEEF1A832D5EA4AC2414389D9E6CBB839AC9255D2AD7" +
                                "EDDE47E264D8E11CFC1EA53003AFDDF054EB1844641C8DF60A9CE9FBE4368D773D0F096195121ADB732E563" +
                                "2EFAB859AF141851B0953E2CD436F9BB131F069F49F7D56256766E824E6E5BEA6D159EE39DC2CECB1A496CB" +
                                "0197390996BD56C47BB17CAB5922FECED2F01C585AB8522C9E9BB2EF556569C81C3F6F077E1017C20004F44" +
                                "4F779A108E1C1A89C5205", 16),
                        new BigInteger("010001", 16)),

                // cdn dc 203
                // -----BEGIN RSA PUBLIC KEY-----
                // MIIBCgKCAQEAv/L6td+mj7Dl81NHfu+Xf1KNtvZPR1tS5xFqkiUson1u7D2ulK05
                // jM8HKvpV1o+1HPPqhaXhasvsX90u3TIHRQ0zuJKJxKAiZo3GK7phHozjAJ9VUFbO
                // 7jKAa5BTE9tXgA5ZwJAiQWb3U6ykwRzk3fFRe5WaW7xfVUiepxyWGdr1eecoWCfB
                // af1TCXfcS7vcyljNT03pwt2YyS5iXE5IB5wBB5yqSSm4GYtWWR67UjIsXBd77TRp
                // foLGpfOdUHxBz4ZSj8D76m1zlpID5J2pF6bH4+ZCz0SUpv3j7bE8WFlvgMfwEPhw
                // xMYidRGayq9YlLlYd4D+Yoq0U6jS3MWTRQIDAQAB
                // -----END RSA PUBLIC KEY-----

                0xbb27580fd5b01626L, create(new BigInteger("BFF2FAB5DFA68FB0E5F353477EEF977F528DB6F64F475B52E7116A92" +
                                "252CA27D6EEC3DAE94AD398CCF072AFA55D68FB51CF3EA85A5E16ACBEC5FDD2EDD3207450D33B89289C4A02" +
                                "2668DC62BBA611E8CE3009F555056CEEE32806B905313DB57800E59C090224166F753ACA4C11CE4DDF1517B" +
                                "959A5BBC5F55489EA71C9619DAF579E7285827C169FD530977DC4BBBDCCA58CD4F4DE9C2DD98C92E625C4E4" +
                                "8079C01079CAA4929B8198B56591EBB52322C5C177BED34697E82C6A5F39D507C41CF86528FC0FBEA6D7396" +
                                "9203E49DA917A6C7E3E642CF4494A6FDE3EDB13C58596F80C7F010F870C4C62275119ACAAF5894B9587780F" +
                                "E628AB453A8D2DCC59345", 16),
                        new BigInteger("010001", 16))
        );
    }

    PublicRsaKey(BigInteger modulus, BigInteger exponent) {
        this.modulus = modulus;
        this.exponent = exponent;
    }

    public static long computeTail(ByteBufAllocator alloc, PublicRsaKey key) {
        ByteBuf modulusBytes = TlSerialUtil.serializeBytes(alloc, CryptoUtil.toByteArray(key.modulus));
        ByteBuf exponentBytes = TlSerialUtil.serializeBytes(alloc, CryptoUtil.toByteArray(key.exponent));

        ByteBuf conc = Unpooled.wrappedBuffer(modulusBytes, exponentBytes);
        byte[] sha1 = CryptoUtil.sha1Digest(CryptoUtil.toByteArray(conc));
        byte[] tail = CryptoUtil.substring(sha1, sha1.length - 8, 8);
        CryptoUtil.reverse(tail);

        return Unpooled.wrappedBuffer(tail).readLong();
    }

    public static PublicRsaKey create(BigInteger exponent, BigInteger modulus) {
        return new PublicRsaKey(exponent, modulus);
    }

    public BigInteger getModulus() {
        return modulus;
    }

    public BigInteger getExponent() {
        return exponent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PublicRsaKey publicKey = (PublicRsaKey) o;
        return modulus.equals(publicKey.modulus) && exponent.equals(publicKey.exponent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modulus, exponent);
    }

    @Override
    public String toString() {
        return "PublicRsaKey{" +
                "modulus=" + modulus +
                ", exponent=" + exponent +
                '}';
    }
}
