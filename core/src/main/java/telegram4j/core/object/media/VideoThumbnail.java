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

import java.util.Objects;
import java.util.Optional;

/**
 * Representation of animated thumbnail of animated profile pictures in MPEG4 format.
 *
 * @see <a href="https://core.telegram.org/api/files#animated-profile-pictures">Animated Profile Pictures</a>
 */
public final class VideoThumbnail implements AnimatedThumbnail {

    private final telegram4j.tl.BaseVideoSize data;

    public VideoThumbnail(telegram4j.tl.BaseVideoSize data) {
        this.data = Objects.requireNonNull(data);
    }

    /**
     * Gets single-char type of applied transformations to video.
     * Can be one of these chars:
     * <ul>
     *   <li>{@code 'u'}: if it's a animated profile photo.</li>
     *   <li>{@code 'v'}: if it's a trimmed and downscaled video previews.</li>
     * </ul>
     *
     * @return The single-char type of applied transformations.
     * @see <a href="https://core.telegram.org/api/files#video-types">Video Thumbnail Types</a>
     */
    public char getType() {
        return data.type().charAt(0);
    }

    /**
     * Gets width of video.
     *
     * @return The width of video.
     */
    public int getWidth() {
        return data.w();
    }

    /**
     * Gets height of video.
     *
     * @return The height of video.
     */
    public int getHeight() {
        return data.h();
    }

    /**
     * Gets video size in bytes.
     *
     * @return The video size in bytes.
     */
    public int getSize() {
        return data.size();
    }

    /**
     * Gets video timestamp (in seconds) that should be used as static preview, if present.
     *
     * @return The video timestamp (in seconds) that should be used as static preview, if present.
     */
    public Optional<Double> getVideoStartTimestamp() {
        return Optional.ofNullable(data.videoStartTs());
    }

    @Override
    public String toString() {
        return "VideoThumbnail{" +
                "data=" + data +
                '}';
    }
}
