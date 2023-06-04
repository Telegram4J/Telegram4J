package telegram4j.mtproto.client;

import telegram4j.mtproto.DataCenter;
import telegram4j.mtproto.store.StoreLayout;

import java.util.Objects;

public class MTProtoClientGroupOptions {
    protected final DataCenter mainDc;
    protected final ClientFactory clientFactory;
    protected final StoreLayout storeLayout;
    protected final UpdateDispatcher updateDispatcher;
    protected final MTProtoOptions mtProtoOptions;

    public MTProtoClientGroupOptions(DataCenter mainDc, ClientFactory clientFactory,
                                     StoreLayout storeLayout, UpdateDispatcher updateDispatcher,
                                     MTProtoOptions mtProtoOptions) {
        this.mainDc = Objects.requireNonNull(mainDc);
        this.clientFactory = Objects.requireNonNull(clientFactory);
        this.storeLayout = Objects.requireNonNull(storeLayout);
        this.updateDispatcher = Objects.requireNonNull(updateDispatcher);
        this.mtProtoOptions = Objects.requireNonNull(mtProtoOptions);
    }

    public DataCenter mainDc() {
        return mainDc;
    }

    public ClientFactory clientFactory() {
        return clientFactory;
    }

    public StoreLayout storeLayout() {
        return storeLayout;
    }

    public UpdateDispatcher updateDispatcher() {
        return updateDispatcher;
    }

    public MTProtoOptions mtProtoOptions() {
        return mtProtoOptions;
    }
}
