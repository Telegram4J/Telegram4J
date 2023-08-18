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
package telegram4j.core.spec.media;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.annotation.Nullable;
import reactor.util.function.Tuples;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.mtproto.internal.Preconditions;
import telegram4j.core.object.media.Poll;
import telegram4j.core.util.ImmutableEnumSet;
import telegram4j.core.util.parser.EntityParserFactory;
import telegram4j.core.util.parser.EntityParserSupport;
import telegram4j.tl.*;
import telegram4j.tl.api.TlEncodingUtil;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class InputMediaPollSpec implements InputMediaSpec {

    private static final InputMediaPollSpec CLOSE_INSTANCE = new InputMediaPollSpec(
            ImmutableEnumSet.of(Poll.Flag.CLOSED), "", List.of(),
            null, null, null, null, null);

    private final ImmutableEnumSet<Poll.Flag> flags;
    private final String question;
    private final List<PollAnswer> answers;
    @Nullable
    private final Duration closePeriod;
    @Nullable
    private final Instant closeTimestamp;
    @Nullable
    private final ByteBuf correctAnswer;
    @Nullable
    private final String solution;
    @Nullable
    private final EntityParserFactory parser;

    private InputMediaPollSpec(BaseBuilder builder) {
        this.flags = ImmutableEnumSet.of(Poll.Flag.class, builder.flags);
        this.question = builder.question;
        this.closePeriod = builder.closePeriod;
        this.closeTimestamp = builder.closeTimestamp;
        this.answers = List.copyOf(builder.answers);
        if (builder instanceof QuizBuilder quizBuilder) {
            this.solution = quizBuilder.solution;
            this.parser = quizBuilder.parser;
            this.correctAnswer = quizBuilder.correctAnswer;
        } else {
            this.solution = null;
            this.parser = null;
            this.correctAnswer = null;
        }
    }

    private InputMediaPollSpec(ImmutableEnumSet<Poll.Flag> flags, String question, List<PollAnswer> answers,
                               @Nullable Duration closePeriod, @Nullable Instant closeTimestamp, @Nullable ByteBuf correctAnswer,
                               @Nullable String solution, @Nullable EntityParserFactory parser) {
        this.flags = flags;
        this.question = question;
        this.answers = answers;
        this.closePeriod = closePeriod;
        this.closeTimestamp = closeTimestamp;
        this.correctAnswer = correctAnswer;
        this.solution = solution;
        this.parser = parser;
    }

    public ImmutableEnumSet<Poll.Flag> flags() {
        return flags;
    }

    public String question() {
        return question;
    }

    public List<PollAnswer> answers() {
        return answers;
    }

    public Optional<Duration> closePeriod() {
        return Optional.ofNullable(closePeriod);
    }

    public Optional<Instant> closeTimestamp() {
        return Optional.ofNullable(closeTimestamp);
    }

    public Optional<ByteBuf> correctAnswer() {
        return Optional.ofNullable(correctAnswer);
    }

    public Optional<String> solution() {
        return Optional.ofNullable(solution);
    }

    public Optional<EntityParserFactory> parser() {
        return Optional.ofNullable(parser);
    }

    @Override
    public Mono<ImmutableInputMediaPoll> resolve(MTProtoTelegramClient client) {
        return Mono.defer(() -> parser()
                .or(() -> client.getMtProtoResources().getDefaultEntityParser())
                .flatMap(parser -> solution().map(s -> EntityParserSupport.parse(client, parser.apply(s.trim()))))
                .orElseGet(() -> Mono.just(Tuples.of(solution().orElse(""), List.of())))
                .map(TupleUtils.function((solution, solutionEntities) -> InputMediaPoll.builder()
                        .poll(telegram4j.tl.Poll.builder()
                                .id(0)
                                .flags(flags.getValue())
                                .question(question)
                                .answers(answers)
                                .closePeriod(closePeriod != null ? Math.toIntExact(closePeriod.getSeconds()) : null)
                                .closeDate(closeTimestamp != null ? Math.toIntExact(closeTimestamp.getEpochSecond()) : null)
                                .build())
                        .correctAnswers(correctAnswer != null ? List.of(correctAnswer) : null)
                        .solution(solution.isEmpty() ? null : solution)
                        .solutionEntities(solution.isEmpty() ? null : solutionEntities)
                        .build())));
    }

    public InputMediaPollSpec withClosed(boolean value) {
        var newFlags = flags.set(Poll.Flag.CLOSED, value);
        if (flags == newFlags) return this;
        return new InputMediaPollSpec(newFlags, question, answers, closePeriod, closeTimestamp, correctAnswer, solution, parser);
    }

    public InputMediaPollSpec withPublicVoters(boolean value) {
        var newFlags = flags.set(Poll.Flag.PUBLIC_VOTERS, value);
        if (flags == newFlags) return this;
        return new InputMediaPollSpec(newFlags, question, answers, closePeriod, closeTimestamp, correctAnswer, solution, parser);
    }

    public InputMediaPollSpec withClosePeriod(@Nullable Duration value) {
        if (Objects.equals(closePeriod, value)) return this;
        return new InputMediaPollSpec(flags, question, answers, value, null, correctAnswer, solution, parser);
    }

    public InputMediaPollSpec withClosePeriod(Optional<Duration> opt) {
        return withClosePeriod(opt.orElse(null));
    }

    public InputMediaPollSpec withCloseTimestamp(@Nullable Instant value) {
        if (Objects.equals(closeTimestamp, value)) return this;
        return new InputMediaPollSpec(flags, question, answers, null, value, correctAnswer, solution, parser);
    }

    public InputMediaPollSpec withCloseTimestamp(Optional<Instant> opt) {
        return withCloseTimestamp(opt.orElse(null));
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InputMediaPollSpec that = (InputMediaPollSpec) o;
        return flags.equals(that.flags) && question.equals(that.question) &&
                answers.equals(that.answers) && Objects.equals(closePeriod, that.closePeriod) &&
                Objects.equals(closeTimestamp, that.closeTimestamp) && Objects.equals(correctAnswer, that.correctAnswer) &&
                Objects.equals(solution, that.solution) && Objects.equals(parser, that.parser);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + flags.hashCode();
        h += (h << 5) + question.hashCode();
        h += (h << 5) + answers.hashCode();
        h += (h << 5) + Objects.hashCode(closePeriod);
        h += (h << 5) + Objects.hashCode(closeTimestamp);
        h += (h << 5) + Objects.hashCode(correctAnswer);
        h += (h << 5) + Objects.hashCode(solution);
        h += (h << 5) + Objects.hashCode(parser);
        return h;
    }

    @Override
    public String toString() {
        return "InputMediaPollSpec{" +
                "flags=" + flags +
                ", question='" + question + '\'' +
                ", answers=" + answers +
                ", closePeriod=" + closePeriod +
                ", closeTimestamp=" + closeTimestamp +
                ", correctAnswer=" + (correctAnswer != null ? ByteBufUtil.hexDump(correctAnswer) : null) +
                ", solution='" + solution + '\'' +
                ", parser=" + parser +
                '}';
    }

    public static QuizBuilder quizBuilder() {
        return new QuizBuilder();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static InputMediaPollSpec close() {
        return CLOSE_INSTANCE;
    }

    public abstract static class BaseBuilder {

        protected static final byte INIT_BIT_QUESTION = 1;
        protected static final byte INIT_BIT_ANSWERS = 0b10;
        protected byte initBits = 0b11;

        protected final EnumSet<Poll.Flag> flags = EnumSet.noneOf(Poll.Flag.class);

        protected String question;
        protected List<PollAnswer> answers;
        @Nullable
        protected Duration closePeriod;
        @Nullable
        protected Instant closeTimestamp;

        protected BaseBuilder() {}

        public BaseBuilder closed(boolean value) {
            if (value) {
                flags.add(Poll.Flag.CLOSED);
            } else {
                flags.remove(Poll.Flag.CLOSED);
            }
            return this;
        }

        public BaseBuilder publicVoters(boolean value) {
            if (value) {
                flags.add(Poll.Flag.PUBLIC_VOTERS);
            } else {
                flags.remove(Poll.Flag.PUBLIC_VOTERS);
            }
            return this;
        }

        public BaseBuilder question(String value) {
            this.question = value;
            initBits &= ~INIT_BIT_QUESTION;
            return this;
        }

        public BaseBuilder answers(Iterable<? extends PollAnswer> values) {
            this.answers = StreamSupport.stream(values.spliterator(), false)
                    .map(Objects::requireNonNull)
                    .collect(Collectors.toList());
            Preconditions.requireArgument(answers.size() <= Poll.MAX_ANSWERS_COUNT, () ->
                    "Too much answers in poll: " + answers.size() + " > " + Poll.MAX_ANSWERS_COUNT);
            if (!answers.isEmpty()) {
                initBits &= ~INIT_BIT_ANSWERS;
            }
            return this;
        }

        public BaseBuilder addAnswer(String text, ByteBuf option) {
            return addAnswer(ImmutablePollAnswer.of(text, option));
        }

        public BaseBuilder addAnswer(PollAnswer value) {
            if (answers == null) {
                answers = new ArrayList<>();
            } else {
                Preconditions.requireArgument(answers.size() + 1 <= Poll.MAX_ANSWERS_COUNT, () ->
                        "Too much answers in poll: " + (answers.size() + 1) + " > " + Poll.MAX_ANSWERS_COUNT);
            }
            answers.add(value);
            initBits &= ~INIT_BIT_ANSWERS;
            return this;
        }

        public BaseBuilder addAnswers(PollAnswer... values) {
            var copy = Arrays.stream(values)
                    .map(Objects::requireNonNull)
                    .collect(Collectors.toList());
            int size = (answers != null ? answers.size() : 0) + copy.size();
            Preconditions.requireArgument(size <= Poll.MAX_ANSWERS_COUNT, () ->
                    "Too much answers in poll: " + size + " > " + Poll.MAX_ANSWERS_COUNT);
            if (this.answers == null) {
                this.answers = copy;
                initBits &= ~INIT_BIT_ANSWERS;
            } else {
                this.answers.addAll(copy);
            }
            return this;
        }

        public BaseBuilder addAnswers(Iterable<? extends PollAnswer> values) {
            var copy = StreamSupport.stream(values.spliterator(), false)
                    .<PollAnswer>map(Objects::requireNonNull)
                    .collect(Collectors.toList());
            int size = (answers != null ? answers.size() : 0) + copy.size();
            Preconditions.requireArgument(size <= Poll.MAX_ANSWERS_COUNT, () ->
                    "Too much answers in poll: " + size + " > " + Poll.MAX_ANSWERS_COUNT);
            if (this.answers == null) {
                this.answers = copy;
                initBits &= ~INIT_BIT_ANSWERS;
            } else {
                this.answers.addAll(copy);
            }
            return this;
        }

        public BaseBuilder closePeriod(@Nullable Duration value) {
            if (value != null) {
                Preconditions.requireArgument(value.compareTo(Poll.MIN_CLOSE_PERIOD) >= 0 &&
                        value.compareTo(Poll.MAX_CLOSE_PERIOD) < 0,
                        "Invalid close period value: " + value);
            }

            this.closePeriod = value;
            this.closeTimestamp = null;
            return this;
        }

        public BaseBuilder closeTimestamp(@Nullable Instant value) {
            this.closeTimestamp = value;
            this.closePeriod = null;
            return this;
        }

        protected void verify(List<String> attributes) {}

        public InputMediaPollSpec build() {
            if (initBits != 0) {
                List<String> attributes = new ArrayList<>(Integer.bitCount(initBits));
                if ((initBits & INIT_BIT_QUESTION) != 0) attributes.add("question");
                if ((initBits & INIT_BIT_ANSWERS) != 0) attributes.add("answers");
                verify(attributes);
                throw new IllegalStateException("Cannot build InputMediaPollSpec, some required attributes is not set: " + attributes);
            }
            Preconditions.requireState(answers.size() >= Poll.MIN_ANSWERS_COUNT, () ->
                    "Too few answers in poll: " + answers.size() + " < " + Poll.MIN_ANSWERS_COUNT);
            return new InputMediaPollSpec(this);
        }
    }

    public static class QuizBuilder extends BaseBuilder {
        protected static final byte INIT_BIT_CORRECT_ANSWER = 0b100;

        private ByteBuf correctAnswer;
        @Nullable
        private String solution;
        @Nullable
        private EntityParserFactory parser;

        public QuizBuilder correctAnswer(ByteBuf value) {
            this.correctAnswer = TlEncodingUtil.copyAsUnpooled(value);
            initBits &= ~INIT_BIT_CORRECT_ANSWER;
            return this;
        }

        public QuizBuilder solution(@Nullable String value) {
            this.solution = value;
            return this;
        }

        public QuizBuilder parser(@Nullable EntityParserFactory value) {
            this.parser = value;
            return this;
        }

        @Override
        public QuizBuilder closed(boolean value) {
            return (QuizBuilder) super.closed(value);
        }

        @Override
        public QuizBuilder publicVoters(boolean value) {
            return (QuizBuilder) super.publicVoters(value);
        }

        @Override
        public QuizBuilder question(String value) {
            return (QuizBuilder) super.question(value);
        }

        @Override
        public QuizBuilder answers(Iterable<? extends PollAnswer> values) {
            return (QuizBuilder) super.answers(values);
        }

        @Override
        public QuizBuilder addAnswer(String text, ByteBuf option) {
            return (QuizBuilder) super.addAnswer(text, option);
        }

        @Override
        public QuizBuilder addAnswer(PollAnswer value) {
            return (QuizBuilder) super.addAnswer(value);
        }

        @Override
        public QuizBuilder addAnswers(PollAnswer... values) {
            return (QuizBuilder) super.addAnswers(values);
        }

        @Override
        public QuizBuilder addAnswers(Iterable<? extends PollAnswer> values) {
            return (QuizBuilder) super.addAnswers(values);
        }

        @Override
        public QuizBuilder closePeriod(@Nullable Duration value) {
            return (QuizBuilder) super.closePeriod(value);
        }

        @Override
        public QuizBuilder closeTimestamp(@Nullable Instant value) {
            return (QuizBuilder) super.closeTimestamp(value);
        }

        @Override
        protected void verify(List<String> attributes) {
            if ((initBits & INIT_BIT_CORRECT_ANSWER) != 0) attributes.add("correctAnswer");
        }

        private QuizBuilder() {
            flags.add(Poll.Flag.QUIZ);
            initBits |= INIT_BIT_CORRECT_ANSWER;
        }
    }

    public static class Builder extends BaseBuilder {

        private Builder() {}

        public Builder multipleChoice(boolean value) {
            if (value) {
                flags.add(Poll.Flag.MULTIPLE_CHOICE);
            } else {
                flags.remove(Poll.Flag.MULTIPLE_CHOICE);
            }
            return this;
        }

        @Override
        public Builder closed(boolean value) {
            return (Builder) super.closed(value);
        }

        @Override
        public Builder publicVoters(boolean value) {
            return (Builder) super.publicVoters(value);
        }

        @Override
        public Builder question(String value) {
            return (Builder) super.question(value);
        }

        @Override
        public Builder answers(Iterable<? extends PollAnswer> values) {
            return (Builder) super.answers(values);
        }

        @Override
        public Builder addAnswer(String text, ByteBuf option) {
            return (Builder) super.addAnswer(text, option);
        }

        @Override
        public Builder addAnswer(PollAnswer value) {
            return (Builder) super.addAnswer(value);
        }

        @Override
        public Builder addAnswers(PollAnswer... values) {
            return (Builder) super.addAnswers(values);
        }

        @Override
        public Builder addAnswers(Iterable<? extends PollAnswer> values) {
            return (Builder) super.addAnswers(values);
        }

        @Override
        public Builder closePeriod(@Nullable Duration value) {
            return (Builder) super.closePeriod(value);
        }

        @Override
        public Builder closeTimestamp(@Nullable Instant value) {
            return (Builder) super.closeTimestamp(value);
        }
    }
}
