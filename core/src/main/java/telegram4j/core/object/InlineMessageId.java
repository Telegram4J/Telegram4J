package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.tl.BaseInputBotInlineMessageID;
import telegram4j.tl.ImmutableInputBotInlineMessageID64;
import telegram4j.tl.InputBotInlineMessageID;
import telegram4j.tl.InputBotInlineMessageID64;

import java.util.Objects;

public final class InlineMessageId {
    private final int dcId;
    private final long ownerId;
    private final int id;
    private final long accessHash;

    private InlineMessageId(int dcId, long ownerId, int id, long accessHash) {
        this.dcId = dcId;
        this.ownerId = ownerId;
        this.id = id;
        this.accessHash = accessHash;
    }

    public static InlineMessageId from(InputBotInlineMessageID id) {
        switch (id.identifier()) {
            case InputBotInlineMessageID64.ID: {
                InputBotInlineMessageID64 d = (InputBotInlineMessageID64) id;

                return new InlineMessageId(d.dcId(), d.ownerId(), d.id(), d.accessHash());
            }
            case BaseInputBotInlineMessageID.ID: {
                BaseInputBotInlineMessageID d = (BaseInputBotInlineMessageID) id;

                long ownerId = d.id() >> 32;
                int msgId = (int) d.id();

                return new InlineMessageId(d.dcId(), ownerId, msgId, d.accessHash());
            }
            default: throw new IllegalArgumentException("Unknown input bot inline message id: " + id);
        }
    }

    public static InlineMessageId of(int dcId, long ownerId, int id, long accessHash) {
        return new InlineMessageId(dcId, ownerId, id, accessHash);
    }

    public int getDcId() {
        return dcId;
    }

    public long getOwnerId() {
        return ownerId;
    }

    public int getId() {
        return id;
    }

    public long getAccessHash() {
        return accessHash;
    }

    public ImmutableInputBotInlineMessageID64 asData() {
        return ImmutableInputBotInlineMessageID64.of(dcId, ownerId, id, accessHash);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InlineMessageId that = (InlineMessageId) o;
        return dcId == that.dcId && ownerId == that.ownerId && id == that.id && accessHash == that.accessHash;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dcId, ownerId, id, accessHash);
    }

    @Override
    public String toString() {
        return "InlineMessageId{" +
                "dcId=" + dcId +
                ", ownerId=" + ownerId +
                ", id=" + id +
                ", accessHash=" + accessHash +
                '}';
    }
}
