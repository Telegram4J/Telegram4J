package telegram4j.mtproto;

import telegram4j.json.api.tl.TlObject;

public class UnencryptedData<T extends TlObject> {

    private final long authKeyId;
    private final long messageId;
    private final int messageLength;
    private final T data;

    public UnencryptedData(long authKeyId, long messageId, int messageLength, T data) {
        this.authKeyId = authKeyId;
        this.messageId = messageId;
        this.messageLength = messageLength;
        this.data = data;
    }

    public long getAuthKeyId() {
        return authKeyId;
    }

    public long getMessageId() {
        return messageId;
    }

    public int getMessageLength() {
        return messageLength;
    }

    public T getData() {
        return data;
    }
}
