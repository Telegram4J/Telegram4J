package telegram4j.mtproto.service;

import telegram4j.mtproto.MTProtoClient;
import telegram4j.mtproto.store.StoreLayout;

public final class ServiceHolder {

    private final AuthService authService;
    private final AccountService accountService;
    private final ChatService chatService;
    private final LangPackService langPackService;
    private final PaymentsService paymentsService;
    private final PhoneService phoneService;
    private final StatsService statsService;
    private final StickersService stickersService;
    private final HelpService helpService;
    private final MessageService messageService;
    private final UpdatesService updatesService;
    private final UserService userService;
    private final BotService botService;

    public ServiceHolder(MTProtoClient client, StoreLayout storeLayout) {
        this.authService = new AuthService(client, storeLayout);
        this.accountService = new AccountService(client, storeLayout);
        this.chatService = new ChatService(client, storeLayout);
        this.langPackService = new LangPackService(client, storeLayout);
        this.paymentsService = new PaymentsService(client, storeLayout);
        this.phoneService = new PhoneService(client, storeLayout);
        this.statsService = new StatsService(client, storeLayout);
        this.stickersService = new StickersService(client, storeLayout);
        this.helpService = new HelpService(client, storeLayout);
        this.messageService = new MessageService(client, storeLayout);
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

    public LangPackService getLangPackService() {
        return langPackService;
    }

    public PaymentsService getPaymentsService() {
        return paymentsService;
    }

    public PhoneService getPhoneService() {
        return phoneService;
    }

    public StatsService getStatsService() {
        return statsService;
    }

    public StickersService getStickersService() {
        return stickersService;
    }

    public HelpService getHelpService() {
        return helpService;
    }

    public MessageService getMessageService() {
        return messageService;
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
