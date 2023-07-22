package telegram4j.core.spec.media;

import telegram4j.core.internal.MonoSpec;
import telegram4j.tl.InputMedia;

// TODO: Implement spec for inputMediaGame
public sealed interface InputMediaSpec extends MonoSpec<InputMedia>
        permits InputMediaContactSpec, InputMediaDiceSpec, InputMediaDocumentSpec,
                InputMediaGeoLiveSpec, InputMediaGeoPointSpec, InputMediaPhotoSpec,
                InputMediaPollSpec, InputMediaUploadedDocumentSpec, InputMediaUploadedPhotoSpec,
                InputMediaVenueSpec {
}
