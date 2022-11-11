package telegram4j.mtproto;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import reactor.util.annotation.Nullable;
import telegram4j.tl.Config;
import telegram4j.tl.DcOption;
import telegram4j.tl.api.TlEncodingUtil;
import telegram4j.tl.mtproto.PQInnerDataDc;

import java.util.Objects;

/** Identifier of the Telegram datacenter with IP address and port. */
public final class DataCenter {
    // https://github.com/tdlib/td/blob/31a46084636486d9a2e1348491cab079c6edf386/td/telegram/net/ConnectionCreator.cpp#L675
    private static final int TEST_DC_SHIFT = 10000;

    private static final byte TEST_MASK = 1 << 1;
    private static final byte STATIC_MASK = 1 << 1;
    private static final byte TCPO_ONLY_MASK = 1 << 1;

    private final Type type;
    private final int id;
    private final String address;
    private final int port;
    private final byte flags;
    @Nullable
    private final ByteBuf secret;

    DataCenter(Type type, int id, String address, int port, byte flags, @Nullable ByteBuf secret) {
        this.type = Objects.requireNonNull(type);
        this.id = id;
        this.address = Objects.requireNonNull(address);
        this.port = port;
        this.flags = flags;
        this.secret = secret;
    }

    /**
     * Create new datacenter identifier from given id, address and port.
     *
     * @param type The type of dc.
     * @param test The type of environment in dc, {@code true} for test and {@code false} for production
     * @param id The identifier of server.
     * @param address The ipv4/ipv6 address of server.
     * @param port The port of server.
     * @return The new datacenter identifier.
     */
    public static DataCenter create(Type type, boolean test, int id, String address, int port,
                                    boolean tcpoOnly, boolean isStatic, @Nullable ByteBuf secret) {
        byte flags = test ? TEST_MASK : 0;
        flags |= tcpoOnly ? TCPO_ONLY_MASK : 0;
        flags |= isStatic ? STATIC_MASK : 0;

        var secretCopy = secret != null ? TlEncodingUtil.copyAsUnpooled(secret) : null;
        return new DataCenter(type, id, address, port, flags, secretCopy);
    }

    public static DataCenter production(Type type, int id, String address, int port,
                                        boolean tcpoOnly, boolean isStatic, @Nullable ByteBuf secret) {
        return create(type, false, id, address, port, tcpoOnly, isStatic, secret);
    }

    public static DataCenter test(Type type, int id, String address, int port,
                                  boolean tcpoOnly, boolean isStatic, @Nullable ByteBuf secret) {
        return create(type, true, id, address, port, tcpoOnly, isStatic, secret);
    }

    /**
     * Constructs new datacenter identifier from specified option and api config.
     *
     * @param config The api config to determine env type, test or production.
     * @param dc The option to convert.
     * @return A new datacenter identifier from specified raw data.
     */
    public static DataCenter from(Config config, DcOption dc) {
        Type type = dc.cdn() ? Type.CDN :
                dc.mediaOnly() ? Type.MEDIA : Type.REGULAR;
        byte flags = config.testMode() ? TEST_MASK : 0;
        flags |= dc.tcpoOnly() ? TCPO_ONLY_MASK : 0;
        flags |= dc.isStatic() ? STATIC_MASK : 0;
        return new DataCenter(type, dc.id(), dc.ipAddress(), dc.port(), flags, dc.secret());
    }

    /**
     * Gets a type of server.
     *
     * @return The type of server.
     */
    public Type getType() {
        return type;
    }

    /**
     * Gets whether datacenter supports only connections with
     * obfuscated transport.
     *
     * @see <a href="https://core.telegram.org/mtproto/mtproto-transports#transport-obfuscation">Transport obfuscation</a>
     * @return {@code true} if supports only connections with
     * obfuscated transport.
     */
    public boolean isTcpObfuscatedOnly() {
        return (flags & TCPO_ONLY_MASK) != 0;
    }

    /**
     * Gets whether {@link #getAddress()} should be used when connecting through a proxy.
     *
     * @return {@code true} if this datacenter requires proxy connection.
     */
    public boolean isStatic() {
        return (flags & STATIC_MASK) != 0;
    }

    /**
     * Gets an identifier of server.
     *
     * @return The identifier of server.
     */
    public int getId() {
        return id;
    }

    /**
     * Gets whether this dc in test environment.
     *
     * @return {@code true} if dc in test enrichment.
     */
    public boolean isTest() {
        return (flags & TEST_MASK) != 0;
    }

    /**
     * Gets an ipv4/ipv6 address of server.
     *
     * @return The ipv4/ipv6 address of server.
     */
    public String getAddress() {
        return address;
    }

    /**
     * Gets a port of server.
     *
     * @return The port of server.
     */
    public int getPort() {
        return port;
    }

    /**
     * Gets whether address of server is IPv6.
     *
     * @return {@code true} if address is IPv6.
     */
    public boolean isIpv6() {
        return address.indexOf(':') != -1;
    }

    /**
     * Gets internal representation of datacenter id.
     *
     * <p> This id can be used for {@link PQInnerDataDc#dc()}.
     *
     * @return The internal representation of datacenter id.
     */
    public int getInternalId() {
        int id = getId();
        if (isTest()) {
            id += TEST_DC_SHIFT;
        }
        return type == Type.MEDIA ? -id : id;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataCenter that = (DataCenter) o;
        return id == that.id && port == that.port && flags == that.flags &&
                type == that.type && address.equals(that.address) &&
                Objects.equals(secret, that.secret);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + type.hashCode();
        h += (h << 5) + id;
        h += (h << 5) + address.hashCode();
        h += (h << 5) + port;
        h += (h << 5) + flags;
        h += (h << 5) + Objects.hashCode(secret);
        return h;
    }

    @Override
    public String toString() {
        return "DataCenter{" +
                "type=" + type +
                ", id=" + id +
                ", address='" + address + '\'' +
                ", port=" + port +
                ", flags=" + flags +
                ", secret=" + (secret != null ? ByteBufUtil.hexDump(secret) : null) +
                '}';
    }

    public enum Type {
        /** Represents DC which should be used to interact with Telegram API. */
        REGULAR,

        /** Represents DC which should be used to upload and download files. */
        MEDIA,

        /**
         * Represents DC which should be used to download files from big channels.
         * @see <a href="https://core.telegram.org/cdn">Encrypted CDNs</a>
         */
        CDN
    }
}
