package telegram4j.mtproto.file;

import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import telegram4j.mtproto.util.CryptoUtil;
import telegram4j.tl.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static telegram4j.mtproto.file.Context.*;
import static telegram4j.mtproto.file.FileReferenceId.deserialize;
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
                .build(), noOpContext());
        var expWebDocument = ofDocument(BaseWebDocument.builder()
                .url("https://www.google.com")
                .accessHash(1337 >> 4)
                // ignored fields
                .mimeType("")
                .attributes(List.of())
                .size(-1)
                .build(), createMediaContext(ImmutablePeerChannel.of(123), 2));
        var expChatPhoto = ofChatPhoto(
                ImmutableBaseUserProfilePhoto.of(0, 1337, 2),
                true, ImmutableInputPeerChat.of(123));
        var expMinPhoto = ofChatPhoto(BasePhoto.builder()
                .id(1337)
                .accessHash(-1111)
                .fileReference(Unpooled.wrappedBuffer(CryptoUtil.random.generateSeed(156)))
                .date(0)
                .addSize(ImmutableBasePhotoSize.of("i", 100, 100, -1))
                .dcId(2)
                .build(), createUserPhotoContext(InputPeerSelf.instance()));
        var doc = BaseDocument.builder()
                .id(1337)
                .accessHash(-1111)
                .fileReference(Unpooled.wrappedBuffer(CryptoUtil.random.generateSeed(8)))
                .date(0)
                .addThumb(ImmutableBasePhotoSize.of("i", 100, 100, -1))
                .dcId(2)
                // ignored fields
                .mimeType("")
                .size(-1)
                .attributes(List.of())
                .build();
        var expDocument = ofDocument(doc, createMediaContext(ImmutablePeerChannel.of(123), 2));
        var expPhoto = ofPhoto(BasePhoto.builder()
                .id(1337)
                .accessHash(-1111)
                .fileReference(Unpooled.wrappedBuffer(CryptoUtil.random.generateSeed(24)))
                .date(0)
                .addSize(ImmutableBasePhotoSize.of("i", 100, 100, -1))
                .dcId(2)
                .build(), createMediaContext(ImmutablePeerChannel.of(123), 4235436));
        var expStickerSet = ofStickerSet(ImmutableInputStickerSetID.of(1337, -1111), 2);
        var expBotInfoDocument = ofDocument(doc,
                createBotInfoContext(ImmutablePeerUser.of(123), 123));

        var actWebDocumentNoProxy = serialize(expWebDocumentNoProxy);
        var actWebDocument = serialize(expWebDocument);
        var actChatPhoto = serialize(expChatPhoto);
        var actMinPhoto = serialize(expMinPhoto);
        var actDocument = serialize(expDocument);
        var actPhoto = serialize(expPhoto);
        var actStickerSet = serialize(expStickerSet);
        var actBotInfoDocument = serialize(expBotInfoDocument);

        assertEquals(expWebDocumentNoProxy, actWebDocumentNoProxy);
        assertEquals(expWebDocument, actWebDocument);
        assertEquals(expChatPhoto, actChatPhoto);
        assertEquals(expMinPhoto, actMinPhoto);
        assertEquals(expDocument, actDocument);
        assertEquals(expPhoto, actPhoto);
        assertEquals(expStickerSet, actStickerSet);
        assertEquals(expBotInfoDocument, actBotInfoDocument);
    }

    static FileReferenceId serialize(FileReferenceId ref) {
        return deserialize(ref.serialize());
    }
}
