package telegram4j.tl;

import com.squareup.javapoet.TypeVariableName;
import telegram4j.tl.model.ImmutableTlParam;

import java.util.function.Supplier;

final class SchemaGeneratorConsts {

    private SchemaGeneratorConsts() {
    }

    static final TypeVariableName GENERIC_TYPE = TypeVariableName.get("T", TlObject.class);
    static final TypeVariableName GENERIC_TYPE_REF = TypeVariableName.get("T");

    static final ImmutableTlParam FLAG_PARAMETER = ImmutableTlParam.of("flags", "#");

    static final Supplier<IllegalStateException> ise = IllegalStateException::new;
}
