package telegram4j.core.auxiliary;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Message;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;

import java.util.List;
import java.util.Optional;

public final class AuxiliaryChannelMessages extends AuxiliaryMessages {
    private final boolean inexact;
    private final int pts;
    private final int count;
    @Nullable
    private final Integer offsetId;

    public AuxiliaryChannelMessages(MTProtoTelegramClient client,
                                    boolean inexact, int pts, int count, @Nullable Integer offsetId,
                                    List<Message> messages, List<Chat> chats, List<User> users) {
        super(client, messages, chats, users);
        this.inexact = inexact;
        this.pts = pts;
        this.count = count;
        this.offsetId = offsetId;
    }

    public boolean isInexact() {
        return inexact;
    }

    public int getPts() {
        return pts;
    }

    public int getCount() {
        return count;
    }

    public Optional<Integer> getOffsetId() {
        return Optional.ofNullable(offsetId);
    }

    @Override
    public String toString() {
        return "AuxiliaryChannelMessages{" +
                "inexact=" + inexact +
                ", pts=" + pts +
                ", count=" + count +
                ", offsetId=" + offsetId +
                "} " + super.toString();
    }
}
