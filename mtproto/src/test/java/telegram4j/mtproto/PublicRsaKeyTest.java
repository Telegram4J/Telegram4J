package telegram4j.mtproto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import telegram4j.mtproto.util.CryptoUtil;

class PublicRsaKeyTest {

    @Test
    void verifyTails() {
        var sha1 = CryptoUtil.createDigest("sha-1");
        for (var e : PublicRsaKeyRegister.createDefault().getBackingMap().entrySet()) {
            long t = PublicRsaKey.computeTail(sha1, e.getValue());
            Assertions.assertEquals(t, e.getKey(), () -> "Incorrect tail for " + e.getValue());
        }
    }
}
