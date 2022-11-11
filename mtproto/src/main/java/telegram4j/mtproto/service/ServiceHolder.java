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

    public ServiceHolder(MTProtoClientGroup groupManager, StoreLayout storeLayout) {
        this.authService = new AuthService(groupManager, storeLayout);
        this.accountService = new AccountService(groupManager, storeLayout);
        this.chatService = new ChatService(groupManager, storeLayout);
        this.phoneService = new PhoneService(groupManager, storeLayout);
        this.stickersService = new StickersService(groupManager, storeLayout);
        this.helpService = new HelpService(groupManager, storeLayout);
        this.uploadService = new UploadService(groupManager, storeLayout);
        this.updatesService = new UpdatesService(groupManager, storeLayout);
        this.userService = new UserService(groupManager, storeLayout);
        this.botService = new BotService(groupManager, storeLayout);
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
