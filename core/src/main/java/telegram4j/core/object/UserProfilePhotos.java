package telegram4j.core.object;

import telegram4j.core.TelegramClient;
import telegram4j.json.UserProfilePhotosData;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class UserProfilePhotos implements TelegramObject {

    private final TelegramClient client;
    private final UserProfilePhotosData data;

    public UserProfilePhotos(TelegramClient client, UserProfilePhotosData data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    public UserProfilePhotosData getData() {
        return data;
    }

    public int getTotalCount() {
        return getData().totalCount();
    }

    public List<List<PhotoSize>> getPhotos() {
        return getData().photos().stream()
                .map(list -> list.stream()
                        .map(data -> new PhotoSize(client, data))
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserProfilePhotos that = (UserProfilePhotos) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(data);
    }

    @Override
    public String toString() {
        return "UserProfilePhotos{data=" + data + '}';
    }
}
