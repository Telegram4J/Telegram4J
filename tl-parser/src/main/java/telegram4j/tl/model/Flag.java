package telegram4j.tl.model;

import com.squareup.javapoet.TypeName;

import java.util.Objects;

public class Flag {
    private final int position;
    private final String name;
    private final TypeName type;

    public Flag(int position, String name, TypeName type) {
        this.position = position;
        this.name = name;
        this.type = type;
    }

    public int getPosition() {
        return position;
    }

    public String getName() {
        return name;
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
                && name.equals(flag.name)
                && type.equals(flag.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position, name, type);
    }

    @Override
    public String toString() {
        return "Flag{" +
                "position=" + position +
                ", name='" + name + '\'' +
                ", type=" + type +
                '}';
    }
}
