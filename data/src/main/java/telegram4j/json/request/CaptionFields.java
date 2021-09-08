package telegram4j.json.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import telegram4j.json.MessageEntityData;
import telegram4j.json.ParseMode;

import java.util.List;
import java.util.Optional;

public interface CaptionFields {

    @JsonProperty("parse_mode")
    Optional<ParseMode> parseMode();

    Optional<String> caption();

    @JsonProperty("caption_entities")
    Optional<List<MessageEntityData>> captionEntities();
}
