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
package telegram4j.mtproto.auth;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class DhPrimeCheckerCache implements DhPrimeChecker {

    // https://github.com/tdlib/td/blob/cf1984844be7ec0c06762d8d617cbb20352ec9a2/td/telegram/DhCache.cpp#L25
    private static final byte[] builtInGoodPrime = ByteBufUtil.decodeHexDump(
            "c71caeb9c6b1c9048e6c522f70f13f73980d40238e3e21c14934d037563d930f48198a0aa7c14058229493d22530f4dbfa336f6e0ac9" +
            "25139543aed44cce7c3720fd51f69458705ac68cd4fe6b6b13abdc9746512969328454f18faf8c595f642477fe96bb2a941d5bcd1d4a" +
            "c8cc49880708fa9b378e3c4f3a9060bee67cf9a4a4a695811051907e162753b56b0f6b410dba74d8a84b2a14b3144e0ef1284754fd17" +
            "ed950d5965b4b9dd46582db1178d169c6bc465b0d6ff9ca3928fef5b9ae4e418fc15e83ebea0f87fa9ff5eed70050ded2849f47bf959" +
            "d956850ce929851f0d8115f635b105ee2e4e15d04b2454bf6f4fadf034b10403119cd8e3b92fcc5b"
    );

    private static final DhPrimeCheckerCache instance = new DhPrimeCheckerCache();

    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Map<String, PrimeStatus> knownPrimes = new HashMap<>();

    private DhPrimeCheckerCache() {
        this.knownPrimes.put(ByteBufUtil.hexDump(builtInGoodPrime), PrimeStatus.GOOD);
    }

    /**
     * Gets common instance of DH prime register.
     *
     * @return A common instance of {@code DhPrimeCheckerCache}.
     */
    public static DhPrimeCheckerCache instance() {
        return instance;
    }

    @Override
    public PrimeStatus lookup(ByteBuf prime) {
        rwl.readLock().lock();
        try {
            return knownPrimes.getOrDefault(ByteBufUtil.hexDump(prime), PrimeStatus.UNKNOWN);
        } finally {
            rwl.readLock().unlock();
        }
    }

    @Override
    public void addGoodPrime(ByteBuf prime) {
        rwl.writeLock().lock();
        try {
            knownPrimes.put(ByteBufUtil.hexDump(prime), PrimeStatus.GOOD);
        } finally {
            rwl.writeLock().unlock();
        }
    }

    @Override
    public void addBadPrime(ByteBuf prime) {
        rwl.writeLock().lock();
        try {
            knownPrimes.put(ByteBufUtil.hexDump(prime), PrimeStatus.GOOD);
        } finally {
            rwl.writeLock().unlock();
        }
    }
}
