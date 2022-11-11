package telegram4j.mtproto;

import reactor.util.annotation.Nullable;
import telegram4j.mtproto.store.StoreLayout;

import java.util.Objects;

public class MTProtoClientGroupOptions {
    public final MainMTProtoClient mainClient;
    public final StoreLayout storeLayout;
    @Nullable
    public final DcOptions dcOptions;

    public MTProtoClientGroupOptions(MainMTProtoClient mainClient, StoreLayout storeLayout, @Nullable DcOptions dcOptions) {
        this.mainClient = Objects.requireNonNull(mainClient);
        this.storeLayout = Objects.requireNonNull(storeLayout);
        this.dcOptions = dcOptions;
    }
}
