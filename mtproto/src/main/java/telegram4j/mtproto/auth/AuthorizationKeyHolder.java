package telegram4j.mtproto.auth;

import telegram4j.mtproto.DataCenter;

import static telegram4j.mtproto.util.CryptoUtil.sha1Digest;
import static telegram4j.mtproto.util.CryptoUtil.substring;

public class AuthorizationKeyHolder {
    private final DataCenter dc;
    private final byte[] value;
    private final byte[] id;

    public AuthorizationKeyHolder(DataCenter dc, byte[] value, byte[] id) {
        this.dc = dc;
        this.value = value;
        this.id = id;
    }

    public AuthorizationKeyHolder(DataCenter dc, byte[] value) {
        this.dc = dc;
        this.value = value;

        byte[] authKeyHash = sha1Digest(value);
        this.id = substring(authKeyHash, authKeyHash.length - 8, 8);
    }

    public DataCenter getDc() {
        return dc;
    }

    public byte[] getAuthKey() {
        return value;
    }

    public byte[] getAuthKeyId() {
        return id;
    }
}
