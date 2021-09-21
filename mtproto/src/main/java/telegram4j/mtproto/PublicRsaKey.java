package telegram4j.mtproto;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class PublicRsaKey {
    private final BigInteger exponent;
    private final BigInteger modulus;

    public static final Map<Long, PublicRsaKey> publicKeys;

    static {
        Map<Long, PublicRsaKey> tmp = new HashMap<>();

        tmp.put(0xc3b42b026ce86b21L, create(
                new BigInteger("c150023e2f70db7985ded064759cfecf0af328e69a41daf4d6f01b538135a6f91f8f8b2a0ec9ba9720ce" +
                        "352efcf6c5680ffc424bd634864902de0b4bd6d49f4e580230e3ae97d95c8b19442b3c0a10d8f5633fecedd6926a7f6" +
                        "dab0ddb7d457f9ea81b8465fcd6fffeed114011df91c059caedaf97625f6c96ecc74725556934ef781d866b34f011fc" +
                        "e4d835a090196e9a5f0e4449af7eb697ddb9076494ca5f81104a305b6dd27665722c46b60e5df680fb16b210607ef21" +
                        "7652e60236c255f6a28315f4083a96791d7214bf64c1df4fd0db1944fb26a2a57031b32eee64ad15a8ba68885cde74a" +
                        "5bfc920f6abf59ba5c75506373e7130f9042da922179251f", 16),
                new BigInteger("010001", 16)));

        tmp.put(0xbc35f3509f7b7a5L, create(
                new BigInteger("aeec36c8ffc109cb099624685b97815415657bd76d8c9c3e398103d7ad16c9bba6f525ed0412d7ae2c2d" +
                        "e2b44e77d72cbf4b7438709a4e646a05c43427c7f184debf72947519680e651500890c6832796dd11f772c25ff8f576" +
                        "755afe055b0a3752c696eb7d8da0d8be1faf38c9bdd97ce0a77d3916230c4032167100edd0f9e7a3a9b602d04367b68" +
                        "9536af0d64b613ccba7962939d3b57682beb6dae5b608130b2e52aca78ba023cf6ce806b1dc49c72cf928a7199d22e3" +
                        "d7ac84e47bc9427d0236945d10dbd15177bab413fbf0edfda09f014c7a7da088dde9759702ca760af2b8e4e97cc055c" +
                        "617bd74c3d97008635b98dc4d621b4891da9fb0473047927", 16),
                new BigInteger("010001", 16)));

        tmp.put(0x15ae5fa8b5529542L, create(
                new BigInteger("bdf2c77d81f6afd47bd30f29ac76e55adfe70e487e5e48297e5a9055c9c07d2b93b4ed3994d3eca5098b" +
                        "f18d978d54f8b7c713eb10247607e69af9ef44f38e28f8b439f257a11572945cc0406fe3f37bb92b79112db69eedf2d" +
                        "c71584a661638ea5becb9e23585074b80d57d9f5710dd30d2da940e0ada2f1b878397dc1a72b5ce2531b6f7dd158e09" +
                        "c828d03450ca0ff8a174deacebcaa22dde84ef66ad370f259d18af806638012da0ca4a70baa83d9c158f3552bc9158e" +
                        "69bf332a45809e1c36905a5caa12348dd57941a482131be7b2355a5f4635374f3bd3ddf5ff925bf4809ee27c1e67d912" +
                        "0c5fe08a9de458b1b4a3c5d0a428437f2beca81f4e2d5ff", 16),
                new BigInteger("010001", 16)));

        tmp.put(0xaeae98e13cd7f94fL, create(
                new BigInteger("b3f762b739be98f343eb1921cf0148cfa27ff7af02b6471213fed9daa0098976e667750324f1abcea4c3" +
                        "1e43b7d11f1579133f2b3d9fe27474e462058884e5e1b123be9cbbc6a443b2925c08520e7325e6f1a6d50e117eb61ea" +
                        "49d2534c8bb4d2ae4153fabe832b9edf4c5755fdd8b19940b81d1d96cf433d19e6a22968a85dc80f0312f596bd2530c" +
                        "1cfb28b5fe019ac9bc25cd9c2a5d8a0f3a1c0c79bcca524d315b5e21b5c26b46babe3d75d06d1cd33329ec782a0f228" +
                        "91ed1db42a1d6c0dea431428bc4d7aabdcf3e0eb6fda4e23eb7733e7727e9a1915580796c55188d2596d2665ad1182b" +
                        "a7abf15aaa5a8b779ea996317a20ae044b820bff35b6e8a1", 16),
                new BigInteger("010001", 16)));

        tmp.put(0x5a181b2235057d98L, create(
                new BigInteger("be6a71558ee577ff03023cfa17aab4e6c86383cff8a7ad38edb9fafe6f323f2d5106cbc8cafb83b869cf" +
                        "fd1ccf121cd743d509e589e68765c96601e813dc5b9dfc4be415c7a6526132d0035ca33d6d6075d4f535122a1cdfe01" +
                        "7041f1088d1419f65c8e5490ee613e16dbf662698c0f54870f0475fa893fc41eb55b08ff1ac211bc045ded31be27d12" +
                        "c96d8d3cfc6a7ae8aa50bf2ee0f30ed507cc2581e3dec56de94f5dc0a7abee0be990b893f2887bd2c6310a1e0a9e3e3" +
                        "8bd34fded2541508dc102a9c9b4c95effd9dd2dfe96c29be647d6c69d66ca500843cfaed6e440196f1dbe0e2e22163c" +
                        "61ca48c79116fa77216726749a976a1c4b0944b5121e8c01", 16),
                new BigInteger("010001", 16)));

        tmp.put(0x5931aac70e0d30f7L, create(
                new BigInteger("F8B7F73EF804D72C5B25408C6840245744324935699DA0E389E76707945BB4D5A309EA9255A9181DBAAA" +
                        "18C208BF958219D15DAEA39F30D70D4ACB4FB5253A47D526470EADAAE388CA4A52B943A37BD1FEE175482AABA3C8BD8" +
                        "849D2BEE1938C978842324A9ABB0E1B3F549BAF4DEF65141B53AA84034E15E23F3BF4103205586BDD61BDF998BEB795" +
                        "DF1924E0484C4F60497CAD934760D579441F81BABA151F61CB4CEA53FE62557E2918A608DF585E6575ECD5E16A3D2D2" +
                        "1F471919214869E265F1DD00F048B2E41F60B413BC98BF977D044A38E9ABEDAE01338468D9D7B9AEBDA2DA877B8585D" +
                        "DDC33BD1514A5E32D7303C026E3C45F77DE561C5DCDFCE99", 16),
                new BigInteger("010001", 16)));

        tmp.put(0x254672538e935938L, create(
                new BigInteger("CEE1D50BBB04E742A1A3FC83559B569E5980E417FF68CF0A658DD6CD2D7AC3AC35B01AA2A63F2880C186" +
                        "ED42DB181B5898A11A23B20824EE963369B531A5D59ECA92F1DECF6860198B2F2B48DDD2ED2D9AF30A7845765E86CD0" +
                        "9017BD9788CF8E6207208C05FC9C6C92A64B079891EB11508EE150EF1E4219A6FD4614129258ED53ADD087A68AE5114" +
                        "A9AA5450D8595CC876A161435CBDB2026F8FBF00FEDCA0A067E9C079172CCCECC09C2B16C428EC776373149DB66AAB9" +
                        "A4DEBF7916B391E832AE5A7892E27DE0AB1B4451C55F90F1F2ECE3ACEF708BC2C5EE022066EE4344C7268D724AABAAC" +
                        "667667D727AC3F2956ED4BDAF7089DDE0AEB18A6652DA16F", 16),
                new BigInteger("010001", 16)));

        publicKeys = Collections.unmodifiableMap(tmp);
    }

    PublicRsaKey(BigInteger exponent, BigInteger modulus) {
        this.exponent = exponent;
        this.modulus = modulus;
    }

    public static PublicRsaKey create(BigInteger exponent, BigInteger modulus) {
        return new PublicRsaKey(exponent, modulus);
    }

    public BigInteger getExponent() {
        return exponent;
    }

    public BigInteger getModulus() {
        return modulus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PublicRsaKey publicKey = (PublicRsaKey) o;
        return exponent.equals(publicKey.exponent) && modulus.equals(publicKey.modulus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(exponent, modulus);
    }

    @Override
    public String toString() {
        return "PublicRsaKey{" +
                "exponent=" + exponent +
                ", modulus=" + modulus +
                '}';
    }
}
