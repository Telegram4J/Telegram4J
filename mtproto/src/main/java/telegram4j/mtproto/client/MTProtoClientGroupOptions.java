package telegram4j.mtproto.client;

import telegram4j.mtproto.DataCenter;
import telegram4j.mtproto.store.StoreLayout;

import java.util.Objects;

public class MTProtoClientGroupOptions {
    public final DataCenter mainDc;
    public final ClientFactory clientFactory;
    public final StoreLayout storeLayout;
    public final UpdateDispatcher updateDispatcher;

    public MTProtoClientGroupOptions(DataCenter mainDc, ClientFactory clientFactory,
                                     StoreLayout storeLayout, UpdateDispatcher updateDispatcher) {
        this.mainDc = Objects.requireNonNull(mainDc);
        this.clientFactory = Objects.requireNonNull(clientFactory);
        this.storeLayout = Objects.requireNonNull(storeLayout);
        this.updateDispatcher = Objects.requireNonNull(updateDispatcher);
    }
}
