package telegram4j.mtproto;

import reactor.core.publisher.Sinks;
import reactor.util.concurrent.Queues;
import telegram4j.tl.Updates;

public final class DefaultMTProtoClient extends BaseMTProtoClient implements MainMTProtoClient {

    private final Sinks.Many<Updates> updates;

    public DefaultMTProtoClient(MTProtoOptions options) {
        super(DcId.Type.MAIN, options.getDatacenter(), options);
        this.updates = Sinks.many().multicast()
                .onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false);
    }

    @Override
    public Sinks.Many<Updates> updates() {
        return updates;
    }

    @Override
    public MTProtoClient createChildClient(DcId.Type type, DataCenter dc) {
        BaseMTProtoClient client = new BaseMTProtoClient(type, dc, options);
        if (dc.getId() == getDatacenter().getId()) {
            client.authKey = authKey;
            client.timeOffset = timeOffset;
            client.serverSalt = serverSalt;
        }

        return client;
    }

    @Override
    protected void emitUpdates(Updates updates) {
        this.updates.emitNext(updates, options.getEmissionHandler());
    }
}
