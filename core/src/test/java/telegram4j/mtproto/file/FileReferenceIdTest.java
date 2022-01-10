package telegram4j.mtproto.file;

import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.api.Test;
import telegram4j.tl.ImmutableBaseUserProfilePhoto;
import telegram4j.tl.InputPeerSelf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static telegram4j.mtproto.file.FileReferenceId.*;

class FileReferenceIdTest {

    @Test
    void chatPhoto() {
        var orig = ofChatPhoto(InputPeerSelf.instance(),
                ImmutableBaseUserProfilePhoto.of(1337, 2), PhotoSizeType.CHAT_PHOTO_BIG);
        String str = orig.serialize(ByteBufAllocator.DEFAULT);
        var deser = deserialize(str);
        assertEquals(orig, deser);
    }
}
