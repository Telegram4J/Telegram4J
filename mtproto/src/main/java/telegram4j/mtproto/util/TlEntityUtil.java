package telegram4j.mtproto.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import reactor.util.annotation.Nullable;
import telegram4j.tl.*;
import telegram4j.tl.api.EmptyObject;
import telegram4j.tl.api.TlObject;

public class TlEntityUtil {

    private TlEntityUtil() {
    }

    public static ByteBuf expandInlineThumb(byte[] bytes) {
        if (bytes.length < 3 || bytes[0] != 0x01) {
            return Unpooled.EMPTY_BUFFER;
        }

        byte[] header = ByteBufUtil.decodeHexDump(
                "ffd8ffe000104a46494600010100000100010000ffdb004300281c1e231e19282321232d2b28303c64413c37373" +
                "c7b585d4964918099968f808c8aa0b4e6c3a0aadaad8a8cc8ffcbdaeef5ffffff9bc1fffffffaffe6fd" +
                "fff8ffdb0043012b2d2d3c353c76414176f8a58ca5f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f" +
                "8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8ffc000110800000000030122" +
                "00021101031101ffc4001f0000010501010101010100000000000000000102030405060708090a0bffc" +
                "400b5100002010303020403050504040000017d01020300041105122131410613516107227114328191" +
                "a1082342b1c11552d1f02433627282090a161718191a25262728292a3435363738393a4344454647484" +
                "94a535455565758595a636465666768696a737475767778797a838485868788898a9293949596979899" +
                "9aa2a3a4a5a6a7a8a9aab2b3b4b5b6b7b8b9bac2c3c4c5c6c7c8c9cad2d3d4d5d6d7d8d9dae1e2e3e4e" +
                "5e6e7e8e9eaf1f2f3f4f5f6f7f8f9faffc4001f01000301010101010101010100000000000001020304" +
                "05060708090a0bffc400b51100020102040403040705040400010277000102031104052131061241510" +
                "761711322328108144291a1b1c109233352f0156272d10a162434e125f11718191a262728292a353637" +
                "38393a434445464748494a535455565758595a636465666768696a737475767778797a8283848586878" +
                "8898a92939495969798999aa2a3a4a5a6a7a8a9aab2b3b4b5b6b7b8b9bac2c3c4c5c6c7c8c9cad2d3d4" +
                "d5d6d7d8d9dae2e3e4e5e6e7e8e9eaf2f3f4f5f6f7f8f9faffda000c03010002110311003f00");

        byte[] footer = ByteBufUtil.decodeHexDump("ffd9");
        ByteBuf expanded = Unpooled.buffer(header.length + bytes.length - 3 + footer.length);
        expanded.writeBytes(header);

        expanded.setByte(164, bytes[1]);
        expanded.setByte(166, bytes[2]);

        expanded.writeBytes(bytes, 3, bytes.length - 3);
        expanded.writeBytes(footer);

        return expanded.asReadOnly();
    }

    public static String stripUsername(String username) {
        return username.toLowerCase().trim()
                .replace(".", "")
                .replace("@", "");
    }

    public static long getRawPeerId(Peer peer) {
        switch (peer.identifier()) {
            case PeerChannel.ID: return ((PeerChannel) peer).channelId();
            case PeerChat.ID: return ((PeerChat) peer).chatId();
            case PeerUser.ID: return ((PeerUser) peer).userId();
            default: throw new IllegalArgumentException("Unknown peer type: " + peer);
        }
    }

    public static InputPeer toInputPeer(InputUser user) {
        switch (user.identifier()) {
            case InputUserFromMessage.ID:
                InputUserFromMessage d = (InputUserFromMessage) user;
                return ImmutableInputPeerUserFromMessage.of(d.peer(), d.msgId(), d.userId());
            case InputUserSelf.ID: return InputPeerSelf.instance();
            case BaseInputUser.ID:
                BaseInputUser v = (BaseInputUser) user;
                return ImmutableInputPeerUser.of(v.userId(), v.accessHash());
            default: throw new IllegalArgumentException("Unknown input user type: " + user);
        }
    }

    public static InputPeer toInputPeer(InputChannel channel) {
        switch (channel.identifier()) {
            case InputChannelFromMessage.ID:
                InputChannelFromMessage d = (InputChannelFromMessage) channel;
                return ImmutableInputPeerChannelFromMessage.of(d.peer(), d.msgId(), d.channelId());
            case BaseInputChannel.ID:
                BaseInputChannel v = (BaseInputChannel) channel;
                return ImmutableInputPeerChannel.of(v.channelId(), v.accessHash());
            default: throw new IllegalArgumentException("Unknown input channel type: " + channel);
        }
    }

    public static InputUser toInputUser(InputPeer peer) {
        switch (peer.identifier()) {
            case InputPeerUserFromMessage.ID:
                InputPeerUserFromMessage d = (InputPeerUserFromMessage) peer;
                return ImmutableInputUserFromMessage.of(d.peer(), d.msgId(), d.userId());
            case InputPeerSelf.ID: return InputUserSelf.instance();
            case InputPeerUser.ID:
                InputPeerUser v = (InputPeerUser) peer;
                return ImmutableBaseInputUser.of(v.userId(), v.accessHash());
            default: throw new IllegalArgumentException("Unknown input peer user type: " + peer);
        }
    }

    public static InputChannel toInputChannel(InputPeer peer) {
        switch (peer.identifier()) {
            case InputPeerChannelFromMessage.ID:
                InputPeerChannelFromMessage d = (InputPeerChannelFromMessage) peer;
                return ImmutableInputChannelFromMessage.of(d.peer(), d.msgId(), d.channelId());
            case InputPeerChannel.ID:
                InputPeerChannel v = (InputPeerChannel) peer;
                return ImmutableBaseInputChannel.of(v.channelId(), v.accessHash());
            default: throw new IllegalArgumentException("Unknown input peer channel type: " + peer);
        }
    }

    @Nullable
    public static <T extends TlObject, R extends T> R unmapEmpty(@Nullable T obj, Class<R> impl) {
        if (obj == null || obj instanceof EmptyObject) {
            return null;
        }
        return impl.cast(obj);
    }
}
