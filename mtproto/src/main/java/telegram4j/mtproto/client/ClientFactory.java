package telegram4j.mtproto.client;

import telegram4j.mtproto.DataCenter;

/** The factory to instantiate clients for specified DC. */
public interface ClientFactory {

    /**
     * Creates a new main client for specified DC using local options.
     *
     * @param dc The DC for which client will be created.
     * @return A new initialized main client.
     */
    MainMTProtoClient createMain(DataCenter dc);

    /**
     * Creates a new client for specified DC using local options.
     *
     * @param dc The DC for which client will be created.
     * @return A new initialized client.
     */
    MTProtoClient create(DataCenter dc);
}
