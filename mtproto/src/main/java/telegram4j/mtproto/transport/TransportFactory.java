package telegram4j.mtproto.transport;

import telegram4j.mtproto.DataCenter;

/** Interface for creating {@code Transport} instances for specified DC. */
public interface TransportFactory {

    /**
     * Creates new {@code Transport} for specified DC.
     *
     * @param dc The DC for which need to create transport.
     * @return A new {@code Transport} instance.
     */
    Transport create(DataCenter dc);
}
