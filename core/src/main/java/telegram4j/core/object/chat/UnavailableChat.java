package telegram4j.core.object.chat;

public sealed interface UnavailableChat extends Chat
        permits BaseUnavailableChat, UnavailableChannel, UnavailableGroupChat {

}
