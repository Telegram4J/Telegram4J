package telegram4j.json.api.tl;

public final class TlTrue implements TlObject {
    public static final int ID = 0x3fedd339;
    public static final TlTrue INSTANCE = new TlTrue();

    private TlTrue() {}

    @Override
    public int identifier() {
        return ID;
    }

    public byte asByte() {
        return 1;
    }

    public boolean asBoolean() {
        return true;
    }

    @Override
    public String toString() {
        return "TlTrue{}";
    }
}
