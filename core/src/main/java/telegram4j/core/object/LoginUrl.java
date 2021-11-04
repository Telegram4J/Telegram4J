package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.json.LoginUrlData;

import java.util.Objects;
import java.util.Optional;

public class LoginUrl {

    private final LoginUrlData data;

    public LoginUrl(LoginUrlData data) {
        this.data = Objects.requireNonNull(data, "data");
    }

    public LoginUrlData getData() {
        return data;
    }

    public String getUrl() {
        return data.url();
    }

    public Optional<String> getForwardText() {
        return data.forwardText();
    }

    public Optional<String> getBotUsername() {
        return data.botUsername();
    }

    public boolean isRequestWriteAccess() {
        return data.requestWriteAccess();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoginUrl that = (LoginUrl) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "LoginUrl{data=" + data + '}';
    }
}
