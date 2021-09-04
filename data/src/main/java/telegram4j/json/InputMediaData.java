package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

@Value.Immutable
@JsonSerialize(as = ImmutableInputMediaData.class)
@JsonDeserialize(as = ImmutableInputMediaData.class)
public interface InputMediaData {

    static ImmutableInputMediaData.Builder builder() {
        return ImmutableInputMediaData.builder();
    }

    InputMediaType type();

    Optional<String> media();

    Optional<String> caption();

    @JsonProperty("parse_mode")
    Optional<ParseMode> parseMode();

    @JsonProperty("caption_entities")
    Optional<List<MessageEntityData>> captionEntities();

    Optional<Integer> width();

    Optional<Integer> height();

    Optional<Integer> duration();

    @JsonProperty("supports_streaming")
    Optional<Boolean> supportsStreaming();

    Optional<String> performer();

    Optional<String> title();

    @JsonProperty("disable_content_type_detection")
    Optional<Boolean> disableContentTypeDetection();
}
