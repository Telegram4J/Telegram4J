package telegram4j.json.mtproto;

public interface TlObject<T extends TlObject<T>> extends TlSerializable<T> {

    int getId();
}
