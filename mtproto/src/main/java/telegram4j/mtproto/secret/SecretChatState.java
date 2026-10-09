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
import java.time.Instant;
import java.util.Objects;

/**
 * Everything about one secret chat that must outlive the process.
 *
 * <p> Secret chats are not stored server-side, so this state is the chat. Losing it loses the
 * conversation with no way to recover it from the server, and losing only part of it is worse
 * than losing all of it: a chat whose key survived but whose {@link #seqNo() counters} did not
 * will be discarded by the peer as soon as it speaks.
 *
 * <p> Instances are immutable, and a change is a new instance built with {@link #toBuilder()}.
 * A store may therefore hand out what it holds without copying, and a caller that fails to
 * persist a change leaves the stored state as it was rather than half-applied. That matters here
 * more than usual: the ordering this class exists to support is that a sequence number is durable
 * before the message carrying it is sent, and a state mutated in place would already have moved
 * on before the write was even attempted.
 */
public final class SecretChatState {

    /** Where a chat is in its lifecycle. */
    public enum Status {
        /**
         * We sent {@code messages.requestEncryption} and the peer has not answered yet.
         * {@link #secret()} is set and {@link #key()} is not.
         *
         * <p> A request can sit here for as long as the peer takes to open the chat, so the
         * secret is persisted rather than kept in memory; a restart in between would otherwise
         * leave a chat the peer can accept but we can never key.
         */
        REQUESTED,
        /** The key is negotiated and the chat can carry messages. */
        READY,
        /** Discarded by either side. Kept so that late messages are dropped rather than reprocessed. */
        DISCARDED
    }

    private final int id;
    private final long accessHash;
    private final long peerId;
    private final Status status;
    @Nullable
    private final BigInteger secret;
    @Nullable
    private final AuthKey key;
    @Nullable
    private final AuthKey oldKey;
    @Nullable
    private final Instant keyCreatedAt;
    @Nullable
    private final byte[] keyHash;
    private final int remainingMessages;
    private final int layer;
    private final int ttl;
    private final SeqNoState seqNo;
    private final RekeyState rekey;

    private SecretChatState(Builder builder) {
        this.id = builder.id;
        this.accessHash = builder.accessHash;
        this.peerId = builder.peerId;
        this.status = Objects.requireNonNull(builder.status, "status");
        this.secret = builder.secret;
        this.key = builder.key;
        this.oldKey = builder.oldKey;
        this.keyCreatedAt = builder.keyCreatedAt;
        this.keyHash = builder.keyHash;
        this.remainingMessages = builder.remainingMessages;
        this.layer = builder.layer;
        this.ttl = builder.ttl;
        this.seqNo = Objects.requireNonNull(builder.seqNo, "seqNo");
        this.rekey = builder.rekey;
    }

    /**
     * Starts a new chat.
     *
     * @param creator whether the chat was created by us, i.e. whether we are its {@code admin_id}.
     */
    public static Builder builder(int id, long accessHash, long peerId, boolean creator) {
        return new Builder(id, accessHash, peerId, creator);
    }

    /** {@return A builder holding this state, for deriving the next one} */
    public Builder toBuilder() {
        return new Builder(this);
    }

    /** {@return The {@code chat_id}, unique per account} */
    public int id() {
        return id;
    }

    public long accessHash() {
        return accessHash;
    }

    /** {@return The id of the other participant} */
    public long peerId() {
        return peerId;
    }

    /**
     * {@return Whether this chat was created by us}
     *
     * <p> Decides the parity of the {@linkplain SeqNoState sequence numbers} and the offset of
     * the key derivation, and is held by the counters because they are what it governs.
     */
    public boolean creator() {
        return seqNo.creator();
    }

    public Status status() {
        return status;
    }

    /** {@return Our private exponent, while the chat is {@link Status#REQUESTED}} */
    @Nullable
    public BigInteger secret() {
        return secret;
    }

    /** {@return The negotiated key, once the chat is {@link Status#READY}} */
    @Nullable
    public AuthKey key() {
        return key;
    }

    /** {@return The key replaced by the last rekey, still accepted for messages already in flight} */
    @Nullable
    public AuthKey oldKey() {
        return oldKey;
    }

    @Nullable
    public Instant keyCreatedAt() {
        return keyCreatedAt;
    }

    /**
     * {@return What clients show as this chat's encryption key, for two people to compare}
     *
     * <p> Fixed for the life of the chat: it comes from the key the chat started with and does
     * not follow later replacements, so a comparison made once stays valid.
     *
     * @see SecretChatKeyExchange#keyHash(AuthKey)
     */
    @Nullable
    public byte[] keyHash() {
        return keyHash == null ? null : keyHash.clone();
    }

    /** {@return How many more messages this key may carry before it must be replaced} */
    public int remainingMessages() {
        return remainingMessages;
    }

    /**
     * {@return The highest layer the peer has told us it supports}
     *
     * <p> Starts at {@value SecretChatUtils#DEFAULT_LAYER} and only ever grows. Both an explicit
     * {@code decryptedMessageActionNotifyLayer} and the {@code layer} of any received
     * {@code decryptedMessageLayer} count as being told.
     */
    public int layer() {
        return layer;
    }

    /** {@return The self-destruct timer of new messages, in seconds, or zero when disabled} */
    public int ttl() {
        return ttl;
    }

    public SeqNoState seqNo() {
        return seqNo;
    }

    public RekeyState rekey() {
        return rekey;
    }

    @Override
    public String toString() {
        return "SecretChatState{id=" + id + ", status=" + status + ", creator=" + creator() +
                ", layer=" + layer + ", " + seqNo + '}';
    }

    /** Collects the changes that make up the next state of a chat. */
    public static final class Builder {
        private final int id;
        private final long accessHash;
        private final long peerId;

        private Status status = Status.REQUESTED;
        @Nullable
        private BigInteger secret;
        @Nullable
        private AuthKey key;
        @Nullable
        private AuthKey oldKey;
        @Nullable
        private Instant keyCreatedAt;
        @Nullable
        private byte[] keyHash;
        private int remainingMessages = SecretChatUtils.DEFAULT_MESSAGES_PER_KEY;
        private int layer = SecretChatUtils.DEFAULT_LAYER;
        private int ttl;
        private SeqNoState seqNo;
        private RekeyState rekey = RekeyState.idle();

        private Builder(int id, long accessHash, long peerId, boolean creator) {
            this.id = id;
            this.accessHash = accessHash;
            this.peerId = peerId;
            this.seqNo = new SeqNoState(creator);
        }

        private Builder(SecretChatState state) {
            this.id = state.id;
            this.accessHash = state.accessHash;
            this.peerId = state.peerId;
            this.status = state.status;
            this.secret = state.secret;
            this.key = state.key;
            this.oldKey = state.oldKey;
            this.keyCreatedAt = state.keyCreatedAt;
            this.keyHash = state.keyHash;
            this.remainingMessages = state.remainingMessages;
            this.layer = state.layer;
            this.ttl = state.ttl;
            this.seqNo = state.seqNo;
            this.rekey = state.rekey;
        }

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public Builder secret(@Nullable BigInteger secret) {
            this.secret = secret;
            return this;
        }

        /**
         * Installs a newly negotiated key, keeping the previous one, and restarts the counters
         * which decide when to replace it again.
         *
         * <p> The old key is kept because a replacement is not instantaneous: messages the peer
         * sent before it processed our commit are still encrypted with it, and arrive after.
         *
         * <p> The private exponent is dropped here: it exists only to reach this point, and
         * keeping it afterwards would leave the material to re-derive the key sitting in storage
         * for the lifetime of the chat.
         */
        public Builder key(AuthKey key, Instant createdAt) {
            this.oldKey = this.key;
            this.key = Objects.requireNonNull(key);
            this.keyCreatedAt = Objects.requireNonNull(createdAt);
            this.remainingMessages = SecretChatUtils.DEFAULT_MESSAGES_PER_KEY;
            this.secret = null;
            // only the key the chat started with decides what the two people compare
            if (this.keyHash == null) {
                this.keyHash = SecretChatKeyExchange.keyHash(key);
            }
            return this;
        }

        /** Restores the hash as stored, for a chat read back from storage. */
        public Builder keyHash(@Nullable byte[] keyHash) {
            this.keyHash = keyHash == null ? null : keyHash.clone();
            return this;
        }

        /** Restores a key as stored, without any of the bookkeeping {@link #key} performs. */
        public Builder restoreKey(@Nullable AuthKey key, @Nullable Instant createdAt) {
            this.key = key;
            this.keyCreatedAt = createdAt;
            return this;
        }

        /** Sets the previous key, or clears it once the peer is known to have moved on. */
        public Builder oldKey(@Nullable AuthKey oldKey) {
            this.oldKey = oldKey;
            return this;
        }

        public Builder remainingMessages(int remainingMessages) {
            this.remainingMessages = remainingMessages;
            return this;
        }

        public Builder layer(int layer) {
            this.layer = layer;
            return this;
        }

        public Builder ttl(int ttl) {
            this.ttl = ttl;
            return this;
        }

        public Builder seqNo(SeqNoState seqNo) {
            this.seqNo = Objects.requireNonNull(seqNo);
            return this;
        }

        public Builder rekey(RekeyState rekey) {
            this.rekey = Objects.requireNonNull(rekey);
            return this;
        }

        public SecretChatState build() {
            return new SecretChatState(this);
        }
    }
}
