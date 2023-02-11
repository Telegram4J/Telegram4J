package telegram4j.mtproto.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import reactor.util.annotation.Nullable;
import telegram4j.tl.*;
import telegram4j.tl.api.EmptyObject;
import telegram4j.tl.api.TlObject;
import telegram4j.tl.storage.FileType;

import java.util.Locale;
import java.util.Objects;

/** Utility class with frequently used methods for mapping TL objects. */
public class TlEntityUtil {

    private TlEntityUtil() {
    }

    public static FileType suggestFileType(@Nullable String mimeType) {
        if (mimeType == null) {
            return FileType.UNKNOWN;
        }

        return switch (mimeType.toLowerCase(Locale.ROOT)) {
            case "image/gif" -> FileType.GIF;
            case "image/jpeg" -> FileType.JPEG;
            case "image/png" -> FileType.PNG;
            case "image/webp" -> FileType.WEBP;
            case "application/pdf" -> FileType.PDF;
            case "audio/mpeg" -> FileType.MP3;
            case "audio/mp4" -> FileType.MP4;
            case "video/quicktime" -> FileType.MOV;
            default -> FileType.UNKNOWN;
        };
    }

    public static String toMimeType(FileType type) {
        return switch (type) {
            case JPEG -> "image/jpeg";
            case GIF -> "image/gif";
            case PNG -> "image/png";
            case PDF -> "application/pdf";
            case MP3 -> "audio/mpeg";
            case MOV -> "video/quicktime";
            case MP4 -> "audio/mp4";
            case WEBP -> "image/webp";
            default -> throw new IllegalArgumentException("Unexpected file type: " + type);
        };
    }

    // https://github.com/telegramdesktop/tdesktop/tree/17de379145684999eed826d22469503097516689/Telegram/SourceFiles/ui/image/image.cpp#L44-#L91
    public static ByteBuf expandInlineThumb(ByteBuf bytes) {
        if (!bytes.isReadable(3) || bytes.getByte(0) != 0x01) {
            throw new IllegalArgumentException();
        }

        byte[] header = ByteBufUtil.decodeHexDump(
                "ffd8ffe000104a46494600010100000100010000ffdb004300281c1e231e19282321232d2b" +
                "28303c64413c37373c7b585d4964918099968f808c8aa0b4e6c3a0aadaad8a8cc8ffcbdaee" +
                "f5ffffff9bc1fffff8f8f8ffaffe6fd8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8" +
                "f8f8f8f8f8f8f8f8ffc000110800000000030122fffff8ffdb0043012b2d2d3c353c764141" +
                "76f8a58ca5f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f8f800021101031101ffc4001f00000105" +
                "01010101010100000000000000000102030405060708090a0bffc400b51000020103030204" +
                "03050504040000017d01020300041105122131410613516107227114328191a1082342b1c1" +
                "1552d1f02433627282090a161718191a25262728292a3435363738393a434445464748494a" +
                "535455565758595a636465666768696a737475767778797a838485868788898a9293949596" +
                "9798999aa2a3a4a5a6a7a8a9aab2b3b4b5b6b7b8b9bac2c3c4c5c6c7c8c9cad2d3d4d5d6d7" +
                "d8d9dae1e2e3e4e5e6e7e8e9eaf1f2f3f4f5f6f7f8f9faffc4001f01000301010101010101" +
                "01010000000000000102030405060708090a0bffc400b51100020102040403040705040400" +
                "010277000102031104052131061241510761711322328108144291a1b1c109233352f01562" +
                "72d10a162434e125f11718191a262728292a35363738393a434445464748494a5354555657" +
                "58595a636465666768696a737475767778797a82838485868788898a92939495969798999a" +
                "a2a3a4a5a6a7a8a9aab2b3b4b5b6b7b8b9bac2c3c4c5c6c7c8c9cad2d3d4d5d6d7d8d9dae2" +
                "e3e4e5e6e7e8e9eaf2f3f4f5f6f7f8f9faffda000c03010002110311003f00");

        byte[] footer = ByteBufUtil.decodeHexDump("ffd9");

        return Unpooled.buffer(header.length + bytes.readableBytes() - 3 + footer.length)
                .writeBytes(header)
                .setByte(164, bytes.getByte(1))
                .setByte(166, bytes.getByte(2))
                .writeBytes(bytes, 3, bytes.readableBytes() - 3)
                .writeBytes(footer);
    }

    public static String stripUsername(String username) {
        username = username.toLowerCase().trim();
        return username.startsWith("@") ? username.substring(1) : username;
    }

    public static long getRawPeerId(Peer peer) {
        return switch (peer.identifier()) {
            case PeerChannel.ID -> ((PeerChannel) peer).channelId();
            case PeerChat.ID -> ((PeerChat) peer).chatId();
            case PeerUser.ID -> ((PeerUser) peer).userId();
            default -> throw new IllegalArgumentException("Unknown peer type: " + peer);
        };
    }

    public static long getRawPeerId(InputChannel inputChannel) {
        return switch (inputChannel.identifier()) {
            case BaseInputChannel.ID -> ((BaseInputChannel) inputChannel).channelId();
            case InputChannelFromMessage.ID -> ((InputChannelFromMessage) inputChannel).channelId();
            default -> throw new IllegalArgumentException("Unknown input channel type: " + inputChannel);
        };
    }

    public static InputPeer toInputPeer(InputUser user) {
        return switch (user.identifier()) {
            case InputUserFromMessage.ID -> {
                var d = (InputUserFromMessage) user;
                yield ImmutableInputPeerUserFromMessage.of(d.peer(), d.msgId(), d.userId());
            }
            case InputUserSelf.ID -> InputPeerSelf.instance();
            case BaseInputUser.ID -> {
                var v = (BaseInputUser) user;
                yield ImmutableInputPeerUser.of(v.userId(), v.accessHash());
            }
            default -> throw new IllegalArgumentException("Unknown input user type: " + user);
        };
    }

    public static InputPeer toInputPeer(InputChannel channel) {
        return switch (channel.identifier()) {
            case InputChannelFromMessage.ID -> {
                var d = (InputChannelFromMessage) channel;
                yield ImmutableInputPeerChannelFromMessage.of(d.peer(), d.msgId(), d.channelId());
            }
            case BaseInputChannel.ID -> {
                var v = (BaseInputChannel) channel;
                yield ImmutableInputPeerChannel.of(v.channelId(), v.accessHash());
            }
            default -> throw new IllegalArgumentException("Unknown input channel type: " + channel);
        };
    }

    public static InputUser toInputUser(InputPeer peer) {
        return switch (peer.identifier()) {
            case InputPeerUserFromMessage.ID -> {
                var d = (InputPeerUserFromMessage) peer;
                yield ImmutableInputUserFromMessage.of(d.peer(), d.msgId(), d.userId());
            }
            case InputPeerSelf.ID -> InputUserSelf.instance();
            case InputPeerUser.ID -> {
                var v = (InputPeerUser) peer;
                yield ImmutableBaseInputUser.of(v.userId(), v.accessHash());
            }
            default -> throw new IllegalArgumentException("Unknown input peer user type: " + peer);
        };
    }

    public static InputChannel toInputChannel(InputPeer peer) {
        return switch (peer.identifier()) {
            case InputPeerChannelFromMessage.ID -> {
                var d = (InputPeerChannelFromMessage) peer;
                yield ImmutableInputChannelFromMessage.of(d.peer(), d.msgId(), d.channelId());
            }
            case InputPeerChannel.ID -> {
                var v = (InputPeerChannel) peer;
                yield ImmutableBaseInputChannel.of(v.channelId(), v.accessHash());
            }
            default -> throw new IllegalArgumentException("Unknown input peer channel type: " + peer);
        };
    }

    public static Peer getUserId(ChannelParticipant data) {
        return switch (data.identifier()) {
            case BaseChannelParticipant.ID -> ImmutablePeerUser.of(((BaseChannelParticipant) data).userId());
            case ChannelParticipantSelf.ID -> ImmutablePeerUser.of(((ChannelParticipantSelf) data).userId());
            case ChannelParticipantAdmin.ID -> ImmutablePeerUser.of(((ChannelParticipantAdmin) data).userId());
            case ChannelParticipantBanned.ID -> ((ChannelParticipantBanned) data).peer();
            case ChannelParticipantLeft.ID -> ((ChannelParticipantLeft) data).peer();
            case ChannelParticipantCreator.ID -> ImmutablePeerUser.of(((ChannelParticipantCreator) data).userId());
            default -> throw new IllegalArgumentException("Unknown channel participant type: " + data);
        };
    }

    @Nullable
    public static <T extends TlObject, R extends T> R unmapEmpty(@Nullable T obj, Class<R> impl) {
        if (obj == null || obj instanceof EmptyObject) {
            return null;
        }
        return impl.cast(obj);
    }

    @Nullable
    public static <T extends TlObject> T unmapEmpty(@Nullable T obj) {
        if (obj == null || obj instanceof EmptyObject) {
            return null;
        }
        return obj;
    }

    // These methods use access hashes even with min entities, just to get the profile's photo
    public static InputPeer photoInputPeer(Channel channel) {
        return ImmutableInputPeerChannel.of(channel.id(),
                Objects.requireNonNull(channel.accessHash())); // TODO: verify this
    }

    public static InputPeer photoInputPeer(BaseUser user) {
        if (user.self()) {
            return InputPeerSelf.instance();
        }
        return ImmutableInputPeerUser.of(user.id(),
                Objects.requireNonNull(user.accessHash())); // TODO: verify this
    }

    public static boolean isUserPeer(InputPeer peer) {
        return switch (peer.identifier()) {
            case InputPeerSelf.ID, InputPeerUser.ID, InputPeerUserFromMessage.ID -> true;
            default -> false;
        };
    }
}
