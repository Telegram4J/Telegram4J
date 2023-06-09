package telegram4j.mtproto.client.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import telegram4j.mtproto.MTProtoException;
import telegram4j.mtproto.RpcException;
import telegram4j.mtproto.TransportException;
import telegram4j.mtproto.util.AES256IGECipher;
import telegram4j.tl.TlDeserializer;
import telegram4j.tl.TlSerialUtil;
import telegram4j.tl.TlSerializer;
import telegram4j.tl.Updates;
import telegram4j.tl.api.TlMethod;
import telegram4j.tl.api.TlObject;
import telegram4j.tl.auth.Authorization;
import telegram4j.tl.auth.LoginTokenSuccess;
import telegram4j.tl.mtproto.*;
import telegram4j.tl.request.InvokeWithLayer;
import telegram4j.tl.request.users.GetUsers;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static telegram4j.mtproto.client.impl.MTProtoClientImpl.*;
import static telegram4j.mtproto.transport.Transport.QUICK_ACK_MASK;
import static telegram4j.mtproto.util.CryptoUtil.*;
import static telegram4j.mtproto.util.TlEntityUtil.schemaTypeName;

class MTProtoEncryption extends ChannelDuplexHandler {

    // limit for service container like a MsgsAck, MsgsStateReq
    static final int MAX_IDS_SIZE = 8192;
    static final int MAX_CONTAINER_SIZE = 1020; // count of messages
    static final int MAX_CONTAINER_LENGTH = 1 << 15; // length in bytes

    final MTProtoClientImpl client;
    final TransportCodec transportCodec;
    final ArrayList<Long> acknowledgments = new ArrayList<>(32);

    MTProtoEncryption(MTProtoClientImpl client, TransportCodec transportCodec) {
        this.client = client;
        this.transportCodec = transportCodec;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws IOException {
        if (!(msg instanceof ByteBuf payload)) {
            throw new IllegalArgumentException("Unexpected type of message to decrypt: " + msg);
        }

        if (payload.readableBytes() == 4) {
            int val = payload.readIntLE();
            payload.release();

            if (!TransportException.isError(val) && transportCodec.delegate().supportsQuickAck()) {
                if (rpcLog.isDebugEnabled()) {
                    rpcLog.debug("[C:0x{}, Q:0x{}] Received quick ack",
                            client.id, Integer.toHexString(val));
                }
                return;
            }

            throw new TransportException(val);
        }

        decryptPayload(ctx, payload);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws IOException {
        if (!(msg instanceof MTProtoClientImpl.RpcRequest req)) {
            throw new IllegalArgumentException("Unexpected type of message to encrypt: " + msg);
        }

        if (!isPingPacket(req.method)) {
            client.stats.incrementQueriesCount();
            client.stats.lastQueryTimestamp = Instant.now();
        }

        if (log.isTraceEnabled() && !client.requests.isEmpty()) {
            log.trace("[C:0x{}] {}", client.id, client.requests.entrySet().stream()
                    .map(e -> "0x" + Long.toHexString(e.getKey()) + ": " + e.getValue())
                    .collect(Collectors.joining(", ", "{", "}")));
        }

        if (client.authData.unauthorized() && msg instanceof RpcQuery query
                && query.method.identifier() != GetUsers.ID // TODO: It's dirty hack to check is auth needed
                && !isAuthMethod(query.method)) {

            client.delayedUntilAuth.addLast(query);
            return;
        }

        var currentAuthKey = client.authData.authKey();
        if (currentAuthKey == null) {
            throw new MTProtoException("No auth key");
        }

        TlObject method = req.method;
        int size = TlSerializer.sizeOf(req.method);
        if (size >= client.options.gzipWrappingSizeThreshold()) {
            ByteBuf serialized = ctx.alloc().ioBuffer(size);
            TlSerializer.serialize(serialized, method);
            ByteBuf gzipped = TlSerialUtil.compressGzip(ctx.alloc(), 9, serialized);

            method = ImmutableGzipPacked.of(gzipped);
            gzipped.release();

            size = TlSerializer.sizeOf(method);
        }

        long containerMsgId = -1;
        // server returns -404 transport error when this packet placed in container
        boolean canContainerize = req.method.identifier() != InvokeWithLayer.ID && size < MAX_CONTAINER_LENGTH;

        Request containerOrRequest = req;
        long requestMessageId = client.authData.nextMessageId();
        int requestSeqNo = client.authData.nextSeqNo(req.method);

        ByteBuf message;
        int padding;
        var statesIds = new ArrayList<Long>();
        var messages = new ArrayList<ContainerMessage>();
        if (canContainerize) {
            for (var e : client.requests.entrySet()) {
                long key = e.getKey();
                var inf = e.getValue();
                if (inf instanceof ContainerRequest) {
                    continue;
                }

                statesIds.add(key);
                if (statesIds.size() == MAX_IDS_SIZE) {
                    break;
                }
            }
        }

        if (canContainerize && !statesIds.isEmpty())
            messages.add(new ContainerMessage(client.authData.nextMessageId(),
                    client.authData.nextSeqNo(false), ImmutableMsgsStateReq.of(statesIds)));
        if (canContainerize && !acknowledgments.isEmpty())
            messages.add(new ContainerMessage(client.authData.nextMessageId(),
                    client.authData.nextSeqNo(false), collectAcks()));

        boolean containerize = canContainerize && !messages.isEmpty();
        if (containerize) {
            messages.add(new ContainerMessage(requestMessageId, requestSeqNo, size, method));

            containerMsgId = client.authData.nextMessageId();
            int containerSeqNo = client.authData.nextSeqNo(false);
            int payloadSize = messages.stream().mapToInt(c -> c.size() + 16).sum();
            int messageSize = 40 + payloadSize;
            int unpadded = (messageSize + 12) % 16;
            padding = 12 + (unpadded != 0 ? 16 - unpadded : 0);

            message = ctx.alloc().buffer(messageSize + padding);
            message.writeLongLE(client.authData.serverSalt());
            message.writeLongLE(client.authData.sessionId());
            message.writeLongLE(containerMsgId);
            message.writeIntLE(containerSeqNo);
            message.writeIntLE(payloadSize + 8);
            message.writeIntLE(MessageContainer.ID);
            message.writeIntLE(messages.size());

            var rpcInCont = req instanceof RpcQuery r
                    ? new QueryContainerRequest(r, containerMsgId)
                    : new RpcContainerRequest(req, containerMsgId);
            client.requests.put(requestMessageId, rpcInCont);
            var msgIds = new long[messages.size()];
            for (int i = 0; i < messages.size(); i++) {
                var c = messages.get(i);
                msgIds[i] = c.messageId();
                if (c.messageId() != requestMessageId) {
                    client.requests.put(c.messageId(), new RpcContainerRequest((TlMethod<?>) c.method(), containerMsgId));
                }

                message.writeLongLE(c.messageId());
                message.writeIntLE(c.seqNo());
                message.writeIntLE(c.size());
                TlSerializer.serialize(message, c.method());
            }

            containerOrRequest = new ContainerRequest(msgIds);
            client.requests.put(containerMsgId, containerOrRequest);
        } else {
            client.requests.put(requestMessageId, req);
            int messageSize = 32 + size;
            int unpadded = (messageSize + 12) % 16;
            padding = 12 + (unpadded != 0 ? 16 - unpadded : 0);

            message = ctx.alloc().buffer(messageSize + padding)
                    .writeLongLE(client.authData.serverSalt())
                    .writeLongLE(client.authData.sessionId())
                    .writeLongLE(requestMessageId)
                    .writeIntLE(requestSeqNo)
                    .writeIntLE(size);
            TlSerializer.serialize(message, method);
        }

        byte[] paddingb = new byte[padding];
        random.nextBytes(paddingb);
        message.writeBytes(paddingb);

        ByteBuf authKey = currentAuthKey.value();
        ByteBuf authKeyId = Unpooled.copyLong(Long.reverseBytes(currentAuthKey.id()));

        ByteBuf messageKeyHash = sha256Digest(authKey.slice(88, 32), message);

        boolean quickAck = false;
        int quickAckToken = -1;
        if (!containerize && AuthData.isContentRelated(req.method) && transportCodec.delegate().supportsQuickAck()) {
            quickAckToken = messageKeyHash.getIntLE(0) | QUICK_ACK_MASK;
            quickAck = true;
        }

        ByteBuf messageKey = messageKeyHash.slice(8, 16);
        AES256IGECipher cipher = createAesCipher(messageKey, authKey, false);

        ByteBuf encrypted = cipher.encrypt(message);
        ByteBuf packet = Unpooled.wrappedBuffer(authKeyId, messageKey, encrypted);

        if (rpcLog.isDebugEnabled()) {
            if (containerOrRequest instanceof ContainerRequest) {
                rpcLog.debug("[C:0x{}, M:0x{}] Sending container: {{}}", client.id,
                        Long.toHexString(containerMsgId), messages.stream()
                                .map(m -> "0x" + Long.toHexString(m.messageId()) + ": " + schemaTypeName(m.method()))
                                .collect(Collectors.joining(", ")));
            } else {
                if (quickAck) {
                    rpcLog.debug("[C:0x{}, M:0x{}, Q:0x{}] Sending request: {}", client.id,
                            Long.toHexString(requestMessageId), Integer.toHexString(quickAckToken),
                            schemaTypeName(req.method));
                } else {
                    rpcLog.debug("[C:0x{}, M:0x{}] Sending request: {}", client.id,
                            Long.toHexString(requestMessageId), schemaTypeName(req.method));
                }
            }
        }

        transportCodec.setQuickAck(quickAck);
        ctx.writeAndFlush(packet, ctx.voidPromise());
    }

    void decryptPayload(ChannelHandlerContext ctx, ByteBuf data) throws IOException {
        long authKeyId = data.readLongLE();

        var currentAuthKey = client.authData.authKey();
        if (currentAuthKey == null) {
            throw new MTProtoException("No auth key");
        }

        if (authKeyId != currentAuthKey.id()) {
            throw new MTProtoException("Incorrect auth key id");
        }

        ByteBuf messageKey = data.readRetainedSlice(16);

        ByteBuf authKey = currentAuthKey.value();
        AES256IGECipher cipher = createAesCipher(messageKey, authKey, true);

        ByteBuf decrypted = cipher.decrypt(data.slice());

        ByteBuf messageKeyHash = sha256Digest(authKey.slice(96, 32), decrypted);
        ByteBuf messageKeyHashSlice = messageKeyHash.slice(8, 16);

        if (!messageKey.equals(messageKeyHashSlice)) {
            messageKey.release();
            throw new MTProtoException("Incorrect message key");
        }
        messageKey.release();

        decrypted.readLongLE();  // server_salt
        long sessionId = decrypted.readLongLE();
        if (client.authData.sessionId() != sessionId) {
            throw new MTProtoException("Incorrect session identifier");
        }
        long messageId = decrypted.readLongLE();
        var res = client.authData.isValidInboundMessageId(messageId);
        if (res != null) {
            String reason = switch (res) {
                case DUPLICATE -> "Duplicate";
                case INVALID_TIME -> "Too old or too new";
                case EVEN -> "Even";
            };

            throw new MTProtoException(reason + " message id received: 0x" + Long.toHexString(messageId));
        }

        decrypted.readIntLE(); // seq_no
        int length = decrypted.readIntLE();
        if (length % 4 != 0) {
            throw new MTProtoException("Data isn't aligned by 4 bytes");
        }

        ByteBuf payload = decrypted.readSlice(length);
        if (decrypted.readableBytes() < 12 || decrypted.readableBytes() > 1024) {
            throw new MTProtoException("Invalid padding length");
        }

        TlObject obj;
        try {
            obj = TlDeserializer.deserialize(payload);
        } finally {
            decrypted.release();
        }

        handleServiceMessage(ctx, obj, messageId);
    }

    Object decompressIfApplicable(Object obj) throws IOException {
        return obj instanceof GzipPacked gzipPacked
                ? TlSerialUtil.decompressGzip(gzipPacked.packedData())
                : obj;
    }

    static RpcException createRpcException(RpcError error, RpcRequest request) {
        String format = String.format("%s returned code: %d, message: %s",
                schemaTypeName(request.method), error.errorCode(),
                error.errorMessage());

        return new RpcException(format, error, request.method);
    }

    boolean handleMsgsAck(Object obj, long messageId) {
        if (obj instanceof MsgsAck msgsAck) {
            if (rpcLog.isDebugEnabled()) {
                rpcLog.debug("[C:0x{}, M:0x{}] Received acknowledge for message(s): [{}]",
                        client.id, Long.toHexString(messageId), msgsAck.msgIds().stream()
                                .map(l -> String.format("0x%x", l))
                                .collect(Collectors.joining(", ")));
            }
            return true;
        }
        return false;
    }

    void decContainer(RpcRequest req) {
        if (req instanceof ContainerizedRequest aux) {
            var cnt = (ContainerRequest) client.requests.get(aux.containerMsgId());
            if (cnt != null && cnt.decrementCnt()) {
                client.requests.remove(aux.containerMsgId());
            }
        }
    }

    void handleServiceMessage(ChannelHandlerContext ctx, Object obj, long messageId) throws IOException {
        if (obj instanceof RpcResult rpcResult) {
            messageId = rpcResult.reqMsgId();
            obj = decompressIfApplicable(rpcResult.result());

            var query = (RpcQuery) client.requests.remove(messageId);
            if (query == null) {
                return;
            }

            client.stats.decrementQueriesCount();
            decContainer(query);
            acknowledgments.add(messageId);

            if (obj instanceof RpcError rpcError) {
                if (rpcError.errorCode() == 401) {
                    client.authData.unauthorized(true);
                }

                if (rpcLog.isDebugEnabled()) {
                    rpcLog.debug("[C:0x{}, M:0x{}] Receiving rpc error, code: {}, message: {}",
                            client.id, Long.toHexString(messageId), rpcError.errorCode(), rpcError.errorMessage());
                }

                RpcException e = createRpcException(rpcError, query);

                if (query.sink.isPublishOnEventLoop()) {
                    query.sink.emitError(e);
                } else {
                    query.sink.emitError(client.options.resultPublisher(), e);
                }
            } else {
                if (rpcLog.isDebugEnabled()) {
                    rpcLog.debug("[C:0x{}, M:0x{}] Receiving rpc result", client.id, Long.toHexString(messageId));
                }

                if (query.sink.isPublishOnEventLoop()) {
                    query.sink.emitValue(obj);
                } else {
                    query.sink.emitValue(client.options.resultPublisher(), obj);
                }

                if (client.authData.unauthorized() && (obj instanceof Authorization || obj instanceof LoginTokenSuccess)) {
                    client.authData.unauthorized(false);

                    if (!client.delayedUntilAuth.isEmpty()) {
                        RpcQuery pendingQuery;
                        while ((pendingQuery = client.delayedUntilAuth.pollFirst()) != null) {
                            ctx.channel().write(pendingQuery);
                        }
                        ctx.channel().flush();
                    }
                }
            }

            return;
        }

        if (obj instanceof MessageContainer messageContainer) {
            if (rpcLog.isDebugEnabled()) {
                rpcLog.debug("[C:0x{}] Handling message container: {}", client.id, messageContainer.messages().stream()
                        .map(msg -> "0x" + Long.toHexString(msg.msgId()) + ": " + schemaTypeName(msg.body()))
                        .collect(Collectors.joining(", ", "{", "}")));
            }

            for (Message message : messageContainer.messages()) {
                handleServiceMessage(ctx, message.body(), message.msgId());
            }
            return;
        }

        // Applicable for updates
        obj = decompressIfApplicable(obj);
        if (obj instanceof Updates updates) {
            if (rpcLog.isTraceEnabled()) {
                rpcLog.trace("[C:0x{}] Receiving updates: {}", client.id, updates);
            }

            client.group.updates().publish(updates);
            return;
        }

        if (obj instanceof Pong pong) {
            messageId = pong.msgId();

            if (rpcLog.isDebugEnabled()) {
                rpcLog.debug("[C:0x{}, M:0x{}] Receiving pong after {}", client.id, Long.toHexString(messageId),
                        Duration.ofNanos(System.nanoTime() - pong.pingId()));
            }

            var query = (RpcRequest) client.requests.remove(messageId);
            decContainer(query);
            client.inflightPing = false;

            if (query instanceof RpcQuery q) {
                if (q.sink.isPublishOnEventLoop()) {
                    q.sink.emitValue(obj);
                } else {
                    q.sink.emitValue(client.options.resultPublisher(), obj);
                }
            }

            return;
        }

        // TODO: necessary?
        // if (obj instanceof FutureSalts) {
        //     FutureSalts futureSalts = (FutureSalts) obj;
        //     messageId = futureSalts.reqMsgId();
        //     if (rpcLog.isDebugEnabled()) {
        //         rpcLog.debug("[C:0x{}, M:0x{}] Receiving future salts", id, Long.toHexString(messageId));
        //     }
        //
        //     resolve(messageId, obj);
        //     return;
        // }

        if (obj instanceof NewSession newSession) {
            if (rpcLog.isDebugEnabled()) {
                rpcLog.debug("[C:0x{}] Receiving new session creation, first message id: 0x{}",
                        client.id, Long.toHexString(newSession.firstMsgId()));
            }

            client.authData.serverSalt(newSession.serverSalt());
            client.authData.lastMessageId(newSession.firstMsgId());
            acknowledgments.add(messageId);

            return;
        }

        // from MessageContainer
        if (handleMsgsAck(obj, messageId)) {
            return;
        }

        if (obj instanceof BadMsgNotification badMsgNotification) {
            if (rpcLog.isDebugEnabled()) {
                if (badMsgNotification instanceof BadServerSalt badServerSalt) {
                    rpcLog.debug("[C:0x{}, M:0x{}] Updating server salt", client.id,
                            Long.toHexString(badServerSalt.badMsgId()));
                } else {
                    rpcLog.debug("[C:0x{}, M:0x{}] Receiving notification, code: {}", client.id,
                            Long.toHexString(badMsgNotification.badMsgId()), badMsgNotification.errorCode());
                }
            }

            if (badMsgNotification instanceof BadServerSalt badServerSalt) {
                client.authData.serverSalt(badServerSalt.newServerSalt());
            }

            client.authData.updateTimeOffset((int) (messageId >> 32));
            emitUnwrapped(ctx, badMsgNotification.badMsgId());
            return;
        }

        if (obj instanceof MsgsStateInfo inf) {
            var req = (RpcRequest) client.requests.remove(inf.reqMsgId());
            if (req != null) {
                MsgsStateReq original = (MsgsStateReq) req.method;
                ByteBuf c = inf.info();
                if (original.msgIds().size() != c.readableBytes()) {
                    rpcLog.error("[C:0x{}, M:0x{}] Received not all states. expected: {}, received: {}",
                            client.id, Long.toHexString(inf.reqMsgId()), original.msgIds().size(),
                            c.readableBytes());
                    return;
                }

                if (rpcLog.isDebugEnabled()) {
                    StringJoiner st = new StringJoiner(", ");
                    var msgIds = original.msgIds();
                    for (int i = 0; i < msgIds.size(); i++) {
                        long msgId = msgIds.get(i);
                        st.add("0x" + Long.toHexString(msgId) + "/" + (c.getByte(i) & 7));
                    }

                    rpcLog.debug("[C:0x{}, M:0x{}] Received states: [{}]", client.id, Long.toHexString(inf.reqMsgId()), st);
                }

                decContainer(req);
                var msgIds = original.msgIds();
                for (int i = 0; i < msgIds.size(); i++) {
                    long msgId = msgIds.get(i);

                    int state = c.getByte(i) & 7;
                    switch (state) {
                        // not received, resend
                        case 1, 2, 3 -> emitUnwrapped(ctx, msgId);
                        case 4 -> { // acknowledged
                            var sub = (RpcRequest) client.requests.get(msgId);
                            if (sub == null) {
                                continue;
                            }
                            if (!isResultAwait(sub.method)) {
                                client.requests.remove(msgId);
                                decContainer(sub);
                            }
                        }
                        default -> rpcLog.debug("[C:0x{}] Unknown message state {}", client.id, state);
                    }
                }
            }
            return;
        }

        if (obj instanceof MsgDetailedInfo info) {
            if (info instanceof BaseMsgDetailedInfo base) {
                if (rpcLog.isDebugEnabled()) {
                    rpcLog.debug("[C:0x{}] Handling message info. msgId: 0x{}, answerId: 0x{}", client.id,
                            Long.toHexString(base.msgId()), Long.toHexString(base.answerMsgId()));
                }
                // acknowledge for base.msgId()
            } else {
                if (rpcLog.isDebugEnabled()) {
                    rpcLog.debug("[C:0x{}] Handling message info. answerId: 0x{}", client.id, Long.toHexString(info.answerMsgId()));
                }
            }

            // TODO
            return;
        }

        if (obj instanceof DestroySessionRes res) {
            // Why DestroySession have concrete type of response, but also have a wrong message_id
            // which can't be used as key of the requests map?

            if (rpcLog.isDebugEnabled()) {
                rpcLog.debug("[C:0x{}] Session 0x{} destroyed {}",
                        client.id, Long.toHexString(res.sessionId()),
                        res.identifier() == DestroySessionOk.ID ? "successfully" : "with nothing");
            }

            return;
        }

        log.warn("[C:0x{}] Unhandled payload: {}", client.id, obj);
    }

    void emitUnwrappedContainer(ChannelHandlerContext ctx, ContainerRequest container) {
        for (long msgId : container.msgIds) {
            var inner = (RpcRequest) client.requests.remove(msgId);
            // If inner is null this mean response for mean was received
            if (inner != null) {
                // This method was called from MessageStateInfo handling;
                // Failed to send acks, just give back to queue
                if (inner.method instanceof MsgsAck acks) {
                    acknowledgments.addAll(acks.msgIds());
                    // There is no need to resend this requests,
                    // because it computed on relevant 'requests' map
                } else if (inner.method.identifier() == MsgsStateReq.ID) {
                    continue;
                } else {
                    RpcRequest single = inner instanceof QueryContainerRequest query
                            ? new RpcQuery(query.method, query.sink)
                            : new RpcRequest(inner.method);

                    ctx.channel().writeAndFlush(single, ctx.voidPromise());
                }
            }
        }
    }

    void emitUnwrapped(ChannelHandlerContext ctx, long possibleCntMsgId) {
        Request request = client.requests.get(possibleCntMsgId);
        if (request instanceof ContainerRequest container) {
            client.requests.remove(possibleCntMsgId);

            emitUnwrappedContainer(ctx, container);
        } else if (request instanceof ContainerizedRequest cntMessage) {
            var cnt = (ContainerRequest) client.requests.remove(cntMessage.containerMsgId());
            if (cnt != null) {
                emitUnwrappedContainer(ctx, cnt);
            }
        } else if (request instanceof RpcRequest rpcRequest) {
            client.requests.remove(possibleCntMsgId);

            ctx.channel().writeAndFlush(rpcRequest, ctx.voidPromise());
        }
    }

    MsgsAck collectAcks() {
        int count = Math.min(acknowledgments.size(), MAX_IDS_SIZE);
        var batch = acknowledgments.subList(0, count);
        var ack = ImmutableMsgsAck.of(batch);
        batch.clear();
        return ack;
    }
}
