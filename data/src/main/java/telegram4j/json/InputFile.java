package telegram4j.json;

import reactor.util.annotation.Nullable;

import java.io.InputStream;

public class InputFile {

    @Nullable
    private final String url;

    @Nullable
    private final InputStream content;

    private InputFile(@Nullable String url, @Nullable InputStream content) {
        this.url = url;
        this.content = content;
    }

    public static InputFile ofUrl(String url) {
        return new InputFile(url, null);
    }

    public static InputFile ofContent(InputStream content) {
        return new InputFile(null, content);
    }

    @Nullable
    public String getUrl() {
        return url;
    }

    @Nullable
    public InputStream getContent() {
        return content;
    }
}
