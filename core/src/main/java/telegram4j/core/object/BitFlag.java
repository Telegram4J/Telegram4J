package telegram4j.core.object;

/** Interface that flags enumerations must implement. */
public interface BitFlag {

    /**
     * Gets flag position, used in the {@link #mask()} as {@code 1 << position}.
     *
     * @return The flag shift position.
     */
    byte position();

    /**
     * Gets bit-mask for flag.
     *
     * @return The bit-mask for flag.
     */
    default int mask() {
        return 1 << position();
    }
}
