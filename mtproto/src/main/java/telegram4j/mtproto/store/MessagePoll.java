package telegram4j.mtproto.store;

import io.netty.buffer.ByteBuf;
import reactor.util.annotation.Nullable;
import telegram4j.tl.InputMediaPoll;
import telegram4j.tl.MessageEntity;
import telegram4j.tl.Peer;
import telegram4j.tl.Poll;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class MessagePoll {
    private final Poll poll;
    @Nullable
    private final ByteBuf correctAnswer;
    @Nullable
    private final String solution;
    @Nullable
    private final List<MessageEntity> solutionEntities;
    private final Peer peer;
    private final int messageId;

    public MessagePoll(Poll data, Peer peer, int messageId) {
        this.poll = Objects.requireNonNull(data);
        this.correctAnswer = null;
        this.solution = null;
        this.solutionEntities = null;
        this.peer = Objects.requireNonNull(peer);
        this.messageId = messageId;
    }

    public MessagePoll(InputMediaPoll data, Peer peer, int messageId) {
        this.poll = data.poll();
        this.correctAnswer = Optional.ofNullable(data.correctAnswers())
                .map(list -> list.isEmpty() ? null : list.get(0))
                .orElse(null);
        this.solution = data.solution();
        this.solutionEntities = data.solutionEntities();
        this.peer = Objects.requireNonNull(peer);
        this.messageId = messageId;
    }

    public MessagePoll(Poll poll, @Nullable ByteBuf correctAnswer, @Nullable String solution,
                       @Nullable List<MessageEntity> solutionEntities, Peer peer, int messageId) {
        this.poll = Objects.requireNonNull(poll);
        this.correctAnswer = correctAnswer;
        this.solution = solution;
        this.solutionEntities = solutionEntities;
        this.peer = Objects.requireNonNull(peer);
        this.messageId = messageId;
    }

    public Poll getPoll() {
        return poll;
    }

    public Optional<ByteBuf> getCorrectAnswer() {
        return Optional.ofNullable(correctAnswer);
    }

    public Optional<String> getSolution() {
        return Optional.ofNullable(solution);
    }

    public Optional<List<MessageEntity>> getSolutionEntities() {
        return Optional.ofNullable(solutionEntities);
    }

    public Peer getPeer() {
        return peer;
    }

    public int getMessageId() {
        return messageId;
    }

    @Override
    public String toString() {
        return "MessagePoll{" +
                "poll=" + poll +
                ", correctAnswer=" + correctAnswer +
                ", solution='" + solution + '\'' +
                ", solutionEntities=" + solutionEntities +
                '}';
    }
}
