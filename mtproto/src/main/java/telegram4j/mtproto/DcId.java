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

import reactor.util.annotation.Nullable;
import telegram4j.mtproto.client.MTProtoClient;

import java.util.Locale;
import java.util.Optional;

/** Value-based Data Center identifier which allows to associate several clients to one DC. */
public final class DcId implements Comparable<DcId> {

    private static final int maxDcId = 1000;
    private static final DcId mainDcId = of(Type.MAIN, 0, 0, false);

    private static final int AUTO_SHIFT = 1 << 7;

    // [ 16 bits of DC id | 8 bits of shift value | `is auto shift` flag | 7-0 bits of type ]
    private final int value;

    DcId(int value) {
        this.value = value;
    }

    private static DcId shifted(Type type, int id, int shift) {
        return of(type, id, shift, false);
    }

    private static DcId autoShift(Type type, int id) {
        return of(type, id, 0, true);
    }

    private static DcId of(Type type, int id, int shift, boolean autoShift) {
        if (id > maxDcId || id < 0)
            throw new IllegalArgumentException("Dc id out of range: " + id);
        if (shift > 0xff || shift < 0)
            throw new IllegalArgumentException("Dc shift out of range: " + shift);
        int value = id << 16 | shift << 8 | (autoShift ? AUTO_SHIFT : 0) | type.ordinal();
        return new DcId(value);
    }

    /**
     * Gets common instance for {@code DcId} which indicates a main client.
     *
     * <p> This id doesn't have a {@link #getId()} and {@link #getShift()}
     * and should be used as marker id.
     *
     * @return A common instance for {@code DcId} which indicates a main client.
     */
    public static DcId main() {
        return mainDcId;
    }

    /**
     * Creates new {@code DcId} which indicates an upload client with automatic sequence number.
     *
     * @param id The DC identifier.
     * @return A new {@code DcId} with specified dc id.
     */
    public static DcId upload(int id) {
        return autoShift(Type.MAIN, id);
    }

    /**
     * Creates new {@code DcId} which indicates an upload client with specified sequence number.
     *
     * @param id The DC identifier.
     * @param shift The sequence number of client.
     * @return A new {@code DcId} with specified parameters.
     */
    public static DcId upload(int id, int shift) {
        return shifted(Type.UPLOAD, id, shift);
    }

    /**
     * Creates new {@code DcId} which indicates a download client with automatic sequence number.
     *
     * @param id The DC identifier.
     * @return A new {@code DcId} with specified dc id.
     */
    public static DcId download(int id) {
        return autoShift(Type.DOWNLOAD, id);
    }

    /**
     * Creates new {@code DcId} which indicates a download client with specified sequence number.
     *
     * @param id The DC identifier.
     * @param shift The sequence number of client.
     * @return A new {@code DcId} with specified parameters.
     */
    public static DcId download(int id, int shift) {
        return shifted(Type.DOWNLOAD, id, shift);
    }

    /**
     * Gets id of DC, if it is not with type {@link Type#MAIN}.
     *
     * @return The id of DC, if present.
     */
    public Optional<Integer> getId() {
        return value == 0 ? Optional.empty() : Optional.of(value >> 16);
    }

    /**
     * Gets whether this id doesn't have an exact shift value for client,
     * and if it is, client should be selected by client group
     * with counting load and other parameters.
     *
     * @return {@code true} if this id is pointing to the auto-selected client.
     */
    public boolean isAutoShift() {
        return (value & AUTO_SHIFT) != 0;
    }

    /**
     * Gets sequence number of connected client, zero-based, if {@link #isAutoShift()} is {@code false}.
     * For id with type {@link Type#MAIN} will return empty object.
     *
     * @return The sequence number of connected client, if present.
     */
    public Optional<Integer> getShift() {
        return isAutoShift() ? Optional.empty() : Optional.of((value & 0xffff) >> 8);
    }

    /**
     * Gets type of connection for specified dc.
     *
     * @return The type of connection for specified dc.
     */
    public Type getType() {
        return Type.of(value & 0x7f);
    }

    /**
     * Compares this id to the specified id.
     * <p>
     * The comparison is based on the {@link #getType() type}
     * and after {@link #getId() id} and {@link #getShift() shift} (if present) at natural order.
     *
     * @param o the other instant to compare to, not null.
     * @return the comparator value, negative if less, positive if greater.
     */
    @Override
    public int compareTo(DcId o) {
        int d = Integer.compare(value & 0x7f, o.value & 0x7f); // type
        if (d != 0) return d;
        d = Integer.compare(value >> 16, o.value >> 16); // id
        if (d != 0) return d;
        d = Boolean.compare(isAutoShift(), o.isAutoShift());
        if (d != 0) return d;
        return Integer.compare((value & 0xffff) >> 8, (o.value & 0xffff) >> 8); // shift
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof DcId that)) return false;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return value;
    }

    /**
     * Returns a string representation of the id in format: {@link #getId() id}+{@link #getShift() shift}.
     *
     * @return A string representation of the object.
     */
    @Override
    public String toString() {
        Type type = getType();
        return switch (type) {
            case MAIN -> "main";
            case UPLOAD, DOWNLOAD -> type.name().toLowerCase(Locale.US) +
                    ":" + getId().orElseThrow() + "+" +
                    getShift().map(Object::toString).orElse("auto");
        };
    }

    /** Types of purpose of the mtproto client. */
    public enum Type {
        /** Represents a {@link MTProtoClient}. */
        MAIN,
        /** Represents an upload client. */
        UPLOAD,
        /** Represents a download client. */
        DOWNLOAD;

        private static Type of(int type) {
            return switch (type) {
                case 0 -> MAIN;
                case 1 -> UPLOAD;
                case 2 -> DOWNLOAD;
                default -> throw new IllegalArgumentException("Unknown type: " + type);
            };
        }
    }
}
