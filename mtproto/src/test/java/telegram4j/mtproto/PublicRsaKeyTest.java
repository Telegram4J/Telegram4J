package telegram4j.mtproto;

import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PublicRsaKeyTest {

    @Test
    void verifyTails() {
        for (var e : PublicRsaKey.publicKeys.entrySet()) {
            long t = PublicRsaKey.computeTail(ByteBufAllocator.DEFAULT, e.getValue());
            Assertions.assertEquals(t, e.getKey(), () -> "Incorrect tail for " + e.getValue());
        }
    }
}
