package telegram4j.mtproto;

import telegram4j.mtproto.auth.AuthorizationContext;

public class MTProtoOptions {
    private final MTProtoResources resources;
    private final AuthorizationContext authorizationContext;
    private final int acksSendThreshold;

    public MTProtoOptions(MTProtoResources resources, AuthorizationContext authorizationContext, int acksSendThreshold) {
        this.resources = resources;
        this.authorizationContext = authorizationContext;
        this.acksSendThreshold = acksSendThreshold;
    }

    public MTProtoResources getResources() {
        return resources;
    }

    public AuthorizationContext getAuthorizationContext() {
        return authorizationContext;
    }

    public int getAcksSendThreshold() {
        return acksSendThreshold;
    }
}
