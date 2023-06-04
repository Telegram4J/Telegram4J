package telegram4j.mtproto.client;

import telegram4j.mtproto.DataCenter;
import telegram4j.mtproto.DcId;

import java.util.Objects;

public class DefaultClientFactory implements ClientFactory {
    private final MTProtoOptions options;

    public DefaultClientFactory(MTProtoOptions options) {
        this.options = Objects.requireNonNull(options);
    }

    @Override
    public MTProtoClient create(MTProtoClientGroup group, DcId.Type type, DataCenter dc) {
        return new MTProtoClientImpl(group, type, dc, options);
    }
}
