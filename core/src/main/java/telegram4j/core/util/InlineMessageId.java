package telegram4j.core.util;

import reactor.util.annotation.Nullable;
import telegram4j.tl.BaseInputBotInlineMessageID;
import telegram4j.tl.ImmutableInputBotInlineMessageID64;
import telegram4j.tl.InputBotInlineMessageID;
import telegram4j.tl.InputBotInlineMessageID64;

import java.util.Objects;

/**
 * One-type representation for {@link InputBotInlineMessageID inline message id}.
 * All incoming {@link BaseInputBotInlineMessageID} would be mapped as {@link InputBotInlineMessageID64} for better compatibility.
 */
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

    /**
     * Creates {@code InlineMessageId} from raw {@link InputBotInlineMessageID} data.
     *
     * @param id The raw id of inline message.
     * @return The new {@code InlineMessageId} from raw data.
     */
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

    /**
     * Creates {@code InlineMessageId} from specified parameters.
     *
     * @param dcId The id of DC which handles this inline message.
     * @param ownerId The id of owner of inline message.
     * @param id The id of inline message.
     * @param accessHash The access hash of inline message.
     * @return The new {@code InlineMessageId} from specified parameters.
     */
    public static InlineMessageId of(int dcId, long ownerId, int id, long accessHash) {
        return new InlineMessageId(dcId, ownerId, id, accessHash);
    }

    /**
     * Gets id of DC which handles this inline message.
     *
     * @return The id of DC which handles this inline message.
     */
    public int getDcId() {
        return dcId;
    }

    /**
     * Gets owner id of inline message.
     *
     * @return The owner id of inline message.
     */
    public long getOwnerId() {
        return ownerId;
    }

    /**
     * Gets id of inline message.
     *
     * @return The id of inline message.
     */
    public int getId() {
        return id;
    }

    /**
     * Gets access hash for inline message.
     *
     * @return The access hash for inline message.
     */
    public long getAccessHash() {
        return accessHash;
    }

    /**
     * Computes raw version of inline message id from these parameters.
     *
     * @return The {@link InputBotInlineMessageID64} from these parameters.
     */
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
