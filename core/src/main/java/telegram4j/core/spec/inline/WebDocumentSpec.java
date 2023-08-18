/*
 * Copyright 2023 Telegram4J
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package telegram4j.core.spec.inline;

import reactor.util.annotation.Nullable;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public final class WebDocumentSpec {
    private final String url;
    @Nullable
    private final WebDocumentFields.Size size;
    @Nullable
    private final String mimeType;
    @Nullable
    private final String filename;
    @Nullable
    private final Duration duration;

    private WebDocumentSpec(String url) {
        this.url = Objects.requireNonNull(url);
        this.size = null;
        this.mimeType = null;
        this.filename = null;
        this.duration = null;
    }

    private WebDocumentSpec(String url, @Nullable WebDocumentFields.Size size, @Nullable String mimeType,
                            @Nullable String filename, @Nullable Duration duration) {
        this.url = url;
        this.size = size;
        this.mimeType = mimeType;
        this.filename = filename;
        this.duration = duration;
    }

    public static WebDocumentSpec of(String url) {
        return new WebDocumentSpec(url);
    }

    public static WebDocumentSpec of(String url, @Nullable WebDocumentFields.Size size, @Nullable String mimeType,
                                     @Nullable String filename, @Nullable Duration duration) {
        Objects.requireNonNull(url);
        return new WebDocumentSpec(url, size, mimeType, filename, duration);
    }

    public String url() {
        return url;
    }

    public Optional<WebDocumentFields.Size> size() {
        return Optional.ofNullable(size);
    }

    public Optional<String> mimeType() {
        return Optional.ofNullable(mimeType);
    }

    public Optional<String> filename() {
        return Optional.ofNullable(filename);
    }

    private Optional<Duration> duration() {
        return Optional.ofNullable(duration);
    }

    public WebDocumentSpec withUrl(String value) {
        Objects.requireNonNull(value);
        if (url.equals(value)) return this;
        return new WebDocumentSpec(value, size, mimeType, filename, duration);
    }

    public WebDocumentSpec withSize(@Nullable WebDocumentFields.Size value) {
        if (Objects.equals(size, value)) return this;
        return new WebDocumentSpec(url, value, mimeType, filename, duration);
    }

    public WebDocumentSpec withSize(Optional<WebDocumentFields.Size> opt) {
        return withSize(opt.orElse(null));
    }

    public WebDocumentSpec withMimeType(@Nullable String value) {
        if (Objects.equals(mimeType, value)) return this;
        return new WebDocumentSpec(url, size, value, filename, duration);
    }

    public WebDocumentSpec withMimeType(Optional<String> opt) {
        return withMimeType(opt.orElse(null));
    }

    public WebDocumentSpec withFilename(@Nullable String value) {
        if (Objects.equals(filename, value)) return this;
        return new WebDocumentSpec(url, size, mimeType, value, duration);
    }

    public WebDocumentSpec withFilename(Optional<String> opt) {
        return withFilename(opt.orElse(null));
    }

    public WebDocumentSpec withDuration(@Nullable Duration value) {
        if (Objects.equals(duration, value)) return this;
        return new WebDocumentSpec(url, size, mimeType, filename, value);
    }

    public WebDocumentSpec withDuration(Optional<Duration> opt) {
        return withDuration(opt.orElse(null));
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WebDocumentSpec that = (WebDocumentSpec) o;
        return url.equals(that.url) && Objects.equals(size, that.size) &&
                Objects.equals(mimeType, that.mimeType) && Objects.equals(filename, that.filename) &&
                Objects.equals(duration, that.duration);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + url.hashCode();
        h += (h << 5) + Objects.hashCode(size);
        h += (h << 5) + Objects.hashCode(mimeType);
        h += (h << 5) + Objects.hashCode(filename);
        h += (h << 5) + Objects.hashCode(duration);
        return h;
    }

    @Override
    public String toString() {
        return "WebDocumentSpec{" +
                "url='" + url + '\'' +
                ", size=" + size +
                ", mimeType='" + mimeType + '\'' +
                ", filename='" + filename + '\'' +
                ", duration=" + duration +
                '}';
    }
}
