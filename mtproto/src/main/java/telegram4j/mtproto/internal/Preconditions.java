package telegram4j.mtproto.internal;

import java.util.function.Supplier;

public class Preconditions {
    private Preconditions() {}

    public static void requireState(boolean expression) {
        if (!expression) {
            throw new IllegalStateException();
        }
    }

    public static void requireState(boolean expression, Supplier<String> message) {
        if (!expression) {
            throw new IllegalStateException(message.get());
        }
    }

    public static void requireArgument(boolean expression) {
        if (!expression) {
            throw new IllegalArgumentException();
        }
    }

    public static void requireArgument(boolean expression, String message) {
        if (!expression) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void requireArgument(boolean expression, Supplier<String> message) {
        if (!expression) {
            throw new IllegalArgumentException(message.get());
        }
    }
}
