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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.annotation.Nullable;
import telegram4j.mtproto.auth.AuthKey;
import telegram4j.mtproto.auth.DhParameters;
import telegram4j.mtproto.auth.DhPrimeChecker;
import telegram4j.mtproto.auth.DhPrimeCheckerCache;
import telegram4j.mtproto.service.ChatService;
import telegram4j.mtproto.store.FileStoreLayout;
import telegram4j.mtproto.store.SecretChatOutbox;
import telegram4j.mtproto.store.FileSecretChatOutbox;
import telegram4j.mtproto.store.SecretChatOutboxImpl;
import telegram4j.mtproto.store.SecretChatStore;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import telegram4j.tl.*;
import telegram4j.tl.request.messages.ImmutableSendEncrypted;
import telegram4j.tl.secret.*;
import telegram4j.tl.messages.BaseDhConfig;
import telegram4j.tl.messages.DhConfigNotModified;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import static telegram4j.mtproto.secret.SecretChatUtils.LAYER;
import static telegram4j.mtproto.secret.SecretChatUtils.MAX_REQUESTED_RESENDS;
import static telegram4j.mtproto.secret.SecretChatUtils.RANDOM_BYTES;
import static telegram4j.mtproto.secret.SecretChatUtils.negotiatedLayer;
import static telegram4j.mtproto.secret.SecretChatUtils.stillReadable;
import static telegram4j.mtproto.util.CryptoUtil.random;

/**
 * Establishes and tears down secret chats.
 *
 * <p> Nothing here decides when a chat should be opened; the caller does that, and this turns it
 * into the exchange the protocol requires and into state that survives a restart.
 *
 * @implNote The order of the writes matters more than it looks. Every state change is persisted
 * before the request that makes it visible to the peer, never after, so that a process which dies
 * mid-exchange leaves a chat we can still key rather than one the peer believes in and we cannot.
 */
public final class SecretChatManager {

    private static final Logger log = Loggers.getLogger(SecretChatManager.class);

    private final ChatService service;
    private final SecretChatStore store;
    private final DhPrimeChecker primeChecker;
    private final boolean acceptLegacyMTProto;
    private final SecretChatOutbox outbox;

    private volatile int dhVersion;
    @Nullable
    private volatile DhParameters parameters;

    /**
     * How many messages one chat may hold behind a gap.
     *
     * <p> Without a bound this grows as long as the peer keeps talking, and a peer whose first
     * message went missing can make it grow on purpose. A hundred matches how long a key is
     * allowed to live, so a chat cannot hold more than one key's worth of history. Worth turning
     * into a setting if a caller ever needs a different trade between memory and tolerance.
     */
    private static final int MAX_HELD_MESSAGES = 100;

    /** Messages held behind a gap, per chat, newest last. Not persisted: a restart refetches them. */
    private final Map<Integer, NavigableMap<Integer, DecryptedMessageLayer>> pending =
        new ConcurrentHashMap<>();

    /**
     * How many messages one request may make us send again.
     *
     * <p> Wider than any outbox shipped here keeps, so it bounds what a peer asking for a great
     * deal can make us send rather than getting in the way of an honest request. A peer left
     * short asks again once it has processed what it got.
     */
    private static final int MAX_ANSWERED_RESENDS = 64;

    /** How many resend requests one chat may send in {@link #RESEND_WINDOW} before it is muted. */
    private static final int MAX_RESENDS = 3;
    private static final Duration RESEND_WINDOW = Duration.ofSeconds(30);
    private static final Duration RESEND_COOLDOWN = Duration.ofMinutes(5);

    private final Map<Integer, ResendLimit> resendLimits = new ConcurrentHashMap<>();
    // the same bound the other way round: answering costs a message per hole, so a peer which
    // keeps asking for what our answers do not satisfy would have us sending without end
    private final Map<Integer, ResendLimit> answerLimits = new ConcurrentHashMap<>();

    /**
     * The lowest number a peer has asked for and we could not produce, per chat.
     *
     * <p> The only unambiguous statement of where a peer is stuck there is. Its own
     * {@link SeqNoState#remoteInSeqNo()} cannot say: clients disagree by one about whether that
     * number counts the messages processed or names the last of them, so deriving a position from
     * it lands either on the hole or one message short of it, with no way to tell which. A resend
     * request names the number itself.
     *
     * <p> Not persisted, and nothing is lost if it is: a peer still stuck asks again on the next
     * message we send it.
     */
    private final Map<Integer, Integer> unanswered = new ConcurrentHashMap<>();


    public SecretChatManager(ChatService service, SecretChatStore store) {
        this(service, store, DhPrimeCheckerCache.instance(), false);
    }

    /**
     * @param acceptLegacyMTProto Whether incoming MTProto 1.0 messages are read; see
     * {@link SecretChatCipher#SecretChatCipher(boolean)} for what that costs and why peers exist
     * which need it.
     */
    public SecretChatManager(ChatService service, SecretChatStore store,
                             DhPrimeChecker primeChecker, boolean acceptLegacyMTProto) {
        this(service, store, primeChecker, acceptLegacyMTProto, null);
    }

    /**
     * @param outbox Where sent messages are kept so that they can be sent again when the peer
     * asks, or {@code null} for a {@link FileSecretChatOutbox} over its default directory. A
     * message the peer loses and this cannot produce is lost to the chat, which then stops
     * showing anything we send after it, so the default is the one that survives a restart: a
     * hole opened while we were not running is exactly the one worth being able to answer.
     */
    public SecretChatManager(ChatService service, SecretChatStore store,
                             DhPrimeChecker primeChecker, boolean acceptLegacyMTProto,
                             @Nullable SecretChatOutbox outbox) {
        this.service = Objects.requireNonNull(service);
        this.store = Objects.requireNonNull(store);
        this.primeChecker = Objects.requireNonNull(primeChecker);
        this.acceptLegacyMTProto = acceptLegacyMTProto;
        if (outbox == null) {
            if (store instanceof FileStoreLayout) {
            // is not used by default for other types not to create unexpected files on disk
                outbox = new FileSecretChatOutbox();
            } else {
                outbox = new SecretChatOutboxImpl();
            }
        }
        this.outbox = outbox;
    }

    /**
     * Asks the given user to open a secret chat.
     *
     * <p> The chat is not usable when this completes: the peer must accept it first, which it may
     * do at any point or never. The returned state is {@link SecretChatState.Status#REQUESTED} and
     * becomes usable when {@link #onUpdate(EncryptedChat)} sees the acceptance.
     *
     * @param user The user to invite.
     * @return A {@link Mono} emitting the stored state of the pending chat.
     */
    public Mono<SecretChatState> requestSecretChat(InputUser user) {
        Objects.requireNonNull(user);

        return dhParameters().flatMap(dh -> {
            var secret = DhParameters.generateSecret();
            var gA = dh.computePublicKeyData(secret);

            return service.requestEncryption(user, random.nextInt(), gA)
                    .flatMap(chat -> {
                        if (!(chat instanceof EncryptedChatWaiting waiting)) {
                            return Mono.error(new SecretChatException(
                                    "Unexpected answer to requestEncryption: " + chat));
                        }

                        var state = SecretChatState.builder(waiting.id(), waiting.accessHash(),
                                        waiting.participantId(), true)
                                .status(SecretChatState.Status.REQUESTED)
                                .secret(secret)
                                .build();

                        log.debug("Requested secret chat {} with user {}", waiting.id(), waiting.participantId());
                        return store.updateSecretChat(state).thenReturn(state);
                    });
        });
    }

    /**
     * Applies an {@code updateEncryption} to the stored state, keying or discarding the chat.
     *
     * @param chat The {@link EncryptedChat} carried by the update.
     * @return A {@link Mono} emitting the new state, or empty for an update that changes nothing.
     */
    public Mono<SecretChatState> onUpdate(EncryptedChat chat) {
        Objects.requireNonNull(chat);

        return switch (chat.identifier()) {
            case EncryptedChatRequested.ID -> accept((EncryptedChatRequested) chat);
            case BaseEncryptedChat.ID -> complete((BaseEncryptedChat) chat);
            case EncryptedChatDiscarded.ID -> discarded((EncryptedChatDiscarded) chat);
            // waiting is our own request coming back, and empty carries nothing to apply
            case EncryptedChatWaiting.ID, EncryptedChatEmpty.ID -> Mono.empty();
            default -> Mono.error(new SecretChatException("Unexpected EncryptedChat: " + chat));
        };
    }

    /**
     * Tears down a secret chat and forgets its key.
     *
     * @param chatId The id of the chat.
     * @param deleteHistory Whether the peer should also drop the conversation.
     * @return A {@link Mono} completing when the chat is gone from the store.
     */
    public Mono<Void> discardSecretChat(int chatId, boolean deleteHistory) {
        return service.discardEncryption(deleteHistory, chatId)
                // the server may already consider it gone, which is the outcome we wanted anyway
                .onErrorResume(e -> {
                    log.debug("Failed to discard secret chat {}, forgetting it regardless", chatId, e);
                    return Mono.just(true);
                })
                .then(store.deleteSecretChat(chatId))
                .then(outbox.deleteAll(chatId));
    }

    /**
     * Tells the peer that its messages up to the given date have been read.
     *
     * <p> Nothing in the chat state depends on this, so a failure is the caller's to ignore: the
     * peer simply keeps showing the messages as unread.
     *
     * @param chatId The id of the chat.
     * @param maxDate The {@code date} of the newest message that has been read.
     * @return A {@link Mono} completing when the server has taken the notice.
     */
    public Mono<Void> markHistoryWasRead(int chatId, int maxDate) {
        return withChat(chatId, state -> service.readEncryptedHistory(
                        ImmutableInputEncryptedChat.of(state.id(), state.accessHash()), maxDate)
                .then());
    }

    /** {@return Every known secret chat, including the ones awaiting acceptance} */
    public Flux<SecretChatState> getSecretChats() {
        return store.getSecretChats();
    }

    /**
     * Sends a text message to a secret chat.
     *
     * @param chatId The id of the chat, which must be {@link SecretChatState.Status#READY}.
     * @param text The message.
     * @return A {@link Mono} completing when the server has taken the message.
     */
    public Mono<Void> sendMessage(int chatId, String text) {
        Objects.requireNonNull(text);

        // also checked after sending, so a chat nobody answers still replaces its key
        return withChat(chatId, state -> send(state, BaseDecryptedMessage.builder()
                .randomId(random.nextLong())
                .ttl(state.ttl())
                .message(text)
                .build(), false)
                .flatMap(sent -> rekeyIfWorn(sent).thenReturn(sent)));
    }

    /**
     * Applies an {@code updateNewEncryptedMessage} to the chat it belongs to.
     *
     * <p> A message that fills a gap releases everything held behind it, so one update can emit
     * several, in the order the peer sent them.
     *
     * <p> TODO low priority: the same message can be handed here twice. Encrypted messages are
     * sequenced by {@code qts}, which {@code DefaultUpdatesManager} tracks but nothing advances
     * on our behalf, so the server repeats updates it considers unacknowledged. Nothing breaks,
     * since a repeat is recognised and dropped, but it costs a decryption and a write each time.
     * Fixing it belongs to update state management rather than here.
     *
     * @param message The {@link EncryptedMessage} carried by the update.
     * @return A {@link Flux} emitting what the peer sent, empty when the message was a service
     * action, a repeat of one already processed, or is being held until the messages before it
     * arrive.
     */
    public Flux<DecryptedMessage> onNewMessage(EncryptedMessage message) {
        Objects.requireNonNull(message);

        int chatId = message.chatId();
        return store.getSecretChat(chatId)
                .switchIfEmpty(Mono.error(() -> new SecretChatException("No secret chat with id " + chatId)))
                .flatMapMany(state -> Flux.defer(() -> decrypt(state, message)));
    }

    /**
     * Gives up on the messages a chat is waiting for and carries on with what came after them.
     *
     * <p> A gap normally clears itself: the missing messages arrive, or the peer resends them
     * when asked. Neither happens once the peer no longer has them, and then the chat is stuck
     * for good. This is the way out, and it is deliberately manual because it throws those
     * messages away: their contents are gone, and if one of them changed the layer or the key,
     * that change is lost with it.
     *
     * <p> It also gives them up on the peer's behalf. Stepping over a message means the next one
     * we send reports having processed it, and a peer that keeps sent messages until they are
     * acknowledged, as TDLib does, drops it at that point. Asking for it afterwards would be
     * asking for something nobody has, which TDLib treats as fatal; the counters see to it that
     * we never do, since they never go back over what they have stepped over.
     *
     * @param chatId The id of the chat.
     * @return A {@link Flux} emitting the messages that were held behind the gap.
     */
    public Flux<DecryptedMessage> skipGap(int chatId) {
        return store.getSecretChat(chatId)
                .switchIfEmpty(Mono.error(() -> new SecretChatException("No secret chat with id " + chatId)))
                .flatMapMany(state -> {
                    var held = pending.get(chatId);
                    if (held == null || held.isEmpty()) {
                        log.debug("Secret chat {} is not waiting for anything", chatId);
                        return Flux.empty();
                    }

                    int resumeAt = held.firstKey();
                    log.warn("Giving up on messages {}..{} of secret chat {}",
                            state.seqNo().inSeqNo(), resumeAt - 1, chatId);

                    var resumed = state.toBuilder()
                            .seqNo(new SeqNoState(state.creator(), resumeAt,
                                    state.seqNo().outSeqNo(), state.seqNo().remoteInSeqNo()))
                            .build();
                    return store.updateSecretChat(resumed)
                            .thenMany(Flux.defer(() -> releaseHeldMessages(resumed)));
                });
    }

    /**
     * Asks the peer to send again what we are missing, unless this chat has been asking too often.
     *
     * <p> The bounds are in the peer's own numbering, since it is the one being asked, and both
     * MadelineProto and TDLib honour them by replaying what they stored.
     *
     * <p> Asking is not free of consequence. TDLib answers from the messages it has yet to see
     * acknowledged, and a request naming one it no longer has, or spanning more than
     * {@value telegram4j.mtproto.secret.SecretChatUtils#MAX_REQUESTED_RESENDS}, ends in an
     * error it treats as fatal and answers by
     * discarding the chat. The range is therefore capped here, and it only ever names messages
     * we have not acknowledged, which are exactly the ones a peer still keeps.
     *
     * <p> Repeats of a range already asked for are skipped: every message stuck behind a gap
     * would otherwise send one, and the ranges nest, so a peer that is already replaying gets
     * asked again for what it is in the middle of sending.
     *
     * <p> Being muted costs nothing but time: the message that prompted the request is held
     * either way, and {@link #skipGap(int)} remains the way out when no resend is ever coming.
     */
    private Mono<Void> requestResend(SecretChatState state, int fromSeqNo, int toSeqNo) {
        var limit = resendLimits.computeIfAbsent(state.id(), id -> new ResendLimit());
        if (limit.covers(fromSeqNo, toSeqNo)) {
            log.debug("Secret chat {} has already asked for {}..{}", state.id(), fromSeqNo, toSeqNo);
            return Mono.empty();
        }
        if (!limit.allow(Instant.now())) {
            log.warn("Secret chat {} has asked for resends too often, not asking again for {}",
                    state.id(), RESEND_COOLDOWN);
            return Mono.empty();
        }

        // TDLib's own bound on what it will answer, and asking for more is fatal to it
        int last = Math.min(toSeqNo, fromSeqNo + MAX_REQUESTED_RESENDS - 1);
        if (last < toSeqNo) {
            log.warn("Secret chat {} is missing {}..{}, asking only for {}..{}",
                    state.id(), fromSeqNo, toSeqNo, fromSeqNo, last);
        }
        limit.asked(fromSeqNo, last);

        var seqNo = state.seqNo();
        return sendService(state, ImmutableDecryptedMessageActionResend.of(
                        seqNo.toRemoteWireSeqNo(fromSeqNo), seqNo.toRemoteWireSeqNo(last)))
                .onErrorResume(e -> {
                    log.warn("Failed to ask secret chat {} for a resend", state.id(), e);
                    return Mono.empty();
                })
                .then();
    }

    /**
     * Sends the peer again what it says it never got.
     *
     * <p> The bounds are in our own numbering, since we are the side being asked, and step by two
     * like every sequence number of one direction. Messages still in the {@linkplain
     * SecretChatOutbox outbox} go out again exactly as they went out the first time; what
     * {@link #answerResend} does about the rest is the interesting half.
     */
    private Mono<SecretChatState> onResend(SecretChatState state, DecryptedMessageActionResend resend) {
        var limit = answerLimits.computeIfAbsent(state.id(), id -> new ResendLimit());
        if (!limit.allow(Instant.now())) {
            log.warn("Secret chat {} is asking for resends too often, not answering for {}",
                    state.id(), RESEND_COOLDOWN);
            return store.updateSecretChat(state).thenReturn(state);
        }

        log.info("Secret chat {} asks us to resend {}..{}", state.id(),
                resend.startSeqNo(), resend.endSeqNo());
        return answerResend(state, resend.startSeqNo(), resend.endSeqNo())
                .then(store.updateSecretChat(state))
                .thenReturn(state);
    }

    /**
     * Sends the peer again everything it has not reported processing.
     *
     * <p> The mirror image of {@link #skipGap(int)}: there the messages we are missing are given
     * up on, here the ones the peer is missing are offered again. A peer stuck behind a hole says
     * nothing about it beyond the {@code in_seq_no} it keeps quoting, which is why this is manual.
     * It is worth reaching for when a chat still delivers the peer's messages while ours stop
     * being answered and {@link SeqNoState#remoteInSeqNo()} stands well behind
     * {@link SeqNoState#outSeqNo()}.
     *
     * <p> It can only offer what the {@linkplain SecretChatOutbox outbox} still holds. A hole
     * older than that cannot be closed from this side at all, and the chat stays stuck in the
     * outgoing direction for good; there is no message that stands in for one whose sequence
     * number is known and whose contents are not.
     *
     * @param chatId The id of the chat.
     * @return A {@link Mono} completing when everything still held has been sent again.
     */
    public Mono<Void> resendUnconfirmed(int chatId) {
        return withChat(chatId, state -> {
            var seqNo = state.seqNo();
            if (seqNo.remoteInSeqNo() >= seqNo.outSeqNo()) {
                log.debug("Secret chat {} has nothing the peer has not confirmed", chatId);
                return Mono.empty();
            }

            log.info("Sending secret chat {} messages {}..{} again, which it never confirmed",
                    chatId, seqNo.remoteInSeqNo(), seqNo.outSeqNo() - 1);
            return answerResend(state, seqNo.toWireSeqNo(seqNo.remoteInSeqNo()),
                    seqNo.toWireSeqNo(seqNo.outSeqNo() - 1));
        });
    }

    /**
     * Sends again the messages in a range of our own wire sequence numbers.
     *
     * <p> The range is taken as a request rather than an instruction: a bound of the wrong parity
     * names messages of the other direction and is refused outright, one beyond what we have sent
     * is trimmed to what exists, and an overlong range is cut to its start, which is the end that
     * matters since the peer is stopped at the oldest hole and everything after it is being held
     * rather than lost.
     *
     * <p> A message the outbox no longer has is passed over, unless the outbox offers something to
     * put in its place; see {@link #standIn}.
     */
    private Mono<Void> answerResend(SecretChatState state, int fromSeqNo, int toSeqNo) {
        var seqNo = state.seqNo();
        if (!seqNo.isOurWireSeqNo(fromSeqNo) || !seqNo.isOurWireSeqNo(toSeqNo) || fromSeqNo > toSeqNo) {
            log.warn("Secret chat {} asked for {}..{}, which is not a range of our messages",
                    state.id(), fromSeqNo, toSeqNo);
            return Mono.empty();
        }
        if (seqNo.outSeqNo() == 0 || fromSeqNo > seqNo.toWireSeqNo(seqNo.outSeqNo() - 1)) {
            log.warn("Secret chat {} asked for {}..{}, beyond the {} messages we have sent",
                    state.id(), fromSeqNo, toSeqNo, seqNo.outSeqNo());
            return Mono.empty();
        }

        int last = Math.min(toSeqNo, seqNo.toWireSeqNo(seqNo.outSeqNo() - 1));
        last = Math.min(last, fromSeqNo + 2 * (MAX_ANSWERED_RESENDS - 1));
        if (last < toSeqNo) {
            log.warn("Secret chat {} asked for {}..{}, answering only up to {}",
                    state.id(), fromSeqNo, toSeqNo, last);
        }

        int count = (last - fromSeqNo) / 2 + 1;
        int first = fromSeqNo;
        // one after another: they are being sent because their order is what the peer is stuck on
        return Flux.range(0, count)
                .concatMap(i -> resend(state, first + 2 * i))
                .then();
    }

    /** Sends one message again, if the outbox still has it and the peer could still read it. */
    private Mono<Void> resend(SecretChatState state, int outSeqNo) {
        return outbox.get(state.id(), outSeqNo)
                .filter(sent -> stillReadable(state, sent))
                .flatMap(sent -> dispatch(state, Unpooled.wrappedBuffer(sent.payload()), sent.service())
                        .thenReturn(true))
                .defaultIfEmpty(false)
                .doOnNext(resent -> {
                    if (!resent) {
                        // the peer has just said where it is stuck, which is worth keeping: it is
                        // the number standInFor would otherwise have to guess at
                        unanswered.merge(state.id(), outSeqNo, Math::min);
                        // said plainly, because it is the end of the chat in that direction unless
                        // somebody decides to gamble on standInFor
                        log.warn("Secret chat {} can no longer send message {} again, and the peer"
                                + " will show nothing after it", state.id(), outSeqNo);
                    }
                })
                .then();
    }

    /**
     * Sends something which merely occupies the number of a message we can no longer produce, in
     * the hope that the peer moves past it.
     *
     * <p><b>This may end the chat, and often will.</b> A stand-in has to carry an
     * {@code in_seq_no} and a layer; the peer records them and then replays the messages it was
     * holding behind the hole through the same check, where anything carrying lower values fails
     * as not monotonic, which TDLib answers by discarding the chat. The only certainly safe values
     * are those of the message right after the lost one, and if we still had that one the outbox
     * would have answered the peer already. What is used instead is the oldest message still kept,
     * which is the lowest value that is certainly high enough for the peer to accept, and the
     * chat survives only if the peer is holding nothing between the hole and it.
     *
     * <p> It is offered because the alternative is not a working chat: a peer stuck behind a hole
     * shows nothing we send ever again, so the choice is between a chat that is already dead in
     * that direction and a chance of reviving it at the risk of losing it outright. Nothing else
     * mends it, and {@link #discardSecretChat(int, boolean)} is the tidy way to give up instead.
     *
     * @param chatId The id of the chat.
     * @param outSeqNo The wire {@code out_seq_no} to stand in for. The one the peer is waiting on
     * is {@link SeqNoState#remoteInSeqNo()} put through {@link SeqNoState#toWireSeqNo(int)}, which
     * is what {@link #standInFor(int)} uses.
     * @return A {@link Mono} completing when the stand-in has been sent.
     */
    public Mono<Void> standInFor(int chatId, int outSeqNo) {
        return withChat(chatId, state -> standIn(state, outSeqNo));
    }

    /**
     * Stands in for the message the peer last asked for and could not be given.
     *
     * <p> A peer names that number itself, in a resend request we failed to answer, and this uses
     * the one it named. Failing that it falls back to what the peer reports having processed,
     * which is a guess by up to one message; see {@link #unanswered} for why it cannot be better
     * than that.
     *
     * @param chatId The id of the chat.
     * @return A {@link Mono} completing when the stand-in has been sent.
     * @see #standInFor(int, int)
     */
    public Mono<Void> standInFor(int chatId) {
        return withChat(chatId, state -> {
            var asked = unanswered.get(chatId);
            if (asked != null) {
                return standIn(state, asked);
            }

            var seqNo = state.seqNo();
            if (seqNo.remoteInSeqNo() >= seqNo.outSeqNo()) {
                log.info("Secret chat {} is not waiting on anything of ours", chatId);
                return Mono.empty();
            }

            // nothing has been asked for since this process started, so the peer's own report is
            // all there is. It is the conservative end of the guess: a client which names the last
            // message it processed rather than counting them puts this one message before the
            // hole, where a stand-in is merely ignored. Better to be told the number
            int guess = seqNo.toWireSeqNo(seqNo.remoteInSeqNo());
            log.warn("Nothing has been asked for in secret chat {} since startup, so message {} is"
                    + " a guess from what the peer last reported. If a resend request named a"
                    + " number, pass that one instead", chatId, guess);
            return standIn(state, guess);
        });
    }

    private Mono<Void> standIn(SecretChatState state, int outSeqNo) {
        var key = state.key();
        if (key == null || state.status() != SecretChatState.Status.READY) {
            return Mono.error(new SecretChatException(
                    "Secret chat " + state.id() + " is not ready, it is " + state.status()));
        }
        if (!state.seqNo().isOurWireSeqNo(outSeqNo)) {
            return Mono.error(new SecretChatException(
                    outSeqNo + " is not the number of a message of ours in secret chat " + state.id()));
        }

        return outbox.get(state.id(), outSeqNo)
                .flatMap(sent -> {
                    log.warn("Secret chat {} still has message {}; sending it again beats standing"
                            + " in for it", state.id(), outSeqNo);
                    return dispatch(state, Unpooled.wrappedBuffer(sent.payload()), sent.service())
                            .thenReturn(true);
                })
                .switchIfEmpty(neighbourOf(state.id(), outSeqNo)
                        .flatMap(neighbour -> {
                            log.info("Standing in for message {} of secret chat {} with the numbers"
                                    + " of its neighbour {}, which the peer accepts and leaves"
                                    + " everything it holds acceptable",
                                    outSeqNo, state.id(), neighbour.outSeqNo());
                            return sendStandIn(state, key, outSeqNo, neighbour.inSeqNo(),
                                    neighbour.layer()).thenReturn(true);
                        }))
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Standing in for message {} of secret chat {} with the chat's current"
                            + " numbers, nothing being kept to guess better. This is the worst"
                            + " guess there is and will most likely end the chat",
                            outSeqNo, state.id());
                    return sendStandIn(state, key, outSeqNo, state.seqNo().inWireSeqNo(),
                            negotiatedLayer(state)).thenReturn(true);
                }))
                // whatever came of it, this number has had its turn
                .doOnNext(ignored -> unanswered.remove(state.id(), outSeqNo))
                .then();
    }

    /**
     * {@return A message next to the given number whose numbers a stand-in may safely carry}
     *
     * <p> Only the two immediate neighbours will do, and the one before is the better of them.
     * The peer recorded that message's {@code in_seq_no} and layer as it processed it, so
     * repeating them is accepted as equal rather than merely allowed, and everything the peer
     * holds behind the hole was sent later and carries no less. The one after works for the same
     * reason from the other side.
     *
     * <p> Anything further away is not safe, however tempting. A message several places along
     * says nothing about what sits between it and the hole, and the peer is holding exactly that,
     * about to replay it through a check the stand-in would have just raised the bar on.
     */
    private Mono<SecretChatOutbox.Sent> neighbourOf(int chatId, int outSeqNo) {
        // one direction's numbers step by two, so the neighbours are two away in each direction
        return outbox.get(chatId, outSeqNo - 2)
                .switchIfEmpty(Mono.defer(() -> outbox.get(chatId, outSeqNo + 2)));
    }

    private Mono<Void> sendStandIn(SecretChatState state, AuthKey key, int outSeqNo,
                                   int inSeqNo, int layer) {
        var noop = DecryptedMessageService.builder()
                .randomId(random.nextLong())
                .action(DecryptedMessageActionNoop.instance())
                .build();
        return dispatch(state, encrypt(state, key, noop, inSeqNo, outSeqNo, layer), true);
    }

    /**
     * Hands on the message the counters are now waiting for, if it arrived early and was held.
     *
     * <p> Called after every message that moves the counters, so closing a gap releases what
     * queued up behind it, one after another, each release exposing the next.
     */
    private Flux<DecryptedMessage> releaseHeldMessages(SecretChatState state) {
        var held = pending.get(state.id());
        if (held == null) {
            return Flux.empty();
        }

        var next = held.remove(state.seqNo().inSeqNo());
        if (next == null) {
            return Flux.empty();
        }

        log.debug("Secret chat {} resumes with the held message {}", state.id(), state.seqNo().inSeqNo());
        return handleDecryptedMessage(state, next);
    }

    private Flux<DecryptedMessage> decrypt(SecretChatState state, EncryptedMessage message) {
        var key = state.key();
        if (state.status() != SecretChatState.Status.READY || key == null) {
            // includes chats the peer discarded: a message arriving afterwards is dropped rather
            // than taken for the start of a new conversation
            log.warn("Dropping message for secret chat {}, which is {}", state.id(), state.status());
            return Flux.empty();
        }

        // a replacement is not instantaneous: messages the peer sent before it processed our
        // commit are still under the old key, and the fingerprint in the payload says which
        var payload = message.bytes();
        var oldKey = state.oldKey();
        if (oldKey != null && payload.readableBytes() >= 8
                && payload.getLongLE(payload.readerIndex()) == oldKey.id()) {
            key = oldKey;
        }

        var decrypted = new SecretChatCipher(acceptLegacyMTProto)
                .decrypt(key, state.creator(), payload);
        if (!(decrypted instanceof DecryptedMessageLayer layer)) {
            // layer 8 wrapped the message directly, without sequence numbers to check
            throw new SecretChatException("Unsupported end-to-end object: " + decrypted);
        }

        return handleDecryptedMessage(state, layer);
    }

    /**
     * Decides what a message is, and either drops it, holds it, or hands it to {@link #deliver}.
     *
     * <p> Only the counters decide: a message is in order, already seen, or too early. Everything
     * that follows from actually having the message lives in {@code deliver}, which is the only
     * part {@link #releaseHeldMessages(SecretChatState)} can reach, since a message taken off the queue is by
     * construction the one the counters are waiting for.
     */
    private Flux<DecryptedMessage> handleDecryptedMessage(SecretChatState state,
        DecryptedMessageLayer layer) {
        var incoming = state.seqNo().accept(layer.inSeqNo(), layer.outSeqNo());
        // kept even for a message we do not deliver: what it reports about the peer's progress
        // is true regardless of whether we could use the message itself
        var builder = state.toBuilder().seqNo(incoming.next());

        return switch (incoming.status()) {
            case REPEATED -> {
                log.debug("Dropping repeated message {} in secret chat {}", incoming.seqNo(), state.id());
                yield store.updateSecretChat(builder.build()).thenMany(Flux.empty());
            }
            case GAP -> {
                // held rather than dropped: the sequence numbers only advance once the hole is
                // filled, so discarding it would end the chat rather than delay one message
                var held = pending.computeIfAbsent(state.id(), id -> new ConcurrentSkipListMap<>());
                if (held.size() >= MAX_HELD_MESSAGES && !held.containsKey(incoming.seqNo())) {
                    // the ones already held are closer to the hole and will be delivered first,
                    // so the newcomer is what gives way. It becomes a gap of its own later, which
                    // skipGap can step over like any other
                    log.warn("Secret chat {} is holding {} messages, dropping message {}",
                            state.id(), held.size(), incoming.seqNo());
                } else {
                    held.putIfAbsent(incoming.seqNo(), layer);
                }

                int missingFrom = state.seqNo().inSeqNo();
                log.warn("Message {} in secret chat {} arrives before {}, holding it and asking for {}..{}",
                        incoming.seqNo(), state.id(), missingFrom, missingFrom, incoming.seqNo() - 1);

                var waiting = builder.build();
                yield store.updateSecretChat(waiting)
                        .then(requestResend(waiting, missingFrom, incoming.seqNo() - 1))
                        .thenMany(Flux.empty());
            }
            case EXPECTED -> {
                // the chat is moving again, so whatever was asked for is settled
                resendLimits.remove(state.id());
                yield deliver(state, builder, layer);
            }
        };
    }

    /**
     * Handles a message which is the one the counters were waiting for.
     *
     * @param builder The state this message produced, with the counters already advanced.
     */
    private Flux<DecryptedMessage> deliver(SecretChatState state, SecretChatState.Builder builder,
                                           DecryptedMessageLayer layer) {
        // any message tells us the layer, not only an explicit notification
        if (layer.layer() > state.layer()) {
            builder.layer(layer.layer());
        }
        builder.remainingMessages(Math.max(0, state.remainingMessages() - 1));

        var inner = layer.message();
        if (!(inner instanceof DecryptedMessageService serviceMessage)) {
            var updated = builder.build();
            return store.updateSecretChat(updated)
                    .then(rekeyIfWorn(updated))
                    // this message may have been the one a later one was waiting for
                    .thenMany(Flux.concat(Flux.just(inner), Flux.defer(() -> releaseHeldMessages(updated))));
        }

        var action = serviceMessage.action();
        // some actions are answered rather than applied, so they take over the state this message
        // produced instead of being folded into the builder
        var answered = answeredAction(builder.build(), action);
        if (answered != null) {
            return answered.flatMapMany(this::releaseHeldMessages);
        }

        applyAction(builder, state, action);
        var updated = builder.build();
        return store.updateSecretChat(updated)
                .then(rekeyIfWorn(updated))
                .thenMany(Flux.defer(() -> releaseHeldMessages(updated)));
    }

    // service actions change the chat rather than being delivered to the caller
    private void applyAction(SecretChatState.Builder builder, SecretChatState state,
                             DecryptedMessageAction action) {
        switch (action.identifier()) {
            case DecryptedMessageActionNotifyLayer.ID -> {
                int announced = ((DecryptedMessageActionNotifyLayer) action).layer();
                if (announced > state.layer()) {
                    builder.layer(announced);
                }
            }
            case DecryptedMessageActionSetMessageTTL.ID ->
                    builder.ttl(((DecryptedMessageActionSetMessageTTL) action).ttlSeconds());
            // doing nothing is what it is for, and it is not a curiosity: a side which has taken a
            // replacement key into use has to send something under it to show the switch took
            // effect, and sends this when it has nothing of its own to say. Arriving after a
            // commitKey, it is the sign that the replacement completed. We send one ourselves in
            // the same place, see #onCommitKey
            case DecryptedMessageActionNoop.ID ->
                    log.debug("Secret chat {} sent a no-op", state.id());
            // said out loud rather than dropped at debug: an action nothing here understands is
            // how a chat comes to be waiting for something we are never going to do
            default -> log.info("Unhandled service action in secret chat {}: {}", state.id(), action);
        }
    }

    // the actions which answer the peer and so cannot be applied to the builder alone; each
    // returns the state it leaves behind, already persisted
    @Nullable
    private Mono<SecretChatState> answeredAction(SecretChatState state, DecryptedMessageAction action) {
        return switch (action.identifier()) {
            case DecryptedMessageActionResend.ID ->
                    onResend(state, (DecryptedMessageActionResend) action);
            case DecryptedMessageActionRequestKey.ID ->
                    onRequestKey(state, (DecryptedMessageActionRequestKey) action);
            case DecryptedMessageActionAcceptKey.ID ->
                    onAcceptKey(state, (DecryptedMessageActionAcceptKey) action);
            case DecryptedMessageActionCommitKey.ID ->
                    onCommitKey(state, (DecryptedMessageActionCommitKey) action);
            case DecryptedMessageActionAbortKey.ID -> {
                log.debug("Peer aborted the key replacement in secret chat {}", state.id());
                var idle = state.toBuilder().rekey(RekeyState.idle()).build();
                yield store.updateSecretChat(idle).thenReturn(idle);
            }
            default -> null;
        };
    }

    /** How long to wait before starting another unprompted key replacement in the same chat. */
    private static final Duration REKEY_RETRY_INTERVAL = Duration.ofMinutes(5);

    private final Map<Integer, Instant> rekeyNotBefore = new ConcurrentHashMap<>();

    /**
     * Asks for a replacement when the current key has carried enough messages or stood long
     * enough, which is what makes the replacement automatic rather than something a caller has
     * to remember.
     */
    private Mono<Void> rekeyIfWorn(SecretChatState state) {
        if (state.rekey().status() != RekeyState.Status.IDLE
                || state.status() != SecretChatState.Status.READY) {
            return Mono.empty();
        }

        var createdAt = state.keyCreatedAt();
        boolean worn = state.remainingMessages() <= 0
                || createdAt != null && createdAt.plus(SecretChatUtils.MAX_KEY_AGE).isBefore(Instant.now());
        if (!worn) {
            return Mono.empty();
        }

        var notBefore = rekeyNotBefore.get(state.id());
        var now = Instant.now();
        if (notBefore != null && now.isBefore(notBefore)) {
            return Mono.empty();
        }
        // a peer that never answers would otherwise have us start one per message, since a chat
        // whose key is worn stays worn until a replacement completes
        rekeyNotBefore.put(state.id(), now.plus(REKEY_RETRY_INTERVAL));

        log.debug("Key of secret chat {} is worn out, replacing it", state.id());
        // a failure here must not fail the message that prompted it: the chat keeps working on
        // the old key, and the next message tries again
        return rekey(state.id())
                .onErrorResume(e -> {
                    log.warn("Failed to start replacing the key of secret chat {}", state.id(), e);
                    return Mono.empty();
                });
    }

    /**
     * Replaces the key of a chat, so that a key which leaks exposes only the messages around it.
     *
     * <p> Called on its own when the current key wears out, so a caller only needs this to force
     * a replacement earlier. Does nothing if one is already in progress.
     *
     * @param chatId The id of the chat.
     * @return A {@link Mono} completing when the request has been sent.
     */
    public Mono<Void> rekey(int chatId) {
        return withChat(chatId, state -> {
            if (state.rekey().status() != RekeyState.Status.IDLE) {
                return Mono.empty();
            }
            return dhParameters().flatMap(dh -> {
                var secret = DhParameters.generateSecret();
                var gA = dh.computePublicKeyData(secret);
                long exchangeId = random.nextLong();

                var next = state.toBuilder()
                        .rekey(RekeyState.requested(exchangeId, secret))
                        .build();

                log.info("Requesting a new key for secret chat {}, exchange 0x{}",
                        chatId, Long.toHexString(exchangeId));
                return sendService(next, ImmutableDecryptedMessageActionRequestKey.of(exchangeId, gA));
            });
        });
    }

    // the peer wants a new key
    private Mono<SecretChatState> onRequestKey(SecretChatState state, DecryptedMessageActionRequestKey request) {
        long exchangeId = request.exchangeId();
        var rekey = state.rekey();

        if (rekey.tiesWith(exchangeId)) {
            // both sides drew the same number, so neither proceeds
            log.warn("Both sides asked to replace the key of secret chat {} with the same exchange id",
                    state.id());
            var idle = state.toBuilder().rekey(RekeyState.idle()).build();
            return store.updateSecretChat(idle).thenReturn(idle);
        }
        if (rekey.status() != RekeyState.Status.IDLE && !rekey.isOutrankedBy(exchangeId)) {
            log.debug("Ignoring the peer's key replacement in secret chat {}, ours outranks it", state.id());
            return store.updateSecretChat(state).thenReturn(state);
        }

        return dhParameters().flatMap(dh -> {
            var secret = DhParameters.generateSecret();
            var gB = dh.computePublicKeyData(secret);
            var pendingKey = SecretChatKeyExchange.deriveKey(dh, request.gA(), secret);

            var next = state.toBuilder()
                    .rekey(RekeyState.accepted(exchangeId, pendingKey))
                    .build();

            log.info("Accepting a new key for secret chat {}, exchange 0x{}",
                    state.id(), Long.toHexString(exchangeId));
            return sendService(next, ImmutableDecryptedMessageActionAcceptKey.of(
                    exchangeId, gB, pendingKey.id()));
        });
    }

    // the peer answered the replacement we asked for
    private Mono<SecretChatState> onAcceptKey(SecretChatState state, DecryptedMessageActionAcceptKey accept) {
        long exchangeId = accept.exchangeId();
        var rekey = state.rekey();
        var secret = rekey.secret();

        if (rekey.status() != RekeyState.Status.REQUESTED || rekey.exchangeId() != exchangeId || secret == null) {
            log.debug("Ignoring acceptKey for an exchange we are not running in secret chat {}", state.id());
            var idle = state.toBuilder().rekey(RekeyState.idle()).build();
            return store.updateSecretChat(idle).thenReturn(idle);
        }

        return dhParameters().flatMap(dh -> {
            AuthKey replacement;
            try {
                replacement = SecretChatKeyExchange.deriveKey(dh, accept.gB(), secret, accept.keyFingerprint());
            } catch (SecretChatException e) {
                log.warn("Aborting the key replacement in secret chat {}: {}", state.id(), e.getMessage());
                return sendService(state.toBuilder().rekey(RekeyState.idle()).build(),
                                ImmutableDecryptedMessageActionAbortKey.of(exchangeId))
                        .then(Mono.error(e));
            }

            // the key is stored before the commit goes out. The other order loses the chat: the
            // peer switches on receiving the commit, and a crash before we stored the key leaves
            // us with no way to derive it again and nothing the peer sends readable
            var next = state.toBuilder()
                    .key(replacement, Instant.now())
                    .rekey(RekeyState.idle())
                    .build();

            log.info("Committing the new key of secret chat {}, fingerprint 0x{}", state.id(),
                    Long.toHexString(replacement.id()));
            return sendService(next, ImmutableDecryptedMessageActionCommitKey.of(
                    exchangeId, replacement.id()), state.key());
        });
    }

    // the peer confirms the replacement we accepted
    private Mono<SecretChatState> onCommitKey(SecretChatState state, DecryptedMessageActionCommitKey commit) {
        long exchangeId = commit.exchangeId();
        var rekey = state.rekey();
        var pendingKey = rekey.pendingKey();

        if (rekey.status() != RekeyState.Status.ACCEPTED || rekey.exchangeId() != exchangeId || pendingKey == null) {
            log.debug("Ignoring commitKey for an exchange we are not running in secret chat {}", state.id());
            return store.updateSecretChat(state).thenReturn(state);
        }
        if (pendingKey.id() != commit.keyFingerprint()) {
            log.warn("Aborting the key replacement in secret chat {}: peer committed a key we did not derive",
                    state.id());
            return sendService(state.toBuilder().rekey(RekeyState.idle()).build(),
                            ImmutableDecryptedMessageActionAbortKey.of(exchangeId))
                    .then(Mono.error(new SecretChatException("Committed key fingerprint mismatch")));
        }

        var next = state.toBuilder()
                .key(pendingKey, Instant.now())
                .rekey(RekeyState.idle())
                .build();

        // a message under the new key, so the peer sees the switch took effect
        log.info("Secret chat {} now uses its replacement key, fingerprint 0x{}", state.id(),
                Long.toHexString(pendingKey.id()));
        return sendService(next, DecryptedMessageActionNoop.instance());
    }

    private Mono<SecretChatState> sendService(SecretChatState state, DecryptedMessageAction action) {
        return sendService(state, action, null);
    }

    private Mono<SecretChatState> sendService(SecretChatState state, DecryptedMessageAction action,
                                              @Nullable AuthKey with) {
        return send(state, DecryptedMessageService.builder()
                .randomId(random.nextLong())
                .action(action)
                .build(), true, with);
    }

    /**
     * Tells the peer which layer we speak, so that it stops holding back on features and, for
     * peers which open a chat in MTProto 1.0, switches to 2.0.
     */
    private Mono<SecretChatState> notifyLayer(SecretChatState state) {
        return send(state, DecryptedMessageService.builder()
                .randomId(random.nextLong())
                .action(ImmutableDecryptedMessageActionNotifyLayer.of(LAYER))
                .build(), true);
    }

    private Mono<SecretChatState> send(SecretChatState state, DecryptedMessage message, boolean serviceMessage) {
        return send(state, message, serviceMessage, null);
    }

    /**
     * @param with the key to encrypt with, when it must not be the chat's current one. Only
     * {@code commitKey} needs this: it is what makes the peer switch, so it has to arrive under
     * the key the peer still holds, while we already hold the replacement.
     */
    private Mono<SecretChatState> send(SecretChatState state, DecryptedMessage message,
                                       boolean serviceMessage, @Nullable AuthKey with) {
        var key = with != null ? with : state.key();
        if (state.status() != SecretChatState.Status.READY || key == null) {
            return Mono.error(new SecretChatException(
                    "Secret chat " + state.id() + " is not ready, it is " + state.status()));
        }

        var allocated = state.seqNo().allocate();
        int layer = negotiatedLayer(state);
        ByteBuf payload = encrypt(state, key, message, allocated.inSeqNo(), allocated.outSeqNo(), layer);

        var next = state.toBuilder()
                .seqNo(allocated.next())
                .remainingMessages(Math.max(0, state.remainingMessages() - 1))
                .build();

        var sent = new SecretChatOutbox.Sent(allocated.outSeqNo(), allocated.inSeqNo(), layer,
                serviceMessage, ByteBufUtil.getBytes(payload));

        // the counter reaches the disk before the message that carries it reaches the peer.
        // The other order loses the chat: after a crash we would hand out a sequence number the
        // peer has already seen, and it discards the chat rather than guess what we meant
        return store.updateSecretChat(next)
                // the outbox is not held to that: it is a cache, and one that failed to record a
                // message costs an answer to a resend request rather than the chat
                .then(outbox.add(state.id(), sent)
                        .onErrorResume(e -> {
                            log.warn("Failed to record message {} of secret chat {} for resending",
                                    allocated.outSeqNo(), state.id(), e);
                            return Mono.empty();
                        }))
                .then(dispatch(state, payload, serviceMessage))
                .thenReturn(next);
    }

    /** Wraps a message in a {@code decryptedMessageLayer} carrying the given numbers, encrypted. */
    private ByteBuf encrypt(SecretChatState state, AuthKey key, DecryptedMessage message,
                            int inSeqNo, int outSeqNo, int announcedLayer) {
        byte[] randomBytes = new byte[RANDOM_BYTES];
        random.nextBytes(randomBytes);

        var layer = DecryptedMessageLayer.builder()
                .randomBytes(Unpooled.wrappedBuffer(randomBytes))
                .layer(announcedLayer)
                .inSeqNo(inSeqNo)
                .outSeqNo(outSeqNo)
                .message(message)
                .build();

        return new SecretChatCipher(acceptLegacyMTProto).encrypt(key, state.creator(), layer);
    }

    /**
     * Hands an encrypted body to the server and releases it, whatever the outcome.
     *
     * <p> The {@code random_id} is drawn here rather than kept with the message, so that replaying
     * one from the {@linkplain SecretChatOutbox outbox} looks to the server like a new request
     * instead of a repeat it may answer from what it already stored. What keeps the peer from
     * showing the message twice is the {@code random_id} inside the encrypted body, which stays
     * what it was.
     */
    private Mono<Void> dispatch(SecretChatState state, ByteBuf payload, boolean serviceMessage) {
        long randomId = random.nextLong();
        var peer = ImmutableInputEncryptedChat.of(state.id(), state.accessHash());

        // the request is built on subscription, so the payload has to outlive this method
        return Mono.defer(() -> serviceMessage
                        ? service.sendEncryptedService(peer, randomId, payload)
                        : service.sendEncrypted(ImmutableSendEncrypted.of(0, peer, randomId, payload)))
                .doFinally(signal -> payload.release())
                .then();
    }

    private Mono<Void> withChat(int chatId, java.util.function.Function<SecretChatState, Mono<?>> action) {
        return store.getSecretChat(chatId)
                .switchIfEmpty(Mono.error(() -> new SecretChatException("No secret chat with id " + chatId)))
                .flatMap(action)
                .then();
    }

    // the peer asked us to open a chat: derive the key from its half and answer with ours
    private Mono<SecretChatState> accept(EncryptedChatRequested requested) {
        return dhParameters().flatMap(dh -> {
            var secret = DhParameters.generateSecret();
            var gB = dh.computePublicKeyData(secret);
            var key = SecretChatKeyExchange.deriveKey(dh, requested.gA(), secret);

            var state = SecretChatState.builder(requested.id(), requested.accessHash(),
                            requested.adminId(), false)
                    .status(SecretChatState.Status.READY)
                    .key(key, Instant.now())
                    .build();

            // stored before the answer goes out: acceptEncryption makes the chat real for the
            // peer, and a crash in between would leave it holding a key we no longer have
            return store.updateSecretChat(state)
                    .then(service.acceptEncryption(
                            ImmutableInputEncryptedChat.of(requested.id(), requested.accessHash()),
                            gB, key.id()))
                    .doOnNext(c -> log.debug("Accepted secret chat {} from user {}",
                            requested.id(), requested.adminId()))
                    .then(notifyLayer(state));
        });
    }

    // the peer accepted the chat we asked for: finish the exchange with the exponent we kept
    private Mono<SecretChatState> complete(BaseEncryptedChat accepted) {
        return store.getSecretChat(accepted.id())
                .switchIfEmpty(Mono.error(() -> new SecretChatException(
                        "No pending secret chat with id " + accepted.id())))
                .flatMap(state -> {
                    if (state.status() != SecretChatState.Status.REQUESTED || state.secret() == null) {
                        // a repeated update for a chat already keyed; nothing left to do
                        log.debug("Ignoring acceptance of secret chat {} in state {}",
                                accepted.id(), state.status());
                        return Mono.empty();
                    }

                    return dhParameters().flatMap(dh -> {
                        var key = SecretChatKeyExchange.deriveKey(dh, accepted.gAOrB(),
                                state.secret(), accepted.keyFingerprint());

                        var keyed = state.toBuilder()
                                .key(key, Instant.now())
                                .status(SecretChatState.Status.READY)
                                .build();

                        log.debug("Secret chat {} accepted by user {}", accepted.id(), accepted.participantId());
                        return store.updateSecretChat(keyed)
                                .then(notifyLayer(keyed));
                    });
                });
    }

    private Mono<SecretChatState> discarded(EncryptedChatDiscarded discarded) {
        return store.getSecretChat(discarded.id())
                .flatMap(state -> {
                    var gone = state.toBuilder().status(SecretChatState.Status.DISCARDED).build();
                    log.debug("Secret chat {} discarded by the peer", discarded.id());
                    // the state is kept rather than deleted so that late messages are dropped
                    // instead of being taken for the start of a new chat. What it sent is not
                    // kept: nothing will ever ask for it again
                    return store.updateSecretChat(gone)
                            .then(outbox.deleteAll(discarded.id()))
                            .thenReturn(gone);
                });
    }

    /**
     * The parameters every secret chat of this account shares.
     *
     * <p> They change only when the server bumps the version, which is why the request quotes the
     * one we hold and may be answered with nothing at all.
     *
     * <p> TODO skip the request entirely for a while once the parameters are known. Nothing about
     * that looks problematic: the {@code version} field is the change signal, so a stale cache is
     * noticed on the next fetch rather than silently; the request always goes to the main DC, so
     * reconnecting elsewhere cannot hand us a different answer; and the prime built into
     * {@link DhPrimeCheckerCache} is the one Telegram has been returning for years. The cost of
     * being stale is a chat that cannot be keyed until the next fetch. Left alone for now because
     * this runs only when a chat is created, accepted or rekeyed, which is rare enough that the
     * round trip does not matter.
     */
    private Mono<DhParameters> dhParameters() {
        return Mono.defer(() -> {
            // no server randomness is requested: the exponents come from SecureRandom, and mixing
            // in bytes the server chose would not make them any less predictable to the server
            return service.getDhConfig(dhVersion, 0)
                    .map(config -> switch (config.identifier()) {
                        case BaseDhConfig.ID -> {
                            var received = (BaseDhConfig) config;
                            var params = DhParameters.create(received.p(), received.g(), primeChecker);
                            parameters = params;
                            dhVersion = received.version();
                            yield params;
                        }
                        case DhConfigNotModified.ID -> {
                            var cached = parameters;
                            if (cached == null) {
                                throw new SecretChatException(
                                        "Server reports unchanged DH config, but none is cached");
                            }
                            yield cached;
                        }
                        default -> throw new SecretChatException("Unexpected DhConfig: " + config);
                    });
        });
    }

    /**
     * How often a chat has recently asked the peer to resend something.
     *
     * <p> A resend request is itself a message with its own sequence number, so it can create a
     * gap on the far side and provoke a request back. Two clients that have each lost something
     * will then keep asking each other forever, which is traffic Telegram sees and we do not
     * want to be the source of. The counter is cleared by any message that arrives in order,
     * since that means the chat is working and the asking was worth it.
     */
    private static final class ResendLimit {
        Instant windowStartedAt = Instant.now();
        int attempts;
        @Nullable
        Instant mutedUntil;
        int askedFrom = -1;
        int askedTo = -1;

        /** {@return Whether the last request already covers this range} */
        synchronized boolean covers(int from, int to) {
            return askedFrom >= 0 && from >= askedFrom && to <= askedTo;
        }

        synchronized void asked(int from, int to) {
            askedFrom = askedFrom < 0 ? from : Math.min(askedFrom, from);
            askedTo = Math.max(askedTo, to);
        }

        synchronized boolean allow(Instant now) {
            if (mutedUntil != null) {
                if (now.isBefore(mutedUntil)) {
                    return false;
                }
                mutedUntil = null;
                windowStartedAt = now;
                attempts = 0;
            }
            if (windowStartedAt.plus(RESEND_WINDOW).isBefore(now)) {
                windowStartedAt = now;
                attempts = 0;
            }
            if (++attempts > MAX_RESENDS) {
                mutedUntil = now.plus(RESEND_COOLDOWN);
                return false;
            }
            return true;
        }
    }
}
