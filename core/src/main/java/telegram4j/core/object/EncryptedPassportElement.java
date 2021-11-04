package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.TelegramClient;
import telegram4j.json.EncryptedPassportElementData;
import telegram4j.json.EncryptedPassportElementType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class EncryptedPassportElement implements TelegramObject {

    private final TelegramClient client;
    private final EncryptedPassportElementData data;

    public EncryptedPassportElement(TelegramClient client, EncryptedPassportElementData data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    public EncryptedPassportElementData getData() {
        return data;
    }

    public EncryptedPassportElementType getType() {
        return data.type();
    }

    // TODO: rename this method
    public Optional<String> getJson() {
        return data.data();
    }

    public Optional<String> getPhoneNumber() {
        return data.phoneNumber();
    }

    public Optional<String> getEmail() {
        return data.email();
    }

    public Optional<List<PassportFile>> getFiles() {
        return data.files().map(list -> list.stream()
                .map(data -> new PassportFile(client, data))
                .collect(Collectors.toList()));
    }

    public Optional<PassportFile> getFrontSide() {
        return data.frontSide().map(data -> new PassportFile(client, data));
    }

    public Optional<PassportFile> getReverseSide() {
        return data.reverseSide().map(data -> new PassportFile(client, data));
    }

    public Optional<PassportFile> getSelfie() {
        return data.selfie().map(data -> new PassportFile(client, data));
    }

    public Optional<List<PassportFile>> getTranslation() {
        return data.translation().map(list -> list.stream()
                .map(data -> new PassportFile(client, data))
                .collect(Collectors.toList()));
    }

    public String getHash() {
        return data.hash();
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EncryptedPassportElement that = (EncryptedPassportElement) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "EncryptedPassportElement{data=" + data + '}';
    }
}
