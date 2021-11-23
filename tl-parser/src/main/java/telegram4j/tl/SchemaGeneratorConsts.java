package telegram4j.tl;

import com.squareup.javapoet.TypeVariableName;

import java.util.function.Supplier;

final class SchemaGeneratorConsts {

    private SchemaGeneratorConsts() {
    }

    static final TypeVariableName GENERIC_TYPE = TypeVariableName.get("T", TlObject.class);
    static final TypeVariableName GENERIC_TYPE_REF = TypeVariableName.get("T");

    static final Supplier<IllegalStateException> ise = IllegalStateException::new;
}
