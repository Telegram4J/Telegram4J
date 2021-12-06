package telegram4j.mtproto;

import io.netty.buffer.ByteBufAllocator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.Logger;
import reactor.util.Loggers;
import telegram4j.tl.TlObject;
import telegram4j.tl.TlSerialUtil;
import telegram4j.tl.Updates;
import telegram4j.tl.mtproto.*;

import java.util.Queue;

public class RpcHandler {

    private static final Logger log = Loggers.getLogger(RpcHandler.class);

    private final MTProtoSession session;

    public RpcHandler(MTProtoSession session) {
        this.session = session;
    }

    public Mono<Void> handle(TlObject payload) {
        return handleServiceMessage(payload, session.getLastMessageId());
    }

    private Mono<Void> handleServiceMessage(Object obj, long messageId) {
        if (isAwaitAcknowledge(messageId)) {
            return sendAcknowledgments();
        }

        // For updates
        if (obj instanceof GzipPacked) {
            GzipPacked gzipPacked = (GzipPacked) obj;
            ByteBufAllocator alloc = session.getConnection().channel().alloc();
            obj = TlSerialUtil.decompressGzip(alloc.buffer()
                    .writeBytes(gzipPacked.packedData()));
        }

        if (obj instanceof RpcError) {
            RpcError rpcError = (RpcError) obj;
            return Mono.error(() -> RpcException.create(rpcError));
        }

        if (obj instanceof RpcResult) {
            RpcResult rpcResult = (RpcResult) obj;

            if (log.isDebugEnabled()) {
                log.debug("Handling rpc result for message: {}", rpcResult.reqMsgId());
            }

            Object unpacked = rpcResult.result();
            if (unpacked instanceof GzipPacked) {
                GzipPacked gzipPacked = (GzipPacked) unpacked;
                ByteBufAllocator alloc = session.getConnection().channel().alloc();
                unpacked = TlSerialUtil.decompressGzip(alloc.buffer()
                        .writeBytes(gzipPacked.packedData()));
            }

            return handleServiceMessage(unpacked, rpcResult.reqMsgId());
        }

        if (obj instanceof MessageContainer) {
            MessageContainer messageContainer = (MessageContainer) obj;
            if (log.isDebugEnabled()) {
                log.debug("Handling message container: {}", messageContainer);
            }

            return Flux.fromIterable(messageContainer.messages())
                    .flatMap(message -> handleServiceMessage(
                            message.body(), message.msgId()))
                    .then();
        }

        if (obj instanceof NewSession) {
            NewSession newSession = (NewSession) obj;

            log.debug("Handling new session salt creation.");

            session.setServerSalt(newSession.serverSalt());

            return acknowledgmentMessage(messageId);
        }

        if (obj instanceof BadMsgNotification) {
            BadMsgNotification badMsgNotification = (BadMsgNotification) obj;
            if (log.isDebugEnabled()) {
                log.debug("Handling bad msg notification: {}", badMsgNotification);
            }

            if (obj instanceof BadServerSalt) {
                BadServerSalt badServerSalt = (BadServerSalt) obj;
                session.setServerSalt(badServerSalt.newServerSalt());
            }

            switch (badMsgNotification.errorCode()) {
                case 16: // msg_id too low
                case 17: // msg_id too high
                    if (session.updateTimeOffset(session.getLastMessageId())) {
                        session.reset();
                    }
                    break;
            }

            session.resolve(badMsgNotification.badMsgId(), null);
            return Mono.empty();
        }

        if (obj instanceof MsgsAck) {
            MsgsAck msgsAck = (MsgsAck) obj;

            if (log.isDebugEnabled()) {
                log.debug("Handling acknowledge for message(s): {}", msgsAck.msgIds());
            }

            return Mono.empty();
        }

        if (obj instanceof Pong) {
            Pong pong = (Pong) obj;

            log.debug("Handling pong: {}", pong.pingId());

            messageId = pong.msgId();
        }

        if (obj instanceof FutureSalts) {
            FutureSalts futureSalts = (FutureSalts) obj;

            messageId = futureSalts.reqMsgId();
        }

        session.resolve(messageId, obj);

        if (obj instanceof Updates) {
            Updates updates = (Updates) obj;
            session.updates().emitNext(updates, Sinks.EmitFailureHandler.FAIL_FAST);
        }

        return acknowledgmentMessage(messageId);
    }

    private Mono<Void> acknowledgmentMessage(long messageId) {
        session.getAcknowledgments().add(messageId);

        return sendAcknowledgments();
    }

    private boolean isAwaitAcknowledge(long messageId) {
        int threshold = session.getMtProtoResources().getAcksSendThreshold();
        Queue<Long> acks = session.getAcknowledgments();
        return acks.contains(messageId) && acks.size() + 1 > threshold;
    }

    private Mono<Void> sendAcknowledgments() {
        return Mono.defer(() -> {
            Queue<Long> acks = session.getAcknowledgments();
            int threshold = session.getMtProtoResources().getAcksSendThreshold();
            if (acks.size() < threshold) {
                return Mono.empty();
            }

            if (log.isDebugEnabled()) {
                log.debug("Sending acknowledges for message(s): {}", acks);
            }

            return session.sendEncrypted(MsgsAck.builder().msgIds(acks).build())
                    .and(Mono.fromRunnable(acks::clear));
        });
    }
}
