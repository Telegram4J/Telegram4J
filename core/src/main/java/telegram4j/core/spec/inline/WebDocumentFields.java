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
import telegram4j.tl.DocumentAttributeImageSize;
import telegram4j.tl.ImmutableDocumentAttributeImageSize;

public class WebDocumentFields {
    private WebDocumentFields() {}

    public static final class Size {
        private final int width;
        private final int height;

        private Size(int width, int height) {
            this.width = width;
            this.height = height;
        }

        public int width() {
            return width;
        }

        public int height() {
            return height;
        }

        public Size withWidth(int value) {
            if (width == value) return this;
            return new Size(value, height);
        }

        public Size withHeight(int value) {
            if (height == value) return this;
            return new Size(width, value);
        }

        public ImmutableDocumentAttributeImageSize asData() {
            return ImmutableDocumentAttributeImageSize.of(width, height);
        }

        public static Size from(DocumentAttributeImageSize value) {
            return new Size(value.w(), value.h());
        }

        public static Size of(int size) {
            return new Size(size, size);
        }

        public static Size of(int width, int height) {
            return new Size(width, height);
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (!(o instanceof Size size)) return false;
            return width == size.width && height == size.height;
        }

        @Override
        public int hashCode() {
            return height + 51 * height;
        }

        @Override
        public String toString() {
            return "Size{" +
                    "width=" + width +
                    ", height=" + height +
                    '}';
        }
    }
}
