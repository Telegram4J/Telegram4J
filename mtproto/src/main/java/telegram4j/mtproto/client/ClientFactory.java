package telegram4j.mtproto.client;

import telegram4j.mtproto.DataCenter;
import telegram4j.mtproto.DcId;

/** The factory to instantiate clients for specified DC. */
@FunctionalInterface
public interface ClientFactory {

    /**
     * Creates a new client for specified DC using local options.
     *
     * @param dc The DC for which client will be created.
     * @return A new initialized client.
     */
    MTProtoClient create(MTProtoClientGroup group, DcId.Type type, DataCenter dc);
}
