package telegram4j.core.spec;

import reactor.util.annotation.Nullable;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.tl.*;

import java.util.Objects;
import java.util.Optional;

public final class InputChatPhotoSpec {
    @Nullable
    private final String photo;
    @Nullable
    private final ImmutableInputChatUploadedPhoto uploaded;

    private InputChatPhotoSpec(String photo) {
        this.photo = Objects.requireNonNull(photo, "photo");
        this.uploaded = null;
    }

    private InputChatPhotoSpec(ImmutableInputChatUploadedPhoto uploaded) {
        this.photo = null;
        this.uploaded = Objects.requireNonNull(uploaded, "uploaded");
    }

    public static InputChatPhotoSpec of(String fileReferenceId) {
        return new InputChatPhotoSpec(fileReferenceId);
    }

    public static InputChatPhotoSpec ofUploaded(@Nullable InputFile file, @Nullable InputFile video,
                                        @Nullable Double videStartTimestamp) {
        return new InputChatPhotoSpec(InputChatUploadedPhoto.builder()
                .file(file)
                .video(video)
                .videoStartTs(videStartTimestamp)
                .build());
    }

    public Optional<String> getPhoto() {
        return Optional.ofNullable(photo);
    }

    public Optional<ImmutableInputChatUploadedPhoto> getUploaded() {
        return Optional.ofNullable(uploaded);
    }

    public InputChatPhoto asData() {
        if (photo != null) {
            return ImmutableBaseInputChatPhoto.of(
                    FileReferenceId.deserialize(photo).asInputPhoto());
        }

        return Objects.requireNonNull(uploaded);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InputChatPhotoSpec that = (InputChatPhotoSpec) o;
        return Objects.equals(photo, that.photo) && Objects.equals(uploaded, that.uploaded);
    }

    @Override
    public int hashCode() {
        return Objects.hash(photo, uploaded);
    }

    @Override
    public String toString() {
        return "InputChatPhotoSpec{" + (photo != null ? photo : uploaded) + '}';
    }
}
