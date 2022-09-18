package telegram4j.mtproto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PublicRsaKeyTest {

    @Test
    void verifyTails() {
        for (var e : PublicRsaKey.publicKeys.entrySet()) {
            long t = PublicRsaKey.computeTail(e.getValue());
            Assertions.assertEquals(t, e.getKey(), () -> "Incorrect tail for " + e.getValue());
        }
    }
}
