package telegram4j.mtproto.store.object;

import reactor.util.annotation.Nullable;
import telegram4j.tl.api.TlObject;

import java.util.Objects;

public class PeerData<M extends TlObject, F extends TlObject> {
    public final M minData;
    @Nullable
    public final F fullData;

    public PeerData(M minData, @Nullable F fullData) {
        this.minData = Objects.requireNonNull(minData);
        this.fullData = fullData;
    }
}
