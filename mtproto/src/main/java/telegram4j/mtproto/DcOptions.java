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
package telegram4j.mtproto;

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import reactor.util.annotation.Nullable;
import telegram4j.tl.Config;
import telegram4j.tl.api.TlEncodingUtil;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static telegram4j.mtproto.DataCenter.*;

/** List-like container with known {@link DataCenter} options, which used in connection creating. */
public final class DcOptions {
    private static final byte TEST_MASK        = 1 << 0;
    private static final byte PREFER_IPV6_MASK = 1 << 1;

    private final List<DataCenter> options;
    private final byte flags;

    DcOptions(List<DataCenter> options, byte flags) {
        this.options = options;
        this.flags = flags;
    }

    static Comparator<DataCenter> dcSearch(DataCenter.Type type, int id, boolean preferIpv6) {
        return Comparator.<DataCenter>comparingInt(dc -> {
                    int d = Integer.compare(id, dc.getId());
                    return d == 0 ? -1 : 1;
                })
                .thenComparingInt(dc -> type == dc.getType() ? -1 : 1)
                .thenComparingInt(dc -> preferIpv6 == dc.isIpv6() ? -2 : -1);
    }

    static Comparator<DataCenter> dcFindFirst(DataCenter.Type type, boolean preferIpv6) {
        return Comparator.<DataCenter>comparingInt(dc -> type == dc.getType() ? -1 : 1)
                .thenComparingInt(dc -> preferIpv6 == dc.isIpv6() ? -2 : -1);
    }

    static Comparator<DataCenter> dcIdComparator(DcId.Type type, int dcId, boolean preferIpv6) {
        // filter options with other ids
        return Comparator.<DataCenter>comparingInt(dc -> {
            int d = Integer.compare(dcId, dc.getId());
            return d == 0 ? -1 : 1;
        })
        .thenComparingInt(dc -> switch (type) {
            case MAIN -> dc.getType() == Type.REGULAR ? -1 : 1;
            // prefer MEDIA dcs for downloading/uploading
            case UPLOAD, DOWNLOAD -> switch (dc.getType()) {
                case MEDIA -> -2;
                case REGULAR -> -1;
                case CDN -> 2;
            };
        })
        .thenComparingInt(dc -> preferIpv6 == dc.isIpv6() ? -2 : -1);
    }

    /**
     * Creates a new {@code DcOptions} list by specified api config.
     * <p> The {@link #isPreferIpv6()} and {@link #isTest()} parameters will be extracted from
     * {@link Config#forceTryIpv6()} and {@link Config#testMode()} attributes respectively.
     *
     * @param config The api configuration.
     * @return A new {@code DcOptions} containing all options from specified config.
     */
    public static DcOptions from(Config config) {
        var list = config.dcOptions().stream()
                .map(dc -> DataCenter.from(config, dc))
                .collect(Collectors.toUnmodifiableList());
        byte flags = config.testMode() ? TEST_MASK : 0;
        flags |= config.forceTryIpv6() ? PREFER_IPV6_MASK : 0;
        return new DcOptions(list, flags);
    }

    /**
     * Creates a new {@code DcOptions} list containing predefined options.
     * The IPv6 preference will be decided by {@code java.net.preferIPv6Addresses} property.
     * Do not rely on this list, because it may have outdated options.
     *
     * @param test {@code true} for creating options for test env.
     * @return A new {@code DcOptions} list containing predefined options.
     */
    public static DcOptions createDefault(boolean test) {
        return createDefault(test, Boolean.getBoolean("java.net.preferIPv6Addresses"));
    }

    /**
     * Creates a new {@code DcOptions} list containing predefined options.
     * Do not rely on this list, because it may have outdated options.
     *
     * @param test {@code true} for creating options for test env.
     * @param preferIpv6 The preference of IPv6 addresses.
     * @return A new {@code DcOptions} list containing predefined options.
     */
    public static DcOptions createDefault(boolean test, boolean preferIpv6) {
        // These options retrieved from Config.dcOptions()
        List<DataCenter> opts;
        if (test) {
            var commonSecret = Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump("ddfdda254c78d9fa202ac536079e88b808"));
            opts = List.of(
                    test(Type.REGULAR, 1, "149.154.175.10", 80, false, false, false, null),
                    test(Type.REGULAR, 1, "149.154.175.10", 80, false, true, false, null),
                    test(Type.REGULAR, 1, "2001:0b28:f23d:f001:0000:0000:0000:000e", 443, false, false, false, null),

                    test(Type.REGULAR, 2, "149.154.167.40", 443, false, true, false, null),
                    test(Type.REGULAR, 2, "2001:067c:04e8:f002:0000:0000:0000:000e", 443, false, false, false, null),
                    test(Type.REGULAR, 2, "207.154.241.73", 14543, true, true, false, commonSecret),

                    test(Type.MEDIA, 2, "207.154.241.73", 14543, true, false, false, commonSecret),

                    test(Type.REGULAR, 3, "149.154.175.117", 443, false, true, false, null),
                    test(Type.REGULAR, 3, "207.154.241.73", 14543, true, false, false, commonSecret),
                    test(Type.REGULAR, 3, "2001:0b28:f23d:f003:0000:0000:0000:000e", 443, false, false, false, null)
            );
        } else {
            opts = List.of(
                    production(Type.REGULAR, 1, "149.154.175.54", 443, false, false, false, null),
                    production(Type.REGULAR, 1, "149.154.175.54", 443, false, true, false, null),
                    production(Type.REGULAR, 1, "2001:0b28:f23d:f001:0000:0000:0000:000a", 443, false, false, false, null),

                    production(Type.REGULAR, 2, "149.154.167.41", 443, false, false, false, null),
                    production(Type.REGULAR, 2, "149.154.167.41", 443, false, true, false, null),
                    production(Type.REGULAR, 2, "2001:067c:04e8:f002:0000:0000:0000:000a", 443, false, false, false, null),

                    production(Type.MEDIA, 2, "149.154.167.151", 443, false, false, false, null),
                    production(Type.MEDIA, 2, "2001:067c:04e8:f002:0000:0000:0000:000b", 443, false, false, false, null),

                    production(Type.REGULAR, 3, "149.154.175.100", 443, false, false, false, null),
                    production(Type.REGULAR, 3, "149.154.175.100", 443, false, true, false, null),
                    production(Type.REGULAR, 3, "2001:0b28:f23d:f003:0000:0000:0000:000a", 443, false, false, false, null),

                    production(Type.REGULAR, 4, "149.154.167.92", 443, false, false, false, null),
                    production(Type.REGULAR, 4, "149.154.167.92", 443, false, true, false, null),
                    production(Type.REGULAR, 4, "2001:067c:04e8:f004:0000:0000:0000:000a", 443, false, false, false, null),

                    production(Type.MEDIA, 4, "149.154.167.43", 443, false, false, false, null),
                    production(Type.MEDIA, 4, "2001:067c:04e8:f004:0000:0000:0000:000b", 443, false, false, false, null),

                    production(Type.REGULAR, 5, "91.108.56.116", 443, false, false, false, null),
                    production(Type.REGULAR, 5, "91.108.56.116", 443, false, true, false, null),
                    production(Type.REGULAR, 5, "2001:0b28:f23f:f005:0000:0000:0000:000a", 443, false, false, false, null),

                    production(Type.CDN, 203, "91.105.192.100", 443, false, false, false, null),
                    production(Type.CDN, 203, "2a0a:f280:0203:000a:5000:0000:0000:0100", 443, false, false, false, null)
            );
        }

        byte flags = test ? TEST_MASK : 0;
        flags |= preferIpv6 ? PREFER_IPV6_MASK : 0;
        return new DcOptions(opts, flags);
    }

    /**
     * Creates a new {@code DcOptions} with specified options and settings.
     *
     * @param options An iterable with dc options.
     * @param test {@code true} for creating options for test env.
     * @param preferIpv6 Whether IPv6 addresses are preferred.
     * @return A new {@code DcOptions} containing all options from specified config.
     */
    public static DcOptions create(Iterable<DataCenter> options, boolean test, boolean preferIpv6) {
        byte flags = test ? TEST_MASK : 0;
        flags |= preferIpv6 ? PREFER_IPV6_MASK : 0;
        return new DcOptions(TlEncodingUtil.copyList(options), flags);
    }

    /**
     * Finds a DC option by specified {@code DcId} identifier.
     * Result will prefer with IPv6 version according to {@link #isPreferIpv6()} setting.
     *
     * @param type The type of client.
     * @param dcId The DC identifier.
     * @return A dc option found by specified id, if present.
     */
    public Optional<DataCenter> find(DcId.Type type, int dcId) {
        return find(type, dcId, isPreferIpv6());
    }

    /**
     * Finds a DC option by specified {@code DcId} identifier.
     *
     * @param type The type of client.
     * @param dcId The DC identifier.
     * @param preferIpv6 The preference of DC option, if {@code true} IPv6 variant of DC will be returned if present.
     * @return A dc option found by specified id, if present.
     */
    public Optional<DataCenter> find(DcId.Type type, int dcId, boolean preferIpv6) {
        return find0(dcIdComparator(type, dcId, preferIpv6));
    }

    /**
     * Finds a DC option by specified type and id.
     * Result will prefer with IPv6 version according to {@link #isPreferIpv6()} setting.
     *
     * @param type The type of identifier.
     * @param id The {@link DataCenter#getId() DC id} value.
     * @return A dc option found by specified type and id, if present.
     */
    public Optional<DataCenter> find(DataCenter.Type type, int id) {
        return find(type, id, isPreferIpv6());
    }

    public Optional<DataCenter> findFirst(DataCenter.Type type) {
        return findFirst(type, isPreferIpv6());
    }

    public Optional<DataCenter> findFirst(DataCenter.Type type, boolean preferIpv6) {
        return find0(dcFindFirst(type, preferIpv6));
    }

    /**
     * Finds a DC option by specified type and id.
     *
     * @param type The type of identifier.
     * @param id The {@link DataCenter#getId() DC id} value.
     * @param preferIpv6 The preference of DC option, if {@code true} IPv6 variant of DC will be returned if present.
     * @return A dc option found by specified type and id, if present.
     */
    public Optional<DataCenter> find(DataCenter.Type type, int id, boolean preferIpv6) {
        return find0(dcSearch(type, id, preferIpv6));
    }

    private Optional<DataCenter> find0(Comparator<DataCenter> comp) {
        return options.stream().min(comp);
    }

    /**
     * Gets immutable backing list of DC options.
     *
     * @return The immutable list of DC options.
     */
    public List<DataCenter> getBackingList() {
        return options;
    }

    /**
     * Gets whether container created for DC options relevant for the test env.
     *
     * @return {@code true} if current list contains DC options relevant for the test env.
     */
    public boolean isTest() {
        return (flags & TEST_MASK) != 0;
    }

    /**
     * Gets whether IPv6 versions of DC options are preferred in search methods.
     *
     * @return {@code true} if IPv6 DC options are preferred.
     */
    public boolean isPreferIpv6() {
        return (flags & PREFER_IPV6_MASK) != 0;
    }

    /**
     * Constructs new {@code DcOptions} with specified address version preference.
     *
     * @param value The new state of preference.
     * @return A new {@code DcOptions} if {@code value} is not equals to current state.
     */
    public DcOptions withPreferIpv6(boolean value) {
        int newFlags = TlEncodingUtil.mask(flags, PREFER_IPV6_MASK, value);
        if (newFlags == flags) return this;
        return new DcOptions(options, flags);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof DcOptions dcOptions)) return false;
        return flags == dcOptions.flags && options.equals(dcOptions.options);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + options.hashCode();
        h += (h << 5) + flags;
        return h;
    }

    @Override
    public String toString() {
        return "DcOptions{" +
                "options=" + options +
                ", test=" + isTest() +
                ", preferIpv6=" + isPreferIpv6() +
                '}';
    }
}
