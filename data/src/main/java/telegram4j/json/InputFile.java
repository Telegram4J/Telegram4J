package telegram4j.json;

import reactor.util.annotation.Nullable;

import java.io.InputStream;
import java.util.Objects;

public class InputFile {

    private final String filename;

    @Nullable
    private final String url;

    @Nullable
    private final InputStream content;

    private InputFile(String filename, @Nullable String url, @Nullable InputStream content) {
        this.filename = filename;
        this.url = url;
        this.content = content;
    }

    public static InputFile ofUrl(String filename, String url) {
        return new InputFile(filename, url, null);
    }

    public static InputFile ofContent(String filename, InputStream content) {
        return new InputFile(filename, null, content);
    }

    public String getFilename() {
        return filename;
    }

    @Nullable
    public String getUrl() {
        return url;
    }

    @Nullable
    public InputStream getContent() {
        return content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InputFile inputFile = (InputFile) o;
        return filename.equals(inputFile.filename) && Objects.equals(url, inputFile.url) &&
                Objects.equals(content, inputFile.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filename, url, content);
    }
}
