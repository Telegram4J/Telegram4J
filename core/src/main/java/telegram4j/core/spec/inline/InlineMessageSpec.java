package telegram4j.core.spec.inline;

import reactor.util.annotation.Nullable;
import telegram4j.core.internal.Preconditions;
import telegram4j.core.spec.Spec;
import telegram4j.core.spec.markup.ReplyMarkupSpec;
import telegram4j.core.spec.media.InputMediaVenueSpec;
import telegram4j.core.util.parser.EntityParserFactory;

import java.util.Objects;
import java.util.Optional;

public final class InlineMessageSpec implements Spec {
    private final Type type;
    @Nullable
    private final InputMediaVenueSpec venue;
    @Nullable
    private final ReplyMarkupSpec replyMarkup;
    private final boolean noWebpage;
    @Nullable
    private final String message;
    @Nullable
    private final EntityParserFactory parser;

    private InlineMessageSpec(Type type, @Nullable InputMediaVenueSpec venue,
                              @Nullable ReplyMarkupSpec replyMarkup, boolean noWebpage,
                              @Nullable String message, @Nullable EntityParserFactory parser) {
        this.type = type;
        this.venue = venue;
        this.replyMarkup = replyMarkup;
        this.noWebpage = noWebpage;
        this.message = message;
        this.parser = parser;
    }

    public Type type() {
        return type;
    }

    public Optional<InputMediaVenueSpec> venue() {
        return Optional.ofNullable(venue);
    }

    public Optional<ReplyMarkupSpec> replyMarkup() {
        return Optional.ofNullable(replyMarkup);
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

    public InlineMessageSpec withVenue(@Nullable InputMediaVenueSpec value) {
        Preconditions.requireState(type == Type.VENUE);
        if (venue == value) return this;
        return new InlineMessageSpec(type, value, replyMarkup, noWebpage, message, parser);
    }

    public InlineMessageSpec withVenue(Optional<InputMediaVenueSpec> opt) {
        return withVenue(opt.orElse(null));
    }

    public InlineMessageSpec withReplyMarkup(@Nullable ReplyMarkupSpec value) {
        if (replyMarkup == value) return this;
        return new InlineMessageSpec(type, venue, value, noWebpage, message, parser);
    }

    public InlineMessageSpec withReplyMarkup(Optional<ReplyMarkupSpec> opt) {
        return withReplyMarkup(opt.orElse(null));
    }

    public InlineMessageSpec withNoWebpage(boolean value) {
        Preconditions.requireState(type == Type.TEXT);
        if (noWebpage == value) return this;
        return new InlineMessageSpec(type, venue, replyMarkup, value, message, parser);
    }

    public InlineMessageSpec withMessage(@Nullable String value) {
        Preconditions.requireState(type == Type.MEDIA_AUTO || type == Type.TEXT);
        if (Objects.equals(message, value)) return this;
        return new InlineMessageSpec(type, venue, replyMarkup, noWebpage, value, parser);
    }

    public InlineMessageSpec withMessage(Optional<String> opt) {
        return withMessage(opt.orElse(null));
    }

    public InlineMessageSpec withParser(@Nullable EntityParserFactory value) {
        Preconditions.requireState(type == Type.MEDIA_AUTO || type == Type.TEXT);
        if (parser == value) return this;
        return new InlineMessageSpec(type, venue, replyMarkup, noWebpage, message, value);
    }

    public InlineMessageSpec withParser(Optional<? extends EntityParserFactory> opt) {
        return withParser(opt.orElse(null));
    }

    public static InlineMessageSpec venue(InputMediaVenueSpec venue, @Nullable ReplyMarkupSpec replyMarkup) {
        Objects.requireNonNull(venue);
        return new InlineMessageSpec(Type.VENUE, venue, replyMarkup, false, null, null);
    }

    public static InlineMessageSpec game(@Nullable ReplyMarkupSpec replyMarkup) {
        return new InlineMessageSpec(Type.GAME, null, replyMarkup, false, null, null);
    }

    public static InlineMessageSpec mediaAuto(String text) {
        return mediaAuto(text, null, null);
    }

    public static InlineMessageSpec mediaAuto(String text, @Nullable EntityParserFactory parser,
                                              @Nullable ReplyMarkupSpec replyMarkup) {
        Objects.requireNonNull(text);
        return new InlineMessageSpec(Type.MEDIA_AUTO, null, replyMarkup, false, text, parser);
    }

    public static InlineMessageSpec text(String text) {
        return text(false, text, null, null);
    }

    public static InlineMessageSpec text(boolean noWebpage, String text, @Nullable EntityParserFactory parser,
                                         @Nullable ReplyMarkupSpec replyMarkup) {
        Objects.requireNonNull(text);
        return new InlineMessageSpec(Type.TEXT, null, replyMarkup, noWebpage, text, parser);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InlineMessageSpec that = (InlineMessageSpec) o;
        return noWebpage == that.noWebpage && type == that.type &&
                Objects.equals(venue, that.venue) && Objects.equals(replyMarkup, that.replyMarkup) &&
                Objects.equals(message, that.message) && Objects.equals(parser, that.parser);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + type.hashCode();
        h += (h << 5) + Objects.hashCode(venue);
        h += (h << 5) + Objects.hashCode(replyMarkup);
        h += (h << 5) + Boolean.hashCode(noWebpage);
        h += (h << 5) + Objects.hashCode(message);
        h += (h << 5) + Objects.hashCode(parser);
        return h;
    }

    @Override
    public String toString() {
        return "InlineMessageSpec{" +
                "type=" + type +
                ", venue=" + venue +
                ", replyMarkup=" + replyMarkup +
                ", noWebpage=" + noWebpage +
                ", message='" + message + '\'' +
                ", parser=" + parser +
                '}';
    }

    public enum Type {
        MEDIA_AUTO,
        TEXT,
        GAME,
        VENUE
    }
}
