package telegram4j;

import java.util.Objects;

public class TelegramClient {

    private static final String BASE_URL = "https://api.telegram.org";

    private final String token;

    private TelegramClient(String token) {
        this.token = Objects.requireNonNull(token, "token");
    }

    public static TelegramClient create(String token) {
        return new TelegramClient(token);
    }
}
