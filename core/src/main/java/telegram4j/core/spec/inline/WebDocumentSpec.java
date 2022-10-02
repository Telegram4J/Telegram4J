package telegram4j.core.spec.inline;

import reactor.util.annotation.Nullable;
import telegram4j.core.spec.Spec;

import java.util.Objects;
import java.util.Optional;

public final class WebDocumentSpec implements Spec {
    private final String url;
    @Nullable
    private final SizeSpec size;
    @Nullable
    private final String mimeType;

    private WebDocumentSpec(String url) {
        this.url = Objects.requireNonNull(url);
        this.size = null;
        this.mimeType = null;
    }

    private WebDocumentSpec(String url, @Nullable SizeSpec size, @Nullable String mimeType) {
        this.url = url;
        this.size = size;
        this.mimeType = mimeType;
    }

    public static WebDocumentSpec of(String url) {
        return new WebDocumentSpec(url);
    }

    public static WebDocumentSpec of(String url, @Nullable SizeSpec size, @Nullable String mimeType) {
        Objects.requireNonNull(url);
        return new WebDocumentSpec(url, size, mimeType);
    }

    public String url() {
        return url;
    }

    public Optional<SizeSpec> size() {
        return Optional.ofNullable(size);
    }

    public Optional<String> mimeType() {
        return Optional.ofNullable(mimeType);
    }

    public WebDocumentSpec withUrl(String value) {
        Objects.requireNonNull(value);
        if (url.equals(value)) return this;
        return new WebDocumentSpec(value, size, mimeType);
    }

    public WebDocumentSpec withSize(@Nullable SizeSpec value) {
        if (Objects.equals(size, value)) return this;
        return new WebDocumentSpec(url, value, mimeType);
    }

    public WebDocumentSpec withSize(Optional<SizeSpec> opt) {
        return withSize(opt.orElse(null));
    }

    public WebDocumentSpec withMimeType(@Nullable String value) {
        if (Objects.equals(mimeType, value)) return this;
        return new WebDocumentSpec(url, size, value);
    }

    public WebDocumentSpec withMimeType(Optional<String> opt) {
        return withMimeType(opt.orElse(null));
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof WebDocumentSpec)) return false;
        WebDocumentSpec that = (WebDocumentSpec) o;
        return url.equals(that.url) && Objects.equals(size, that.size) && Objects.equals(mimeType, that.mimeType);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + url.hashCode();
        h += (h << 5) + Objects.hashCode(size);
        h += (h << 5) + Objects.hashCode(mimeType);
        return h;
    }

    @Override
    public String toString() {
        return "WebDocumentSpec{" +
                "url='" + url + '\'' +
                ", size=" + size +
                ", mimeType='" + mimeType + '\'' +
                '}';
    }
}
