package telegram4j.mtproto;

import reactor.netty.tcp.TcpClient;

public class MTProtoOptions {
    private final TcpClient tcpClient;
    private final SessionResources mtProtoResources;

    public MTProtoOptions(TcpClient tcpClient, SessionResources mtProtoResources) {
        this.tcpClient = tcpClient;
        this.mtProtoResources = mtProtoResources;
    }

    public TcpClient getTcpClient() {
        return tcpClient;
    }

    public SessionResources getMtProtoResources() {
        return mtProtoResources;
    }
}
