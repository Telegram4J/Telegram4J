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

import reactor.util.annotation.Nullable;
import telegram4j.mtproto.auth.AuthKey;

import java.math.BigInteger;
import java.util.Objects;

/**
 * Progress of a key replacement, which secret chats perform every hundred messages or every week
 * so that a compromised key does not expose the whole conversation.
 *
 * <p> The exchange is a second Diffie-Hellman handshake carried inside the chat itself, over the
 * {@code decryptedMessageActionRequestKey}, {@code AcceptKey}, {@code CommitKey} and
 * {@code AbortKey} service actions, tagged with an {@code exchange_id} both sides quote back.
 *
 * <p> Either side may start one, and both may start one at the same moment. The tie is broken by
 * the exchange id: the larger one proceeds and the smaller is abandoned. Identical ids mean both
 * sides drew the same number, and then neither proceeds.
 */
public final class RekeyState {

    /** Where a key replacement is in its lifecycle. */
    public enum Status {
        /** No replacement in progress. */
        IDLE,
        /** We sent {@code requestKey} and are waiting for the peer to accept it. */
        REQUESTED,
        /** We answered the peer's {@code requestKey} and are waiting for it to commit. */
        ACCEPTED
    }

    private static final RekeyState IDLE = new RekeyState(Status.IDLE, 0, null, null);

    private final Status status;
    private final long exchangeId;
    @Nullable
    private final BigInteger secret;
    @Nullable
    private final AuthKey pendingKey;

    private RekeyState(Status status, long exchangeId,
                       @Nullable BigInteger secret, @Nullable AuthKey pendingKey) {
        this.status = status;
        this.exchangeId = exchangeId;
        this.secret = secret;
        this.pendingKey = pendingKey;
    }

    /** {@return The state of a chat with no replacement in progress} */
    public static RekeyState idle() {
        return IDLE;
    }

    /**
     * {@return The state after asking the peer for a new key}
     *
     * @param exchangeId the identifier quoted in every message of this exchange.
     * @param secret our private exponent, needed to finish the handshake when the peer accepts.
     */
    public static RekeyState requested(long exchangeId, BigInteger secret) {
        return new RekeyState(Status.REQUESTED, exchangeId, Objects.requireNonNull(secret), null);
    }

    /**
     * {@return The state after accepting the peer's request}
     *
     * @param exchangeId the identifier taken from the peer's request.
     * @param pendingKey the key already derived, which becomes current once the peer commits.
     */
    public static RekeyState accepted(long exchangeId, AuthKey pendingKey) {
        return new RekeyState(Status.ACCEPTED, exchangeId, null, Objects.requireNonNull(pendingKey));
    }

    public Status status() {
        return status;
    }

    /** {@return The identifier of the exchange in progress, meaningless when {@link Status#IDLE}} */
    public long exchangeId() {
        return exchangeId;
    }

    /** {@return Our private exponent, while {@link Status#REQUESTED}} */
    @Nullable
    public BigInteger secret() {
        return secret;
    }

    /** {@return The key awaiting the peer's commit, while {@link Status#ACCEPTED}} */
    @Nullable
    public AuthKey pendingKey() {
        return pendingKey;
    }

    /**
     * {@return Whether a request carrying the given id should win over the one in progress}
     *
     * <p> Only meaningful when a replacement is already in progress. The larger id proceeds; equal
     * ids mean both sides drew the same number and neither does.
     *
     * <p> The comparison is signed, because the id is a TL {@code long} and both reference
     * implementations compare it as one. Half of all ids are negative, so reading it as unsigned
     * would leave the two sides disagreeing about who won for half of the collisions.
     */
    public boolean isOutrankedBy(long otherExchangeId) {
        return status != Status.IDLE && otherExchangeId > exchangeId;
    }

    /** {@return Whether both sides drew the same id, in which case neither replacement proceeds} */
    public boolean tiesWith(long otherExchangeId) {
        return status != Status.IDLE && otherExchangeId == exchangeId;
    }

    @Override
    public String toString() {
        return status == Status.IDLE
                ? "RekeyState{IDLE}"
                : "RekeyState{" + status + ", exchangeId=0x" + Long.toHexString(exchangeId) + '}';
    }
}
