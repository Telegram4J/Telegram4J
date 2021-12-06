package telegram4j.tl;

import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeVariableName;
import telegram4j.tl.model.ImmutableTlParam;

import javax.lang.model.element.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;

final class SchemaGeneratorConsts {

    private SchemaGeneratorConsts() {
    }

    static final Pattern FLAG_PATTERN = Pattern.compile("^flags\\.(\\d+)\\?(.+)$");
    static final Pattern VECTOR_PATTERN = Pattern.compile("^[vV]ector<%?([A-Za-z0-9._<>]+)>$");

    static final List<String> ignoredTypes = Arrays.asList(
            "bool", "true", "false", "null", "vector",
            "jsonvalue", "jsonobjectvalue", "httpwait");

    static final List<String> primitiveTypes = Arrays.asList(
            "bool", "true", "vector", "jsonvalue", "jsonobjectvalue");

    static final TypeVariableName genericType = TypeVariableName.get("T", TlObject.class);
    static final TypeVariableName genericTypeRef = TypeVariableName.get("T");

    static final ImmutableTlParam flagParameter = ImmutableTlParam.of("flags", "#");

    static final Supplier<IllegalStateException> ise = IllegalStateException::new;

    static final MethodSpec privateConstructor = MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PRIVATE)
            .build();

    static final List<NameTransformer> namingExceptions = Arrays.asList(
            NameTransformer.create("messages.StickerSet", "messages.StickerSetWithDocuments"));
}
