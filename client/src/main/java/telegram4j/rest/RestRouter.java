package telegram4j.rest;

public interface RestRouter {

    TelegramResponse exchange(TelegramRequest request);
}
