package telegram4j.rest;

import telegram4j.rest.service.ChatService;

public final class RestTelegramClient {

    private final ChatService chatService;

    public RestTelegramClient(RestRouter restRouter) {
        this.chatService = new ChatService(restRouter);
    }

    public ChatService getChatService() {
        return chatService;
    }
}
