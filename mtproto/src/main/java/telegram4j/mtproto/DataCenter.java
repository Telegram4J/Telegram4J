package telegram4j.mtproto;

import reactor.util.annotation.Nullable;

import java.util.List;
import java.util.Objects;

/** Identifier of the Telegram datacenter with IP address and port. */
public final class DataCenter {
    // https://github.com/tdlib/td/blob/31a46084636486d9a2e1348491cab079c6edf386/td/telegram/net/ConnectionCreator.cpp#L675
    private static final int TEST_DC_SHIFT = 10000;

    private final Type type;
    private final int id;
    private final String address;
    private final int port;

    /** Latest information about all types of datacenters. */
    public static final List<DataCenter> list;

    static {
        list = List.of(
                create(Type.PRODUCTION, 1, "149.154.175.54", 443),
                create(Type.PRODUCTION, 1, "2001:0b28:f23d:f001:0000:0000:0000:000a", 443),
                create(Type.PRODUCTION, 2, "149.154.167.41", 443),
                create(Type.PRODUCTION, 2, "2001:067c:04e8:f002:0000:0000:0000:000a", 443),
                create(Type.PRODUCTION, 3, "149.154.175.100", 443),
                create(Type.PRODUCTION, 3, "2001:0b28:f23d:f003:0000:0000:0000:000a", 443),
                create(Type.PRODUCTION, 4, "149.154.167.92", 443),
                create(Type.PRODUCTION, 4, "2001:067c:04e8:f004:0000:0000:0000:000a", 443),
                create(Type.PRODUCTION, 5, "91.108.56.116", 443),
                create(Type.PRODUCTION, 5, "2001:0b28:f23f:f005:0000:0000:0000:000a", 443),

                create(Type.TEST, 1, "149.154.175.100", 443),
                create(Type.TEST, 1, "2001:0b28:f23d:f001:0000:0000:0000:000e", 443),
                create(Type.TEST, 2, "149.154.167.40", 443),
                create(Type.TEST, 2, "2001:067c:04e8:f002:0000:0000:0000:000e", 443),
                create(Type.TEST, 3, "149.154.175.117", 443),
                create(Type.TEST, 3, "2001:0b28:f23d:f003:0000:0000:0000:000e", 443),

                create(Type.MEDIA, 2, "149.154.167.151", 443),
                create(Type.MEDIA, 2, "2001:067c:04e8:f002:0000:0000:0000:000b", 443),
                create(Type.MEDIA, 4, "149.154.167.43", 443),
                create(Type.MEDIA, 4, "2001:067c:04e8:f004:0000:0000:0000:000b", 443),

                create(Type.CDN, 203, "91.105.192.100", 443),
                create(Type.CDN, 203, "2a0a:f280:0203:000a:5000:0000:0000:0100", 443)
        );
    }

    DataCenter(Type type, int id, String address, int port) {
        this.type = Objects.requireNonNull(type);
        this.id = id;
        this.address = Objects.requireNonNull(address);
        this.port = port;
    }

    /**
     * Create new datacenter identifier from given id, address and port.
     *
     * @param id The identifier of server.
     * @param address The ipv4/ipv6 address of server.
     * @param port The port of server.
     * @return The new datacenter identifier.
     */
    public static DataCenter create(Type type, int id, String address, int port) {
        return new DataCenter(type, id, address, port);
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
     * Gets an identifier of server.
     *
     * @return The identifier of server.
     */
    public int getId() {
        return id;
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
     * @return The internal representation of datacenter id.
     */
    public int getInternalId() {
        int id = getId();
        if (type == Type.TEST) {
            id += TEST_DC_SHIFT;
        }
        // TODO: isMediaOnly ? -id : id
        return id;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataCenter that = (DataCenter) o;
        return id == that.id && port == that.port && type == that.type && address.equals(that.address);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + type.hashCode();
        h += (h << 5) + id;
        h += (h << 5) + address.hashCode();
        h += (h << 5) + port;
        return h;
    }

    @Override
    public String toString() {
        return "DataCenter{" +
                "type=" + type +
                ", id=" + id +
                ", address='" + address + '\'' +
                ", port=" + port +
                '}';
    }

    public enum Type {
        PRODUCTION,
        MEDIA,
        TEST,
        CDN
    }
}
