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
import telegram4j.tl.model.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static telegram4j.tl.Strings.camelize;
import static telegram4j.tl.Strings.screamilize;

@SupportedAnnotationTypes("telegram4j.tl.GenerateSchema")
public class SchemaGenerator extends AbstractProcessor {

    private static final List<String> ignoredTypes = Collections.unmodifiableList(Arrays.asList(
            "bool", "true", "false", "null", "int", "long",
            "string", "flags", "vector", "#"));

    private static final String METHOD_PACKAGE_PREFIX = ".request";
    private static final String TEMPLATE_PACKAGE_INFO = "template-package-info";
    private static final String UTIL_PACKAGE = "telegram4j.tl";
    private static final String API_SCHEMA = "api.json";
    private static final String MTPROTO_SCHEMA = "mtproto.json";
    private static final String INDENT = "\t";
    private static final Pattern FLAG_PATTERN = Pattern.compile("^flags\\.(\\d+)\\?(.+)$");
    private static final Pattern VECTOR_PATTERN = Pattern.compile("^[vV]ector<%?([A-Za-z0-9._<>]+)>$");

    private final Map<String, TlEntityObject> computed = new HashMap<>();

    private Filer filer;
    private Messager messager;
    private Elements elements;
    private Types types;
    private Trees trees;
    private Symbol.PackageSymbol currentElement;

    // api and mtproto schemas
    private TlSchema schema;
    private Map<String, List<TlEntityObject>> typeTree;

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
            InputStream api = filer.getResource(StandardLocation.ANNOTATION_PROCESSOR_PATH, "", API_SCHEMA).openInputStream();
            InputStream mtproto = filer.getResource(StandardLocation.ANNOTATION_PROCESSOR_PATH, "", MTPROTO_SCHEMA).openInputStream();

            TlSchema apiSchema = mapper.readValue(api, TlSchema.class);
            TlSchema mtprotoSchema = mapper.readValue(mtproto, TlSchema.class);

            List<TlEntityObject> tlConstructors = new ArrayList<>(apiSchema.constructors());
            tlConstructors.addAll(mtprotoSchema.constructors());

            List<TlEntityObject> tlMethods = new ArrayList<>(apiSchema.methods());
            tlMethods.addAll(mtprotoSchema.methods());

            schema = ImmutableTlSchema.of(tlConstructors, tlMethods);
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

        if (typeTree == null) {
            typeTree = schema.constructors().stream()
                    .filter(c -> !ignoredTypes.contains(normalizeName(c.type()).toLowerCase()) &&
                            !c.type().equalsIgnoreCase("Object"))
                    .collect(Collectors.groupingBy(c -> getPackageName(c.type()) +
                            "." + normalizeName(c.type())));

            // prepare package-info.java file for types

            preparePackages();
        }

        if (annotations.size() > 1) {
            messager.printMessage(Diagnostic.Kind.ERROR, "[TL parser] Generation package must be specified once!", currentElement);
            return true;
        }

        try {
            // region constructors

            Map<String, Set<TlParam>> sharedParams = typeTree.entrySet().stream()
                    .filter(e -> e.getValue().size() > 1)
                    .collect(Collectors.groupingBy(Map.Entry::getKey,
                            flatMapping(e -> e.getValue().stream()
                                            .flatMap(c -> c.params().stream())
                                            .filter(p -> e.getValue().stream()
                                                    .allMatch(c -> c.params().contains(p))),
                                    Collectors.toCollection(LinkedHashSet::new))));

            for (Map.Entry<String, Set<TlParam>> e : sharedParams.entrySet()) {
                String name = normalizeName(e.getKey());
                Set<TlParam> params = e.getValue();
                String packageName = getPackageName(e.getKey());
                String qualifiedName = e.getKey();

                boolean canMakeEnum = typeTree.get(qualifiedName).stream()
                        .mapToInt(c -> c.params().size()).sum() == 0 &&
                        !name.equals("Object");

                String shortenName = extractEnumName(qualifiedName);

                TypeSpec.Builder superType = canMakeEnum
                        ? TypeSpec.enumBuilder(name)
                        : TypeSpec.interfaceBuilder(name);

                superType.addModifiers(Modifier.PUBLIC);
                if (canMakeEnum) {
                    superType.addSuperinterface(TlObject.class);

                    MethodSpec.Builder ofMethod = MethodSpec.methodBuilder("of")
                            .addParameter(int.class, "identifier")
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                            .returns(ClassName.get(packageName, name))
                            .beginControlFlow("switch (identifier)");

                    for (TlEntityObject constructor : typeTree.get(qualifiedName)) {
                        String subtypeName = normalizeName(constructor.name());
                        String constName = screamilize(subtypeName.substring(shortenName.length()));

                        superType.addEnumConstant(constName, TypeSpec.anonymousClassBuilder(
                                        "$L", "0x" + Integer.toHexString(constructor.id()))
                                .build());

                        ofMethod.addCode("case $L: return $L;\n",
                                "0x" + Integer.toHexString(constructor.id()),
                                constName);
                        computed.put(getPackageName(constructor.name()) + "." + subtypeName, constructor);
                    }

                    superType.addField(int.class, "identifier", Modifier.PRIVATE, Modifier.FINAL);

                    superType.addMethod(MethodSpec.constructorBuilder()
                            .addParameter(int.class, "identifier")
                            .addStatement("this.identifier = identifier")
                            .build());

                    ofMethod.addCode("default: throw new IllegalArgumentException($S + Integer.toHexString(identifier));\n",
                            "Incorrect type identifier: 0x");
                    ofMethod.endControlFlow();

                    superType.addMethod(MethodSpec.methodBuilder("identifier")
                            .addAnnotation(Override.class)
                            .returns(TypeName.INT)
                            .addModifiers(Modifier.PUBLIC)
                            .addCode("return identifier;")
                            .build());

                    superType.addMethod(ofMethod.build());
                } else {
                    superType.addSuperinterface(TlSerializable.class);

                    for (TlParam param : params) {
                        if (param.type().equals("#")) {
                            continue;
                        }

                        TypeName paramType = parseType(param.type());
                        if (param.name().equals("value") && name.equals("JSONValue")) { // TODO: create tl-serializable Jackson' JsonNode subtype
                            paramType = TypeName.OBJECT;
                        }

                        boolean optionalInExt = typeTree.get(qualifiedName).stream()
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
                }

                writeTo(JavaFile.builder(packageName, superType.build())
                        .indent(INDENT)
                        .skipJavaLangImports(true)
                        .build());
            }

            for (TlEntityObject constructor : schema.constructors()) {
                String type = normalizeName(constructor.type());
                String name = normalizeName(constructor.name());
                String packageName = getPackageName(constructor.type());
                String qualifiedTypeName = packageName + "." + type;

                boolean multiple = typeTree.getOrDefault(qualifiedTypeName, Collections.emptyList()).size() > 1;

                // add Base* prefix to prevent matching with supertype name, e.g. SecureValueError
                if (type.equalsIgnoreCase(name) && multiple) {
                    name = "Base" + name;
                } else if (!multiple && !type.equals("Object")) { // use type name if this object type is singleton
                    name = type;
                }

                if (ignoredTypes.contains(type.toLowerCase()) || computed.containsKey(packageName + "." + name)) {
                    continue;
                }

                TypeSpec.Builder builder = TypeSpec.interfaceBuilder(name)
                        .addModifiers(Modifier.PUBLIC);

                if (multiple) {
                    builder.addSuperinterface(ClassName.get(packageName, type));
                }

                builder.addSuperinterface(TlObject.class);

                builder.addField(FieldSpec.builder(TypeName.INT, "ID",
                                Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("0x" + Integer.toHexString(constructor.id()))
                        .build());

                builder.addMethod(MethodSpec.methodBuilder("builder")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(ClassName.get(packageName, "Immutable" + name, "Builder"))
                        .addCode("return Immutable$L.builder();", name)
                        .build());

                Set<TlParam> attributes = new LinkedHashSet<>(constructor.params());
                collectAttributesRecursive(type, attributes);

                boolean optional = attributes.stream().allMatch(p -> p.type().startsWith("flags."));
                AnnotationSpec.Builder value = AnnotationSpec.builder(Value.Immutable.class);
                if (optional) {
                    value.addMember("singleton", "true");

                    builder.addMethod(MethodSpec.methodBuilder("instance")
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                            .returns(ClassName.get(packageName, "Immutable" + name))
                            .addCode("return Immutable$L.of();", name)
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

                    boolean optionalInExt = typeTree.getOrDefault(qualifiedTypeName, Collections.emptyList()).stream()
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

                writeTo(JavaFile.builder(packageName, builder.build())
                        .indent(INDENT)
                        .skipJavaLangImports(true)
                        .build());

                computed.put(packageName + "." + name, constructor);
            }

            // endregion

            // region methods

            TypeVariableName genericType = TypeVariableName.get("T", TlObject.class);
            TypeVariableName genericTypeRef = TypeVariableName.get("T");

            for (TlEntityObject method : schema.methods()) {
                String type = normalizeName(method.type());
                String name = normalizeName(method.name());
                String packageNameRaw = getPackageName();
                String packageName = getPackageName(method.name())
                        .replace(packageNameRaw, packageNameRaw + METHOD_PACKAGE_PREFIX);

                String qualifiedTypeName = getPackageName(method.type()) + "." + type;

                boolean generic = method.params().stream()
                        .anyMatch(p -> p.type().equals("!X"));

                TypeSpec.Builder typeSpec = TypeSpec.interfaceBuilder(name)
                        .addModifiers(Modifier.PUBLIC);

                if (generic) {
                    typeSpec.addTypeVariable(genericType);
                }

                boolean canMakeEnum = typeTree.getOrDefault(qualifiedTypeName, Collections.emptyList()).stream()
                        .mapToInt(c -> c.params().size()).sum() == 0 &&
                        !type.equals("Object");

                // TODO: check the signature of HttpWait
                String importMethod = method.type().equals("HttpWait") ? packageName + "." + name : method.type();

                TypeName returnType = ParameterizedTypeName.get(
                        ClassName.get(TlMethod.class),
                        parseType(canMakeEnum && !method.type().equals("HttpWait") ? type : importMethod).box());

                typeSpec.addSuperinterface(returnType);

                typeSpec.addField(FieldSpec.builder(TypeName.INT, "ID",
                                Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("0x" + Integer.toHexString(method.id()))
                        .build());

                ClassName immutableTypeRaw = ClassName.get(packageName, "Immutable" + name);
                ClassName immutableTypeBuilderRaw = ClassName.get(packageName, "Immutable" + name, "Builder");
                TypeName immutableType = generic ? ParameterizedTypeName.get(immutableTypeRaw, genericTypeRef) : immutableTypeRaw;
                TypeName immutableBuilderType = generic
                        ? ParameterizedTypeName.get(immutableTypeBuilderRaw, genericTypeRef)
                        : immutableTypeBuilderRaw;

                MethodSpec.Builder builder = MethodSpec.methodBuilder("builder")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(immutableBuilderType)
                        .addCode("return $T.builder();", immutableTypeRaw);

                if (generic) {
                    builder.addTypeVariable(genericType);
                }

                typeSpec.addMethod(builder.build());

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

                    typeSpec.addMethod(instance.build());
                }

                typeSpec.addAnnotation(value.build());

                typeSpec.addMethod(MethodSpec.methodBuilder("identifier")
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

                    typeSpec.addMethod(attribute.build());
                }

                writeTo(JavaFile.builder(packageName, typeSpec.build())
                        .indent(INDENT)
                        .skipJavaLangImports(true)
                        .build());
            }

            // endregion

            // region serializer

            Map<String, List<TlEntityObject>> enumTypes = typeTree.entrySet().stream()
                    .filter(e -> e.getValue().stream()
                            .mapToInt(c -> c.params().size())
                            .sum() == 0)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            TypeVariableName serializableType = TypeVariableName.get("T", TlSerializable.class);
            MethodSpec privateConstructor = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PRIVATE)
                    .build();

            TypeSpec.Builder serializer = TypeSpec.classBuilder("TlSerializer")
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addMethod(privateConstructor);

            // special method for supertype objects
            serializer.addMethod(MethodSpec.methodBuilder("serializeExact")
                    .returns(ByteBuf.class)
                    .addTypeVariable(serializableType)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameters(Arrays.asList(
                            ParameterSpec.builder(ByteBufAllocator.class, "allocator").build(),
                            ParameterSpec.builder(serializableType, "payload").build()))
                    .beginControlFlow("if (payload instanceof $T)", TlObject.class)
                    .addStatement("return serialize(allocator, ($T) payload)", TlObject.class)
                    .nextControlFlow("else")
                    .addStatement("throw new IllegalArgumentException(\"Unrecorded TL Serializable supertype: \" + payload.getClass())")
                    .endControlFlow()
                    .build());

            MethodSpec.Builder serializeMethod = MethodSpec.methodBuilder("serialize")
                    .returns(ByteBuf.class)
                    .addTypeVariable(genericType)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameters(Arrays.asList(
                            ParameterSpec.builder(ByteBufAllocator.class, "allocator").build(),
                            ParameterSpec.builder(genericType, "payload").build()));

            serializeMethod.beginControlFlow("switch (payload.identifier())");

            for (TlEntityObject constructor : schema.constructors()) {
                String type = normalizeName(constructor.type());
                String name = normalizeName(constructor.name());
                String packageName = getPackageName(constructor.name());
                String qualifiedTypeName = packageName + "." + type;

                // Enums must be handled after types
                if (enumTypes.containsKey(qualifiedTypeName)) {
                    continue;
                }

                boolean multiple = typeTree.getOrDefault(qualifiedTypeName, Collections.emptyList()).size() > 1;

                if (type.equalsIgnoreCase(name) && multiple) {
                    name = "Base" + name;
                } else if (!multiple && !type.equals("Object")) { // see mtproto schema, gzip_packed/Object
                    name = type;
                }

                if (ignoredTypes.contains(type.toLowerCase())) {
                    continue;
                }

                TypeName payloadType = ClassName.get(packageName, name);

                String methodName = "serialize" + name;

                if (serializer.methodSpecs.stream().anyMatch(spec -> spec.name.equals(methodName))) {
                    continue;
                }

                serializeMethod.addCode("case $L: return $L(allocator, ($T) payload);\n",
                        "0x" + Integer.toHexString(constructor.id()),
                        methodName, payloadType);

                Set<TlParam> attributes = new LinkedHashSet<>(constructor.params());
                collectAttributesRecursive(type, attributes);

                MethodSpec.Builder serializerBuilder = MethodSpec.methodBuilder(methodName)
                        .returns(ByteBuf.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addParameters(Arrays.asList(
                                ParameterSpec.builder(ByteBufAllocator.class, "allocator").build(),
                                ParameterSpec.builder(payloadType, "payload").build()));

                serializerBuilder.addCode("return allocator.buffer()\n");
                serializerBuilder.addCode("\t\t.writeIntLE(payload.identifier())");

                for (TlParam param : attributes) {
                    String paramType = param.type().toLowerCase();
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
                                    .map(f -> "(payload." + formatFieldName(f.getName()) +
                                            "() != null ? 1 : 0) << " + f.getPosition())
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
                        case "int256":
                        case "int128":
                            method = "writeBytes";
                            break;
                        case "string":
                        case "bytes":
                            wrapping = "writeString(allocator, payload.$L())";
                            method = "writeBytes";
                            break;
                        default:
                            Matcher vector = VECTOR_PATTERN.matcher(paramType);
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
                                wrapping = "serialize" + specific + "Vector(allocator, payload.$L())";
                            } else if (paramType.startsWith("flags.")) {
                                if (paramType.endsWith("true")) {
                                    method = null;
                                    break;
                                }
                                wrapping = "serializeFlags(allocator, payload.$L())";
                            } else {
                                // TODO: use concrete method
                                wrapping = "serializeExact(allocator, payload.$L())";
                            }
                            method = "writeBytes";
                    }

                    if (method != null) {
                        if (paramType.equals("#")) {
                            serializerBuilder.addCode("\n\t\t.$L(" + wrapping + ")", method);
                        } else {
                            serializerBuilder.addCode("\n\t\t.$L(" + wrapping + ")", method, paramName);
                        }
                    }
                }
                serializerBuilder.addCode(";");

                serializer.addMethod(serializerBuilder.build());
            }

            for (TlEntityObject method : schema.methods()) {
                String name = normalizeName(method.name());
                String methodName = "serialize" + name;
                boolean generic = method.params().stream()
                        .anyMatch(p -> p.type().equals("!X"));

                String packageNameRaw = getPackageName();
                String packageName = getPackageName(method.name())
                        .replace(packageNameRaw, packageNameRaw + METHOD_PACKAGE_PREFIX);

                ClassName typeRaw = ClassName.get(packageName, name);
                TypeName type = generic ? ParameterizedTypeName.get(typeRaw, ClassName.get(TlObject.class)) : typeRaw;

                if (serializer.methodSpecs.stream().anyMatch(spec -> spec.name.equals(methodName))) {
                    continue;
                }

                serializeMethod.addCode("case $L: return $L(allocator, ($T) payload);\n",
                        "0x" + Integer.toHexString(method.id()),
                        methodName, type);

                TypeName payloadType = generic ? ParameterizedTypeName.get(typeRaw, genericTypeRef) : typeRaw;

                MethodSpec.Builder serializerBuilder = MethodSpec.methodBuilder(methodName)
                        .returns(ByteBuf.class)
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addParameters(Arrays.asList(
                                ParameterSpec.builder(ByteBufAllocator.class, "allocator").build(),
                                ParameterSpec.builder(payloadType, "payload").build()));

                if (generic) {
                    serializerBuilder.addTypeVariable(genericType);
                }

                serializerBuilder.addCode("return allocator.buffer()\n");
                serializerBuilder.addCode("\t\t.writeIntLE(payload.identifier())");

                for (TlParam param : method.params()) {
                    String paramType = param.type().toLowerCase();
                    String paramName = formatFieldName(param.name());

                    String wrapping = "payload.$L()";
                    String method0;
                    switch (paramType) {
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
                                    .map(f -> "(payload." + formatFieldName(f.getName()) +
                                            "() != null ? 1 : 0) << " + f.getPosition())
                                    .collect(Collectors.joining(" | "));
                        case "int":
                            method0 = "writeIntLE";
                            break;
                        case "int256":
                        case "int128":
                            method0 = "writeBytes";
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
                            Matcher vector = VECTOR_PATTERN.matcher(paramType);
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
                                wrapping = "serialize" + specific + "Vector(allocator, payload.$L())";
                            } else if (paramType.startsWith("flags.")) {
                                if (paramType.endsWith("true")) {
                                    method0 = null;
                                    break;
                                }
                                wrapping = "serializeFlags(allocator, payload.$L())";
                            } else {
                                // TODO: use concrete method
                                wrapping = "serializeExact(allocator, payload.$L())";
                            }
                            method0 = "writeBytes";
                    }

                    if (method0 != null) {
                        if (paramType.equals("#")) {
                            serializerBuilder.addCode("\n\t\t.$L(" + wrapping + ")", method0);
                        } else {
                            serializerBuilder.addCode("\n\t\t.$L(" + wrapping + ")", method0, paramName);
                        }
                    }
                }
                serializerBuilder.addCode(";");

                serializer.addMethod(serializerBuilder.build());
            }

            // Enums serialization
            for (Iterator<List<TlEntityObject>> iterator = enumTypes.values().iterator(); iterator.hasNext(); ) {
                List<TlEntityObject> chunk = iterator.next();
                for (int i = 0; i < chunk.size(); i++) {
                    int id = chunk.get(i).id();

                    String sep = "\n";
                    if (i + 1 == chunk.size() && !iterator.hasNext()) {
                        sep = " return allocator.buffer().writeIntLE(payload.identifier());\n";
                    }
                    serializeMethod.addCode("case $L:" + sep, "0x" + Integer.toHexString(id));
                }
            }

            serializeMethod.addCode("default: throw new IllegalArgumentException($S + Integer.toHexString(payload.identifier()));\n",
                    "Incorrect TlObject identifier: 0x");
            serializeMethod.endControlFlow();
            serializer.addMethod(serializeMethod.build());

            writeTo(JavaFile.builder(getPackageName(), serializer.build())
                    .addStaticImport(ClassName.get(UTIL_PACKAGE, "TlSerialUtil"),
                            "BOOL_FALSE_ID", "BOOL_TRUE_ID",
                            "serializeBytesVector", "serializeFlags",
                            "serializeIntVector", "serializeLongVector", "serializeStringVector",
                            "serializeVector", "writeString")
                    .indent(INDENT)
                    .skipJavaLangImports(true)
                    .build());

            // endregion

            // region deserialization

            TypeSpec.Builder deserializer = TypeSpec.classBuilder("TlDeserializer")
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addMethod(privateConstructor);

            MethodSpec.Builder deserializeMethod = MethodSpec.methodBuilder("deserialize")
                    .returns(serializableType)
                    .addTypeVariable(serializableType)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(ParameterSpec.builder(ByteBuf.class, "payload").build());

            deserializeMethod.addStatement("int identifier = payload.readIntLE()");
            deserializeMethod.beginControlFlow("switch (identifier)");

            for (TlEntityObject constructor : schema.constructors()) {
                String type = normalizeName(constructor.type());
                String name = normalizeName(constructor.name());
                String packageName = getPackageName(constructor.type());
                String qualifiedTypeName = packageName + "." + type;

                // Enums must be handled after types
                if (enumTypes.containsKey(qualifiedTypeName)) {
                    continue;
                }

                boolean multiple = typeTree.getOrDefault(qualifiedTypeName, Collections.emptyList()).size() > 1;

                if (type.equalsIgnoreCase(name) && multiple) {
                    name = "Base" + name;
                } else if (!multiple && !type.equals("Object")) {
                    name = type;
                }

                String methodName = "deserialize" + name;

                if (ignoredTypes.contains(type.toLowerCase())) {
                    continue;
                }

                if (deserializer.methodSpecs.stream().anyMatch(spec -> spec.name.equals(methodName))) {
                    continue;
                }

                deserializeMethod.addCode("case $L: return (T) $L(payload);\n",
                        "0x" + Integer.toHexString(constructor.id()), methodName);

                Set<TlParam> attributes = new LinkedHashSet<>(constructor.params());
                collectAttributesRecursive(type, attributes);

                ClassName typeName = ClassName.get(packageName, name);

                MethodSpec.Builder deserializerBuilder = MethodSpec.methodBuilder(methodName)
                        .returns(typeName)
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addParameter(ParameterSpec.builder(ByteBuf.class, "payload").build());

                boolean withFlags = attributes.stream().anyMatch(p -> p.type().equals("#"));
                if (withFlags) {
                    ClassName builder = ClassName.get(packageName, "Immutable" + name, "Builder");
                    deserializerBuilder.addCode("$T builder = $T.builder()", builder, typeName);
                } else if (attributes.isEmpty()) {
                    deserializerBuilder.addCode("return $T.instance();", typeName);
                } else {
                    deserializerBuilder.addCode("return $T.builder()", typeName);
                }

                for (TlParam param : attributes) {
                    String paramType = param.type();

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

            // Enums deserialization
            for (List<TlEntityObject> chunk : enumTypes.values()) {
                for (int i = 0; i < chunk.size(); i++) {
                    TlEntityObject obj = chunk.get(i);
                    if (i + 1 == chunk.size()) {
                        String type = normalizeName(obj.type());
                        String packageName = getPackageName(obj.type());
                        deserializeMethod.addCode("case $L: return (T) $T.of(identifier);\n", "0x" + Integer.toHexString(obj.id()),
                                ClassName.get(packageName, type));
                    } else {
                        deserializeMethod.addCode("case $L:\n", "0x" + Integer.toHexString(obj.id()));
                    }
                }
            }

            deserializeMethod.addCode("default: throw new IllegalArgumentException($S + Integer.toHexString(identifier));\n",
                    "Incorrect TlObject identifier: 0x");
            deserializeMethod.endControlFlow();
            deserializer.addMethod(deserializeMethod.build());

            writeTo(JavaFile.builder(getPackageName(), deserializer.build())
                    .addStaticImport(ClassName.get(UTIL_PACKAGE, "TlSerialUtil"),
                            "BOOL_FALSE_ID",
                            "BOOL_TRUE_ID", "deserializeBytesVector",
                            "deserializeIntVector",
                            "deserializeLongVector", "deserializeStringVector",
                            "deserializeVector", "readBytes", "readString",
                            "readInt256", "readInt128")
                    .indent(INDENT)
                    .skipJavaLangImports(true)
                    .build());

            // endregion
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return true;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        SupportedSourceVersion sourceVersion = getClass().getAnnotation(SupportedSourceVersion.class);
        if (sourceVersion != null) {
            return sourceVersion.value();
        }
        return SourceVersion.latestSupported();
    }

    private void preparePackages() {
        try {

            String processingPackageName = getPackageName();
            String template = filer.getResource(StandardLocation.ANNOTATION_PROCESSOR_PATH,
                    "", TEMPLATE_PACKAGE_INFO).getCharContent(true)
                    .toString();

            Set<String> packages = typeTree.keySet().stream()
                    .map(this::getPackageName)
                    .filter(s -> !s.equals(processingPackageName))
                    .collect(Collectors.toSet());

            // collect methods packages with prefix 'request'
            packages.addAll(schema.methods().stream()
                    .map(e -> getPackageName(e.name()))
                    .map(s -> s.replace(getPackageName(), getPackageName() + METHOD_PACKAGE_PREFIX))
                    .collect(Collectors.toSet()));

            for (String pack : packages) {
                String complete = template.replace("${package-name}", pack);
                try (Writer writer = filer.createSourceFile(pack + ".package-info").openWriter()) {
                    writer.write(complete);
                    writer.flush();
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to prepare packages", e);
        }
    }

    private static String normalizeName(String type) {
        Matcher vector = VECTOR_PATTERN.matcher(type);
        if (vector.matches()) {
            type = vector.group(1);
        }

        Matcher flag = FLAG_PATTERN.matcher(type);
        if (flag.matches()) {
            type = flag.group(2);
        }

        int dotIdx = type.lastIndexOf('.');
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

        return camelize(type);
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
        switch (type.toLowerCase(Locale.ROOT)) {
            case "!x":
            case "x": return TypeVariableName.get("T", TlSerializable.class);
            case "int": return TypeName.INT;
            case "true": return ClassName.get(TlTrue.class);
            case "bool": return TypeName.BOOLEAN;
            case "long": return TypeName.LONG;
            case "double": return TypeName.DOUBLE;
            case "bytes":
            case "int128":
            case "int256": return TypeName.get(byte[].class);
            case "string": return TypeName.get(String.class);
            case "object": return ClassName.get(TlObject.class);
            default:
                Matcher flag = FLAG_PATTERN.matcher(type);
                if (flag.matches()) {
                    TypeName innerType = parseType(flag.group(2));
                    return innerType.box();
                }

                Matcher vector = VECTOR_PATTERN.matcher(type);
                if (vector.matches()) {
                    TypeName templateType = parseType(vector.group(1));
                    return ParameterizedTypeName.get(ClassName.get(List.class), templateType.box());
                }

                return ClassName.get(getPackageName(type), normalizeName(type));
        }
    }

    private void collectAttributesRecursive(String name, Set<TlParam> params) {
        TlEntityObject constructor = computed.get(name);
        if (constructor == null || constructor.name().equals(name)) {
            return;
        }
        params.addAll(constructor.params());
        collectAttributesRecursive(constructor.name(), params);
    }

    private String getPackageName(String type) {
        Matcher vector = VECTOR_PATTERN.matcher(type);
        if (vector.matches()) {
            type = vector.group(1);
        }

        Matcher flag = FLAG_PATTERN.matcher(type);
        if (flag.matches()) {
            type = flag.group(2);
        }

        int ldotidx = type.lastIndexOf('.');
        String packageName = getPackageName();
        if (ldotidx == -1) {
            return packageName;
        }
        if (type.startsWith(packageName)) {
            return type.substring(0, ldotidx);
        }
        return packageName + "." + type.substring(0, ldotidx);
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

    private String extractEnumName(String type) {
        return typeTree.getOrDefault(type, Collections.emptyList()).stream()
                .map(c -> normalizeName(c.name()))
                .reduce(SchemaGenerator::findCommonPart)
                .orElseGet(() -> normalizeName(type));
    }

    private static String findCommonPart(String s1, String s2) {
        if (s1.equals(s2)) {
            return s1;
        }
        for (int i = 0; i < Math.min(s1.length(), s2.length()); i++) {
            if (s1.charAt(i) != s2.charAt(i)) {
                return s1.substring(0, i);
            }
        }
        return s1;
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
            case "int128": return "readInt128(payload)";
            case "int256": return "readInt256(payload)";
            default:
                if (type.equals("#")) {
                    return null;
                }

                Matcher flag = FLAG_PATTERN.matcher(type);
                if (flag.matches()) {
                    int position = Integer.parseInt(flag.group(1));
                    String typeRaw = flag.group(2);

                    String innerMethod = deserializeMethod(typeRaw);
                    return "(flags & " + (1 << position) + ") != 0 ? " + innerMethod + " : null";
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
    static <T, U, A, R> Collector<T, ?, R> flatMapping(Function<? super T, ? extends Stream<? extends U>> mapper,
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
}
