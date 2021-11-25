package telegram4j.tl.model;

import com.squareup.javapoet.TypeName;

import java.util.Objects;

public class Flag {
    private final int position;
    private final TlParam param;
    private final TypeName type;

    public Flag(int position, TlParam param, TypeName type) {
        this.position = position;
        this.param = param;
        this.type = type;
    }

    public int getPosition() {
        return position;
    }

    public TlParam getParam() {
        return param;
    }

    public TypeName getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Flag flag = (Flag) o;
        return position == flag.position
                && param.equals(flag.param)
                && type.equals(flag.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, param, type);
    }

    @Override
    public String toString() {
        return "Flag{" +
                "position=" + position +
                ", param=" + param +
                ", type=" + type +
                '}';
    }
}
