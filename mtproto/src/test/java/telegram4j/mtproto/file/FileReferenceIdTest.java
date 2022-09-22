package telegram4j.mtproto.file;

import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import telegram4j.mtproto.util.CryptoUtil;
import telegram4j.tl.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static telegram4j.mtproto.file.FileReferenceId.*;

class FileReferenceIdTest {

    @Test
    void all() {
        var expWebDocumentNoProxy = ofDocument(WebDocumentNoProxy.builder()
                .url("https://www.google.com")
                // ignored fields
                .mimeType("")
                .size(-1)
                .attributes(List.of())
                .build(), 2, InputPeerSelf.instance());
        var expWebDocument = ofDocument(BaseWebDocument.builder()
                .url("https://www.google.com")
                .accessHash(1337 >> 4)
                // ignored fields
                .mimeType("")
                .attributes(List.of())
                .size(-1)
                .build(), 2, InputPeerSelf.instance());
        var expChatPhoto = ofChatPhoto(ImmutableBaseUserProfilePhoto.of(0, 1337, 2),
                true, -1, InputPeerSelf.instance());
        var expMinPhoto = ofChatPhoto(BasePhoto.builder()
                .id(1337)
                .accessHash(-1111)
                .fileReference(Unpooled.wrappedBuffer(CryptoUtil.random.generateSeed(8)))
                .date(0)
                .sizes(List.of(BasePhotoSize.builder()
                        .type("i")
                        .w(100)
                        .h(100)
                        .size(-1)
                        .build()))
                .dcId(2)
                .build(), 'i', 1337, ImmutableInputPeerUser.of(1234, -4321));
        var expDocument = ofDocument(BaseDocument.builder()
                .id(1337)
                .accessHash(-1111)
                .fileReference(Unpooled.wrappedBuffer(CryptoUtil.random.generateSeed(8)))
                .date(0)
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
                .build(), '\0', 1, InputPeerSelf.instance());
        var expPhoto = ofPhoto(BasePhoto.builder()
                .id(1337)
                .accessHash(-1111)
                .fileReference(Unpooled.wrappedBuffer(CryptoUtil.random.generateSeed(8)))
                .date(0)
                .sizes(List.of(BasePhotoSize.builder()
                        .type("i")
                        .w(100)
                        .h(100)
                        .size(-1)
                        .build()))
                .dcId(2)
                .build(), '\0', 1, InputPeerSelf.instance());
        var expStickerSet = ofStickerSet(ImmutableInputStickerSetID.of(1337, -1111), 2);

        var actWebDocumentNoProxy = serialize(expWebDocumentNoProxy);
        var actWebDocument = serialize(expWebDocument);
        var actChatPhoto = serialize(expChatPhoto);
        var actMinPhoto = serialize(expMinPhoto);
        var actDocument = serialize(expDocument);
        var actPhoto = serialize(expPhoto);
        var actStickerSet = serialize(expStickerSet);

        assertEquals(expWebDocumentNoProxy, actWebDocumentNoProxy);
        assertEquals(expWebDocument, actWebDocument);
        assertEquals(expChatPhoto, actChatPhoto);
        assertEquals(expMinPhoto, actMinPhoto);
        assertEquals(expDocument, actDocument);
        assertEquals(expPhoto, actPhoto);
        assertEquals(expStickerSet, actStickerSet);
    }

    static FileReferenceId serialize(FileReferenceId ref) {
        return deserialize(ref.serialize());
    }
}
