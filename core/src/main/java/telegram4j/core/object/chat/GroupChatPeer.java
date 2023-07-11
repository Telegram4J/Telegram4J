package telegram4j.core.object.chat;

public sealed interface GroupChatPeer extends Chat
        permits GroupChat, UnavailableGroupChat {

}
