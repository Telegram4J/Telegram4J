package telegram4j.mtproto.client;

import telegram4j.mtproto.DataCenter;
import telegram4j.mtproto.DcId;
import telegram4j.mtproto.client.impl.MTProtoClientImpl;

import java.util.Objects;

public class DefaultClientFactory implements ClientFactory {
    private final MTProtoOptions mtprotoOptions;
    private final MTProtoClient.Options clientOptions;

    public DefaultClientFactory(MTProtoOptions mtprotoOptions, MTProtoClient.Options clientOptions) {
        this.mtprotoOptions = Objects.requireNonNull(mtprotoOptions);
        this.clientOptions = Objects.requireNonNull(clientOptions);
    }

    @Override
    public MTProtoClient create(MTProtoClientGroup group, DcId.Type type, DataCenter dc) {
        return new MTProtoClientImpl(group, type, dc, mtprotoOptions, clientOptions);
    }
}
