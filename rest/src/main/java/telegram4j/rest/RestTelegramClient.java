package telegram4j.rest;

import telegram4j.rest.service.ApplicationService;
import telegram4j.rest.service.ChatService;
import telegram4j.rest.service.CommandService;

public final class RestTelegramClient {

    private final ApplicationService applicationService;
    private final ChatService chatService;
    private final CommandService commandService;

    public RestTelegramClient(RestRouter restRouter) {
        this.chatService = new ChatService(restRouter);
        this.applicationService = new ApplicationService(restRouter);
        this.commandService = new CommandService(restRouter);
    }

    public ChatService getChatService() {
        return chatService;
    }

    public ApplicationService getApplicationService() {
        return applicationService;
    }

    public CommandService getCommandService() {
        return commandService;
    }
}
