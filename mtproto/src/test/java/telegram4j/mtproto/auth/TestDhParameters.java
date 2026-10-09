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

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

/** Real Diffie-Hellman parameters, for tests that need a group rather than a mock. */
public final class TestDhParameters {

    private TestDhParameters() {
    }

    /**
     * The 2048-bit safe prime Telegram has been returning for years, used with {@code g = 3}.
     *
     * <p> Also the one built into {@link DhPrimeCheckerCache}, so tests do not pay for a
     * primality check.
     */
    public static final String PRIME =
            "c71caeb9c6b1c9048e6c522f70f13f73980d40238e3e21c14934d037563d930f" +
            "48198a0aa7c14058229493d22530f4dbfa336f6e0ac925139543aed44cce7c37" +
            "20fd51f69458705ac68cd4fe6b6b13abdc9746512969328454f18faf8c595f64" +
            "2477fe96bb2a941d5bcd1d4ac8cc49880708fa9b378e3c4f3a9060bee67cf9a4" +
            "a4a695811051907e162753b56b0f6b410dba74d8a84b2a14b3144e0ef1284754" +
            "fd17ed950d5965b4b9dd46582db1178d169c6bc465b0d6ff9ca3928fef5b9ae4" +
            "e418fc15e83ebea0f87fa9ff5eed70050ded2849f47bf959d956850ce929851f" +
            "0d8115f635b105ee2e4e15d04b2454bf6f4fadf034b10403119cd8e3b92fcc5b";

    public static final int G = 3;

    public static DhParameters create() {
        return DhParameters.create(Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump(PRIME)),
                G, DhPrimeCheckerCache.instance());
    }
}
