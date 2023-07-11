package telegram4j.core.object.chat;

public sealed interface ChannelPeer extends Chat
        permits Channel, BroadcastChannel, SupergroupChat, UnavailableChannel {
}
