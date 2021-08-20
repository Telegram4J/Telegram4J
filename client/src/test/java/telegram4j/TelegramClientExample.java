package telegram4j;

public class TelegramClientExample {

    public static void main(String[] args) {
        TelegramClient telegramClient = TelegramClient.create(System.getenv("T4J_TOKEN"));

        telegramClient.getRestClient().getApplicationService().getUpdates().log().subscribe();

        while (true);
    }
}
