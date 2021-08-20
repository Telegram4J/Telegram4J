package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;
import java.util.Optional;

public interface InputMediaData {

    InputMediaType type();

    @Value.Immutable
    @JsonSerialize(as = ImmutableInputMediaPhotoData.class)
    @JsonDeserialize(as = ImmutableInputMediaPhotoData.class)
    interface InputMediaPhotoData extends InputMediaData {
        @Override
        default InputMediaType type() {
            return InputMediaType.PHOTO;
        }

        String media();

        Optional<String> caption();

        @JsonProperty("parse_mode")
        Optional<String> parseMode();

        @JsonProperty("caption_entities")
        Optional<List<MessageEntityData>> captionEntities();
    }

    @Value.Immutable
    @JsonSerialize(as = ImmutableInputMediaVideoData.class)
    @JsonDeserialize(as = ImmutableInputMediaVideoData.class)
    interface InputMediaVideoData extends InputMediaData {
        @Override
        default InputMediaType type() {
            return InputMediaType.VIDEO;
        }

        String media();

        Optional<String> thumb();

        Optional<String> caption();

        @JsonProperty("parse_mode")
        Optional<String> parseMode();

        @JsonProperty("caption_entities")
        Optional<List<MessageEntityData>> captionEntities();

        Optional<Integer> width();

        Optional<Integer> height();

        Optional<Integer> duration();

        @JsonProperty("supports_streaming")
        Optional<Boolean> supportsStreaming();
    }

    @Value.Immutable
    @JsonSerialize(as = ImmutableInputMediaAnimationData.class)
    @JsonDeserialize(as = ImmutableInputMediaAnimationData.class)
    interface InputMediaAnimationData extends InputMediaData {
        @Override
        default InputMediaType type() {
            return InputMediaType.ANIMATION;
        }

        String media();

        Optional<String> thumb();

        Optional<String> caption();

        @JsonProperty("parse_mode")
        Optional<String> parseMode();

        @JsonProperty("caption_entities")
        Optional<List<MessageEntityData>> captionEntities();

        Optional<Integer> width();

        Optional<Integer> height();

        Optional<Integer> duration();
    }

    @Value.Immutable
    @JsonSerialize(as = ImmutableInputMediaAudioData.class)
    @JsonDeserialize(as = ImmutableInputMediaAudioData.class)
    interface InputMediaAudioData extends InputMediaData {
        @Override
        default InputMediaType type() {
            return InputMediaType.AUDIO;
        }

        String media();

        Optional<String> thumb();

        Optional<String> caption();

        @JsonProperty("parse_mode")
        Optional<String> parseMode();

        @JsonProperty("caption_entities")
        Optional<List<MessageEntityData>> captionEntities();

        Optional<Integer> duration();

        Optional<String> performer();

        Optional<String> title();
    }

    @Value.Immutable
    @JsonSerialize(as = ImmutableInputMediaDocumentData.class)
    @JsonDeserialize(as = ImmutableInputMediaDocumentData.class)
    interface InputMediaDocumentData extends InputMediaData {
        @Override
        default InputMediaType type() {
            return InputMediaType.DOCUMENT;
        }

        String media();

        Optional<String> thumb();

        Optional<String> caption();

        @JsonProperty("parse_mode")
        Optional<String> parseMode();

        @JsonProperty("caption_entities")
        Optional<List<MessageEntityData>> captionEntities();

        @JsonProperty("disable_content_type_detection")
        Optional<Boolean> disableContentTypeDetection();
    }
}
