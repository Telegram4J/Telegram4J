package telegram4j.core.auxiliary;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Message;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.util.Id;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AuxiliaryMessagesSlice extends AuxiliaryMessages {

    private final boolean inexact;
    private final int count;
    @Nullable
    private final Integer nextRate;
    @Nullable
    private final Integer offsetId;

    public AuxiliaryMessagesSlice(MTProtoTelegramClient client, List<Message> messages, Map<Id, Chat> chats, Map<Id, User> users,
                                  boolean inexact, int count, @Nullable Integer nextRate, @Nullable Integer offsetId) {
        super(client, messages, chats, users);
        this.inexact = inexact;
        this.count = count;
        this.nextRate = nextRate;
        this.offsetId = offsetId;
    }

    public boolean isInexact() {
        return inexact;
    }

    public int getCount() {
        return count;
    }

    public Optional<Integer> getNextRate() {
        return Optional.ofNullable(nextRate);
    }

    public Optional<Integer> getOffsetId() {
        return Optional.ofNullable(offsetId);
    }

    @Override
    public String toString() {
        return "AuxiliaryMessagesSlice{" +
                "inexact=" + inexact +
                ", count=" + count +
                ", nextRate=" + nextRate +
                ", offsetId=" + offsetId +
                "} " + super.toString();
    }
}
