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
package telegram4j.core.object.media;

/**
 * Type of static thumbnails for documents and photos.
 */
public sealed interface Thumbnail
        permits CachedThumbnail, PhotoThumbnail, ProgressiveThumbnail,
                StrippedThumbnail, VectorThumbnail {

    /**
     * Gets a single-char type of thumbnail representing
     * applied server-side transformations.
     *
     * @return The type of thumbnail.
     * @see <a href="https://core.telegram.org/api/files#image-thumbnail-types">Static Thumbnail Types</a>
     */
    char getType();
}
