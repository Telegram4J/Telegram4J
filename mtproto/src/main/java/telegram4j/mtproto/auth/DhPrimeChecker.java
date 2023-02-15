package telegram4j.mtproto.auth;

import io.netty.buffer.ByteBuf;

public interface DhPrimeChecker {
    // TODO: consider impls where prime caching will be backed with DB

    PrimeStatus lookup(ByteBuf prime);

    void addGoodPrime(ByteBuf prime);

    void addBadPrime(ByteBuf prime);

    enum PrimeStatus {
        GOOD,
        BAD,
        UNKNOWN
    }
}
