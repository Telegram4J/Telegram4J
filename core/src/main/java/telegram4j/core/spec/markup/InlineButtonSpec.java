package telegram4j.core.spec.markup;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import reactor.util.annotation.Nullable;
import telegram4j.core.internal.Preconditions;
import telegram4j.core.object.markup.KeyboardButton;
import telegram4j.core.util.Id;
import telegram4j.tl.api.TlEncodingUtil;

import java.util.Objects;
import java.util.Optional;

public final class InlineButtonSpec implements KeyboardButtonSpec {
    private final KeyboardButton.Type type;
    private final String text;
    @Nullable
    private final ByteBuf data;
    @Nullable
    private final String query;
    @Nullable
    private final String url;
    @Nullable
    private final String forwardText;
    // Can't be replaced; different positions in bit-set
    private final boolean requestWriteAccess;
    private final boolean requiresPassword;
    private final boolean samePeer;
    @Nullable
    private final Id userId;

    private InlineButtonSpec(KeyboardButton.Type type, String text) {
        this.type = Objects.requireNonNull(type);
        this.text = Objects.requireNonNull(text);
        this.data = null;
        this.query = null;
        this.url = null;
        this.forwardText = null;
        this.requestWriteAccess = false;
        this.requiresPassword = false;
        this.samePeer = false;
        this.userId = null;
    }

    private InlineButtonSpec(KeyboardButton.Type type, String text, @Nullable ByteBuf data,
                             @Nullable String query, @Nullable String url, @Nullable String forwardText,
                             boolean requestWriteAccess, boolean requiresPassword,
                             boolean samePeer, @Nullable Id userId) {
        this.type = type;
        this.text = text;
        this.data = data;
        this.query = query;
        this.url = url;
        this.forwardText = forwardText;
        this.requestWriteAccess = requestWriteAccess;
        this.requiresPassword = requiresPassword;
        this.samePeer = samePeer;
        this.userId = userId;
    }

    @Override
    public KeyboardButton.Type type() {
        return type;
    }

    @Override
    public String text() {
        return text;
    }

    public Optional<ByteBuf> data() {
        return Optional.ofNullable(data).map(ByteBuf::duplicate);
    }

    public Optional<String> query() {
        return Optional.ofNullable(query);
    }

    public Optional<String> url() {
        return Optional.ofNullable(url);
    }

    public Optional<String> forwardText() {
        return Optional.ofNullable(forwardText);
    }

    public boolean requestWriteAccess() {
        return requestWriteAccess;
    }

    public boolean requiresPassword() {
        return requiresPassword;
    }

    public boolean samePeer() {
        return samePeer;
    }

    public Optional<Id> userId() {
        return Optional.ofNullable(userId);
    }

    public InlineButtonSpec withText(String value) {
        Objects.requireNonNull(value);
        if (text.equals(value)) return this;
        return new InlineButtonSpec(type, value, data, query, url, forwardText,
                requestWriteAccess, requiresPassword, samePeer, userId);
    }

    public InlineButtonSpec withData(@Nullable ByteBuf value) {
        Preconditions.requireState(type == KeyboardButton.Type.CALLBACK, () -> "unexpected type: " + type);
        if (data == value) return this;
        ByteBuf newValue = value != null ? TlEncodingUtil.copyAsUnpooled(value) : null;
        return new InlineButtonSpec(type, text, newValue, query, url, forwardText,
                requestWriteAccess, requiresPassword, samePeer, userId);
    }

    public InlineButtonSpec withData(Optional<ByteBuf> opt) {
        return withData(opt.orElse(null));
    }

    public InlineButtonSpec withQuery(@Nullable String value) {
        Preconditions.requireState(type == KeyboardButton.Type.SWITCH_INLINE, () -> "unexpected type: " + type);
        if (Objects.equals(query, value)) return this;
        return new InlineButtonSpec(type, text, data, value, url, forwardText,
                requestWriteAccess, requiresPassword, samePeer, userId);
    }

    public InlineButtonSpec withQuery(Optional<String> opt) {
        return withQuery(opt.orElse(null));
    }

    public InlineButtonSpec withUrl(@Nullable String value) {
        Preconditions.requireState(type == KeyboardButton.Type.URL_AUTH || type == KeyboardButton.Type.URL,
                () -> "unexpected type: " + type);
        if (Objects.equals(url, value)) return this;
        return new InlineButtonSpec(type, text, data, query, value, forwardText,
                requestWriteAccess, requiresPassword, samePeer, userId);
    }

    public InlineButtonSpec withUrl(Optional<String> opt) {
        return withUrl(opt.orElse(null));
    }

    public InlineButtonSpec withForwardText(@Nullable String value) {
        Preconditions.requireState(type == KeyboardButton.Type.URL_AUTH, () -> "unexpected type: " + type);
        if (Objects.equals(forwardText, value)) return this;
        return new InlineButtonSpec(type, text, data, query, url, value,
                requestWriteAccess, requiresPassword, samePeer, userId);
    }

    public InlineButtonSpec withForwardText(Optional<String> opt) {
        return withForwardText(opt.orElse(null));
    }

    public InlineButtonSpec withRequestWriteAccess(boolean value) {
        Preconditions.requireState(type == KeyboardButton.Type.URL_AUTH, () -> "unexpected type: " + type);
        if (requestWriteAccess == value) return this;
        return new InlineButtonSpec(type, text, data, query, url, forwardText,
                value, requiresPassword, samePeer, userId);
    }

    public InlineButtonSpec withRequiresPassword(boolean value) {
        Preconditions.requireState(type == KeyboardButton.Type.CALLBACK, () -> "unexpected type: " + type);
        if (requiresPassword == value) return this;
        return new InlineButtonSpec(type, text, data, query, url, forwardText,
                requestWriteAccess, value, samePeer, userId);
    }

    public InlineButtonSpec withSamePeer(boolean value) {
        Preconditions.requireState(type == KeyboardButton.Type.SWITCH_INLINE, () -> "unexpected type: " + type);
        if (samePeer == value) return this;
        return new InlineButtonSpec(type, text, data, query, url, forwardText,
                requestWriteAccess, requiresPassword, value, userId);
    }

    public InlineButtonSpec withUserId(@Nullable Id value) {
        Preconditions.requireState(type == KeyboardButton.Type.URL_AUTH || type == KeyboardButton.Type.USER_PROFILE,
                () -> "unexpected type: " + type);
        if (Objects.equals(userId, value)) return this;
        return new InlineButtonSpec(type, text, data, query, url, forwardText,
                requestWriteAccess, requiresPassword, samePeer, value);
    }

    public InlineButtonSpec withUserId(Optional<Id> opt) {
        return withUserId(opt.orElse(null));
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InlineButtonSpec that = (InlineButtonSpec) o;
        return requestWriteAccess == that.requestWriteAccess &&
                requiresPassword == that.requiresPassword &&
                samePeer == that.samePeer && type == that.type &&
                text.equals(that.text) && Objects.equals(data, that.data) &&
                Objects.equals(query, that.query) && Objects.equals(url, that.url) &&
                Objects.equals(forwardText, that.forwardText) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + type.hashCode();
        h += (h << 5) + text.hashCode();
        h += (h << 5) + Objects.hashCode(data);
        h += (h << 5) + Objects.hashCode(query);
        h += (h << 5) + Objects.hashCode(url);
        h += (h << 5) + Objects.hashCode(forwardText);
        h += (h << 5) + Boolean.hashCode(requestWriteAccess);
        h += (h << 5) + Boolean.hashCode(requiresPassword);
        h += (h << 5) + Boolean.hashCode(samePeer);
        h += (h << 5) + Objects.hashCode(userId);
        return h;
    }

    @Override
    public String toString() {
        return "InlineButtonSpec{" +
                "type=" + type +
                ", text='" + text + '\'' +
                ", data='" + (data != null ? ByteBufUtil.hexDump(data) : null) + '\'' +
                ", query='" + query + '\'' +
                ", url='" + url + '\'' +
                ", forwardText='" + forwardText + '\'' +
                ", requestWriteAccess=" + requestWriteAccess +
                ", requiresPassword=" + requiresPassword +
                ", samePeer=" + samePeer +
                ", userId=" + userId +
                '}';
    }

    public static InlineButtonSpec buy(String text) {
        return new InlineButtonSpec(KeyboardButton.Type.BUY, text);
    }

    public static InlineButtonSpec callback(String text, boolean requiresPassword, ByteBuf data) {
        return new InlineButtonSpec(KeyboardButton.Type.CALLBACK, text)
                .withRequiresPassword(requiresPassword)
                .withData(data);
    }

    public static InlineButtonSpec userProfile(String text, Id userId) {
        return new InlineButtonSpec(KeyboardButton.Type.USER_PROFILE, text)
                .withUserId(userId);
    }

    public static InlineButtonSpec urlAuth(String text, boolean requestWriteAccess,
                                           @Nullable String forwardText, String url, Id botId) {
        return new InlineButtonSpec(KeyboardButton.Type.URL_AUTH, text)
                .withRequestWriteAccess(requestWriteAccess)
                .withForwardText(Optional.ofNullable(forwardText))
                .withUrl(url)
                .withUserId(botId);
    }

    public static InlineButtonSpec game(String text) {
        return new InlineButtonSpec(KeyboardButton.Type.GAME, text);
    }

    public static InlineButtonSpec switchInline(String text, boolean samePeer, String query) {
        return new InlineButtonSpec(KeyboardButton.Type.SWITCH_INLINE, text)
                .withSamePeer(samePeer)
                .withQuery(query);
    }

    public static InlineButtonSpec url(String text, String url) {
        return new InlineButtonSpec(KeyboardButton.Type.URL, text)
                .withUrl(url);
    }
}
