package telegram4j.mtproto.client;

import telegram4j.mtproto.DataCenter;

import java.util.Objects;

public class DefaultClientFactory implements ClientFactory {
    private final MTProtoOptions options;

    public DefaultClientFactory(MTProtoOptions options) {
        this.options = Objects.requireNonNull(options);
    }

    @Override
    public MainMTProtoClient createMain(DataCenter dc) {
        return new DefaultMainMTProtoClient(dc, options);
    }

    @Override
    public MTProtoClient create(DataCenter dc) {
        return new BaseMTProtoClient(dc, options);
    }
}
