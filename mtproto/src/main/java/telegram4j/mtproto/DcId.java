package telegram4j.mtproto;

import reactor.util.annotation.Nullable;

/** Value-based Data Center identifier which allows to associate several clients to one DC. */
public final class DcId implements Comparable<DcId> {

    private final int id;
    private final int shift;

    DcId(int id, int shift) {
        this.id = id;
        this.shift = shift;
    }

    /**
     * Creates mew {@code DcId} by specified datacenter info and sequence number.
     *
     * @param id The datacenter info.
     * @param shift The sequence number of client.
     * @return A new {@code DcId}.
     */
    public static DcId of(DataCenter id, int shift) {
        return new DcId(id.getInternalId(), shift);
    }

    /**
     * Creates mew {@code DcId} by specified internal representation of dc id and sequence number.
     *
     * @param id The internal representation of dc id.
     * @param shift The sequence number of client.
     * @return A new {@code DcId}.
     */
    public static DcId of(int id, int shift) {
        return new DcId(id, shift);
    }

    /**
     * Gets internal representation of DC's id.
     *
     * @see DataCenter#getInternalId()
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
     * Compares this id to the specified id.
     * <p>
     * The comparison is based on the {@link #getId() internal} representation of id
     * and after {@link #getShift() sequence number} at natural order.
     *
     * @param o the other instant to compare to, not null.
     * @return the comparator value, negative if less, positive if greater.
     * @throws NullPointerException if otherInstant is null
     */
    @Override
    public int compareTo(DcId o) {
        int d = Integer.compare(id, o.id);
        if (d != 0) return d;
        return Integer.compare(shift, o.shift);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DcId dcId = (DcId) o;
        return id == dcId.id && shift == dcId.shift;
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + id;
        h += (h << 5) + shift;
        return h;
    }

    @Override
    public String toString() {
        return "DcId{" +
                "id=" + id +
                ", shift=" + shift +
                '}';
    }
}
