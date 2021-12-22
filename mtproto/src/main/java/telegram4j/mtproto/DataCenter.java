package telegram4j.mtproto;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class DataCenter {
    private final int id;
    private final String address;
    private final int port;

    public static final List<DataCenter> productionDataCenters;
    public static final List<DataCenter> testDataCenters;
    public static final List<DataCenter> mediaDataCenters;

    static {
        productionDataCenters = List.of(create(1, "149.154.175.53", 443),
                create(2, "149.154.167.50", 443),
                create(3, "149.154.175.100", 443),
                create(4, "149.154.167.92", 443),
                create(5, "91.108.56.128", 443));

        testDataCenters = List.of(create(1, "149.154.175.10", 443),
                create(2, "149.154.167.40", 443),
                create(3, "149.154.175.117", 443));

        mediaDataCenters = List.of(create(2, "149.154.167.151", 443),
                create(4, "149.154.164.250", 443));
    }

    DataCenter(int id, String address, int port) {
        this.id = id;
        this.address = address;
        this.port = port;
    }

    public static DataCenter create(int id, String address, int port) {
        return new DataCenter(id, address, port);
    }

    public int getId() {
        return id;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
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
