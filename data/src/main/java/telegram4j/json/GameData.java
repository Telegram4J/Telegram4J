package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableGameData.class)
@JsonDeserialize(as = ImmutableGameData.class)
public interface GameData {

    static ImmutableGameData.Builder builder() {
        return ImmutableGameData.builder();
    }

    String title();

    String description();

    List<PhotoSizeData> photo();

    Optional<String> text();

    @JsonProperty("text_entities")
    Optional<List<MessageEntityData>> textEntities();

    Optional<AnimationData> animation();
}
