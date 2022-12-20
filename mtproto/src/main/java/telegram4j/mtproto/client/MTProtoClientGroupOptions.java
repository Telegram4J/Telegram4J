package telegram4j.mtproto.client;

import telegram4j.mtproto.store.StoreLayout;

import java.util.Objects;

public class MTProtoClientGroupOptions {
    public final MainMTProtoClient mainClient;
    public final ClientFactory clientFactory;
    public final StoreLayout storeLayout;

    public MTProtoClientGroupOptions(MainMTProtoClient mainClient, ClientFactory clientFactory, StoreLayout storeLayout) {
        this.mainClient = Objects.requireNonNull(mainClient);
        this.clientFactory = Objects.requireNonNull(clientFactory);
        this.storeLayout = Objects.requireNonNull(storeLayout);
    }
}
