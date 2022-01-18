package telegram4j.mtproto.file;

import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.api.Test;
import telegram4j.mtproto.util.CryptoUtil;
import telegram4j.tl.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static telegram4j.mtproto.file.FileReferenceId.*;

class FileReferenceIdTest {

    @Test
    void chatPhoto() {
        var expChatPhoto = ofChatPhoto(InputPeerSelf.instance(),
                ImmutableBaseUserProfilePhoto.of(1337, 2), PhotoSizeType.CHAT_PHOTO_BIG);
        var expDocument = ofDocument(BaseDocument.builder()
                .id(1337)
                .accessHash(-1111)
                .fileReference(CryptoUtil.random.generateSeed(8))
                .date(Math.toIntExact(System.currentTimeMillis() / 1000))
                .thumbs(List.of(BasePhotoSize.builder()
                        .type("i")
                        .w(100)
                        .h(100)
                        .size(-1)
                        .build()))
                .dcId(2)
                // ignored fields
                .mimeType("")
                .size(-1)
                .attributes(List.of())
                .build());
        var expPhoto = ofPhoto(BasePhoto.builder()
                .id(1337)
                .accessHash(-1111)
                .fileReference(CryptoUtil.random.generateSeed(8))
                .date(Math.toIntExact(System.currentTimeMillis() / 1000))
                .sizes(List.of(BasePhotoSize.builder()
                        .type("i")
                        .w(100)
                        .h(100)
                        .size(-1)
                        .build()))
                .dcId(2)
                .build());
        var expStickerSet = ofStickerSet(ImmutableInputStickerSetID.of(1337, -1111), 2);

        var actChatPhoto = serialize(expChatPhoto);
        var actDocument = serialize(expDocument);
        var actPhoto = serialize(expPhoto);
        var actStickerSet = serialize(expStickerSet);

        assertEquals(expChatPhoto, actChatPhoto);
        assertEquals(expDocument, actDocument);
        assertEquals(expPhoto, actPhoto);
        assertEquals(expStickerSet, actStickerSet);
    }

    static FileReferenceId serialize(FileReferenceId ref) {
        return deserialize(ref.serialize(ByteBufAllocator.DEFAULT));
    }
}
