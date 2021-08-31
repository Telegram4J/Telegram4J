package telegram4j.rest;

import reactor.util.annotation.Nullable;
import reactor.util.function.Tuple2;
import telegram4j.json.InputFile;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

public class MultipartRequest<J> {

    @Nullable
    private final J json;
    private final List<Tuple2<String, InputFile>> files;

    private MultipartRequest(@Nullable J json, List<Tuple2<String, InputFile>> files) {
        this.json = json;
        this.files = files;
    }

    public static <B> MultipartRequest<B> ofBody(@Nullable B body) {
        return new MultipartRequest<>(body, Collections.emptyList());
    }

    public static <B> MultipartRequest<B> ofBodyAndFiles(@Nullable B body, List<Tuple2<String, InputFile>> files) {
        return new MultipartRequest<>(body, files);
    }

    @Nullable
    public J getJson() {
        return json;
    }

    public List<Tuple2<String, InputFile>> getFiles() {
        return files;
    }
}
