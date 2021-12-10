package telegram4j.core;

import telegram4j.core.event.EventDispatcher;
import telegram4j.mtproto.MTProtoClientManager;
import telegram4j.mtproto.store.StoreLayout;

public class MTProtoResources {
    private final MTProtoClientManager clientManager;
    private final StoreLayout storeLayout;
    private final EventDispatcher eventDispatcher;

    public MTProtoResources(MTProtoClientManager clientManager, StoreLayout storeLayout,
                            EventDispatcher eventDispatcher) {
        this.clientManager = clientManager;
        this.storeLayout = storeLayout;
        this.eventDispatcher = eventDispatcher;
    }

    public MTProtoClientManager getClientManager() {
        return clientManager;
    }

    public StoreLayout getStoreLayout() {
        return storeLayout;
    }

    public EventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }
}
