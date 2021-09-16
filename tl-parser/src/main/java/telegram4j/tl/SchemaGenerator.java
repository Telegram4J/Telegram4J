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
import telegram4j.tl.model.TlConstructor;
import telegram4j.tl.model.TlMethod;
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
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SupportedAnnotationTypes("telegram4j.tl.GenerateSchema")
public class SchemaGenerator extends AbstractProcessor {
    private static final int STAGE_SUPERTYPES = 0x1;
    private static final int STAGE_TYPES = 0x2;
    private static final int STAGE_METHODS = 0x4;
    private static final int STAGE_SERIALIZER = 0x8;
    private static final int STAGE_DESERIALIZER = 0x10;

    private static final List<String> ignoredTypes = Collections.unmodifiableList(Arrays.asList(
            "bool", "true", "false", "null", "int", "long",
            "string", "flags", "vector", "#"));

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
    private int progress = 0x1f;

    private TlSchema schema;
    private Map<String, List<TlConstructor>> typeTree;

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

        typeTree = schema.constructors().stream()
                .filter(c -> !ignoredTypes.contains(normalizeName(c.type()).toLowerCase()))
                .collect(Collectors.groupingBy(c -> normalizeName(c.type())));
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

        String packageName = currentElement.getQualifiedName().toString();

        try {
            // region constructors

            if ((progress & STAGE_SUPERTYPES) != 0) {
                Map<String, Set<TlParam>> sharedParams = typeTree.entrySet().stream()
                        .filter(e -> e.getValue().size() > 1)
                        .collect(Collectors.groupingBy(Map.Entry::getKey,
                                flatMapping(e -> e.getValue().stream()
                                                .flatMap(c -> c.params().stream())
                                                .filter(p -> e.getValue().stream()
                                                        .allMatch(c -> c.params().contains(p))),
                                        Collectors.toCollection(LinkedHashSet::new))));

                for (Map.Entry<String, Set<TlParam>> e : sharedParams.entrySet()) {
                    String name = e.getKey();
                    Set<TlParam> params = e.getValue();
                    if (computed.containsKey(name) || ignoredTypes.contains(name.toLowerCase())) {
                        continue;
                    }

                    TypeSpec.Builder superType = TypeSpec.interfaceBuilder(name)
                            .addModifiers(Modifier.PUBLIC)
                            .addSuperinterface(TlSerializable.class);

                    for (TlParam param : params) {
                        if (param.type().equals("#")) {
                            continue;
                        }

                        TypeName paramType = parseType(param.type());
                        if (param.name().equals("value") && name.equals("JSONValue")) { // TODO: create tl-serializable Jackson' JsonNode subtype
                            paramType = TypeName.OBJECT;
                        }

                        boolean optionalInExt = typeTree.get(name).stream()
                                .flatMap(c -> c.params().stream())
                                .anyMatch(p -> p.type().startsWith("flags.") &&
                                        p.name().equals(param.name()));

                        if (optionalInExt) {
                            paramType = paramType.box();
                        }

                        MethodSpec.Builder attribute = MethodSpec.methodBuilder(formatFieldName(param.name()))
                                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                .returns(paramType);

                        if (param.type().startsWith("flags.")) {
                            attribute.addAnnotation(Nullable.class);
                        }

                        superType.addMethod(attribute.build());
                    }

                    JavaFile file = JavaFile.builder(packageName, superType.build())
                            .indent(INDENT)
                            .build();

                    writeTo(file);
                }

                progress &= ~STAGE_SUPERTYPES;
            }

            if ((progress & STAGE_TYPES) != 0) {

                for (TlConstructor constructor : schema.constructors()) {
                    String type = normalizeName(constructor.type());
                    String alias = normalizeName(constructor.predicate());

                    boolean multiple = typeTree.getOrDefault(type, Collections.emptyList()).size() > 1;

                    // add Base* prefix to prevent matching with supertype name, e.g. SecureValueError
                    if (type.equalsIgnoreCase(alias) && multiple) {
                        alias = "Base" + alias;
                    } else if (!multiple) { // use type name if this object type is singleton
                        alias = type;
                    }

                    if (ignoredTypes.contains(type.toLowerCase()) || computed.containsKey(alias)) {
                        continue;
                    }

                    TypeSpec.Builder builder = TypeSpec.interfaceBuilder(alias)
                            .addModifiers(Modifier.PUBLIC);

                    String alias0 = alias;
                    Set<ClassName> superTypes = typeTree.get(type).stream()
                            .map(t -> normalizeName(t.type()))
                            .filter(s -> !s.equals(alias0))
                            .map(t -> ClassName.get(packageName, t))
                            .collect(Collectors.toSet());

                    builder.addSuperinterfaces(superTypes);

                    builder.addSuperinterface(TlObject.class);

                    builder.addField(FieldSpec.builder(TypeName.INT, "ID",
                                    Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                            .initializer("0x" + Integer.toHexString(constructor.id()))
                            .build());

                    builder.addMethod(MethodSpec.methodBuilder("builder")
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                            .returns(ClassName.get(packageName, "Immutable" + alias, "Builder"))
                            .addCode("return Immutable$L.builder();", alias)
                            .build());

                    Set<TlParam> attributes = new LinkedHashSet<>(constructor.params());
                    collectAttributesRecursive(type, attributes);

                    boolean optional = attributes.stream().allMatch(p -> p.type().startsWith("flags."));
                    AnnotationSpec.Builder value = AnnotationSpec.builder(Value.Immutable.class);
                    if (optional) {
                        value.addMember("singleton", "true");

                        builder.addMethod(MethodSpec.methodBuilder("instance")
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .returns(ClassName.get(packageName, "Immutable" + alias))
                                .addCode("return Immutable$L.of();", alias)
                                .build());
                    }
                    builder.addAnnotation(value.build());

                    builder.addMethod(MethodSpec.methodBuilder("identifier")
                            .addAnnotation(Override.class)
                            .returns(TypeName.INT)
                            .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                            .addCode("return ID;")
                            .build());

                    for (TlParam param : attributes) {
                        if (param.type().equals("#")) {
                            continue;
                        }

                        TypeName paramType = parseType(param.type());

                        boolean optionalInExt = typeTree.get(type).stream()
                                .flatMap(c -> c.params().stream())
                                .anyMatch(p -> p.type().startsWith("flags.") &&
                                        p.name().equals(param.name()));

                        if (optionalInExt || type.equals("JSONValue") && param.name().equals("value")) {
                            paramType = paramType.box();
                        }

                        MethodSpec.Builder attribute = MethodSpec.methodBuilder(formatFieldName(param.name()))
                                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                .returns(paramType);

                        if (param.type().startsWith("flags.")) {
                            attribute.addAnnotation(Nullable.class);
                        }

                        builder.addMethod(attribute.build());
                    }

                    JavaFile file = JavaFile.builder(packageName, builder.build())
                            .indent(INDENT)
                            .build();

                    writeTo(file);

                    computed.put(alias, constructor);
                }

                progress &= ~STAGE_TYPES;
                return false;
            }

            // endregion

            // region methods

            TypeVariableName genericType = TypeVariableName.get("T", TlObject.class);
            TypeVariableName genericTypeRef = TypeVariableName.get("T");
            if ((progress & STAGE_METHODS) != 0) {

                for (TlMethod method : schema.methods()) {
                    String name = normalizeName(method.method());
                    boolean generic = !method.method().contains(".");
                    String returnTypeName = normalizeName(method.type());
                    TypeName returnType = parseType(returnTypeName);

                    TypeSpec.Builder type = TypeSpec.interfaceBuilder(name);

                    if (generic) {
                        type.addTypeVariable(genericType);
                    }

                    type.addSuperinterface(ParameterizedTypeName.get(ClassName.get(telegram4j.tl.TlMethod.class), returnType.box()));

                    type.addField(FieldSpec.builder(TypeName.INT, "ID",
                                    Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                            .initializer("0x" + Integer.toHexString(method.id()))
                            .build());

                    ClassName immutableTypeRaw = ClassName.get(packageName, "Immutable" + name);
                    ClassName immutableTypeBuilderRaw = ClassName.get(packageName, "Immutable" + name, "Builder");
                    TypeName immutableType = generic ? ParameterizedTypeName.get(immutableTypeRaw, genericTypeRef) : immutableTypeRaw;
                    TypeName immutableBuilderType = generic ? ParameterizedTypeName.get(immutableTypeBuilderRaw, genericTypeRef) : immutableTypeBuilderRaw;

                    MethodSpec.Builder builder = MethodSpec.methodBuilder("builder")
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                            .returns(immutableBuilderType)
                            .addCode("return $T.builder();", immutableTypeRaw);

                    if (generic) {
                        builder.addTypeVariable(genericType);
                    }

                    type.addMethod(builder.build());

                    AnnotationSpec.Builder value = AnnotationSpec.builder(Value.Immutable.class);

                    boolean allOptional = method.params().stream().allMatch(p -> p.type().startsWith("flags."));
                    if (allOptional) {
                        value.addMember("singleton", "true");

                        MethodSpec.Builder instance = MethodSpec.methodBuilder("instance")
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .returns(immutableType)
                                .addCode("return $T.of();", immutableTypeRaw);

                        if (generic) {
                            instance.addTypeVariable(genericType);
                        }

                        type.addMethod(instance.build());
                    }

                    type.addAnnotation(value.build());

                    type.addMethod(MethodSpec.methodBuilder("identifier")
                            .addAnnotation(Override.class)
                            .returns(TypeName.INT)
                            .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                            .addCode("return ID;")
                            .build());

                    for (TlParam param : method.params()) {
                        if (param.type().equals("#")) {
                            continue;
                        }

                        TypeName paramType = parseType(param.type());

                        MethodSpec.Builder attribute = MethodSpec.methodBuilder(formatFieldName(param.name()))
                                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                .returns(paramType);

                        if (param.type().startsWith("flags.")) {
                            attribute.addAnnotation(Nullable.class);
                        }

                        type.addMethod(attribute.build());
                    }

                    JavaFile file = JavaFile.builder(packageName, type.build())
                            .indent(INDENT)
                            .build();

                    writeTo(file);
                }

                progress &= ~STAGE_METHODS;
            }

            // endregion

            // region serializer

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
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addParameters(Arrays.asList(
                                ParameterSpec.builder(ByteBufAllocator.class, "allocator").build(),
                                ParameterSpec.builder(generalType, "payload").build()));

                serializeMethod.beginControlFlow("switch (payload.identifier())");

                for (TlConstructor constructor : schema.constructors()) {
                    String type = normalizeName(constructor.type());
                    String alias = normalizeName(constructor.predicate());

                    boolean multiple = typeTree.getOrDefault(type, Collections.emptyList()).size() > 1;

                    if (type.equalsIgnoreCase(alias) && multiple) {
                        alias = "Base" + alias;
                    } else if (!multiple) {
                        alias = type;
                    }

                    String methodName = "serialize" + alias;

                    if (ignoredTypes.contains(type.toLowerCase()) ||
                            serializer.methodSpecs.stream().anyMatch(spec -> spec.name.equals(methodName))) {
                        continue;
                    }

                    serializeMethod.addCode("case $L: return $L(allocator, ($L) payload);\n",
                            "0x" + Integer.toHexString(constructor.id()),
                            methodName, alias);

                    Set<TlParam> attributes = new LinkedHashSet<>(constructor.params());
                    collectAttributesRecursive(type, attributes);

                    MethodSpec.Builder serializerMethodBuilder = MethodSpec.methodBuilder(methodName)
                            .returns(ByteBuf.class)
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                            .addParameters(Arrays.asList(
                                    ParameterSpec.builder(ByteBufAllocator.class, "allocator").build(),
                                    ParameterSpec.builder(parseType(alias), "payload").build()));

                    serializerMethodBuilder.addCode("return allocator.buffer()\n");
                    serializerMethodBuilder.addCode("\t\t.writeIntLE(payload.identifier())");

                    for (TlParam param : attributes) {
                        String paramType = normalizeName(param.type());

                        boolean multipleParam = typeTree.getOrDefault(paramType, Collections.emptyList()).size() > 1;
                        if (multipleParam) {
                            paramType = "base" + paramType;
                        }

                        paramType = paramType.toLowerCase();
                        String paramName = formatFieldName(param.name());

                        String wrapping = "payload.$L()";
                        String method;
                        switch (paramType) {
                            case "true":
                                method = null;
                                break;
                            case "bool":
                                method = "writeIntLE";
                                wrapping = "payload.$L() ? BOOL_TRUE_ID : BOOL_FALSE_ID";
                                break;
                            case "#":
                                wrapping = attributes.stream()
                                        .filter(p -> p.type().startsWith("flags."))
                                        .map(this::parseFlag)
                                        .map(f -> "(payload." + formatFieldName(f.name) +
                                                "() != null ? 1 : 0) << " + f.position)
                                        .collect(Collectors.joining(" | "));
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
                            case "bytes":
                                wrapping = "writeString(allocator, payload.$L())";
                                method = "writeBytes";
                                break;
                            default:
                                Matcher vector = VECTOR_PATTERN.matcher(paramType);
                                if (paramType.startsWith("flags.")) {
                                    if (paramType.endsWith("true")) {
                                        method = null;
                                        break;
                                    }
                                    wrapping = "serializeFlags(allocator, payload.$L())";
                                } else if (vector.matches()) {
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
                                        case "string":
                                            specific = "String";
                                            break;
                                    }
                                    wrapping = "serialize" + specific + "Vector(allocator, payload.$L())";
                                } else {
                                    // TODO: use concrete method
                                    wrapping = "serializeExact(allocator, payload.$L())";
                                }
                                method = "writeBytes";
                        }

                        if (method != null) {
                            if (paramType.equals("#")) {
                                serializerMethodBuilder.addCode("\n\t\t.$L(" + wrapping + ")", method);
                            } else {
                                serializerMethodBuilder.addCode("\n\t\t.$L(" + wrapping + ")", method, paramName);
                            }
                        }
                    }
                    serializerMethodBuilder.addCode(";");

                    serializer.addMethod(serializerMethodBuilder.build());
                }

                for (TlMethod method : schema.methods()) {
                    String name = normalizeName(method.method());
                    String methodName = "serialize" + name;

                    if (serializer.methodSpecs.stream().anyMatch(spec -> spec.name.equals(methodName))) {
                        continue;
                    }

                    serializeMethod.addCode("case $L: return $L(allocator, ($L) payload);\n",
                            "0x" + Integer.toHexString(method.id()),
                            methodName, name);

                    MethodSpec.Builder serializerBuilder = MethodSpec.methodBuilder(methodName)
                            .returns(ByteBuf.class)
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                            .addParameters(Arrays.asList(
                                    ParameterSpec.builder(ByteBufAllocator.class, "allocator").build(),
                                    ParameterSpec.builder(parseType(name), "payload").build()));

                    serializerBuilder.addCode("return allocator.buffer()\n");
                    serializerBuilder.addCode("\t\t.writeIntLE(payload.identifier())");

                    for (TlParam param : method.params()) {
                        String paramTypeName = normalizeName(param.type());

                        boolean multipleParam = typeTree.getOrDefault(paramTypeName, Collections.emptyList()).size() > 1;
                        if (multipleParam) {
                            paramTypeName = "base" + paramTypeName;
                        }

                        paramTypeName = paramTypeName.toLowerCase();
                        String paramName = formatFieldName(param.name());

                        String wrapping = "payload.$L()";
                        String method0;
                        switch (paramTypeName) {
                            case "true":
                                method0 = null;
                                break;
                            case "bool":
                                method0 = "writeIntLE";
                                wrapping = "payload.$L() ? BOOL_TRUE_ID : BOOL_FALSE_ID";
                                break;
                            case "#":
                                wrapping = method.params().stream()
                                        .filter(p -> p.type().startsWith("flags."))
                                        .map(this::parseFlag)
                                        .map(f -> "(payload." + formatFieldName(f.name) +
                                                "() != null ? 1 : 0) << " + f.position)
                                        .collect(Collectors.joining(" | "));
                            case "int":
                                method0 = "writeIntLE";
                                break;
                            case "long":
                                method0 = "writeLongLE";
                                break;
                            case "double":
                                method0 = "writeDoubleLE";
                                break;
                            case "string":
                            case "bytes":
                                wrapping = "writeString(allocator, payload.$L())";
                                method0 = "writeBytes";
                                break;
                            default:
                                Matcher vector = VECTOR_PATTERN.matcher(paramTypeName);
                                if (paramTypeName.startsWith("flags.")) {
                                    if (paramTypeName.endsWith("true")) {
                                        method0 = null;
                                        break;
                                    }
                                    wrapping = "serializeFlags(allocator, payload.$L())";
                                } else if (vector.matches()) {
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
                                        case "string":
                                            specific = "String";
                                            break;
                                    }
                                    wrapping = "serialize" + specific + "Vector(allocator, payload.$L())";
                                } else {
                                    // TODO: use concrete method
                                    wrapping = "serializeExact(allocator, payload.$L())";
                                }
                                method0 = "writeBytes";
                        }

                        if (method0 != null) {
                            if (paramTypeName.equals("#")) {
                                serializerBuilder.addCode("\n\t\t.$L(" + wrapping + ")", method0);
                            } else {
                                serializerBuilder.addCode("\n\t\t.$L(" + wrapping + ")", method0, paramName);
                            }
                        }
                    }
                    serializerBuilder.addCode(";");

                    serializer.addMethod(serializerBuilder.build());
                }

                serializeMethod.addCode("default: throw new IllegalArgumentException(\"Incorrect TlObject identifier: \" + payload.identifier());\n");
                serializeMethod.endControlFlow();
                serializer.addMethod(serializeMethod.build());

                JavaFile file = JavaFile.builder(packageName, serializer.build())
                        .addStaticImport(ClassName.get(UTIL_PACKAGE, "TlSerialUtil"),
                                "BOOL_FALSE_ID", "BOOL_TRUE_ID",
                                "serializeBytesVector", "serializeFlags",
                                "serializeIntVector", "serializeLongVector", "serializeStringVector",
                                "serializeVector", "writeString")
                        .indent(INDENT)
                        .build();

                writeTo(file);

                progress &= ~STAGE_SERIALIZER;
                return false;
            }

            if ((progress & STAGE_DESERIALIZER) != 0) {
                MethodSpec privateConstructor = MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PRIVATE)
                        .build();

                TypeSpec.Builder deserializer = TypeSpec.classBuilder("TlDeserializer")
                        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .addMethod(privateConstructor);

                TypeVariableName generalType = TypeVariableName.get("T", TlSerializable.class);

                MethodSpec.Builder serializeMethod = MethodSpec.methodBuilder("deserialize")
                        .returns(generalType)
                        .addTypeVariable(generalType)
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addParameter(ParameterSpec.builder(ByteBuf.class, "payload").build());

                serializeMethod.addStatement("int identifier = payload.readIntLE()");
                serializeMethod.beginControlFlow("switch (identifier)");

                for (TlConstructor constructor : schema.constructors()) {
                    String type = normalizeName(constructor.type());
                    String alias = normalizeName(constructor.predicate());

                    boolean multiple = typeTree.getOrDefault(type, Collections.emptyList()).size() > 1;

                    if (type.equalsIgnoreCase(alias) && multiple) {
                        alias = "Base" + alias;
                    } else if (!multiple) {
                        alias = type;
                    }

                    String methodName = "deserialize" + alias;

                    if (ignoredTypes.contains(type.toLowerCase()) ||
                            deserializer.methodSpecs.stream().anyMatch(spec -> spec.name.equals(methodName))) {
                        continue;
                    }

                    serializeMethod.addCode("case $L: return (T) $L(payload);\n",
                            "0x" + Integer.toHexString(constructor.id()), methodName);

                    Set<TlParam> attributes = new LinkedHashSet<>(constructor.params());
                    collectAttributesRecursive(type, attributes);

                    ClassName typeName = ClassName.get(packageName, alias);
                    MethodSpec.Builder deserializerBuilder = MethodSpec.methodBuilder(methodName)
                            .returns(typeName)
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                            .addParameter(ParameterSpec.builder(ByteBuf.class, "payload").build());

                    boolean withFlags = attributes.stream().anyMatch(p -> p.type().equals("#"));
                    if (withFlags) {
                        ClassName builder = ClassName.get(packageName, "Immutable" + alias, "Builder");
                        deserializerBuilder.addCode("$T builder = $T.builder()", builder, typeName);
                    } else {
                        if (attributes.isEmpty()) {
                            deserializerBuilder.addCode("return $T.instance();", typeName);
                        } else {
                            deserializerBuilder.addCode("return $T.builder()", typeName);
                        }
                    }

                    for (TlParam param : attributes) {
                        String paramType = normalizeName(param.type());

                        boolean multipleParam = typeTree.getOrDefault(paramType, Collections.emptyList()).size() > 1;
                        if (multipleParam) {
                            paramType = "base" + paramType;
                        }

                        String paramName = formatFieldName(param.name());
                        if (paramType.equals("#")) {
                            deserializerBuilder.addCode(";\n");
                            deserializerBuilder.addCode("int flags = payload.readIntLE();\n");
                            deserializerBuilder.addCode("return builder");
                        }

                        String unwrapping = deserializeMethod(paramType);
                        if (unwrapping != null) {
                            deserializerBuilder.addCode("\n\t\t.$L(" + unwrapping + ")", paramName);
                        }
                    }
                    if (!attributes.isEmpty()) {
                        deserializerBuilder.addCode("\n\t\t.build();");
                    }
                    deserializer.addMethod(deserializerBuilder.build());
                }

                serializeMethod.addCode("default: throw new IllegalArgumentException(\"Incorrect TlObject identifier: \" + identifier);\n");
                serializeMethod.endControlFlow();
                deserializer.addMethod(serializeMethod.build());

                JavaFile file = JavaFile.builder(packageName, deserializer.build())
                        .addStaticImport(ClassName.get(UTIL_PACKAGE, "TlSerialUtil"),
                                "BOOL_FALSE_ID",
                                "BOOL_TRUE_ID", "deserializeBytesVector",
                                "deserializeIntVector",
                                "deserializeLongVector", "deserializeStringVector",
                                "deserializeVector", "readBytes", "readString")
                        .indent(INDENT)
                        .build();

                writeTo(file);

                progress &= ~STAGE_DESERIALIZER;
            }

            // endregion
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
        if (FLAG_PATTERN.matcher(type).matches() || VECTOR_PATTERN.matcher(type).matches()) {
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

        char f = type.charAt(0);
        if (Character.isUpperCase(f)) {
            type = Character.toLowerCase(f) + type.substring(1);
        }

        // This is a strange and in some places illogical problem
        // solution of matching attribute names with java keywords
        if (!SourceVersion.isName(type)) {
            type += "State";
        }

        return type;
    }

    private TypeName parseType(String type) {
        if (type.equalsIgnoreCase("!x") || type.equalsIgnoreCase("x")) {
            return TypeVariableName.get("T", TlSerializable.class);
        }
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
            TypeName innerType = parseType(flag.group(2));
            return innerType.box();
        }

        Matcher vector = VECTOR_PATTERN.matcher(type);
        if (vector.matches()) {
            String template = normalizeName(vector.group(1));
            TypeName templateType = parseType(template);
            return ParameterizedTypeName.get(ClassName.get(List.class), templateType.box());
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
        TypeName type = parseType(matcher.group(2));
        return new Flag(position, param.name(), type);
    }

    private void writeTo(JavaFile file) {
        try {
            file.writeTo(filer);
        } catch (Throwable ignored) {
        }
    }

    @Nullable
    private String deserializeMethod(String type) {
        switch (type.toLowerCase()) {
            case "true": return "TlTrue.INSTANCE";
            case "bool": return "payload.readIntLE() == BOOL_TRUE_ID";
            case "int": return "payload.readIntLE()";
            case "long": return "payload.readLongLE()";
            case "double": return "payload.readDouble()";
            case "bytes": return "readBytes(payload)";
            case "string": return "readString(payload)";
            default:
                if (type.equals("#")) {
                    return null;
                }

                if (type.startsWith("flags.")) {
                    Matcher matcher = FLAG_PATTERN.matcher(type);
                    matcher.matches();
                    int position = Integer.parseInt(matcher.group(1));
                    String typeRaw = matcher.group(2);

                    String innerMethod = deserializeMethod(typeRaw);
                    return "(flags & " + (int) Math.pow(2, position) + ") != 0 ? " + innerMethod + " : null";
                }

                Matcher vector = VECTOR_PATTERN.matcher(type);
                if (vector.matches()) {
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
                        case "string":
                            specific = "String";
                            break;
                    }
                    return "deserialize" + specific + "Vector(payload)";
                }
                return "deserialize(payload)";
        }
    }

    // java 9
    public static <T, U, A, R> Collector<T, ?, R> flatMapping(Function<? super T, ? extends Stream<? extends U>> mapper,
                                                              Collector<? super U, A, R> downstream) {
        BiConsumer<A, ? super U> downstreamAccumulator = downstream.accumulator();
        return Collector.of(downstream.supplier(),
                (r, t) -> {
                    try (Stream<? extends U> result = mapper.apply(t)) {
                        if (result != null) {
                            result.sequential().forEach(u -> downstreamAccumulator.accept(r, u));
                        }
                    }
                },
                downstream.combiner(), downstream.finisher(),
                downstream.characteristics().toArray(new Collector.Characteristics[0]));
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
}
