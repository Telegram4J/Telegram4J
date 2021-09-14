package telegram4j.tl;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.squareup.javapoet.*;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symbol;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.immutables.value.Value;
import reactor.util.annotation.Nullable;
import telegram4j.json.api.tl.TlSerializable;
import telegram4j.json.api.tl.TlTrue;
import telegram4j.json.api.tl.TlObject;
import telegram4j.tl.model.TlConstructor;
import telegram4j.tl.model.TlParam;
import telegram4j.tl.model.TlSchema;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SupportedAnnotationTypes("telegram4j.tl.GenerateSchema")
public class SchemaGenerator extends AbstractProcessor {
    private static final int STAGE_GENERAL_SUPERCLASSES = 0x1;
    private static final int STAGE_TYPES = 0x2;
    private static final int STAGE_SERIALIZER = 0x4;

    private static final List<String> ignoredTypes = Collections.unmodifiableList(Arrays.asList(
            "bool", "true", "false", "null", "int", "long",
            "string", "flags", "vector", "#"));

    private static final ClassName LIST_CLASS_NAME = ClassName.get(List.class);
    private static final ClassName OPTIONAL_CLASS_NAME = ClassName.get(Optional.class);

    private static final String UTIL_PACKAGE = "telegram4j.tl";
    private static final String API_SCHEMA = "api.json";
    private static final String INDENT = "\t";
    private static final Pattern FLAG_PATTERN = Pattern.compile("^flags\\.(\\d+)\\?(.+)");
    private static final Pattern VECTOR_PATTERN = Pattern.compile("^[vV]ector<([A-Za-z0-9._<>]+)>$");

    private final Map<String, TlConstructor> computed = new HashMap<>();

    private Filer filer;
    private Messager messager;
    private Elements elements;
    private Types types;
    private Trees trees;
    private Symbol.PackageSymbol currentElement;
    private int progress = 0x7;

    private TlSchema schema;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
        this.elements = processingEnv.getElementUtils();
        this.types = processingEnv.getTypeUtils();
        this.trees = Trees.instance(processingEnv);

        ObjectMapper mapper = JsonMapper.builder()
                .addModules(new Jdk8Module())
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .visibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
                .visibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.PUBLIC_ONLY)
                .visibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY)
                .serializationInclusion(JsonInclude.Include.NON_ABSENT)
                .build();

        try {
            InputStream in = filer.getResource(StandardLocation.ANNOTATION_PROCESSOR_PATH, "", API_SCHEMA).openInputStream();
            schema = mapper.readValue(in, TlSchema.class);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        if (currentElement == null) {
            currentElement = (Symbol.PackageSymbol) roundEnv
                    .getElementsAnnotatedWith(GenerateSchema.class)
                    .iterator().next();
        }

        if (annotations.size() > 2) {
            messager.printMessage(Diagnostic.Kind.ERROR, "[TL parser] Generation package must be specified once!", currentElement);
            return true;
        }

        try {
            String packageName = currentElement.getQualifiedName().toString();

            // generate super types
            if ((progress & STAGE_GENERAL_SUPERCLASSES) != 0) {
                Map<String, Integer> counter = new HashMap<>();
                Set<TlConstructor> generalSuperclasses = schema.constructors().stream()
                        .peek(c -> counter.compute(normalizeName(c.type()), (s, i) -> i == null ? 0 : i + 1))
                        .filter(c -> normalizeName(c.type()).equalsIgnoreCase(normalizeName(c.predicate())) ||
                                counter.getOrDefault(normalizeName(c.type()), 0) > 2)
                        .collect(Collectors.toCollection(LinkedHashSet::new));

                for (TlConstructor constructor : generalSuperclasses) {
                    String name = normalizeName(constructor.type());
                    if (computed.containsKey(name) || ignoredTypes.contains(name.toLowerCase())) {
                        continue;
                    }

                    TypeSpec.Builder superType = TypeSpec.interfaceBuilder(name)
                            .addModifiers(Modifier.PUBLIC)
                            .addSuperinterface(TlObject.class)
                            .addAnnotation(AnnotationSpec.builder(Value.Immutable.class).build())
                            .addSuperinterface(TlSerializable.class);

                    // override #getId() method
                    superType.addField(FieldSpec.builder(TypeName.INT, "ID",
                                    Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                            .initializer("0x" + Integer.toHexString(constructor.id()))
                            .build());

                    superType.addMethod(MethodSpec.methodBuilder("identifier")
                            .addAnnotation(Override.class)
                            .returns(TypeName.INT)
                            .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                            .addCode("return ID;")
                            .build());

                    for (TlParam param : constructor.params()) {
                        if (param.type().equals("#")) {
                            continue;
                        }

                        MethodSpec.Builder method = MethodSpec.methodBuilder(formatFieldName(param.name()))
                                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                .returns(mapType(param.type()));

                        if (param.type().startsWith("flags.")) {
                            method.addAnnotation(Nullable.class);
                        }

                        superType.addMethod(method.build());
                    }

                    JavaFile.builder(packageName, superType.build())
                            .indent(INDENT)
                            .build()
                            .writeTo(filer);

                    computed.put(name, constructor);
                }

                Set<String> unrecordedSuperTypes = counter.keySet().stream()
                        .filter(integer -> generalSuperclasses.stream()
                                .noneMatch(c -> normalizeName(c.type()).equals(integer)))
                        .collect(Collectors.toSet());

                for (String name : unrecordedSuperTypes) {
                    if (ignoredTypes.contains(name.toLowerCase()) || computed.containsKey(name)) {
                        continue;
                    }

                    try {
                        JavaFile.builder(packageName, TypeSpec.interfaceBuilder(name)
                                        .addModifiers(Modifier.PUBLIC)
                                        .addSuperinterface(TlSerializable.class)
                                        .build())
                                .indent(INDENT)
                                .build()
                                .writeTo(filer);
                    } catch (Throwable ignored) {}
                    computed.put(name, null);
                }

                progress &= ~STAGE_GENERAL_SUPERCLASSES;
                return false;
            }

            // generate types
            if ((progress & STAGE_TYPES) != 0) {

                for (TlConstructor constructor : schema.constructors()) {
                    String type = normalizeName(constructor.type());
                    String alias = normalizeName(constructor.predicate());
                    if (ignoredTypes.contains(type.toLowerCase()) || ignoredTypes.contains(alias.toLowerCase()) ||
                            computed.containsKey(alias)) {
                        continue;
                    }

                    TypeSpec.Builder builder = TypeSpec.interfaceBuilder(alias)
                            .addModifiers(Modifier.PUBLIC);

                    if (!alias.equals(type)) {
                        computed.entrySet().stream()
                                .filter(e -> (e.getValue() != null ? normalizeName(e.getValue().predicate()) : e.getKey()).equals(type))
                                .map(e -> ClassName.get(packageName, type))
                                .forEach(builder::addSuperinterface);
                    }

                    builder.addSuperinterface(TlObject.class);
                    builder.addAnnotation(AnnotationSpec.builder(Value.Immutable.class).build());

                    // override #getId() method
                    builder.addField(FieldSpec.builder(TypeName.INT, "ID",
                                    Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                            .initializer("0x" + Integer.toHexString(constructor.id()))
                            .build());

                    builder.addMethod(MethodSpec.methodBuilder("identifier")
                            .addAnnotation(Override.class)
                            .returns(TypeName.INT)
                            .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                            .addCode("return ID;")
                            .build());

                    // object attributes

                    Set<TlParam> attributes = new LinkedHashSet<>(constructor.params());
                    collectAttributesRecursive(type, attributes);

                    for (TlParam param : attributes) {
                        if (param.type().equals("#")) {
                            continue;
                        }

                        MethodSpec.Builder attribute = MethodSpec.methodBuilder(formatFieldName(param.name()))
                                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                .returns(mapType(param.type()));

                        if (param.type().startsWith("flags.")) {
                            attribute.addAnnotation(Nullable.class);
                        }

                        builder.addMethod(attribute.build());
                    }

                    try {
                        JavaFile.builder(packageName, builder.build())
                                .indent(INDENT)
                                .build()
                                .writeTo(filer);
                    } catch (Throwable ignored) {
                    }

                    computed.put(alias, constructor);
                }

                progress &= ~STAGE_TYPES;
                return false;
            }

            // generate serializers
            if ((progress & STAGE_SERIALIZER) != 0) {
                MethodSpec privateConstructor = MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PRIVATE)
                        .build();

                TypeSpec.Builder serializer = TypeSpec.classBuilder("TlSerializer")
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addMethod(privateConstructor);

                TypeVariableName serializableType = TypeVariableName.get("T", TlSerializable.class);

                // special method for supertype objects
                serializer.addMethod(MethodSpec.methodBuilder("serializeExact")
                        .returns(ByteBuf.class)
                        .addTypeVariable(serializableType)
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addParameters(Arrays.asList(
                                ParameterSpec.builder(ByteBufAllocator.class, "allocator").build(),
                                ParameterSpec.builder(serializableType, "payload").build()))
                        .beginControlFlow("if (payload instanceof TlObject)")
                        .addStatement("return serialize(allocator, (TlObject) payload)")
                        .nextControlFlow("else")
                        .addStatement("throw new IllegalArgumentException(\"Unrecorded TL Serializable supertype: \" + payload.getClass())")
                        .endControlFlow()
                        .build());

                TypeVariableName generalType = TypeVariableName.get("T", TlObject.class);

                MethodSpec.Builder serializeMethod = MethodSpec.methodBuilder("serialize")
                        .returns(ByteBuf.class)
                        .addTypeVariable(generalType)
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .addParameters(Arrays.asList(
                                ParameterSpec.builder(ByteBufAllocator.class, "allocator").build(),
                                ParameterSpec.builder(generalType, "payload").build()));

                serializeMethod.beginControlFlow("switch (payload.identifier())");

                for (TlConstructor constructor : schema.constructors()) {
                    String type = normalizeName(constructor.type());
                    String alias = normalizeName(constructor.predicate());
                    String methodName = "serialize" + alias;
                    if (ignoredTypes.contains(type.toLowerCase()) || ignoredTypes.contains(alias.toLowerCase()) ||
                            serializer.methodSpecs.stream()
                                    .anyMatch(spec -> spec.name.equals(methodName))) {
                        continue;
                    }

                    serializeMethod.addCode("case $L: return $L(allocator, ($L) payload);\n",
                            "0x" + Integer.toHexString(constructor.id()),
                            methodName, alias);

                    // object attributes

                    Set<TlParam> attributes = new LinkedHashSet<>(constructor.params());
                    collectAttributesRecursive(type, attributes);

                    MethodSpec.Builder serializerMethodBuilder = MethodSpec.methodBuilder(methodName)
                            .returns(ByteBuf.class)
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                            .addParameters(Arrays.asList(
                                    ParameterSpec.builder(ByteBufAllocator.class, "allocator").build(),
                                    ParameterSpec.builder(mapType(alias), "payload").build()));

                    serializerMethodBuilder.addCode("return allocator.buffer()\n");
                    serializerMethodBuilder.addCode("\t\t.writeIntLE(payload.identifier())");

                    for (TlParam param : attributes) {
                        String paramType = normalizeName(param.type()).toLowerCase();
                        String paramName = formatFieldName(param.name());

                        String wrapping = "payload.$L()";
                        String method;
                        switch (paramType) {
                            case "true":
                                method = "writeByte";
                                wrapping = "payload.$L().asByte()";
                                break;
                            case "bool":
                                method = "writeBoolean";
                                break;
                            case "#":
                                wrapping = attributes.stream()
                                        .filter(p -> p.type().startsWith("flags."))
                                        .map(this::parseFlag)
                                        .sorted(Comparator.comparingInt(f -> f.position))
                                        .map(f -> "payload." + formatFieldName(f.name) + "()")
                                        .collect(Collectors.joining(", ", "calculateFlags(", ")"));
                            case "int":
                                method = "writeIntLE";
                                break;
                            case "long":
                                method = "writeLongLE";
                                break;
                            case "double":
                                method = "writeDoubleLE";
                                break;
                            case "string":
                                wrapping = "writeString(allocator, payload.$L())";
                            case "bytes":
                                method = "writeBytes";
                                break;
                            default:
                                Matcher vector;
                                if (paramType.startsWith("flags.")) {
                                    wrapping = "serializeFlags(allocator, payload.$L())";
                                } else if ((vector = VECTOR_PATTERN.matcher(paramType)).matches()) {;
                                    String innerType = vector.group(1);
                                    String specific = "";
                                    switch (innerType.toLowerCase()) {
                                        case "int":
                                            specific = "Int";
                                            break;
                                        case "long":
                                            specific = "Long";
                                            break;
                                        case "bytes":
                                            specific = "Bytes";
                                            break;
                                    }
                                    wrapping = "serialize" + specific + "Vector(allocator, payload.$L())";
                                } else {
                                    // TODO: use concrete method
                                    wrapping = "serializeExact(allocator, payload.$L())";
                                }
                                method = "writeBytes";
                        }

                        if (paramType.equals("#")) {
                            serializerMethodBuilder.addCode("\n\t\t.$L(" + wrapping + ")", method);
                        } else {
                            serializerMethodBuilder.addCode("\n\t\t.$L(" + wrapping + ")", method, paramName);
                        }
                    }
                    serializerMethodBuilder.addCode(";");

                    serializer.addMethod(serializerMethodBuilder.build());
                }

                serializeMethod.addCode("default: throw new IllegalArgumentException(\"Incorrect TlObject identifier: \" + payload.identifier());\n");
                serializeMethod.endControlFlow();
                serializer.addMethod(serializeMethod.build());

                try {
                    JavaFile.builder(packageName, serializer.build())
                            .addStaticImport(ClassName.get(UTIL_PACKAGE, "TlSerialUtil"),
                                    "calculateFlags", "serializeByteVector", "serializeFlags",
                                    "serializeIntVector", "serializeLongVector", "serializeVector",
                                    "writeString")
                            .indent(INDENT)
                            .build()
                            .writeTo(filer);
                } catch (Throwable ignored) {
                }

                progress &= ~STAGE_TYPES;
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return true;
    }

    private static String camelize(String type) {
        if (!type.contains("_")) {
            return type;
        }

        StringBuilder builder = new StringBuilder(type.length());
        for (int i = 0; i < type.length(); i++) {
            char c = type.charAt(i);
            if (c == '_') {
                char n = Character.toUpperCase(type.charAt(i++ + 1));
                builder.append(n);
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    private static String normalizeName(String type) {
        Matcher flag = FLAG_PATTERN.matcher(type);
        if (flag.matches()) {
            return type;
        }

        int dotIdx = type.indexOf('.');
        if (dotIdx != -1) {
            type = type.substring(dotIdx + 1);
        }

        int spaceIdx = type.indexOf(' ');
        if (spaceIdx != -1) {
            type = type.substring(0, spaceIdx);
        }

        char first = type.charAt(0);
        if (Character.isLowerCase(first)) {
            type = Character.toUpperCase(first) + type.substring(1);
        }

        return type;
    }

    private static String formatFieldName(String type) {
        type = camelize(type);

        // keyword handling
        if (!SourceVersion.isName(type)) {
            type += "State";
        }

        return type;
    }

    private TypeName mapType(String type) {
        if (type.equalsIgnoreCase("int")) {
            return TypeName.INT;
        }
        if (type.equalsIgnoreCase("true")) {
            return ClassName.get(TlTrue.class);
        }
        if (type.equalsIgnoreCase("bool")) {
            return TypeName.BOOLEAN;
        }
        if (type.equalsIgnoreCase("long")) {
            return TypeName.LONG;
        }
        if (type.equalsIgnoreCase("double")) {
            return TypeName.DOUBLE;
        }
        if (type.equalsIgnoreCase("bytes")) {
            return TypeName.get(byte[].class);
        }
        if (type.equalsIgnoreCase("string")) {
            return TypeName.get(String.class);
        }
        Matcher flag = FLAG_PATTERN.matcher(type);
        if (flag.matches()) {
            TypeName innerType = mapType(flag.group(2));
            return innerType.box();
        }

        Matcher vector = VECTOR_PATTERN.matcher(type);
        if (vector.matches()) {
            String template = normalizeName(vector.group(1));
            TypeName templateType = mapType(template);
            return ParameterizedTypeName.get(LIST_CLASS_NAME, templateType.box());
        }

        return ClassName.get(getPackageName(), normalizeName(type));
    }

    private void collectAttributesRecursive(String type, Set<TlParam> params) {
        type = normalizeName(type);
        TlConstructor constructor = computed.get(type);
        if (constructor == null || normalizeName(constructor.type()).equals(type)) {
            return;
        }
        params.addAll(constructor.params());
        collectAttributesRecursive(constructor.type(), params);
    }

    private String getPackageName() {
        return currentElement.getQualifiedName().toString();
    }

    private Flag parseFlag(TlParam param) {
        Matcher matcher = FLAG_PATTERN.matcher(param.type());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Incorrect flag type: " + param.name() + "#" + param.type());
        }

        int position = Integer.parseInt(matcher.group(1));
        TypeName type = mapType(matcher.group(2));
        return new Flag(position, param.name(), type);
    }

    static class Flag {
        private final int position;
        private final String name;
        private final TypeName type;

        Flag(int position, String name, TypeName type) {
            this.position = position;
            this.name = name;
            this.type = type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Flag flag = (Flag) o;
            return position == flag.position && name.equals(flag.name) && type.equals(flag.type);
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
}
