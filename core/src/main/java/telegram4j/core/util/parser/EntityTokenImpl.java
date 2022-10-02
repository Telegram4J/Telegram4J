package telegram4j.core.util.parser;

import reactor.util.annotation.Nullable;
import telegram4j.core.object.MessageEntity;

class EntityTokenImpl implements EntityToken {
    private final MessageEntity.Type type;
    private final int offset;
    @Nullable
    private final String arg;

    EntityTokenImpl(MessageEntity.Type type, int offset, @Nullable String arg) {
        this.type = type;
        this.offset = offset;
        this.arg = arg;
    }

    @Override
    public MessageEntity.Type type() {
        return type;
    }

    @Override
    public int position() {
        return offset;
    }

    @Nullable
    @Override
    public String arg() {
        return arg;
    }

    @Override
    public String toString() {
        return "EntityTokenImpl{" +
                "type=" + type +
                ", offset=" + offset +
                ", arg='" + arg + '\'' +
                '}';
    }
}
