package telegram4j.mtproto;

import telegram4j.mtproto.store.StoreLayout;
import telegram4j.mtproto.transport.Transport;

public class SessionResources {
    private final Transport transport;
    private final StoreLayout storeLayout;
    private final int acksSendThreshold;

    public SessionResources(Transport transport, StoreLayout storeLayout, int acksSendThreshold) {
        this.transport = transport;
        this.storeLayout = storeLayout;
        this.acksSendThreshold = acksSendThreshold;
    }

    public Transport getTransport() {
        return transport;
    }

    public StoreLayout getStoreLayout() {
        return storeLayout;
    }

    public int getAcksSendThreshold() {
        return acksSendThreshold;
    }
}
