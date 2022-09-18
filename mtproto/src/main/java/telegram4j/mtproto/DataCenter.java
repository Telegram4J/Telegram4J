package telegram4j.mtproto;

import reactor.util.annotation.Nullable;

import java.util.List;
import java.util.Objects;

/** Identifier of the Telegram datacenter with IP address and port. */
public final class DataCenter {
    private final int id;
    private final String address;
    private final int port;

    /** Latest information about <b>production</b> datacenters with IPv4 addresses. */
    public static final List<DataCenter> productionDataCentersIpv4;

    /** Latest information about <b>production</b> datacenters with IPv6 addresses. */
    public static final List<DataCenter> productionDataCentersIpv6;

    /** Latest information about <b>test</b> datacenters with IPv4 addresses. */
    public static final List<DataCenter> testDataCentersIpv4;

    /** Latest information about <b>test</b> datacenters with IPv6 addresses. */
    public static final List<DataCenter> testDataCentersIpv6;

    /** Latest information about <b>media</b> datacenters with IPv4 addresses. */
    public static final List<DataCenter> mediaDataCentersIpv4;

    /** Latest information about <b>media</b> datacenters with IPv6 addresses. */
    public static final List<DataCenter> mediaDataCentersIpv6;

    /** Latest information about <b>cdn</b> datacenters with IPv4 addresses. */
    public static final List<DataCenter> cdnDataCentersIpv4;

    /** Latest information about <b>media</b> datacenters with IPv6 addresses. */
    public static final List<DataCenter> cdnDataCentersIpv6;

    static {
        productionDataCentersIpv4 = List.of(
                create(1, "149.154.175.54", 443),
                create(2, "149.154.167.50", 443),
                create(2, "149.154.167.51", 443),
                create(3, "149.154.175.100", 443),
                create(4, "149.154.167.92", 443),
                create(5, "91.108.56.116", 443)
        );

        productionDataCentersIpv6 = List.of(
                create(1, "2001:0b28:f23d:f001:0000:0000:0000:000a", 443),
                create(2, "2001:067c:04e8:f002:0000:0000:0000:000a", 443),
                create(3, "2001:0b28:f23d:f003:0000:0000:0000:000a", 443),
                create(4, "2001:067c:04e8:f004:0000:0000:0000:000a", 443),
                create(5, "2001:0b28:f23f:f005:0000:0000:0000:000a", 443)
        );

        testDataCentersIpv4 = List.of(
                create(1, "149.154.175.10", 443),
                create(2, "149.154.167.40", 443),
                create(3, "149.154.175.117", 443)
        );

        testDataCentersIpv6 = List.of(
                create(1, "2001:0b28:f23d:f001:0000:0000:0000:000e", 443),
                create(2, "2001:067c:04e8:f002:0000:0000:0000:000e", 443),
                create(3, "2001:0b28:f23d:f003:0000:0000:0000:000e", 443)
        );

        mediaDataCentersIpv4 = List.of(
                create(2, "149.154.167.151", 443),
                create(4, "149.154.165.136", 443)
        );

        mediaDataCentersIpv6 = List.of(
                create(2, "2001:067c:04e8:f002:0000:0000:0000:000b", 443),
                create(4, "2001:067c:04e8:f004:0000:0000:0000:000b", 443)
        );

        cdnDataCentersIpv4 = List.of(
                create(203, "91.105.192.100", 443)
        );

        cdnDataCentersIpv6 = List.of(
                create(203, "2a0a:f280:0203:000a:5000:0000:0000:0100", 443)
        );
    }

    DataCenter(int id, String address, int port) {
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
    public static DataCenter create(int id, String address, int port) {
        return new DataCenter(id, address, port);
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

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataCenter that = (DataCenter) o;
        return id == that.id && port == that.port && address.equals(that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, address, port);
    }

    @Override
    public String toString() {
        return "DataCenter{" +
                "id=" + id +
                ", address='" + address + '\'' +
                ", port=" + port +
                '}';
    }
}
