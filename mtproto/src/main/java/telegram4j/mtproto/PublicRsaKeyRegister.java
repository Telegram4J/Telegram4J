package telegram4j.mtproto;

import reactor.util.annotation.Nullable;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Map-like container with known public RSA keys, used for creating auth keys. */
public final class PublicRsaKeyRegister {
    private static final BigInteger commonExponent = new BigInteger("10001", 16);

    private final Map<Long, PublicRsaKey> map;

    private PublicRsaKeyRegister(Map<Long, PublicRsaKey> map) {
        this.map = map;
    }

    /**
     * Creates new public RSA key register, which contains predefined keys.
     * Do not rely on this register, because it may have outdated keys.
     *
     * @return A new {@code PublicRsaKeyRegister} instance with predefined keys.
     */
    public static PublicRsaKeyRegister createDefault() {
        return new PublicRsaKeyRegister(Map.of(
                // prod dc 1
                // -----BEGIN RSA PUBLIC KEY-----
                // MIIBCgKCAQEAwVACPi9w23mF3tBkdZz+zwrzKOaaQdr01vAbU4E1pvkfj4sqDsm6
                // lyDONS789sVoD/xCS9Y0hkkC3gtL1tSfTlgCMOOul9lcixlEKzwKENj1Yz/s7daS
                // an9tqw3bfUV/nqgbhGX81v/+7RFAEd+RwFnK7a+XYl9sluzHRyVVaTTveB2GazTw
                // Efzk2DWgkBluml8OREmvfraX3bkHZJTKX4EQSjBbbdJ2ZXIsRrYOXfaA+xayEGB+
                // 8hdlLmAjbCVfaigxX0CDqWeR1yFL9kwd9P0NsZRPsmoqVwMbMu7mStFai6aIhc3n
                // Slv8kg9qv1m6XHVQY3PnEw+QQtqSIXklHwIDAQAB
                // -----END RSA PUBLIC KEY-----

                0xc3b42b026ce86b21L, PublicRsaKey.create(new BigInteger("C150023E2F70DB7985DED064759CFECF0AF328E69A41DAF4D6F01B53" +
                        "8135A6F91F8F8B2A0EC9BA9720CE352EFCF6C5680FFC424BD634864902DE0B4BD6D49F4E580230E3AE97D95" +
                        "C8B19442B3C0A10D8F5633FECEDD6926A7F6DAB0DDB7D457F9EA81B8465FCD6FFFEED114011DF91C059CAED" +
                        "AF97625F6C96ECC74725556934EF781D866B34F011FCE4D835A090196E9A5F0E4449AF7EB697DDB9076494C" +
                        "A5F81104A305B6DD27665722C46B60E5DF680FB16B210607EF217652E60236C255F6A28315F4083A96791D7" +
                        "214BF64C1DF4FD0DB1944FB26A2A57031B32EEE64AD15A8BA68885CDE74A5BFC920F6ABF59BA5C75506373E" +
                        "7130F9042DA922179251F", 16), commonExponent),

                // prod dc 2
                // -----BEGIN PUBLIC KEY-----
                // MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAruw2yP/BCcsJliRoW5eB
                // VBVle9dtjJw+OYED160Wybum9SXtBBLXriwt4rROd9csv0t0OHCaTmRqBcQ0J8fx
                // hN6/cpR1GWgOZRUAiQxoMnlt0R93LCX/j1dnVa/gVbCjdSxpbrfY2g2L4frzjJvd
                // l84Kd9ORYjDEAyFnEA7dD556OptgLQQ2e2iVNq8NZLYTzLp5YpOdO1doK+ttrltg
                // gTCy5SrKeLoCPPbOgGsdxJxyz5KKcZnSLj16yE5HvJQn0CNpRdENvRUXe6tBP78O
                // 39oJ8BTHp9oIjd6XWXAsp2CvK45Ol8wFXGF710w9lwCGNbmNxNYhtIkdqfsEcwR5
                // JwIDAQAB
                // -----END PUBLIC KEY-----

                0xbc35f3509f7b7a5L, PublicRsaKey.create(new BigInteger("AEEC36C8FFC109CB099624685B97815415657BD76D8C9C3E398103D7A" +
                        "D16C9BBA6F525ED0412D7AE2C2DE2B44E77D72CBF4B7438709A4E646A05C43427C7F184DEBF72947519680E" +
                        "651500890C6832796DD11F772C25FF8F576755AFE055B0A3752C696EB7D8DA0D8BE1FAF38C9BDD97CE0A77D" +
                        "3916230C4032167100EDD0F9E7A3A9B602D04367B689536AF0D64B613CCBA7962939D3B57682BEB6DAE5B60" +
                        "8130B2E52ACA78BA023CF6CE806B1DC49C72CF928A7199D22E3D7AC84E47BC9427D0236945D10DBD15177BA" +
                        "B413FBF0EDFDA09F014C7A7DA088DDE9759702CA760AF2B8E4E97CC055C617BD74C3D97008635B98DC4D621" +
                        "B4891DA9FB0473047927", 16), commonExponent),

                // prod dc 3
                // -----BEGIN PUBLIC KEY-----
                // MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvfLHfYH2r9R70w8prHbl
                // Wt/nDkh+XkgpflqQVcnAfSuTtO05lNPspQmL8Y2XjVT4t8cT6xAkdgfmmvnvRPOO
                // KPi0OfJXoRVylFzAQG/j83u5K3kRLbae7fLccVhKZhY46lvsueI1hQdLgNV9n1cQ
                // 3TDS2pQOCtovG4eDl9wacrXOJTG2990VjgnIKNA0UMoP+KF03qzryqIt3oTvZq03
                // DyWdGK+AZjgBLaDKSnC6qD2cFY81UryRWOab8zKkWAnhw2kFpcqhI0jdV5QaSCEx
                // vnsjVaX0Y1N0870931/5Jb9ICe4nweZ9kSDF/gip3kWLG0o8XQpChDfyvsqB9OLV
                // /wIDAQAB
                // -----END PUBLIC KEY-----

                0x15ae5fa8b5529542L, PublicRsaKey.create(new BigInteger("BDF2C77D81F6AFD47BD30F29AC76E55ADFE70E487E5E48297E5A9055" +
                        "C9C07D2B93B4ED3994D3ECA5098BF18D978D54F8B7C713EB10247607E69AF9EF44F38E28F8B439F257A1157" +
                        "2945CC0406FE3F37BB92B79112DB69EEDF2DC71584A661638EA5BECB9E23585074B80D57D9F5710DD30D2DA" +
                        "940E0ADA2F1B878397DC1A72B5CE2531B6F7DD158E09C828D03450CA0FF8A174DEACEBCAA22DDE84EF66AD3" +
                        "70F259D18AF806638012DA0CA4A70BAA83D9C158F3552BC9158E69BF332A45809E1C36905A5CAA12348DD57" +
                        "941A482131BE7B2355A5F4635374F3BD3DDF5FF925BF4809EE27C1E67D9120C5FE08A9DE458B1B4A3C5D0A4" +
                        "28437F2BECA81F4E2D5FF", 16), commonExponent),

                // prod dc 4
                // -----BEGIN PUBLIC KEY-----
                // MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAs/ditzm+mPND6xkhzwFI
                // z6J/968CtkcSE/7Z2qAJiXbmZ3UDJPGrzqTDHkO30R8VeRM/Kz2f4nR05GIFiITl
                // 4bEjvpy7xqRDspJcCFIOcyXm8abVDhF+th6knSU0yLtNKuQVP6voMrnt9MV1X92L
                // GZQLgdHZbPQz0Z5qIpaKhdyA8DEvWWvSUwwc+yi1/gGaybwlzZwqXYoPOhwMebzK
                // Uk0xW14htcJrRrq+PXXQbRzTMynseCoPIoke0dtCodbA3qQxQovE16q9zz4Otv2k
                // 4j63cz53J+mhkVWAeWxVGI0lltJmWtEYK6er8VqqWot3nqmWMXogrgRLggv/Nbbo
                // oQIDAQAB
                // -----END PUBLIC KEY-----

                0xaeae98e13cd7f94fL, PublicRsaKey.create(new BigInteger("B3F762B739BE98F343EB1921CF0148CFA27FF7AF02B6471213FED9DA" +
                        "A0098976E667750324F1ABCEA4C31E43B7D11F1579133F2B3D9FE27474E462058884E5E1B123BE9CBBC6A44" +
                        "3B2925C08520E7325E6F1A6D50E117EB61EA49D2534C8BB4D2AE4153FABE832B9EDF4C5755FDD8B19940B81" +
                        "D1D96CF433D19E6A22968A85DC80F0312F596BD2530C1CFB28B5FE019AC9BC25CD9C2A5D8A0F3A1C0C79BCC" +
                        "A524D315B5E21B5C26B46BABE3D75D06D1CD33329EC782A0F22891ED1DB42A1D6C0DEA431428BC4D7AABDCF" +
                        "3E0EB6FDA4E23EB7733E7727E9A1915580796C55188D2596D2665AD1182BA7ABF15AAA5A8B779EA996317A2" +
                        "0AE044B820BFF35B6E8A1", 16), commonExponent),

                // prod dc 5
                // -----BEGIN PUBLIC KEY-----
                // MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvmpxVY7ld/8DAjz6F6q0
                // 5shjg8/4p6047bn6/m8yPy1RBsvIyvuDuGnP/RzPEhzXQ9UJ5Ynmh2XJZgHoE9xb
                // nfxL5BXHplJhMtADXKM9bWB11PU1Eioc3+AXBB8QiNFBn2XI5UkO5hPhbb9mJpjA
                // 9Uhw8EdfqJP8QetVsI/xrCEbwEXe0xvifRLJbY08/Gp66KpQvy7g8w7VB8wlgePe
                // xW3pT13Ap6vuC+mQuJPyiHvSxjEKHgqePji9NP3tJUFQjcECqcm0yV7/2d0t/pbC
                // m+ZH1sadZspQCEPPrtbkQBlvHb4OLiIWPGHKSMeRFvp3IWcmdJqXahxLCUS1Eh6M
                // AQIDAQAB
                // -----END PUBLIC KEY-----

                0x5a181b2235057d98L, PublicRsaKey.create(new BigInteger("BE6A71558EE577FF03023CFA17AAB4E6C86383CFF8A7AD38EDB9FAFE" +
                        "6F323F2D5106CBC8CAFB83B869CFFD1CCF121CD743D509E589E68765C96601E813DC5B9DFC4BE415C7A6526" +
                        "132D0035CA33D6D6075D4F535122A1CDFE017041F1088D1419F65C8E5490EE613E16DBF662698C0F54870F0" +
                        "475FA893FC41EB55B08FF1AC211BC045DED31BE27D12C96D8D3CFC6A7AE8AA50BF2EE0F30ED507CC2581E3D" +
                        "EC56DE94F5DC0A7ABEE0BE990B893F2887BD2C6310A1E0A9E3E38BD34FDED2541508DC102A9C9B4C95EFFD9" +
                        "DD2DFE96C29BE647D6C69D66CA500843CFAED6E440196F1DBE0E2E22163C61CA48C79116FA77216726749A9" +
                        "76A1C4B0944B5121E8C01", 16), commonExponent),

                // cdn dc 121
                // -----BEGIN RSA PUBLIC KEY-----
                // MIIBCgKCAQEA4tWHcGJlElkxuxKQJwFjJaulmVHgdxNA3wgI2E8XbNnA88y51Xog
                // V5m8BEYuTSP4llXZY4ZSJW5VlFXnmsJT/hmjyeFqqTajyAW6nb9vwZX291QvqD/1
                // ZCFBy7TLvCM0lbNIEhcLMf33ZV8AetLAd+uRLF6QHosys5w0iJ7x+UbGwDxyfeic
                // 8EJJnsKaXrUOwRycMRN+V/zDySa0EYl1u1EB1MDX1/jIV1IQEbLvdBH4vsVTVEdW
                // KHlzOcFzT9qX/g8XibCPiHLJvqQb8hVibvs9NaANyClcBEt3mOucG1/46Lilkc/K
                // d4nlCcohk0jIHNp8symUzNWRPUGmTs3SPwIDAQAB
                // -----END RSA PUBLIC KEY-----

                0x995effd323b5db80L, PublicRsaKey.create(new BigInteger("E2D587706265125931BB129027016325ABA59951E0771340DF0808D8" +
                        "4F176CD9C0F3CCB9D57A205799BC04462E4D23F89655D9638652256E559455E79AC253FE19A3C9E16AA936A" +
                        "3C805BA9DBF6FC195F6F7542FA83FF5642141CBB4CBBC233495B34812170B31FDF7655F007AD2C077EB912C" +
                        "5E901E8B32B39C34889EF1F946C6C03C727DE89CF042499EC29A5EB50EC11C9C31137E57FCC3C926B411897" +
                        "5BB5101D4C0D7D7F8C857521011B2EF7411F8BEC55354475628797339C1734FDA97FE0F1789B08F8872C9BE" +
                        "A41BF215626EFB3D35A00DC8295C044B7798EB9C1B5FF8E8B8A591CFCA7789E509CA219348C81CDA7CB3299" +
                        "4CCD5913D41A64ECDD23F", 16), commonExponent),

                // cdn dc 201
                // -----BEGIN RSA PUBLIC KEY-----
                // MIIBCgKCAQEAug6fETVb7NkXYYu5ueZuM0pqw1heuqUrZNYomQN0lS0o7i6mAWwb
                // 1/FiscFK+y4LQSSEx+oUzXAhjmll9fmb4e7PbUiXo8MuXO0Rj3e5416DXfTiOYGW
                // XlFRV0aQzu8agy1epKwkFDidnmy7g5rJJV0q1+3eR+Jk2OEc/B6lMAOv3fBU6xhE
                // ZByN9gqc6fvkNo13PQ8JYZUSGttzLlYy76uFmvFBhRsJU+LNQ2+bsTHwafSffVYl
                // Z2boJOblvqbRWe453CzssaSWywGXOQmWvVbEe7F8q1ki/s7S8BxYWrhSLJ6bsu9V
                // ZWnIHD9vB34QF8IABPRE93mhCOHBqJxSBQIDAQAB
                // -----END RSA PUBLIC KEY-----

                0xc884b3e62d09e5c5L, PublicRsaKey.create(new BigInteger("BA0E9F11355BECD917618BB9B9E66E334A6AC3585EBAA52B64D62899" +
                        "0374952D28EE2EA6016C1BD7F162B1C14AFB2E0B412484C7EA14CD70218E6965F5F99BE1EECF6D4897A3C32" +
                        "E5CED118F77B9E35E835DF4E23981965E5151574690CEEF1A832D5EA4AC2414389D9E6CBB839AC9255D2AD7" +
                        "EDDE47E264D8E11CFC1EA53003AFDDF054EB1844641C8DF60A9CE9FBE4368D773D0F096195121ADB732E563" +
                        "2EFAB859AF141851B0953E2CD436F9BB131F069F49F7D56256766E824E6E5BEA6D159EE39DC2CECB1A496CB" +
                        "0197390996BD56C47BB17CAB5922FECED2F01C585AB8522C9E9BB2EF556569C81C3F6F077E1017C20004F44" +
                        "4F779A108E1C1A89C5205", 16), commonExponent),

                // cdn dc 203
                // -----BEGIN RSA PUBLIC KEY-----
                // MIIBCgKCAQEAv/L6td+mj7Dl81NHfu+Xf1KNtvZPR1tS5xFqkiUson1u7D2ulK05
                // jM8HKvpV1o+1HPPqhaXhasvsX90u3TIHRQ0zuJKJxKAiZo3GK7phHozjAJ9VUFbO
                // 7jKAa5BTE9tXgA5ZwJAiQWb3U6ykwRzk3fFRe5WaW7xfVUiepxyWGdr1eecoWCfB
                // af1TCXfcS7vcyljNT03pwt2YyS5iXE5IB5wBB5yqSSm4GYtWWR67UjIsXBd77TRp
                // foLGpfOdUHxBz4ZSj8D76m1zlpID5J2pF6bH4+ZCz0SUpv3j7bE8WFlvgMfwEPhw
                // xMYidRGayq9YlLlYd4D+Yoq0U6jS3MWTRQIDAQAB
                // -----END RSA PUBLIC KEY-----

                0xbb27580fd5b01626L, PublicRsaKey.create(new BigInteger("BFF2FAB5DFA68FB0E5F353477EEF977F528DB6F64F475B52E7116A92" +
                        "252CA27D6EEC3DAE94AD398CCF072AFA55D68FB51CF3EA85A5E16ACBEC5FDD2EDD3207450D33B89289C4A02" +
                        "2668DC62BBA611E8CE3009F555056CEEE32806B905313DB57800E59C090224166F753ACA4C11CE4DDF1517B" +
                        "959A5BBC5F55489EA71C9619DAF579E7285827C169FD530977DC4BBBDCCA58CD4F4DE9C2DD98C92E625C4E4" +
                        "8079C01079CAA4929B8198B56591EBB52322C5C177BED34697E82C6A5F39D507C41CF86528FC0FBEA6D7396" +
                        "9203E49DA917A6C7E3E642CF4494A6FDE3EDB13C58596F80C7F010F870C4C62275119ACAAF5894B9587780F" +
                        "E628AB453A8D2DCC59345", 16), commonExponent)
        ));
    }

    /**
     * Creates new public RSA keys register with specified keys.
     * This method will compute map containing fingerprints of keys for indexing them.
     *
     * @see PublicRsaKey#computeTail(PublicRsaKey)
     * @param keys An iterable with keys.
     * @return A new {@code PublicRsaKeyRegister} containing all specified public RSA keys associated with their fingerprints.
     */
    public static PublicRsaKeyRegister create(Iterable<PublicRsaKey> keys) {
        Map<Long, PublicRsaKey> map = new HashMap<>();
        for (PublicRsaKey key : keys) {
            long fingerprint = PublicRsaKey.computeTail(key);
            if (map.put(fingerprint, key) != null) {
                throw new IllegalArgumentException("Detected public RSA key duplicate, fingerprint: "
                        + fingerprint + ", key: " + key);
            }
        }
        return new PublicRsaKeyRegister(Collections.unmodifiableMap(map));
    }

    /**
     * Returns the key to which associated this fingerprint, if present.
     *
     * @param fingerprint The fingerprint of key.
     * @return The key with specified fingerprint, if present.
     */
    public Optional<PublicRsaKey> find(long fingerprint) {
        return Optional.ofNullable(map.get(fingerprint));
    }

    /**
     * Returns the first found key and his fingerprint, if present.
     *
     * @param fingerprints An iterable with key's fingerprints.
     * @return The first key and his fingerprint, if present.
     */
    public Optional<Tuple2<Long, PublicRsaKey>> findAny(Iterable<Long> fingerprints) {
        for (long fingerprint : fingerprints) {
            var key = map.get(fingerprint);
            if (key != null) {
                return Optional.of(Tuples.of(fingerprint, key));
            }
        }
        return Optional.empty();
    }

    /**
     * Gets backing immutable map of keys and their fingerprints.
     *
     * @return The backing immutable map of keys and their fingerprints.
     */
    public Map<Long, PublicRsaKey> getBackingMap() {
        return map;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PublicRsaKeyRegister that = (PublicRsaKeyRegister) o;
        return map.equals(that.map);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + map.hashCode();
        return h;
    }

    @Override
    public String toString() {
        return "PublicRsaKeyRegister{" +
                "map=" + map +
                '}';
    }
}
