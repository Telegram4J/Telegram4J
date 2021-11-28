package telegram4j.mtproto;

public class MTProtoOptions {
    private final MTProtoResources resources;
    private final int acksSendThreshold;

    public MTProtoOptions(MTProtoResources resources, int acksSendThreshold) {
        this.resources = resources;
        this.acksSendThreshold = acksSendThreshold;
    }

    public MTProtoResources getResources() {
        return resources;
    }

    public int getAcksSendThreshold() {
        return acksSendThreshold;
    }
}
