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

    public Mono<Message> sendMessage(SendMessage request) {
        return client.sendAwait(request)
                .zipWith(toPeer(request.peer()))
                .map(TupleUtils.function((updates, peer) -> {
                    var upd = transformMessageUpdate(request, updates, peer);

                    client.updates().emitNext(upd.getT2(), Sinks.EmitFailureHandler.FAIL_FAST);

                    return upd.getT1();
                }));
    }

    public Mono<Message> sendMedia(SendMedia request) {
        return client.sendAwait(request)
                .zipWith(toPeer(request.peer()))
                .map(TupleUtils.function((updates, peer) -> {
                    var upd = transformMessageUpdate(request, updates, peer);

                    client.updates().emitNext(upd.getT2(), Sinks.EmitFailureHandler.FAIL_FAST);

                    return upd.getT1();
                }));
    }

    public Mono<Message> editMessage(EditMessage request) {
        return client.sendAwait(request)
                .map(updates -> {
                    switch (updates.identifier()) {
                        case BaseUpdates.ID:
                            BaseUpdates casted = (BaseUpdates) updates;

                            UpdateEditMessageFields update = casted.updates().stream()
                                    .filter(upd -> upd instanceof UpdateEditMessageFields)
                                    .map(upd -> (UpdateEditMessageFields) upd)
                                    .findFirst()
                                    .orElseThrow();

                            client.updates().emitNext(updates, Sinks.EmitFailureHandler.FAIL_FAST);

                            return update.message();
                        default:
                            throw new IllegalArgumentException("Unknown updates type: " + updates);
                    }
                });
    }

    // Short-send related updates object should be transformed to the updateShort.
    // https://core.telegram.org/api/updates#updates-sequence
    static Tuple2<Message, Updates> transformMessageUpdate(BaseSendMessageRequest request, Updates updates, Peer peer) {
        switch (updates.identifier()) {
            case UpdateShortSentMessage.ID: {
                UpdateShortSentMessage casted = (UpdateShortSentMessage) updates;
                Message message = BaseMessage.builder()
                        .flags(request.flags() | casted.flags())
                        .peerId(peer)
                        .replyTo(mapNullable(request.replyToMsgId(), ImmutableMessageReplyHeader::of))
                        .message(request.message())
                        .id(casted.id())
                        .replyMarkup(request.replyMarkup())
                        .media(casted.media())
                        .entities(casted.entities())
                        .date(casted.date())
                        .ttlPeriod(casted.ttlPeriod())
                        .build();

                Updates upds = UpdateShort.builder()
                        .date(casted.date())
                        .update(UpdateNewMessage.builder()
                                .message(message)
                                .pts(casted.pts())
                                .ptsCount(casted.ptsCount())
                                .build())
                        .build();

                return Tuples.of(message, upds);
            }
            case UpdateShortMessage.ID: {
                UpdateShortMessage casted = (UpdateShortMessage) updates;

                Message message = BaseMessage.builder()
                        .flags(request.flags() | casted.flags())
                        .peerId(peer)
                        .replyTo(casted.replyTo())
                        .message(request.message())
                        .id(casted.id())
                        .replyMarkup(request.replyMarkup())
                        .fwdFrom(casted.fwdFrom())
                        .entities(casted.entities())
                        .date(casted.date())
                        .viaBotId(casted.viaBotId())
                        .ttlPeriod(casted.ttlPeriod())
                        .build();

                Updates upds = UpdateShort.builder()
                        .date(casted.date())
                        .update(UpdateNewMessage.builder()
                                .message(message)
                                .pts(casted.pts())
                                .ptsCount(casted.ptsCount())
                                .build())
                        .build();

                return Tuples.of(message, upds);
            }
            case UpdateShortChatMessage.ID: {
                UpdateShortChatMessage casted = (UpdateShortChatMessage) updates;

                Message message = BaseMessage.builder()
                        .flags(request.flags() | casted.flags())
                        .peerId(peer)
                        .viaBotId(casted.viaBotId())
                        .replyTo(casted.replyTo())
                        .fwdFrom(casted.fwdFrom())
                        .message(request.message())
                        .id(casted.id())
                        .replyMarkup(request.replyMarkup())
                        .entities(casted.entities())
                        .date(casted.date())
                        .ttlPeriod(casted.ttlPeriod())
                        .build();

                Updates upds = ImmutableUpdateShort.builder()
                        .date(casted.date())
                        .update(UpdateNewMessage.builder()
                                .message(message)
                                .pts(casted.pts())
                                .ptsCount(casted.ptsCount())
                                .build())
                        .build();

                return Tuples.of(message, upds);
            }
            case BaseUpdates.ID: {
                BaseUpdates casted = (BaseUpdates) updates;

                UpdateMessageID updateMessageID = casted.updates().stream()
                        .filter(upd -> upd instanceof UpdateMessageID)
                        .map(upd -> (UpdateMessageID) upd)
                        .findFirst()
                        .orElseThrow(IllegalStateException::new);

                if (updateMessageID.randomId() != request.randomId()) {
                    throw new IllegalArgumentException("Incorrect random id. Excepted: " + request.randomId()
                            + ", received: " + updateMessageID.randomId());
                }

                Message message = casted.updates().stream()
                        .filter(upd -> upd instanceof UpdateNewMessageFields)
                        .map(upd -> ((UpdateNewMessageFields) upd).message())
                        .findFirst()
                        .orElseThrow(IllegalStateException::new);

                return Tuples.of(message, casted);
            }
            default: throw new IllegalArgumentException("Unknown updates type: " + updates);
        }
    }
}
