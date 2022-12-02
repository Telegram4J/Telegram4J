package telegram4j.mtproto;

import reactor.core.publisher.Sinks;
import reactor.util.concurrent.Queues;
import telegram4j.tl.Updates;

public final class DefaultMTProtoClient extends BaseMTProtoClient implements MainMTProtoClient {

    private final Sinks.Many<Updates> updates;

    DefaultMTProtoClient(DataCenter dc, MTProtoOptions options) {
        super(dc, options);
        this.updates = Sinks.many().multicast()
                .onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false);
    }

    public DefaultMTProtoClient(MTProtoOptions options) {
        this(options.getDatacenter(), options);
    }

    @Override
    public Sinks.Many<Updates> updates() {
        return updates;
    }

    @Override
    public MTProtoClient createChildClient(DataCenter dc) {
        BaseMTProtoClient client = new BaseMTProtoClient(dc, options);
        if (dc.getId() == getDatacenter().getId()) {
            client.authKey = authKey;
            client.timeOffset = timeOffset;
            client.serverSalt = serverSalt;
        }

        return client;
    }

    @Override
    public MainMTProtoClient create(DataCenter dc) {
        DefaultMTProtoClient client = new DefaultMTProtoClient(dc, options);
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
