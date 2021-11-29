package telegram4j.mtproto;

import reactor.netty.tcp.TcpClient;

public class MTProtoOptions {
    private final TcpClient tcpClient;
    private final MTProtoResources mtProtoResources;

    public MTProtoOptions(TcpClient tcpClient, MTProtoResources mtProtoResources) {
        this.tcpClient = tcpClient;
        this.mtProtoResources = mtProtoResources;
    }

    public TcpClient getTcpClient() {
        return tcpClient;
    }

    public MTProtoResources getMtProtoResources() {
        return mtProtoResources;
    }
}
