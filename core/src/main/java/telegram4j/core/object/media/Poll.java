package telegram4j.core.object.media;

import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.internal.MappingUtil;
import telegram4j.core.object.MessageEntity;
import telegram4j.core.object.TelegramObject;
import telegram4j.core.spec.markup.ReplyMarkupSpec;
import telegram4j.core.util.BitFlag;
import telegram4j.core.util.Id;
import telegram4j.mtproto.store.MessagePoll;
import telegram4j.tl.ImmutableInputMediaPoll;
import telegram4j.tl.ImmutablePeerUser;
import telegram4j.tl.ImmutablePoll;
import telegram4j.tl.PollAnswer;
import telegram4j.tl.request.messages.EditMessage;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/** <a href="https://core.telegram.org/api/poll">Poll</a> object, which represents active or closed poll. */
public final class Poll implements TelegramObject {

    public static final int MAX_ANSWERS_COUNT = 10;

    private final MTProtoTelegramClient client;
    private final MessagePoll data;
    @Nullable
    private final Id peer;

    public Poll(MTProtoTelegramClient client, MessagePoll data) {
        this.client = Objects.requireNonNull(client);
        this.data = Objects.requireNonNull(data);
        this.peer = Id.of(data.getPeer());
    }

    public Poll(MTProtoTelegramClient client, telegram4j.tl.Poll data, @Nullable Id peer) {
        this.client = Objects.requireNonNull(client);
        this.peer = peer;
        this.data = new MessagePoll(data, ImmutablePeerUser.of(-1), -1);
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    /**
     * Gets the server-assigned poll id.
     *
     * @return The id of poll.
     */
    public long getId() {
        return data.getPoll().id();
    }

    /**
     * Gets set of poll flags.
     *
     * @return The set of poll flags.
     */
    public Set<Flag> getFlags() {
        return Flag.of(data.getPoll());
    }

    /**
     * Gets the non-formatted poll question.
     *
     * @return The poll question.
     */
    public String getQuestion() {
        return data.getPoll().question();
    }

    /**
     * Gets list of answers (2-10) of poll.
     *
     * @return The list of answers.
     */
    public List<PollAnswer> getAnswers() {
        return data.getPoll().answers();
    }

    /**
     * Gets the duration during which the poll is active, if present.
     *
     * @return The duration during which the poll is active, if present.
     */
    public Optional<Duration> getClosePeriod() {
        return Optional.ofNullable(data.getPoll().closePeriod()).map(Duration::ofSeconds);
    }

    /**
     * Gets the timestamp, when poll will be closed, if present.
     *
     * @return The timestamp of poll close, if present.
     */
    public Optional<Instant> getCloseTimestamp() {
        return Optional.ofNullable(data.getPoll().closeDate()).map(Instant::ofEpochSecond);
    }

    /**
     * Gets correct answer of quiz, if poll is quiz and client have original information.
     *
     * @return The correct answer of quiz, if poll is quiz and client have original information.
     */
    public Optional<ByteBuf> getCorrectAnswer() {
        return data.getCorrectAnswer();
    }

    /**
     * Gets the explanation of quiz solution, if poll is quiz and client have original information.
     *
     * @return The explanation of quiz solution, if poll is quiz and client have original information.
     */
    public Optional<String> getSolution() {
        return data.getSolution();
    }

    /**
     * Gets list of message entities of the explanation quiz solution, if poll is quiz and client have original information.
     *
     * @return The list of message entities of the explanation quiz solution,
     * if poll is quiz and client have original information otherwise empty list.
     */
    public List<MessageEntity> getSolutionEntities() {
        return data.getSolutionEntities()
                .map(l -> {
                    String solution = getSolution().orElseThrow();
                    return l.stream()
                            .map(d -> new MessageEntity(client, d, solution))
                            .collect(Collectors.toList());
                })
                .orElse(List.of());
    }

    /**
     * Requests to close poll if user have created this poll.
     *
     * @return A {@link Mono} completing when action is complete.
     */
    public Mono<Void> close() {
        return close(null);
    }

    /**
     * Requests to close poll if bot have created this poll.
     *
     * @param replyMarkupSpec The reply markup that will be used for edited message.
     * @return A {@link Mono} completing when action is complete.
     */
    public Mono<Void> close(@Nullable ReplyMarkupSpec replyMarkupSpec) {
        if (peer == null) {
            return Mono.error(new IllegalStateException("No context information for this poll known"));
        }

        return client.asInputPeer(peer)
                .switchIfEmpty(MappingUtil.unresolvedPeer(peer))
                .flatMap(p -> Mono.just(EditMessage.builder()
                                .peer(p)
                                .id(data.getMessageId())
                                .media(ImmutableInputMediaPoll.of(ImmutablePoll.of(0, ImmutablePoll.CLOSED_MASK, "", List.of()))))
                        .flatMap(builder -> Mono.justOrEmpty(replyMarkupSpec)
                                .flatMap(s -> s.asData(client))
                                .doOnNext(builder::replyMarkup)
                                .then(Mono.fromSupplier(builder::build)))
                        .flatMap(data -> client.getServiceHolder().getChatService().editMessage(data))
                        .then());
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Poll poll = (Poll) o;
        return data.equals(poll.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "Poll{data=" + data + '}';
    }

    /** An enumeration of {@link telegram4j.tl.Poll} bit-flags. */
    public enum Flag implements BitFlag {
        /** Whether poll is closed. */
        CLOSED(telegram4j.tl.Poll.CLOSED_POS),

        /** Whether cast votes are publicly visible to all users (non-anonymous poll). */
        PUBLIC_VOTERS(telegram4j.tl.Poll.PUBLIC_VOTERS_POS),

        /** Whether poll allows to choice multiple options. This flag can't be set with {@link #QUIZ}. */
        MULTIPLE_CHOICE(telegram4j.tl.Poll.MULTIPLE_CHOICE_POS),

        /** Whether poll is quiz. This flag can't be set with {@link #MULTIPLE_CHOICE}. */
        QUIZ(telegram4j.tl.Poll.QUIZ_POS);

        private final byte position;

        Flag(byte position) {
            this.position = position;
        }

        public static Set<Flag> of(telegram4j.tl.Poll data) {
            var set = EnumSet.allOf(Flag.class);
            int flags = data.flags();
            set.removeIf(f -> (flags & f.mask()) == 0);
            return set;
        }

        @Override
        public byte position() {
            return position;
        }
    }
}
