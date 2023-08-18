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

public final class MaskCoordinates {
    private final telegram4j.tl.MaskCoords data;

    public MaskCoordinates(telegram4j.tl.MaskCoords data) {
        this.data = Objects.requireNonNull(data);
    }

    public Type getType() {
        return Type.ALL[data.n()];
    }

    public double getX() {
        return data.x();
    }

    public double getY() {
        return data.y();
    }

    public double getZoom() {
        return data.zoom();
    }

    @Override
    public String toString() {
        return "MaskCoordinates{" +
                "data=" + data +
                '}';
    }

    public enum Type {
        FOREHEAD,
        EYES,
        MOUTH,
        CHIN;

        static final Type[] ALL = values();
    }
}
