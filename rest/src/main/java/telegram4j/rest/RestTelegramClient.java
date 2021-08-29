package telegram4j.rest;

import telegram4j.rest.service.ApplicationService;
import telegram4j.rest.service.ChatService;
import telegram4j.rest.service.CommandService;
import telegram4j.rest.service.UserService;

public final class RestTelegramClient {

    private final ApplicationService applicationService;
    private final ChatService chatService;
    private final CommandService commandService;
    private final UserService userService;

    public RestTelegramClient(RestRouter restRouter) {
        this.chatService = new ChatService(restRouter);
        this.applicationService = new ApplicationService(restRouter);
        this.commandService = new CommandService(restRouter);
        this.userService = new UserService(restRouter);
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

    public UserService getUserService() {
        return userService;
    }
}
