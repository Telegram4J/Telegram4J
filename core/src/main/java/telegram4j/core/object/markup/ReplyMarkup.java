package telegram4j.core.object.markup;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Message;
import telegram4j.core.object.TelegramObject;
import telegram4j.core.util.BitFlag;
import telegram4j.tl.*;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ReplyMarkup implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.ReplyMarkup data;

    public ReplyMarkup(MTProtoTelegramClient client, telegram4j.tl.ReplyMarkup data) {
        this.client = Objects.requireNonNull(client);
        this.data = Objects.requireNonNull(data);
    }

    /**
     * Gets type of reply markup.
     *
     * @return The {@link Type} of reply markup.
     */
    public Type getType() {
        return Type.of(data);
    }

    /**
     * Gets {@link EnumSet} of markup flags.
     *
     * @return The {@link EnumSet} of markup flags.
     */
    public EnumSet<Flag> getFlags() {
        return Flag.of(data);
    }

    /**
     * Gets nested mutable lists with reply {@link KeyboardButton buttons} if {@link #getType() type}
     * is {@link Type#KEYBOARD} or {@link Type#INLINE}.
     *
     * @return The nested mutable lists with {@link KeyboardButton reply buttons}.
     */
    public Optional<List<List<KeyboardButton>>> getButtons() {
        List<KeyboardButtonRow> rows;
        switch (data.identifier()) {
            case ReplyInlineMarkup.ID -> rows = ((ReplyInlineMarkup) data).rows();
            case ReplyKeyboardMarkup.ID -> rows = ((ReplyKeyboardMarkup) data).rows();
            case ReplyKeyboardForceReply.ID, ReplyKeyboardHide.ID -> {
                return Optional.empty();
            }
            default -> throw new IllegalArgumentException("Unexpected ReplyMarkup type: " + data);
        }

        return Optional.of(rows.stream()
                .map(r -> r.buttons().stream()
                        .map(b -> new KeyboardButton(client, b))
                        .collect(Collectors.toList()))
                .collect(Collectors.toList()));
    }

    /**
     * Gets the text which will be displayed in the ui
     * input field when the keyboard is active, if {@link #getType() type} is
     * {@link Type#KEYBOARD} or {@link Type#FORCE_REPLY} and present.
     *
     * @return The text which will be displayed in the input field, if present.
     */
    public Optional<String> getPlaceholder() {
        return switch (data.identifier()) {
            case ReplyKeyboardMarkup.ID -> Optional.ofNullable(((ReplyKeyboardMarkup) data).placeholder());
            case ReplyKeyboardForceReply.ID -> Optional.ofNullable(((ReplyKeyboardForceReply) data).placeholder());
            case ReplyInlineMarkup.ID, ReplyKeyboardHide.ID -> Optional.empty();
            default -> throw new IllegalArgumentException("Unexpected ReplyMarkup type: " + data);
        };
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    public enum Type {
        INLINE,
        FORCE_REPLY,
        HIDE,
        KEYBOARD;

        public static Type of(telegram4j.tl.ReplyMarkup data) {
            return switch (data.identifier()) {
                case ReplyInlineMarkup.ID -> INLINE;
                case ReplyKeyboardForceReply.ID -> FORCE_REPLY;
                case ReplyKeyboardHide.ID -> HIDE;
                case ReplyKeyboardMarkup.ID -> KEYBOARD;
                default -> throw new IllegalArgumentException("Unexpected ReplyMarkup type: " + data);
            };
        }
    }

    public enum Flag implements BitFlag {

        /** Whether this keyboard is resizing after flipping device. */
        RESIZE(ReplyKeyboardMarkup.RESIZE_POS),

        /** Whether when the keyboard becomes unavailable after pressing the button. */
        SINGLE_USE(ReplyKeyboardMarkup.SINGLE_USE_POS),

        /**
         * Whether this keyboard is only for specific users selected
         * via @mention in the {@link Message#getContent() text} or via {@link Message#getReplyTo() message reply}.
         */
        SELECTIVE(ReplyKeyboardMarkup.SELECTIVE_POS),

        PERSISTENT(ReplyKeyboardMarkup.PERSISTENT_POS);

        private final byte position;

        Flag(byte position) {
            this.position = position;
        }

        @Override
        public byte position() {
            return position;
        }

        public static EnumSet<Flag> of(telegram4j.tl.ReplyMarkup data) {
            if (data instanceof ReplyInlineMarkup)
                return EnumSet.noneOf(Flag.class);

            int flags = switch (data.identifier()) {
                case ReplyKeyboardForceReply.ID -> ((ReplyKeyboardForceReply) data).flags();
                case ReplyKeyboardHide.ID -> ((ReplyKeyboardHide) data).flags();
                case ReplyKeyboardMarkup.ID -> ((ReplyKeyboardMarkup) data).flags();
                default -> throw new IllegalArgumentException("Unexpected ReplyMarkup type: " + data);
            };

            var set = EnumSet.allOf(Flag.class);
            set.removeIf(f -> (flags & f.mask()) == 0);
            return set;
        }
    }
}
