package telegram4j.mtproto.client;

import reactor.core.publisher.Sinks;
import reactor.util.concurrent.Queues;
import telegram4j.mtproto.DataCenter;
import telegram4j.tl.Updates;

final class DefaultMainMTProtoClient extends BaseMTProtoClient implements MainMTProtoClient {

    private final Sinks.Many<Updates> updates;

    DefaultMainMTProtoClient(DataCenter dc, MTProtoOptions options) {
        super(dc, options);
        this.updates = Sinks.many().multicast()
                .onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false);
    }

    @Override
    public Sinks.Many<Updates> updates() {
        return updates;
    }

    @Override
    protected void emitUpdates(Updates updates) {
        this.updates.emitNext(updates, Sinks.EmitFailureHandler.FAIL_FAST);
    }
}
