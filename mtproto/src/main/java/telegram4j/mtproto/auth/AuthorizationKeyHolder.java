package telegram4j.mtproto.auth;

import static telegram4j.mtproto.util.CryptoUtil.*;

public class AuthorizationKeyHolder {
    private final byte[] value;
    private final byte[] id;

    public AuthorizationKeyHolder(byte[] value, byte[] id) {
        this.value = value;
        this.id = id;
    }

    public AuthorizationKeyHolder(byte[] value) {
        this.value = value;

        byte[] authKeyHash = sha1Digest(value);
        this.id = substring(authKeyHash, authKeyHash.length - 8, 8);
    }

    public byte[] getAuthKey() {
        return value;
    }

    public byte[] getAuthKeyId() {
        return id;
    }
}
