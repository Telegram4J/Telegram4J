package telegram4j.mtproto;

import telegram4j.mtproto.crypto.MTProtoAuthorizationContext;

public class MTProtoOptions {
    private final MTProtoResources resources;
    private final MTProtoAuthorizationContext authorizationContext;
    private final int acksSendThreshold;

    public MTProtoOptions(MTProtoResources resources, MTProtoAuthorizationContext authorizationContext, int acksSendThreshold) {
        this.resources = resources;
        this.authorizationContext = authorizationContext;
        this.acksSendThreshold = acksSendThreshold;
    }

    public MTProtoResources getResources() {
        return resources;
    }

    public MTProtoAuthorizationContext getAuthorizationContext() {
        return authorizationContext;
    }

    public int getAcksSendThreshold() {
        return acksSendThreshold;
    }
}
