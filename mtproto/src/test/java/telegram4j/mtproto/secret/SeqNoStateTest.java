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

import org.junit.jupiter.api.Test;
import telegram4j.mtproto.secret.SeqNoState.Status;

import static org.junit.jupiter.api.Assertions.*;

class SeqNoStateTest {

    @Test
    void creatorParity() {
        // https://core.telegram.org/api/end-to-end/seq_no: the initiator sends odd out_seq_no
        // and even in_seq_no, the other participant the other way round
        var creator = new SeqNoState(true).allocate();
        assertEquals(0, creator.inSeqNo());
        assertEquals(1, creator.outSeqNo());

        var participant = new SeqNoState(false).allocate();
        assertEquals(1, participant.inSeqNo());
        assertEquals(0, participant.outSeqNo());
    }

    @Test
    void allocatingLeavesTheSourceAlone() {
        // the counter may not advance without the caller keeping the result: it has to reach the
        // disk before the message it belongs to reaches the peer
        var state = new SeqNoState(true);

        assertEquals(1, state.allocate().outSeqNo());
        assertEquals(1, state.allocate().outSeqNo(), "allocate() advanced the state it was called on");
        assertEquals(0, state.outSeqNo());

        assertEquals(1, state.allocate().next().outSeqNo());
    }

    @Test
    void countersAdvanceIndependently() {
        var state = new SeqNoState(true);

        var first = state.allocate();
        assertEquals(1, first.outSeqNo());
        var second = first.next().allocate();
        assertEquals(3, second.outSeqNo());
        assertEquals(0, second.inSeqNo());

        // receiving advances only the incoming counter
        var received = second.next().accept(1, 0);
        assertEquals(Status.EXPECTED, received.status());
        assertEquals(1, received.next().inSeqNo());
        assertEquals(2, received.next().outSeqNo());

        var third = received.next().allocate();
        assertEquals(2, third.inSeqNo());
        assertEquals(5, third.outSeqNo());
    }

    @Test
    void twoSidesStayInStep() {
        var creator = new SeqNoState(true);
        var participant = new SeqNoState(false);

        for (int i = 0; i < 16; i++) {
            var sent = creator.allocate();
            creator = sent.next();
            var atParticipant = participant.accept(sent.inSeqNo(), sent.outSeqNo());
            assertEquals(Status.EXPECTED, atParticipant.status());
            assertEquals(i, atParticipant.seqNo());
            participant = atParticipant.next();

            var reply = participant.allocate();
            participant = reply.next();
            var atCreator = creator.accept(reply.inSeqNo(), reply.outSeqNo());
            assertEquals(Status.EXPECTED, atCreator.status());
            assertEquals(i, atCreator.seqNo());
            creator = atCreator.next();
        }

        assertEquals(16, creator.inSeqNo());
        assertEquals(16, creator.outSeqNo());
        assertEquals(16, creator.remoteInSeqNo());
    }

    @Test
    void outgoingCounterIgnoresWhatThePeerReports() {
        // the regression this class exists for: a peer that replies before counting the message
        // it is replying to reports one less than we have sent, and adopting that value would
        // make the next message reuse a sequence number
        var creator = new SeqNoState(true);
        for (int i = 0; i < 3; i++) {
            creator = creator.allocate().next();
        }

        // the participant answers acknowledging only one of our three messages
        var lagging = new SeqNoState(false).accept(0, 1).next();
        var reply = lagging.allocate();
        assertEquals(3, reply.inSeqNo());
        assertEquals(0, reply.outSeqNo());

        var atCreator = creator.accept(reply.inSeqNo(), reply.outSeqNo());
        assertEquals(Status.EXPECTED, atCreator.status());
        assertEquals(1, atCreator.next().remoteInSeqNo());
        assertEquals(3, atCreator.next().outSeqNo(), "outgoing counter must not follow the peer");
        assertEquals(7, atCreator.next().allocate().outSeqNo());
    }

    @Test
    void detectsRepeatsAndGaps() {
        var participant = new SeqNoState(false);

        var first = participant.accept(0, 1);
        assertEquals(Status.EXPECTED, first.status());
        participant = first.next();

        assertEquals(Status.REPEATED, participant.accept(0, 1).status());

        // the creator's third message arrives before its second
        var gap = participant.accept(0, 5);
        assertEquals(Status.GAP, gap.status());
        assertEquals(2, gap.seqNo());
        assertEquals(1, gap.next().inSeqNo(), "a gap must not advance the counter");

        var second = participant.accept(0, 3);
        assertEquals(Status.EXPECTED, second.status());
        assertEquals(Status.EXPECTED, second.next().accept(0, 5).status());
    }

    @Test
    void rejectsOurOwnParity() {
        var creator = new SeqNoState(true);
        var mirrored = creator.allocate();

        // our own message handed back to us has both parities the wrong way round
        var e = assertThrowsExactly(SecretChatException.class,
                () -> creator.accept(mirrored.inSeqNo(), mirrored.outSeqNo()));
        assertTrue(e.getMessage().startsWith("Unexpected in_seq_no"), e.getMessage());
    }

    @Test
    void rejectsImpossibleAcknowledgements() {
        var creator = new SeqNoState(true).allocate().next();

        // the peer claims to have processed messages we never sent
        assertThrowsExactly(SecretChatException.class, () -> creator.accept(7, 0));

        // ...and one of slack is allowed, for a peer that counts the message it is replying to
        var accepted = creator.accept(5, 0);
        assertEquals(Status.EXPECTED, accepted.status());

        // an older message catching up reports less, which is ordinary reordering rather than a
        // violation: the highest report stands, and the message is still classified
        var late = accepted.next().accept(1, 2);
        assertEquals(Status.EXPECTED, late.status());
        assertEquals(2, late.next().remoteInSeqNo(), "an older report must not roll the peer back");
    }

    @Test
    void theAcknowledgementIsKeptEvenWhenTheMessageIsNot() {
        // a repeat still tells us the peer has moved on, and that is worth recording
        var creator = new SeqNoState(true, 1, 4, 0);

        var repeated = creator.accept(5, 0);
        assertEquals(Status.REPEATED, repeated.status());
        assertEquals(2, repeated.next().remoteInSeqNo());
        assertEquals(1, repeated.next().inSeqNo());
    }

    @Test
    void resendBoundsUseThePeerNumbering() {
        var creator = new SeqNoState(true);
        var participant = new SeqNoState(false);

        // asking the participant to resend its messages 0..2 must name them the way it numbers
        // its own out_seq_no, which is the parity the creator reads them with
        assertEquals(0, creator.toRemoteWireSeqNo(0));
        assertEquals(4, creator.toRemoteWireSeqNo(2));

        var third = participant.allocate().next().allocate().next().allocate();
        assertEquals(creator.toRemoteWireSeqNo(2), third.outSeqNo());
    }

    @Test
    void restoresPersistedCounters() {
        var restored = new SeqNoState(true, 7, 9, 6);

        var allocated = restored.allocate();
        assertEquals(14, allocated.inSeqNo());
        assertEquals(19, allocated.outSeqNo());

        var received = allocated.next().accept(13, 14);
        assertEquals(Status.EXPECTED, received.status());
        assertEquals(8, received.next().inSeqNo());
        assertEquals(10, received.next().outSeqNo());
        assertEquals(6, received.next().remoteInSeqNo());
    }
}
