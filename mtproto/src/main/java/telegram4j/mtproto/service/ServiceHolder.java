package telegram4j.mtproto.service;

import telegram4j.mtproto.MTProtoClient;
import telegram4j.mtproto.store.StoreLayout;

public final class ServiceHolder {

    private final AuthService authService;
    private final AccountService accountService;
    private final ChatService chatService;
    private final PhoneService phoneService;
    private final StickersService stickersService;
    private final HelpService helpService;
    private final UploadService uploadService;
    private final UpdatesService updatesService;
    private final UserService userService;
    private final BotService botService;

    public ServiceHolder(MTProtoClient client, StoreLayout storeLayout) {
        this.authService = new AuthService(client, storeLayout);
        this.accountService = new AccountService(client, storeLayout);
        this.chatService = new ChatService(client, storeLayout);
        this.phoneService = new PhoneService(client, storeLayout);
        this.stickersService = new StickersService(client, storeLayout);
        this.helpService = new HelpService(client, storeLayout);
        this.uploadService = new UploadService(client, storeLayout);
        this.updatesService = new UpdatesService(client, storeLayout);
        this.userService = new UserService(client, storeLayout);
        this.botService = new BotService(client, storeLayout);
    }

    public AuthService getAuthService() {
        return authService;
    }

    public AccountService getAccountService() {
        return accountService;
    }

    public ChatService getChatService() {
        return chatService;
    }

    public PhoneService getPhoneService() {
        return phoneService;
    }

    public StickersService getStickersService() {
        return stickersService;
    }

    public HelpService getHelpService() {
        return helpService;
    }

    public UploadService getUploadService() {
        return uploadService;
    }

    public UpdatesService getUpdatesService() {
        return updatesService;
    }

    public UserService getUserService() {
        return userService;
    }

    public BotService getBotService() {
        return botService;
    }
}
