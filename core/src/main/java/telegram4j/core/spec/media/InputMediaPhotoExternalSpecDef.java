package telegram4j.core.spec.media;

import org.immutables.value.Value;
import telegram4j.tl.InputMediaPhotoExternal;

import java.time.Duration;
import java.util.Optional;

@Value.Immutable(builder = false)
interface InputMediaPhotoExternalSpecDef extends InputMediaSpec {

    @Override
    default Type type() {
        return Type.PHOTO_EXTERNAL;
    }

    String url();

    Optional<Duration> autoDeleteDuration();

    @Override
    default InputMediaPhotoExternal asData() {
        return InputMediaPhotoExternal.builder()
                .url(url())
                .ttlSeconds(autoDeleteDuration()
                        .map(Duration::getSeconds)
                        .map(Math::toIntExact)
                        .orElse(null))
                .build();
    }
}
