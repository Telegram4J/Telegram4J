package telegram4j.tl.mtproto.authkey;

import org.immutables.value.Value;
import telegram4j.tl.mtproto.TlObject;

@Value.Immutable
public interface ReqDhParams extends TlObject {

    static ImmutableReqDhParams.Builder builder() {
        return ImmutableReqDhParams.builder();
    }

    int ID = 0xd712e4be;

    byte[] nonce();

    byte[] serverNonce();

    byte[] p();

    byte[] q();

    PqInnerData encryptedData();

    @Override
    default int getId() {
        return ID;
    }
}
