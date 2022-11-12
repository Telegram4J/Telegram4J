package telegram4j.mtproto.service;

import telegram4j.mtproto.MTProtoClientGroup;
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

    public ServiceHolder(MTProtoClientGroup clientGroup, StoreLayout storeLayout) {
        this.authService = new AuthService(clientGroup, storeLayout);
        this.accountService = new AccountService(clientGroup, storeLayout);
        this.chatService = new ChatService(clientGroup, storeLayout);
        this.phoneService = new PhoneService(clientGroup, storeLayout);
        this.stickersService = new StickersService(clientGroup, storeLayout);
        this.helpService = new HelpService(clientGroup, storeLayout);
        this.uploadService = new UploadService(clientGroup, storeLayout);
        this.updatesService = new UpdatesService(clientGroup, storeLayout);
        this.userService = new UserService(clientGroup, storeLayout);
        this.botService = new BotService(clientGroup, storeLayout);
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
