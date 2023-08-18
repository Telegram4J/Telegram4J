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
package telegram4j.core.spec;

import reactor.util.annotation.Nullable;
import telegram4j.core.spec.markup.ReplyMarkupSpec;
import telegram4j.core.spec.media.InputMediaSpec;
import telegram4j.core.util.parser.EntityParserFactory;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class EditMessageSpec {
    private static final EditMessageSpec INSTANCE = new EditMessageSpec();

    private final boolean noWebpage;
    @Nullable
    private final String message;
    @Nullable
    private final InputMediaSpec media;
    @Nullable
    private final ReplyMarkupSpec replyMarkup;
    @Nullable
    private final Instant scheduleTimestamp;
    @Nullable
    private final EntityParserFactory parser;

    private EditMessageSpec() {
        this.message = null;
        this.noWebpage = false;
        this.parser = null;
        this.replyMarkup = null;
        this.scheduleTimestamp = null;
        this.media = null;
    }

    private EditMessageSpec(Builder builder) {
        this.noWebpage = builder.noWebpage;
        this.message = builder.message;
        this.parser = builder.parser;
        this.replyMarkup = builder.replyMarkup;
        this.scheduleTimestamp = builder.scheduleTimestamp;
        this.media = builder.media;
    }

    private EditMessageSpec(boolean noWebpage, @Nullable String message,
                            @Nullable InputMediaSpec media, @Nullable ReplyMarkupSpec replyMarkup,
                            @Nullable Instant scheduleTimestamp, @Nullable EntityParserFactory parser) {
        this.noWebpage = noWebpage;
        this.message = message;
        this.media = media;
        this.replyMarkup = replyMarkup;
        this.scheduleTimestamp = scheduleTimestamp;
        this.parser = parser;
    }

    public Optional<InputMediaSpec> media() {
        return Optional.ofNullable(media);
    }

    public boolean noWebpage() {
        return noWebpage;
    }

    public Optional<String> message() {
        return Optional.ofNullable(message);
    }

    public Optional<EntityParserFactory> parser() {
        return Optional.ofNullable(parser);
    }

    public Optional<ReplyMarkupSpec> replyMarkup() {
        return Optional.ofNullable(replyMarkup);
    }

    public Optional<Instant> scheduleTimestamp() {
        return Optional.ofNullable(scheduleTimestamp);
    }

    public EditMessageSpec withNoWebpage(boolean value) {
        if (noWebpage == value) return this;
        return canonize(new EditMessageSpec(value, message, media, replyMarkup, scheduleTimestamp, parser));
    }

    public EditMessageSpec withMessage(@Nullable String value) {
        if (Objects.equals(message, value)) return this;
        return canonize(new EditMessageSpec(noWebpage, value, media, replyMarkup, scheduleTimestamp, parser));
    }

    public EditMessageSpec withMessage(Optional<String> opt) {
        return withMessage(opt.orElse(null));
    }

    public EditMessageSpec withMedia(@Nullable InputMediaSpec value) {
        if (media == value) return this;
        return canonize(new EditMessageSpec(noWebpage, message, value, replyMarkup, scheduleTimestamp, parser));
    }

    public EditMessageSpec withMedia(Optional<? extends InputMediaSpec> opt) {
        return withMedia(opt.orElse(null));
    }

    public EditMessageSpec withParser(@Nullable EntityParserFactory value) {
        if (parser == value) return this;
        return canonize(new EditMessageSpec(noWebpage, message, media, replyMarkup, scheduleTimestamp, value));
    }

    public EditMessageSpec withParser(Optional<? extends EntityParserFactory> opt) {
        return withParser(opt.orElse(null));
    }

    public EditMessageSpec withReplyMarkup(@Nullable ReplyMarkupSpec value) {
        if (replyMarkup == value) return this;
        return canonize(new EditMessageSpec(noWebpage, message, media, value, scheduleTimestamp, parser));
    }

    public EditMessageSpec withReplyMarkup(Optional<ReplyMarkupSpec> opt) {
        return withReplyMarkup(opt.orElse(null));
    }

    public EditMessageSpec withScheduleTimestamp(@Nullable Instant value) {
        if (scheduleTimestamp == value) return this;
        return canonize(new EditMessageSpec(noWebpage, message, media, replyMarkup, value, parser));
    }

    public EditMessageSpec withScheduleTimestamp(Optional<Instant> opt) {
        return withScheduleTimestamp(opt.orElse(null));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EditMessageSpec other)) return false;
        return equalsTo(other);
    }

    private boolean equalsTo(EditMessageSpec other) {
        return noWebpage == other.noWebpage
                && Objects.equals(message, other.message)
                && Objects.equals(media, other.media)
                && Objects.equals(replyMarkup, other.replyMarkup)
                && Objects.equals(scheduleTimestamp, other.scheduleTimestamp)
                && Objects.equals(parser, other.parser);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + Boolean.hashCode(noWebpage);
        h += (h << 5) + Objects.hashCode(message);
        h += (h << 5) + Objects.hashCode(media);
        h += (h << 5) + Objects.hashCode(replyMarkup);
        h += (h << 5) + Objects.hashCode(scheduleTimestamp);
        h += (h << 5) + Objects.hashCode(parser);
        return h;
    }

    @Override
    public String toString() {
        return "EditMessageSpec{" +
                "noWebpage=" + noWebpage +
                ", message='" + message + '\'' +
                ", media=" + media +
                ", replyMarkup=" + replyMarkup +
                ", scheduleTimestamp=" + scheduleTimestamp +
                ", parser=" + parser +
                '}';
    }

    public static EditMessageSpec of() {
        return INSTANCE;
    }

    private static EditMessageSpec canonize(EditMessageSpec other) {
        return INSTANCE.equals(other) ? INSTANCE : other;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean noWebpage;
        private String message;
        private InputMediaSpec media;
        private ReplyMarkupSpec replyMarkup;
        private Instant scheduleTimestamp;
        private EntityParserFactory parser;

        private Builder() {
        }

        public Builder from(EditMessageSpec instance) {
            noWebpage(instance.noWebpage);
            message(instance.message);
            media(instance.media);
            replyMarkup(instance.replyMarkup);
            scheduleTimestamp(instance.scheduleTimestamp);
            parser(instance.parser);
            return this;
        }

        public Builder noWebpage(boolean noWebpage) {
            this.noWebpage = noWebpage;
            return this;
        }

        public Builder media(@Nullable InputMediaSpec media) {
            this.media = media;
            return this;
        }

        public Builder media(Optional<? extends InputMediaSpec> opt) {
            this.media = opt.orElse(null);
            return this;
        }

        public Builder message(@Nullable String message) {
            this.message = message;
            return this;
        }

        public Builder message(Optional<String> opt) {
            this.message = opt.orElse(null);
            return this;
        }

        public Builder parser(@Nullable EntityParserFactory parser) {
            this.parser = parser;
            return this;
        }

        public Builder parser(Optional<? extends EntityParserFactory> opt) {
            this.parser = opt.orElse(null);
            return this;
        }

        public Builder replyMarkup(@Nullable ReplyMarkupSpec replyMarkup) {
            this.replyMarkup = replyMarkup;
            return this;
        }

        public Builder replyMarkup(Optional<ReplyMarkupSpec> opt) {
            this.replyMarkup = opt.orElse(null);
            return this;
        }

        public Builder scheduleTimestamp(@Nullable Instant scheduleTimestamp) {
            this.scheduleTimestamp = scheduleTimestamp;
            return this;
        }

        public Builder scheduleTimestamp(Optional<Instant> opt) {
            this.scheduleTimestamp = opt.orElse(null);
            return this;
        }

        public EditMessageSpec build() {
            return canonize(new EditMessageSpec(this));
        }
    }
}
