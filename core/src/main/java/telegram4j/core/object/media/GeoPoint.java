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

/** Geo point representation. */
public final class GeoPoint {

    private final telegram4j.tl.BaseGeoPoint data;

    public GeoPoint(telegram4j.tl.BaseGeoPoint data) {
        this.data = Objects.requireNonNull(data);
    }

    /**
     * Gets longitude coordinates of geo point.
     *
     * @return The longitude coordinates of geo point.
     */
    public double getLongitude() {
        return data.longitude();
    }

    /**
     * Gets latitude coordinates of geo point.
     *
     * @return The latitude coordinates of geo point.
     */
    public double getLatitude() {
        return data.lat();
    }

    /**
     * Gets access hash for geo point.
     *
     * @return The access hash for geo point.
     */
    public long getAccessHash() {
        return data.accessHash();
    }

    /**
     * Gets estimated horizontal accuracy of the location, in meters; defined by the sender.
     *
     * @return The estimated horizontal accuracy of the location, in meters; defined by the sender.
     */
    public Optional<Integer> getAccuracyRadius() {
        return Optional.ofNullable(data.accuracyRadius());
    }

    @Override
    public String toString() {
        return "GeoPoint{" +
                "data=" + data +
                '}';
    }
}
