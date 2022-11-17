package telegram4j.mtproto;

import reactor.util.annotation.Nullable;

/** Value-based Data Center identifier which allows to associate several clients to one DC. */
public final class DcId implements Comparable<DcId> {

    /**
     * Special value of client {@link #getShift()} to get
     * the most unloaded client or create new using auto-incremented
     * shift value.
     */
    public static final int AUTO_SHIFT = -1;

    private final byte type;
    private final int id;
    private final int shift;

    DcId(byte type, int id, int shift) {
        this.type = type;
        this.id = id;
        this.shift = shift;
    }

    private static DcId of(Type type, int id, int shift) {
        return new DcId((byte) type.ordinal(), id, shift);
    }

    /**
     * Creates new {@code DcId} which indicates a main client.
     *
     * @param id The DC identifier.
     * @return A new {@code DcId} with specified parameters.
     */
    public static DcId main(int id) {
        return of(Type.MAIN, id, 0);
    }

    /**
     * Creates new {@code DcId} which indicates an upload client with specified sequence number.
     *
     * @param id The DC identifier.
     * @param shift The sequence number of client.
     * @return A new {@code DcId} with specified parameters.
     */
    public static DcId upload(int id, int shift) {
        return of(Type.UPLOAD, id, shift);
    }

    /**
     * Creates new {@code DcId} which indicates a non-media client with specified sequence number.
     *
     * @param id The DC identifier.
     * @param shift The sequence number of client.
     * @return A new {@code DcId} with specified parameters.
     */
    public static DcId regular(int id, int shift) {
        return of(Type.REGULAR, id, shift);
    }

    /**
     * Creates new {@code DcId} which indicates a download client with specified sequence number.
     *
     * @param id The DC identifier.
     * @param shift The sequence number of client.
     * @return A new {@code DcId} with specified parameters.
     */
    public static DcId download(int id, int shift) {
        return of(Type.DOWNLOAD, id, shift);
    }

    /**
     * Gets id of DC.
     *
     * @return The internal representation of DC's id.
     */
    public int getId() {
        return id;
    }

    /**
     * Gets sequence number of connected client, zero-based.
     *
     * @return The sequence number of connected client.
     */
    public int getShift() {
        return shift;
    }

    /**
     * Gets type of connection for specified dc.
     *
     * @return The type of connection for specified dc.
     */
    public Type getType() {
        return Type.of(type);
    }

    /**
     * Constructs new {@code DcId} with shift which equals to {@code shift + delta}.
     *
     * @param delta The value to be added to current shift.
     * @return A new {@code DcId} with new shift value, or if {@code delta} is 0 returns current id.
     */
    public DcId shift(int delta) {
        if (getType() == Type.MAIN)
            throw new IllegalStateException("Main clients can't have a shift");
        int newShift = Math.addExact(this.shift, delta);
        if (newShift < 0)
            throw new IllegalArgumentException("Invalid shift: " + delta);
        if (newShift == this.shift) return this;
        return new DcId(type, id, newShift);
    }

    /**
     * Compares this id to the specified id.
     * <p>
     * The comparison is based on the {@link #getId() internal} representation of id
     * and after {@link #getShift() sequence number} at natural order.
     *
     * @param o the other instant to compare to, not null.
     * @return the comparator value, negative if less, positive if greater.
     * @throws NullPointerException if {@code o} is null
     */
    @Override
    public int compareTo(DcId o) {
        int d = Byte.compare(type, o.type);
        if (d != 0) return d;
        d = Integer.compare(id, o.id);
        if (d != 0) return d;
        return Integer.compare(shift, o.shift);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DcId dcId = (DcId) o;
        return type == dcId.type && id == dcId.id && shift == dcId.shift;
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + type;
        h += (h << 5) + id;
        h += (h << 5) + shift;
        return h;
    }

    /**
     * Returns a string representation of the id in format: {@link #getId() id}+{@link #getShift() shift}.
     *
     * @return A string representation of the object.
     */
    @Override
    public String toString() {
        return id + "+" + shift;
    }

    public enum Type {
        /** Represents a {@link MainMTProtoClient}. {@link DcId#getShift()} will always is 0. */
        MAIN,
        /** Represents any non-media client exclude main clients. */
        REGULAR,
        /** Represents an upload client. */
        UPLOAD,
        /** Represents a download client. */
        DOWNLOAD;

        private static Type of(byte type) {
            switch (type) {
                case 0: return MAIN;
                case 1: return REGULAR;
                case 2: return UPLOAD;
                case 3: return DOWNLOAD;
                default: throw new IllegalArgumentException("Unknown type: " + type);
            }
        }
    }
}
