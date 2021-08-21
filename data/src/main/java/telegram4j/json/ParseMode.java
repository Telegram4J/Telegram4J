package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ParseMode {
    @JsonProperty("MarkdownV2")
    MARKDOWN_V_2,

    HTML,

    @JsonProperty("Markdown")
    MARKDOWN
}
