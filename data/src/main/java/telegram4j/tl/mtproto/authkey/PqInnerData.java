package telegram4j.tl.mtproto.authkey;

import org.immutables.value.Value;
import telegram4j.json.api.tl.TlObject;

@Value.Immutable
public interface PqInnerData extends TlObject {

    static ImmutablePqInnerData.Builder builder() {
        return ImmutablePqInnerData.builder();
    }

    int ID = 0x83c95aec;

    byte[] nonce();

    byte[] serverNonce();

    byte[] p();

    byte[] q();

    @Override
    default int identifier() {
        return ID;
    }
}
