/*
 * Copyright 2023 Telegram4J
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package telegram4j.mtproto.secret;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import telegram4j.mtproto.DcId;
import telegram4j.mtproto.auth.DhPrimeCheckerCache;
import telegram4j.mtproto.auth.TestDhParameters;
import telegram4j.mtproto.client.MTProtoClient;
import telegram4j.mtproto.client.MTProtoClientGroup;
import telegram4j.mtproto.client.UpdateDispatcher;
import telegram4j.mtproto.service.ChatService;
import telegram4j.mtproto.store.SecretChatOutbox;
import telegram4j.mtproto.store.SecretChatOutboxImpl;
import telegram4j.mtproto.store.StoreLayoutImpl;
import telegram4j.tl.*;
import telegram4j.tl.api.TlMethod;
import telegram4j.tl.messages.ImmutableBaseDhConfig;
import telegram4j.tl.messages.ImmutableBaseSentEncryptedMessage;
import telegram4j.tl.request.messages.AcceptEncryption;
import telegram4j.tl.request.messages.DiscardEncryption;
import telegram4j.tl.request.messages.GetDhConfig;
import telegram4j.tl.request.messages.RequestEncryption;
import telegram4j.tl.request.messages.SendEncrypted;
import telegram4j.tl.request.messages.SendEncryptedService;
import telegram4j.tl.secret.BaseDecryptedMessage;
import telegram4j.tl.secret.DecryptedMessageAction;
import telegram4j.tl.secret.DecryptedMessageLayer;
import telegram4j.tl.secret.DecryptedMessageService;
import telegram4j.tl.secret.ImmutableDecryptedMessageActionResend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Drives both halves of a secret chat against a stand-in for the server, so the flow that only
 * ever runs across two devices can be checked without either.
 */
class SecretChatManagerTest {

    static final long INITIATOR = 1111L;
    static final long PARTICIPANT = 2222L;
    static final int CHAT_ID = 4242;
    static final long ACCESS_HASH = 0x1122334455667788L;
    /** Mirrors {@code SecretChatManager.MAX_HELD_MESSAGES}, which is not visible from here. */
    static final int MAX_HELD = 100;

    /** What the two sides agree on through the server rather than through the chat. */
    static final class World {
        ByteBuf gA;
        ByteBuf gB;
        long acceptedFingerprint;
        boolean discarded;
    }

    /**
     * One side's view of the server, so that what a side sends lands in its own outbox.
     *
     * <p> A shared outbox cannot say who sent what, and handing a side its own message back looks
     * exactly like the mirroring the protocol is built to reject. That only matters once both
     * sides can speak in the same round, as they do when each asks to replace the key at once.
     */
    static final class FakeServer implements MTProtoClientGroup {
        final World world;
        final List<EncryptedMessage> outbox = new ArrayList<>();

        FakeServer(World world) {
            this.world = world;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <R> Mono<R> send(DcId id, TlMethod<? extends R> method) {
            return Mono.fromSupplier(() -> {
                if (method instanceof GetDhConfig) {
                    return (R) ImmutableBaseDhConfig.of(TestDhParameters.G,
                            Unpooled.wrappedBuffer(ByteBufUtil.decodeHexDump(TestDhParameters.PRIME)), 42,
                            Unpooled.EMPTY_BUFFER);
                }
                if (method instanceof RequestEncryption request) {
                    world.gA = request.gA();
                    return (R) ImmutableEncryptedChatWaiting.of(CHAT_ID, ACCESS_HASH, 0, INITIATOR, PARTICIPANT);
                }
                if (method instanceof AcceptEncryption request) {
                    world.gB = request.gB();
                    world.acceptedFingerprint = request.keyFingerprint();
                    return (R) ImmutableBaseEncryptedChat.of(CHAT_ID, ACCESS_HASH, 0, INITIATOR, PARTICIPANT,
                            world.gB, world.acceptedFingerprint);
                }
                if (method instanceof SendEncrypted request) {
                    outbox.add(ImmutableBaseEncryptedMessage.of(request.randomId(), CHAT_ID, 0,
                            request.data(), EncryptedFileEmpty.instance()));
                    return (R) ImmutableBaseSentEncryptedMessage.of(0);
                }
                if (method instanceof SendEncryptedService request) {
                    outbox.add(ImmutableEncryptedMessageService.of(request.randomId(), CHAT_ID, 0,
                            request.data()));
                    return (R) ImmutableBaseSentEncryptedMessage.of(0);
                }
                if (method instanceof DiscardEncryption) {
                    world.discarded = true;
                    return (R) Boolean.TRUE;
                }
                throw new AssertionError("Unexpected request: " + method);
            });
        }

        @Override public MTProtoClient main() { throw new UnsupportedOperationException(); }
        @Override public UpdateDispatcher updates() { throw new UnsupportedOperationException(); }
        @Override public Mono<Void> start() { return Mono.empty(); }
        @Override public Mono<Void> close() { return Mono.empty(); }
        @Override public Mono<MTProtoClient> getOrCreateClient(DcId id) { throw new UnsupportedOperationException(); }
    }

    /** A chat with a manager and a store on each end. */
    static final class Chat {
        final World world = new World();
        final FakeServer initiatorServer = new FakeServer(world);
        final FakeServer participantServer = new FakeServer(world);
        final StoreLayoutImpl initiatorStore = new StoreLayoutImpl(c -> c);
        final StoreLayoutImpl participantStore = new StoreLayoutImpl(c -> c);
        final SecretChatManager initiator;
        final SecretChatManager participant;
        final SecretChatOutbox initiatorOutbox;
        final SecretChatOutbox participantOutbox;

        Chat() {
            this(SecretChatOutboxImpl.DEFAULT_CAPACITY);
        }

        Chat(int outboxCapacity) {
            this(new SecretChatOutboxImpl(outboxCapacity),
                new SecretChatOutboxImpl(outboxCapacity));
        }

        Chat(SecretChatOutbox initiatorOutbox, SecretChatOutbox participantOutbox) {
            this.initiatorOutbox = initiatorOutbox;
            this.participantOutbox = participantOutbox;
            initiator = new SecretChatManager(new ChatService(initiatorServer, initiatorStore),
                    initiatorStore, DhPrimeCheckerCache.instance(), false, initiatorOutbox);
            participant = new SecretChatManager(new ChatService(participantServer, participantStore),
                    participantStore, DhPrimeCheckerCache.instance(), false, participantOutbox);
        }

        /** Runs the exchange that gives both sides a key, including the layer each announces. */
        Chat establish() {
            initiator.requestSecretChat(ImmutableBaseInputUser.of(PARTICIPANT, 0)).block();
            participant.onUpdate(requested()).block();
            initiator.onUpdate(accepted()).block();
            settle();
            return this;
        }

        ImmutableEncryptedChatRequested requested() {
            return ImmutableEncryptedChatRequested.builder()
                    .id(CHAT_ID).accessHash(ACCESS_HASH).date(0)
                    .adminId(INITIATOR).participantId(PARTICIPANT).gA(world.gA)
                    .build();
        }

        ImmutableBaseEncryptedChat accepted() {
            return ImmutableBaseEncryptedChat.of(CHAT_ID, ACCESS_HASH, 0, INITIATOR, PARTICIPANT,
                    world.gB, world.acceptedFingerprint);
        }

        /** Delivers everything either side is holding, until neither has anything left to say. */
        void settle() {
            while (!initiatorServer.outbox.isEmpty() || !participantServer.outbox.isEmpty()) {
                var toParticipant = List.copyOf(initiatorServer.outbox);
                initiatorServer.outbox.clear();
                toParticipant.forEach(m -> participant.onNewMessage(m).blockFirst());

                var toInitiator = List.copyOf(participantServer.outbox);
                participantServer.outbox.clear();
                toInitiator.forEach(m -> initiator.onNewMessage(m).blockFirst());
            }
        }

        SecretChatState initiatorState() {
            var state = initiatorStore.getSecretChat(CHAT_ID).block();
            assertNotNull(state);
            return state;
        }

        SecretChatState participantState() {
            var state = participantStore.getSecretChat(CHAT_ID).block();
            assertNotNull(state);
            return state;
        }

        /**
         * Sends a service action from the participant, which has no API of its own for one.
         *
         * <p> The point is the actions no correct client would send: a manager can only be made to
         * produce well-formed ones, and what happens to the rest is exactly what wants testing.
         */
        EncryptedMessage participantSends(DecryptedMessageAction action) {
            var state = participantState();
            var allocated = state.seqNo().allocate();
            var layer = DecryptedMessageLayer.builder()
                    .randomBytes(Unpooled.wrappedBuffer(new byte[31]))
                    .layer(SecretChatUtils.LAYER)
                    .inSeqNo(allocated.inSeqNo())
                    .outSeqNo(allocated.outSeqNo())
                    .message(DecryptedMessageService.builder().randomId(1).action(action).build())
                    .build();

            participantStore.updateSecretChat(state.toBuilder().seqNo(allocated.next()).build()).block();
            var key = state.key();
            assertNotNull(key);

            // the cipher hands back a buffer from the allocator, and an incoming message is not
            // the manager's to release: in a real chat it belongs to whoever deserialized the
            // update. Copying it out and letting the original go keeps this from being reported
            // as a leak, which would bury a real one
            var payload = new SecretChatCipher(false).encrypt(key, state.creator(), layer);
            try {
                return ImmutableEncryptedMessageService.of(1, CHAT_ID, 0,
                        Unpooled.wrappedBuffer(ByteBufUtil.getBytes(payload)));
            } finally {
                payload.release();
            }
        }
    }

    @Test
    void bothSidesEndUpWithTheSameKey() {
        var chat = new Chat();

        // the initiator asks for a chat and gets back something not yet usable
        var requested = chat.initiator.requestSecretChat(ImmutableBaseInputUser.of(PARTICIPANT, 0)).block();
        assertNotNull(requested);
        assertEquals(SecretChatState.Status.REQUESTED, requested.status());
        assertNull(requested.key());
        assertNotNull(requested.secret(), "the exponent is needed to finish the exchange later");
        assertTrue(requested.creator());

        // the participant sees the request and accepts it, which keys its side immediately
        var accepted = chat.participant.onUpdate(chat.requested()).block();
        assertNotNull(accepted);
        assertEquals(SecretChatState.Status.READY, accepted.status());
        assertFalse(accepted.creator());
        assertNotNull(accepted.key());

        // and the initiator finishes with the exponent it kept
        var completed = chat.initiator.onUpdate(chat.accepted()).block();
        assertNotNull(completed);
        assertEquals(SecretChatState.Status.READY, completed.status());
        assertNotNull(completed.key());
        assertNull(completed.secret(), "the exponent outlived its purpose");

        assertEquals(accepted.key().id(), completed.key().id());
        assertEquals(accepted.key().value(), completed.key().value());
        assertEquals(completed.key().id(), chat.initiatorState().key().id());
        assertEquals(accepted.key().id(), chat.participantState().key().id());
    }

    @Test
    void keyingAnnouncesOurLayer() {
        var chat = new Chat();
        chat.initiator.requestSecretChat(ImmutableBaseInputUser.of(PARTICIPANT, 0)).block();

        // accepting sends a notifyLayer, which is what tells a peer holding back on MTProto 1.0
        // that it can stop
        chat.participant.onUpdate(chat.requested()).block();
        assertEquals(1, chat.participantServer.outbox.size());
        assertInstanceOf(EncryptedMessageService.class, chat.participantServer.outbox.get(0));

        chat.initiator.onUpdate(chat.accepted()).block();
        chat.settle();

        assertEquals(SecretChatUtils.LAYER, chat.initiatorState().layer());
        assertEquals(SecretChatUtils.LAYER, chat.participantState().layer());
    }

    @Test
    void aMessageWrittenByOneSideReadsOnTheOther() {
        var chat = new Chat().establish();

        chat.initiator.sendMessage(CHAT_ID, "hello").block();
        assertEquals(1, chat.initiatorServer.outbox.size());

        var received = chat.participant.onNewMessage(chat.initiatorServer.outbox.get(0)).blockFirst();
        assertInstanceOf(BaseDecryptedMessage.class, received);
        assertEquals("hello", ((BaseDecryptedMessage) received).message());
    }

    @Test
    void bothDirectionsWorkAndCountersStayInStep() {
        var chat = new Chat().establish();

        for (int i = 0; i < 8; i++) {
            chat.initiatorServer.outbox.clear();
            chat.initiator.sendMessage(CHAT_ID, "from initiator " + i).block();
            var toParticipant = chat.participant.onNewMessage(chat.initiatorServer.outbox.get(0)).blockFirst();
            assertEquals("from initiator " + i, ((BaseDecryptedMessage) toParticipant).message());

            chat.participantServer.outbox.clear();
            chat.participant.sendMessage(CHAT_ID, "from participant " + i).block();
            var toInitiator = chat.initiator.onNewMessage(chat.participantServer.outbox.get(0)).blockFirst();
            assertEquals("from participant " + i, ((BaseDecryptedMessage) toInitiator).message());
        }
    }

    @Test
    void aRepeatedMessageIsDroppedRatherThanDelivered() {
        var chat = new Chat().establish();

        chat.initiator.sendMessage(CHAT_ID, "once").block();
        var message = chat.initiatorServer.outbox.get(0);

        assertNotNull(chat.participant.onNewMessage(message).blockFirst());
        assertNull(chat.participant.onNewMessage(message).blockFirst(), "the repeat was delivered");
    }

    @Test
    void aMessageArrivingOutOfOrderIsNotDelivered() {
        var chat = new Chat().establish();

        chat.initiator.sendMessage(CHAT_ID, "first").block();
        chat.initiator.sendMessage(CHAT_ID, "second").block();
        assertEquals(2, chat.initiatorServer.outbox.size());

        // the second arrives alone; it must wait rather than be handed over out of order
        assertNull(chat.participant.onNewMessage(chat.initiatorServer.outbox.get(1)).blockFirst());

        var first = chat.participant.onNewMessage(chat.initiatorServer.outbox.get(0)).blockFirst();
        assertEquals("first", ((BaseDecryptedMessage) first).message());
    }

    @Test
    void aMessageHeldBehindAGapIsDeliveredOnceItIsFilled() {
        var chat = new Chat().establish();

        chat.initiator.sendMessage(CHAT_ID, "first").block();
        chat.initiator.sendMessage(CHAT_ID, "second").block();
        var first = chat.initiatorServer.outbox.get(0);
        var second = chat.initiatorServer.outbox.get(1);

        // the second overtakes the first: held, not dropped, and the peer is asked for what is
        // missing rather than the chat being left to die on the gap
        assertNull(chat.participant.onNewMessage(second).blockFirst());
        assertFalse(chat.participantServer.outbox.isEmpty(), "no resend was requested");

        // and when the first arrives, both come out, in the order they were sent
        var delivered = chat.participant.onNewMessage(first)
                .cast(BaseDecryptedMessage.class)
                .map(BaseDecryptedMessage::message)
                .collectList()
                .block();
        assertEquals(List.of("first", "second"), delivered);
    }

    @Test
    void resendRequestsAreCappedSoTwoStuckClientsCannotLoop() {
        var chat = new Chat().establish();

        // twenty messages pile up behind one that never arrives
        chat.initiator.sendMessage(CHAT_ID, "lost").block();
        for (int i = 0; i < 20; i++) {
            chat.initiator.sendMessage(CHAT_ID, "held " + i).block();
        }

        chat.participantServer.outbox.clear();
        for (int i = 1; i <= 20; i++) {
            assertNull(chat.participant.onNewMessage(chat.initiatorServer.outbox.get(i)).blockFirst());
        }

        // every one of them is held, but the peer is asked a handful of times rather than twenty:
        // each request covers a wider range than the last, and once the cap is reached the chat
        // stops asking until something arrives in order
        assertTrue(chat.participantServer.outbox.size() <= 3,
                "sent " + chat.participantServer.outbox.size() + " resend requests");

        // and nothing was lost by staying quiet: filling the gap still releases everything
        var delivered = chat.participant.onNewMessage(chat.initiatorServer.outbox.get(0))
                .cast(BaseDecryptedMessage.class)
                .map(BaseDecryptedMessage::message)
                .collectList()
                .block();
        assertEquals(21, delivered.size());
        assertEquals("lost", delivered.get(0));
        assertEquals("held 19", delivered.get(20));
    }

    @Test
    void messagesArrivingBackwardsAreAllDelivered() {
        var chat = new Chat().establish();

        // the initiator answers its own progress in each message, so the later ones report more
        // than the earlier ones; delivered newest first, the older reports must not be taken for
        // the peer going backwards
        for (int i = 0; i < 5; i++) {
            chat.participant.sendMessage(CHAT_ID, "ignored " + i).block();
            chat.initiator.onNewMessage(chat.participantServer.outbox.remove(0)).blockFirst();
            chat.initiator.sendMessage(CHAT_ID, "message " + i).block();
        }
        assertEquals(5, chat.initiatorServer.outbox.size());

        var backwards = new ArrayList<>(chat.initiatorServer.outbox);
        Collections.reverse(backwards);

        var delivered = new ArrayList<String>();
        for (var message : backwards) {
            chat.participant.onNewMessage(message)
                    .cast(BaseDecryptedMessage.class)
                    .map(BaseDecryptedMessage::message)
                    .doOnNext(delivered::add)
                    .blockLast();
        }

        // nothing was lost and nothing was reordered
        assertEquals(List.of("message 0", "message 1", "message 2", "message 3", "message 4"),
                delivered);
    }

    @Test
    void aChatWillNotHoldMoreThanItsShare() {
        var chat = new Chat().establish();

        // a peer whose first message went missing keeps talking, and every one of those is held
        chat.initiator.sendMessage(CHAT_ID, "lost").block();
        for (int i = 0; i < 150; i++) {
            chat.initiator.sendMessage(CHAT_ID, "held " + i).block();
        }

        chat.participantServer.outbox.clear();
        for (int i = 1; i < chat.initiatorServer.outbox.size(); i++) {
            assertNull(chat.participant.onNewMessage(chat.initiatorServer.outbox.get(i)).blockFirst());
        }

        // filling the hole releases what was kept, and no more than the cap allows
        var delivered = chat.participant.onNewMessage(chat.initiatorServer.outbox.get(0))
                .cast(BaseDecryptedMessage.class)
                .map(BaseDecryptedMessage::message)
                .collectList()
                .block();
        assertNotNull(delivered);
        // the hole itself plus at most a cap's worth, and a hundred and fifty were offered.
        // Not an exact count: this many messages wears the key out, and the rekey that starts
        // takes a sequence number of its own without carrying anything to deliver
        assertTrue(delivered.size() <= MAX_HELD + 1,
                "released " + delivered.size() + ", held more than the cap allows");
        assertTrue(delivered.size() >= MAX_HELD,
                "released " + delivered.size() + ", held fewer than the cap allows");

        // and what it kept is the run nearest the hole, in order
        assertEquals("lost", delivered.get(0));
        assertEquals("held 0", delivered.get(1));
        assertEquals("held 1", delivered.get(2));
    }

    @Test
    void aGapCanBeGivenUpOnWhenNobodyWillResend() {
        var chat = new Chat().establish();

        chat.initiator.sendMessage(CHAT_ID, "lost").block();
        chat.initiator.sendMessage(CHAT_ID, "arrived").block();
        var arrived = chat.initiatorServer.outbox.get(1);

        assertNull(chat.participant.onNewMessage(arrived).blockFirst());

        // the peer is TDLib and will never answer the resend, so the hole has to be stepped over
        var released = chat.participant.skipGap(CHAT_ID)
                .cast(BaseDecryptedMessage.class)
                .map(BaseDecryptedMessage::message)
                .collectList()
                .block();
        assertEquals(List.of("arrived"), released);

        // and the chat works again afterwards
        chat.initiatorServer.outbox.clear();
        chat.initiator.sendMessage(CHAT_ID, "after").block();
        var next = chat.participant.onNewMessage(chat.initiatorServer.outbox.get(0)).blockFirst();
        assertEquals("after", ((BaseDecryptedMessage) next).message());
    }

    @Test
    void aMessageThePeerNeverGotIsSentAgainWhenItAsks() {
        var chat = new Chat().establish();

        chat.initiator.sendMessage(CHAT_ID, "lost").block();
        chat.initiator.sendMessage(CHAT_ID, "arrived").block();
        var arrived = chat.initiatorServer.outbox.get(1);

        // only the second arrives, so the participant holds it and asks for the first
        chat.participantServer.outbox.clear();
        assertNull(chat.participant.onNewMessage(arrived).blockFirst());
        assertEquals(1, chat.participantServer.outbox.size(), "no resend was requested");

        // the initiator answers out of its outbox rather than ignoring the request
        chat.initiatorServer.outbox.clear();
        chat.initiator.onNewMessage(chat.participantServer.outbox.get(0)).blockLast();
        assertEquals(1, chat.initiatorServer.outbox.size(), "the request went unanswered");

        // and what comes back is the message itself, which releases what was held behind it
        var delivered = chat.participant.onNewMessage(chat.initiatorServer.outbox.get(0))
                .cast(BaseDecryptedMessage.class)
                .map(BaseDecryptedMessage::message)
                .collectList()
                .block();
        assertEquals(List.of("lost", "arrived"), delivered);
    }

    @Test
    void aResentMessageIsTheOneThatWasSent() {
        var chat = new Chat().establish();
        chat.initiator.sendMessage(CHAT_ID, "only").block();

        var original = chat.initiatorServer.outbox.get(0);
        // the last one sent, since keying the chat spent a sequence number on the layer notice
        var seqNo = chat.initiatorState().seqNo();
        int outSeqNo = seqNo.toWireSeqNo(seqNo.outSeqNo() - 1);

        chat.initiatorServer.outbox.clear();
        chat.initiator.onNewMessage(chat.participantSends(
                ImmutableDecryptedMessageActionResend.of(outSeqNo, outSeqNo))).blockLast();

        // byte for byte: a resend has to carry the sequence numbers of the message it replaces,
        // and re-encrypting would produce a message the peer counts as the next one instead
        assertEquals(1, chat.initiatorServer.outbox.size());
        assertEquals(original.bytes(), chat.initiatorServer.outbox.get(0).bytes());
    }

    @Test
    void nothingIsSentInPlaceOfAMessageTheOutboxNoLongerHas() {
        // nothing is kept, which is also what a chat looks like after a restart that predates
        // the outbox, or after enough messages to push the one being asked for out of it
        var chat = new Chat(0).establish();

        chat.initiator.sendMessage(CHAT_ID, "lost").block();
        chat.initiator.sendMessage(CHAT_ID, "arrived").block();
        chat.participantServer.outbox.clear();
        assertNull(chat.participant.onNewMessage(chat.initiatorServer.outbox.get(1)).blockFirst());

        chat.initiatorServer.outbox.clear();
        chat.initiator.onNewMessage(chat.participantServer.outbox.get(0)).blockLast();

        // a message standing in for the lost one would have to carry some in_seq_no, and the
        // current one is ahead of what the held messages carry. A peer which checks that, as
        // TDLib does when it replays what it was holding, finds every one of them out of order
        // and discards the chat. Leaving the hole open leaves the chat merely stuck
        assertTrue(chat.initiatorServer.outbox.isEmpty(),
                "sent something in place of a message whose in_seq_no is lost with it");
    }

    @Test
    void everySentMessageIsOfferedToTheOutbox() {
        // including the service messages, which take a sequence number like any other and so can
        // be the one the peer is stuck on
        var chat = new Chat(4).establish();
        for (int i = 0; i < 10; i++) {
            chat.initiator.sendMessage(CHAT_ID, "message " + i).block();
        }

        var seqNo = chat.initiatorState().seqNo();
        assertNull(chat.initiatorOutbox.get(CHAT_ID, seqNo.toWireSeqNo(5)).block(),
                "an old message was kept");

        var newest = chat.initiatorOutbox.get(CHAT_ID, seqNo.toWireSeqNo(seqNo.outSeqNo() - 1)).block();
        assertNotNull(newest, "the newest was dropped");
        // the numbers a stand-in would have to copy are kept beside the body, since they cannot
        // be read back out of it without the key
        assertEquals(seqNo.toWireSeqNo(seqNo.outSeqNo() - 1), newest.outSeqNo());
        assertEquals(seqNo.inWireSeqNo(), newest.inSeqNo());
        assertEquals(SecretChatUtils.LAYER, newest.layer());
    }

    @Test
    void aResendRequestNamingMessagesWeNeverSentIsRefused() {
        var chat = new Chat().establish();
        chat.initiator.sendMessage(CHAT_ID, "only").block();

        var seqNo = chat.initiatorState().seqNo();
        chat.initiatorServer.outbox.clear();

        // beyond what we have sent, which no correct peer asks for
        chat.initiator.onNewMessage(chat.participantSends(ImmutableDecryptedMessageActionResend.of(
                seqNo.toWireSeqNo(20), seqNo.toWireSeqNo(40)))).blockLast();
        assertTrue(chat.initiatorServer.outbox.isEmpty(), "answered for messages we never sent");

        // the wrong parity names the participant's own messages, not ours
        chat.initiator.onNewMessage(chat.participantSends(ImmutableDecryptedMessageActionResend.of(
                seqNo.toRemoteWireSeqNo(0), seqNo.toRemoteWireSeqNo(0)))).blockLast();
        assertTrue(chat.initiatorServer.outbox.isEmpty(), "answered a range of the other direction");
    }

    @Test
    void aMessageUnderAKeyNobodyHasAnyMoreIsNotSentAgain() {
        var chat = new Chat().establish();
        chat.initiator.sendMessage(CHAT_ID, "under the first key").block();

        var seqNo = chat.initiatorState().seqNo();
        int outSeqNo = seqNo.toWireSeqNo(seqNo.outSeqNo() - 1);
        chat.settle();

        // each side keeps the current key and the one before it, so after two replacements the
        // message kept in the outbox is readable to nobody
        chat.initiator.rekey(CHAT_ID).block();
        chat.settle();
        chat.participant.rekey(CHAT_ID).block();
        chat.settle();

        chat.initiatorServer.outbox.clear();
        chat.initiator.onNewMessage(chat.participantSends(
                ImmutableDecryptedMessageActionResend.of(outSeqNo, outSeqNo))).blockLast();

        // it is still in the outbox, but sending it would reach the peer under a key it has
        // forgotten, which it has no answer to beyond giving up on the chat
        assertTrue(chat.initiatorServer.outbox.isEmpty(),
                "sent a message under a key the peer can no longer read");
    }

    @Test
    void standingInIsManualAndCopiesTheOldestMessageStillKept() {
        var chat = new Chat(4).establish();
        for (int i = 1; i <= 5; i++) {
            chat.initiator.sendMessage(CHAT_ID, "message " + i).block();
        }

        // only the last arrives, so the participant holds it and asks for the other four
        chat.participantServer.outbox.clear();
        assertNull(chat.participant.onNewMessage(chat.initiatorServer.outbox.get(4)).blockFirst());
        assertEquals(1, chat.participantServer.outbox.size(), "no resend was requested");

        chat.initiatorServer.outbox.clear();
        chat.initiator.onNewMessage(chat.participantServer.outbox.get(0)).blockLast();
        assertEquals(3, chat.initiatorServer.outbox.size(),
                "answering a resend request sent something for a message it no longer has");

        // the oldest is gone, and nothing is offered in its place until somebody asks for that
        var seqNo = chat.initiatorState().seqNo();
        chat.initiator.standInFor(CHAT_ID).block();
        assertEquals(4, chat.initiatorServer.outbox.size(), "nothing was sent to stand in");

        var oldest = chat.initiatorOutbox.get(CHAT_ID, seqNo.toWireSeqNo(2)).block();
        assertNotNull(oldest);

        // the numbers are the whole point, and our own side is deliberately not strict enough
        // about them to notice, so they are read off the message itself
        var key = chat.participantState().key();
        assertNotNull(key);
        var standIn = (DecryptedMessageLayer) new SecretChatCipher(false).decrypt(key,
                chat.participantState().creator(), chat.initiatorServer.outbox.get(3).bytes().copy());
        assertEquals(seqNo.toWireSeqNo(1), standIn.outSeqNo(), "stood in for the wrong message");
        assertEquals(oldest.inSeqNo(), standIn.inSeqNo(),
                "an in_seq_no of its own, which makes everything the peer holds unacceptable to it");
        assertEquals(oldest.layer(), standIn.layer(), "a layer of its own, which fails the same way");

        // and the chat moves again, one message the poorer
        var delivered = new ArrayList<String>();
        for (var message : List.copyOf(chat.initiatorServer.outbox)) {
            chat.participant.onNewMessage(message)
                    .cast(BaseDecryptedMessage.class)
                    .map(BaseDecryptedMessage::message)
                    .doOnNext(delivered::add)
                    .blockLast();
        }
        assertEquals(List.of("message 2", "message 3", "message 4", "message 5"), delivered);
    }

    @Test
    void standingInAimsAtTheNumberThePeerNamed() {
        // nothing is kept, so the request cannot be answered and what it named is all we know
        var chat = new Chat(0).establish();
        for (int i = 0; i < 3; i++) {
            chat.initiator.sendMessage(CHAT_ID, "message " + i).block();
        }

        var seqNo = chat.initiatorState().seqNo();
        int asked = seqNo.toWireSeqNo(2);
        chat.initiator.onNewMessage(chat.participantSends(
                ImmutableDecryptedMessageActionResend.of(asked, asked))).blockLast();

        // what the peer reports having processed puts it somewhere else, and that is the whole
        // point: clients disagree by one about what that number counts, while a resend request
        // names the number outright
        int reported = seqNo.toWireSeqNo(chat.initiatorState().seqNo().remoteInSeqNo());
        assertNotEquals(asked, reported, "the two sources agree here, so this proves nothing");

        chat.initiatorServer.outbox.clear();
        chat.initiator.standInFor(CHAT_ID).block();
        assertEquals(1, chat.initiatorServer.outbox.size());

        var key = chat.participantState().key();
        assertNotNull(key);
        var standIn = (DecryptedMessageLayer) new SecretChatCipher(false).decrypt(key,
                chat.participantState().creator(), chat.initiatorServer.outbox.get(0).bytes().copy());
        assertEquals(asked, standIn.outSeqNo(), "stood in for a number the peer never asked about");
    }

    /**
     * An outbox which quietly loses one message, which is what a hole looks like from our side:
     * the counter moved on, so the peer will ask about it, and there is nothing to answer with.
     *
     * <p> Eviction cannot stand in for this. It drops the oldest first, so what is kept is always
     * a run with no holes inside it, and the message before a hole is gone along with it.
     */
    static final class LosingOutbox extends SecretChatOutboxImpl {
        final int lost;

        LosingOutbox(int lost) {
            this.lost = lost;
        }

        @Override
        public Mono<Void> add(int chatId, Sent message) {
            return message.outSeqNo() == lost ? Mono.empty() : super.add(chatId, message);
        }
    }

    @Test
    void aStandInCopiesTheNeighbourOfTheHoleAndNothingFurther() {
        // keying spends the first number, so the second message sent after it lands on wire 5
        int hole = 5;
        var chat = new Chat(new LosingOutbox(hole), new SecretChatOutboxImpl()).establish();
        for (int i = 0; i < 4; i++) {
            chat.initiator.sendMessage(CHAT_ID, "message " + i).block();
        }

        var seqNo = chat.initiatorState().seqNo();
        assertEquals(hole, seqNo.toWireSeqNo(2), "the hole is not where this test expects it");
        assertNull(chat.initiatorOutbox.get(CHAT_ID, hole).block(), "the hole was kept after all");

        var before = chat.initiatorOutbox.get(CHAT_ID, hole - 2).block();
        var after = chat.initiatorOutbox.get(CHAT_ID, hole + 2).block();
        assertNotNull(before);
        assertNotNull(after);

        chat.initiatorServer.outbox.clear();
        chat.initiator.standInFor(CHAT_ID, hole).block();
        assertEquals(1, chat.initiatorServer.outbox.size());

        var key = chat.participantState().key();
        assertNotNull(key);
        var standIn = (DecryptedMessageLayer) new SecretChatCipher(false).decrypt(key,
                chat.participantState().creator(), chat.initiatorServer.outbox.get(0).bytes().copy());

        // the one before the hole is what the peer recorded as it processed it, so repeating it is
        // accepted as equal and leaves everything held behind the hole acceptable too
        assertEquals(hole, standIn.outSeqNo());
        assertEquals(before.inSeqNo(), standIn.inSeqNo(), "took its numbers from somewhere else");
        assertEquals(before.layer(), standIn.layer());
        assertTrue(standIn.inSeqNo() <= after.inSeqNo(),
                "carries more than the messages the peer is holding, which is fatal to it");
    }

    @Test
    void standingInWithNothingKeptFallsBackToTheChatsOwnNumbers() {
        var chat = new Chat(0).establish();
        chat.initiator.sendMessage(CHAT_ID, "lost").block();
        chat.initiator.sendMessage(CHAT_ID, "held").block();
        chat.participant.onNewMessage(chat.initiatorServer.outbox.get(1)).blockFirst();

        var seqNo = chat.initiatorState().seqNo();
        chat.initiatorServer.outbox.clear();
        chat.initiator.standInFor(CHAT_ID).block();
        assertEquals(1, chat.initiatorServer.outbox.size());

        var key = chat.participantState().key();
        assertNotNull(key);
        var standIn = (DecryptedMessageLayer) new SecretChatCipher(false).decrypt(key,
                chat.participantState().creator(), chat.initiatorServer.outbox.get(0).bytes().copy());

        // the worst guess there is, and the one that ends the chat when the peer is holding
        // anything at all behind the hole. It is offered because nothing else is left
        assertEquals(seqNo.toWireSeqNo(seqNo.remoteInSeqNo()), standIn.outSeqNo());
        assertEquals(seqNo.inWireSeqNo(), standIn.inSeqNo());
    }

    @Test
    void onlyWhatTheOutboxStillHasIsSentAgain() {
        var chat = new Chat(4).establish();
        for (int i = 0; i < 10; i++) {
            chat.initiator.sendMessage(CHAT_ID, "message " + i).block();
        }

        var seqNo = chat.initiatorState().seqNo();
        int last = seqNo.outSeqNo() - 1;

        // the newest four, all of which are still kept
        chat.initiatorServer.outbox.clear();
        chat.initiator.onNewMessage(chat.participantSends(ImmutableDecryptedMessageActionResend.of(
                seqNo.toWireSeqNo(last - 3), seqNo.toWireSeqNo(last)))).blockLast();
        assertEquals(4, chat.initiatorServer.outbox.size());

        // and a range beginning before what is kept, which is the shape that matters: the peer is
        // stopped on the oldest number it names, and that is the one that fell out of the outbox
        chat.initiatorServer.outbox.clear();
        chat.initiator.onNewMessage(chat.participantSends(ImmutableDecryptedMessageActionResend.of(
                seqNo.toWireSeqNo(0), seqNo.toWireSeqNo(3)))).blockLast();
        assertTrue(chat.initiatorServer.outbox.isEmpty(), "answered with something it never sent");
    }

    @Test
    void aPeerStuckBehindAHoleCanBeGivenEverythingAgain() {
        var chat = new Chat().establish();

        // the participant never gets the first, so it holds the rest and stops confirming
        chat.initiator.sendMessage(CHAT_ID, "lost").block();
        chat.initiator.sendMessage(CHAT_ID, "held").block();
        chat.participant.onNewMessage(chat.initiatorServer.outbox.get(1)).blockFirst();

        // the only sign on our side is the peer's own count standing still
        var stuck = chat.initiatorState().seqNo();
        assertTrue(stuck.remoteInSeqNo() < stuck.outSeqNo(),
                "the peer confirmed everything, so there is nothing to recover from");

        chat.initiatorServer.outbox.clear();
        chat.participantServer.outbox.clear();
        chat.initiator.resendUnconfirmed(CHAT_ID).block();
        assertEquals(stuck.outSeqNo() - stuck.remoteInSeqNo(), chat.initiatorServer.outbox.size(),
                "sent back less than the peer is missing");

        var delivered = new ArrayList<String>();
        for (var message : List.copyOf(chat.initiatorServer.outbox)) {
            chat.participant.onNewMessage(message)
                    .cast(BaseDecryptedMessage.class)
                    .map(BaseDecryptedMessage::message)
                    .doOnNext(delivered::add)
                    .blockLast();
        }
        assertEquals(List.of("lost", "held"), delivered);
    }

    @Test
    void rekeyingLeavesBothSidesOnTheSameNewKey() {
        var chat = new Chat().establish();
        var before = chat.initiatorState();

        chat.initiator.rekey(CHAT_ID).block();
        chat.settle();

        var initiator = chat.initiatorState();
        var participant = chat.participantState();

        assertEquals(RekeyState.Status.IDLE, initiator.rekey().status());
        assertEquals(RekeyState.Status.IDLE, participant.rekey().status());
        assertEquals(initiator.key().id(), participant.key().id(), "the two sides hold different keys");
        assertNotEquals(before.key().id(), initiator.key().id(), "the key did not actually change");
        assertEquals(before.key().id(), initiator.oldKey().id(), "the replaced key was not kept");
    }

    @Test
    void messagesKeepFlowingAcrossAReplacement() {
        var chat = new Chat().establish();

        chat.initiator.rekey(CHAT_ID).block();
        chat.settle();

        chat.initiatorServer.outbox.clear();
        chat.initiator.sendMessage(CHAT_ID, "after rekey").block();
        var received = chat.participant.onNewMessage(chat.initiatorServer.outbox.get(0)).blockFirst();
        assertEquals("after rekey", ((BaseDecryptedMessage) received).message());

        chat.participantServer.outbox.clear();
        chat.participant.sendMessage(CHAT_ID, "and back").block();
        var back = chat.initiator.onNewMessage(chat.participantServer.outbox.get(0)).blockFirst();
        assertEquals("and back", ((BaseDecryptedMessage) back).message());
    }

    @Test
    void aMessageSentUnderTheOldKeyStillReads() {
        var chat = new Chat().establish();

        // the two sides do not switch at the same moment: the initiator installs the new key as
        // it sends the commit, and the participant only when that commit arrives. Anything the
        // participant writes in between is still under the old key
        chat.initiator.rekey(CHAT_ID).block();
        var requestKey = chat.initiatorServer.outbox.remove(0);

        chat.participant.onNewMessage(requestKey).blockFirst();
        var acceptKey = chat.participantServer.outbox.remove(0);

        chat.participant.sendMessage(CHAT_ID, "in flight").block();
        var inFlight = chat.participantServer.outbox.remove(0);

        chat.initiator.onNewMessage(acceptKey).blockFirst();
        assertNotNull(chat.initiatorState().oldKey(), "the initiator did not keep the key it replaced");

        var received = chat.initiator.onNewMessage(inFlight).blockFirst();
        assertEquals("in flight", ((BaseDecryptedMessage) received).message());
    }

    @Test
    void simultaneousRequestsLeaveOneWinner() {
        var chat = new Chat().establish();

        // both sides ask at once, and neither has heard the other yet
        chat.initiator.rekey(CHAT_ID).block();
        chat.participant.rekey(CHAT_ID).block();
        chat.settle();

        var initiator = chat.initiatorState();
        var participant = chat.participantState();

        // whoever won, the two must agree on the key and neither may be left mid-exchange
        assertEquals(RekeyState.Status.IDLE, initiator.rekey().status());
        assertEquals(RekeyState.Status.IDLE, participant.rekey().status());
        assertEquals(initiator.key().id(), participant.key().id());
    }

    @Test
    void aWornKeyIsReplacedWithoutBeingAsked() {
        var chat = new Chat().establish();
        var original = chat.initiatorState().key().id();

        // the key is good for a hundred messages, and nothing outside the manager is expected
        // to count them
        for (int i = 0; i < SecretChatUtils.DEFAULT_MESSAGES_PER_KEY; i++) {
            chat.participant.sendMessage(CHAT_ID, "message " + i).block();
            chat.settle();
        }

        var initiator = chat.initiatorState();
        var participant = chat.participantState();

        assertNotEquals(original, initiator.key().id(), "the key was never replaced");
        assertEquals(initiator.key().id(), participant.key().id());
        assertEquals(RekeyState.Status.IDLE, initiator.rekey().status());
        assertEquals(RekeyState.Status.IDLE, participant.rekey().status());

        // and the chat still works afterwards
        chat.initiatorServer.outbox.clear();
        chat.initiator.sendMessage(CHAT_ID, "still here").block();
        var received = chat.participant.onNewMessage(chat.initiatorServer.outbox.get(0)).blockFirst();
        assertEquals("still here", ((BaseDecryptedMessage) received).message());
    }

    @Test
    void aChatNobodyAnswersStillReplacesItsKey() {
        var chat = new Chat().establish();
        var original = chat.initiatorState().key().id();

        // only the initiator ever speaks, so a check that runs only on incoming messages would
        // never fire
        for (int i = 0; i < SecretChatUtils.DEFAULT_MESSAGES_PER_KEY; i++) {
            chat.initiator.sendMessage(CHAT_ID, "message " + i).block();
            chat.settle();
        }

        assertNotEquals(original, chat.initiatorState().key().id(), "the key was never replaced");
        assertEquals(chat.initiatorState().key().id(), chat.participantState().key().id());
    }

    @Test
    void rejectsAnAcceptanceQuotingTheWrongFingerprint() {
        var chat = new Chat();
        chat.initiator.requestSecretChat(ImmutableBaseInputUser.of(PARTICIPANT, 0)).block();

        var e = assertThrows(SecretChatException.class,
                () -> chat.initiator.onUpdate(ImmutableBaseEncryptedChat.of(CHAT_ID, ACCESS_HASH, 0,
                        INITIATOR, PARTICIPANT, chat.world.gA, 0xdeadbeefL)).block());
        assertTrue(e.getMessage().startsWith("Key fingerprint mismatch"), e.getMessage());
    }

    @Test
    void ignoresARepeatedAcceptance() {
        var chat = new Chat();
        chat.initiator.requestSecretChat(ImmutableBaseInputUser.of(PARTICIPANT, 0)).block();
        chat.participant.onUpdate(chat.requested()).block();

        assertNotNull(chat.initiator.onUpdate(chat.accepted()).block());
        // the same update again must not rerun the exchange against a spent exponent
        assertNull(chat.initiator.onUpdate(chat.accepted()).block());
    }

    @Test
    void discardingForgetsTheChat() {
        var chat = new Chat();
        chat.initiator.requestSecretChat(ImmutableBaseInputUser.of(PARTICIPANT, 0)).block();
        chat.initiator.discardSecretChat(CHAT_ID, true).block();

        assertTrue(chat.world.discarded);
        assertNull(chat.initiatorStore.getSecretChat(CHAT_ID).block());
    }

    @Test
    void aChatDiscardedByThePeerIsKeptAsDiscarded() {
        var chat = new Chat();
        chat.initiator.requestSecretChat(ImmutableBaseInputUser.of(PARTICIPANT, 0)).block();

        var state = chat.initiator.onUpdate(
                ImmutableEncryptedChatDiscarded.builder().id(CHAT_ID).build()).block();
        assertNotNull(state);
        assertEquals(SecretChatState.Status.DISCARDED, state.status());
        // kept rather than deleted, so a late message is dropped instead of starting a new chat
        assertNotNull(chat.initiatorStore.getSecretChat(CHAT_ID).block());
    }
}
