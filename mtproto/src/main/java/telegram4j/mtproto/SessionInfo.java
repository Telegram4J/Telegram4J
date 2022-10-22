package telegram4j.mtproto;

import reactor.util.annotation.Nullable;

public final class SessionInfo {
    private final int timeOffset;
    private final long sessionId;
    private final int seqNo;
    private final long serverSalt;
    private final long lastMessageId;

    public SessionInfo(int timeOffset, long sessionId, int seqNo, long serverSalt, long lastMessageId) {
        this.timeOffset = timeOffset;
        this.sessionId = sessionId;
        this.seqNo = seqNo;
        this.serverSalt = serverSalt;
        this.lastMessageId = lastMessageId;
    }

    /**
     * Gets calculated server time offset used in message id generation.
     *
     * @return The calculated server time offset.
     */
    public int getTimeOffset() {
        return timeOffset;
    }

    /**
     * Gets id of current mtproto session.
     *
     * @return The id of current mtproto session
     */
    public long getSessionId() {
        return sessionId;
    }

    /**
     * Gets current packet sequence number.
     *
     * @return The current packet sequence number.
     */
    public int getSeqNo() {
        return seqNo;
    }

    /**
     * Gets active server salt.
     *
     * @return The active server salt.
     */
    public long getServerSalt() {
        return serverSalt;
    }

    /**
     * Gets latest message id of sent messages.
     *
     * @return The latest message id of sent messages.
     */
    public long getLastMessageId() {
        return lastMessageId;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SessionInfo that = (SessionInfo) o;
        return timeOffset == that.timeOffset && sessionId == that.sessionId &&
                seqNo == that.seqNo && serverSalt == that.serverSalt &&
                lastMessageId == that.lastMessageId;
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + timeOffset;
        h += (h << 5) + Long.hashCode(sessionId);
        h += (h << 5) + seqNo;
        h += (h << 5) + Long.hashCode(serverSalt);
        h += (h << 5) + Long.hashCode(lastMessageId);
        return h;
    }
}
