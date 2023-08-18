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
package telegram4j.core.object;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public class PeerNotifySettings {
    private final telegram4j.tl.PeerNotifySettings data;

    public PeerNotifySettings(telegram4j.tl.PeerNotifySettings data) {
        this.data = Objects.requireNonNull(data);
    }

    public Optional<Boolean> isShowPreviews() {
        return Optional.ofNullable(data.showPreviews());
    }

    public Optional<Boolean> isSilent() {
        return Optional.ofNullable(data.silent());
    }

    public Optional<Instant> getMuteUntilTimestamp() {
        return Optional.ofNullable(data.muteUntil()).map(Instant::ofEpochSecond);
    }

    // TODO: implement
    // public Optional<NotificationSound> getIosSound() {
    //     return Optional.empty();
    // }

    // public Optional<NotificationSound> getAndroidSound() {
    //     return Optional.empty();
    // }

    // public Optional<NotificationSound> getOtherSound() {
    //     return Optional.empty();
    // }

    @Override
    public String toString() {
        return "PeerNotifySettings{" +
                "data=" + data +
                '}';
    }
}
