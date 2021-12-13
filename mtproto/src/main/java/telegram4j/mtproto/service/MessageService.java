package telegram4j.mtproto.service;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.function.TupleUtils;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import telegram4j.mtproto.MTProtoClient;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.mtproto.util.CryptoUtil;
import telegram4j.tl.*;
import telegram4j.tl.contacts.ResolvedPeer;
import telegram4j.tl.request.contacts.ImmutableResolveUsername;
import telegram4j.tl.request.messages.*;
import telegram4j.tl.request.upload.SaveFilePart;

import java.security.MessageDigest;

import static telegram4j.mtproto.service.ServiceUtil.mapNullable;

public class MessageService extends RpcService {

    private static final int PART_SIZE = 512 * 1024;
    private static final int TEN_MB = 10 * 1024 * 1024;
    private static final int LIMIT_MB = 2000 * 1024 * 1024;
    private static final int PARALLELISM = 3;

    public MessageService(MTProtoClient client, StoreLayout storeLayout) {
        super(client, storeLayout);
    }

    public Mono<ResolvedPeer> resolveUsername(String username) {
        return Mono.defer(() -> {
            String corrected = username.toLowerCase()
                    .trim().replace(".", "");

            if (corrected.startsWith("@")) {
                corrected = corrected.substring(1);
            }

            return client.sendAwait(ImmutableResolveUsername.of(corrected));
        });
    }

    public Mono<InputFile> saveFile(ByteBuf data, String name) {
        long fileId = CryptoUtil.random.nextLong();
        int parts = (int) Math.ceil((float) data.readableBytes() / PART_SIZE);
        boolean big = data.readableBytes() > TEN_MB;

        if (data.readableBytes() > LIMIT_MB) {
            return Mono.error(new IllegalArgumentException("File size is under limit. Size: "
                    + data.readableBytes() + ", limit: " + LIMIT_MB));
        }

        if (big) {

            return Mono.empty();
        }

        MessageDigest md5 = CryptoUtil.MD5.get();

        return Flux.range(0, parts)
                .flatMap(filePart -> {
                    ByteBuf part = data.readBytes(Math.min(PART_SIZE, data.readableBytes()));
                    byte[] partBytes = CryptoUtil.toByteArray(part);

                    synchronized (md5) {
                        md5.update(partBytes);
                    }

                    SaveFilePart req = SaveFilePart.builder()
                            .fileId(fileId)
                            .filePart(filePart)
                            .bytes(partBytes)
                            .build();

                    return client.sendAwait(req);
                })
                .then(Mono.fromSupplier(() -> ImmutableBaseInputFile.of(fileId,
                        parts, name, ByteBufUtil.hexDump(md5.digest()))));
    }

    public Mono<BaseMessage> sendMessage(SendMessage request) {
        return client.sendAwait(request)
                .zipWith(toPeer(request.peer()))
                .map(TupleUtils.function((updates, peer) -> {
                    Tuple2<BaseMessage, Updates> upd = transformMessageUpdate(request, updates, peer);

                    client.updates().emitNext(upd.getT2(), Sinks.EmitFailureHandler.FAIL_FAST);

                    return upd.getT1();
                }));
    }

    public Mono<BaseMessage> sendMedia(SendMedia request) {
        return client.sendAwait(request)
                .zipWith(toPeer(request.peer()))
                .map(TupleUtils.function((updates, peer) -> {
                    Tuple2<BaseMessage, Updates> upd = transformMessageUpdate(request, updates, peer);

                    client.updates().emitNext(upd.getT2(), Sinks.EmitFailureHandler.FAIL_FAST);

                    return upd.getT1();
                }));
    }

    // Short-send related updates object should be transformed to the updateShort.
    // https://core.telegram.org/api/updates#updates-sequence
    protected static Tuple2<BaseMessage, Updates> transformMessageUpdate(BaseSendMessageRequest request, Updates updates, Peer peer) {
        BaseMessage message;
        Updates update;
        if (updates instanceof UpdateShortSentMessage) {
            UpdateShortSentMessage updates0 = (UpdateShortSentMessage) updates;

            message = BaseMessage.builder()
                    .flags(request.flags() & updates0.flags())
                    .peerId(peer)
                    .replyTo(mapNullable(request.replyToMsgId(), ImmutableMessageReplyHeader::of))
                    .message(request.message())
                    .id(updates0.id())
                    .replyMarkup(request.replyMarkup())
                    .media(updates0.media())
                    .entities(updates0.entities())
                    .date(updates0.date())
                    .ttlPeriod(updates0.ttlPeriod())
                    .build();

            update = ImmutableUpdateShort.builder()
                    .date(updates0.date())
                    .update(UpdateNewMessage.builder()
                            .message(message)
                            .pts(updates0.pts())
                            .ptsCount(updates0.ptsCount())
                            .build())
                    .build();
        } else if (updates instanceof UpdateShortMessage) {
            UpdateShortMessage updates0 = (UpdateShortMessage) updates;

            message = BaseMessage.builder()
                    .flags(request.flags() & updates0.flags())
                    .peerId(peer)
                    .replyTo(updates0.replyTo())
                    .message(request.message())
                    .id(updates0.id())
                    .replyMarkup(request.replyMarkup())
                    .fwdFrom(updates0.fwdFrom())
                    .entities(updates0.entities())
                    .date(updates0.date())
                    .viaBotId(updates0.viaBotId())
                    .ttlPeriod(updates0.ttlPeriod())
                    .build();

            update = ImmutableUpdateShort.builder()
                    .date(updates0.date())
                    .update(UpdateNewMessage.builder()
                            .message(message)
                            .pts(updates0.pts())
                            .ptsCount(updates0.ptsCount())
                            .build())
                    .build();
        } else if (updates instanceof UpdateShortChatMessage) {
            UpdateShortChatMessage updates0 = (UpdateShortChatMessage) updates;

            message = BaseMessage.builder()
                    .flags(request.flags() & updates0.flags())
                    .peerId(peer)
                    .viaBotId(updates0.viaBotId())
                    .replyTo(updates0.replyTo())
                    .fwdFrom(updates0.fwdFrom())
                    .message(request.message())
                    .id(updates0.id())
                    .replyMarkup(request.replyMarkup())
                    .entities(updates0.entities())
                    .date(updates0.date())
                    .ttlPeriod(updates0.ttlPeriod())
                    .build();

            update = ImmutableUpdateShort.builder()
                    .date(updates0.date())
                    .update(UpdateNewMessage.builder()
                            .message(message)
                            .pts(updates0.pts())
                            .ptsCount(updates0.ptsCount())
                            .build())
                    .build();
        } else if (updates instanceof BaseUpdates) {
            BaseUpdates updates0 = (BaseUpdates) updates;

            UpdateMessageID updateMessageID = updates0.updates().stream()
                    .filter(updates1 -> updates1 instanceof UpdateMessageID)
                    .map(updates1 -> (UpdateMessageID) updates1)
                    .findFirst()
                    .orElseThrow(IllegalStateException::new);

            if (updateMessageID.randomId() != request.randomId()) {
                throw new IllegalArgumentException("Incorrect random id. Excepted: " + request.randomId()
                        + ", received: " + updateMessageID.randomId());
            }

            message = updates0.updates().stream()
                    .filter(updates1 -> updates1 instanceof UpdateNewMessageFields)
                    .map(updates1 -> (UpdateNewMessage) updates1)
                    .filter(updates1 -> updates1.message() instanceof BaseMessage) // TODO: what is MessageService type??
                    .map(updates1 -> (BaseMessage) updates1.message())
                    .findFirst()
                    .orElseThrow(IllegalStateException::new);

            update = updates0;
        } else {
            throw new IllegalStateException("Unknown updates type: " + updates);
        }

        return Tuples.of(message, update);
    }
}
