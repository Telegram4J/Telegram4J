package telegram4j.rest;

import telegram4j.rest.service.ApplicationService;
import telegram4j.rest.service.ChatService;

public final class RestTelegramClient {

    private final ApplicationService applicationService;
    private final ChatService chatService;

    public RestTelegramClient(RestRouter restRouter) {
        this.chatService = new ChatService(restRouter);
        this.applicationService = new ApplicationService(restRouter);
    }

    public ChatService getChatService() {
        return chatService;
    }

    public ApplicationService getApplicationService() {
        return applicationService;
    }
}
