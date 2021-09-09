package telegram4j.rest;

import reactor.util.annotation.Nullable;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import telegram4j.json.InputFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MultipartRequest<J> {

    @Nullable
    private final J json;
    private final Collection<Tuple2<String, InputFile>> files;

    private MultipartRequest(@Nullable J json, Collection<Tuple2<String, InputFile>> files) {
        this.json = json;
        this.files = files;
    }

    public static <B> MultipartRequest<B> ofBody(@Nullable B body) {
        return new MultipartRequest<>(body, Collections.emptyList());
    }

    public static <B> MultipartRequest<B> ofBodyAndFiles(@Nullable B body, Collection<Tuple2<String, InputFile>> files) {
        return new MultipartRequest<>(body, files);
    }

    public MultipartRequest<J> addFile(String name, InputFile file) {
        List<Tuple2<String, InputFile>> added = new ArrayList<>(files);
        added.add(Tuples.of(name, file));
        return new MultipartRequest<>(json, added);
    }

    public MultipartRequest<J> addFiles(Collection<? extends Tuple2<String, InputFile>> files) {
        List<Tuple2<String, InputFile>> added = new ArrayList<>(this.files);
        added.addAll(files);
        return new MultipartRequest<>(json, added);
    }

    @Nullable
    public J getJson() {
        return json;
    }

    public Collection<Tuple2<String, InputFile>> getFiles() {
        return files;
    }
}
