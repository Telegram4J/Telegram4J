package telegram4j.tl;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.squareup.javapoet.*;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.processing.JavacFiler;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.immutables.value.Value;
import reactor.core.Exceptions;
import reactor.util.annotation.Nullable;
import telegram4j.tl.model.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static telegram4j.tl.SchemaGeneratorConsts.*;
import static telegram4j.tl.SourceNames.*;
import static telegram4j.tl.Strings.camelize;
import static telegram4j.tl.Strings.screamilize;

@SupportedAnnotationTypes("telegram4j.tl.GenerateSchema")
public class SchemaGenerator extends AbstractProcessor {

    private static final String METHOD_PACKAGE_PREFIX = "request";
    private static final String MTPROTO_PACKAGE_PREFIX = "mtproto";
    private static final String TEMPLATE_PACKAGE_INFO = "package-info.template";
    private static final String UTIL_PACKAGE = "telegram4j.tl";
    private static final String API_SCHEMA = "api.json";
    private static final String MTPROTO_SCHEMA = "mtproto.json";
    private static final String INDENT = "\t";

    private final Map<String, TlEntityObject> computed = new HashMap<>();

    private JavacFiler filer;
    private Messager messager;
    private JavacElements elements;
    private JavacTypes types;
    private Trees trees;

    private Symbol.PackageSymbol currentElement;
    private TlSchema apiSchema;
    private TlSchema mtprotoSchema;
    private List<TlSchema> schemas;
    private Map<String, List<TlEntityObject>> typeTree;

    private int iteration;
    private int schemaIteration;
    private TlSchema schema;

    // processing resources

    private final Set<String> computedSerializers = new HashSet<>();
    private final Set<String> computedDeserializers = new HashSet<>();
    private final Set<String> computedMethodSerializers = new HashSet<>();

    private final TypeSpec.Builder serializer = TypeSpec.classBuilder("TlSerializer")
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addMethod(privateConstructor);

    private final MethodSpec.Builder serializeMethod = MethodSpec.methodBuilder("serialize")
            .returns(ByteBuf.class)
            .addTypeVariable(genericType)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(ByteBufAllocator.class, "allocator")
            .addParameter(genericType, "payload")
            .beginControlFlow("switch (payload.identifier())");

    private final TypeSpec.Builder deserializer = TypeSpec.classBuilder("TlDeserializer")
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addMethod(privateConstructor);

    private final MethodSpec.Builder deserializeMethod = MethodSpec.methodBuilder("deserialize")
            .returns(genericTypeRef)
            .addTypeVariable(genericTypeRef)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addParameter(ByteBuf.class, "payload")
            .addStatement("int identifier = payload.readIntLE()")
            .beginControlFlow("switch (identifier)")
            // This need because methods can return bool or vector objects
            .addComment("Primitive types")
            .addCode("case BOOL_TRUE_ID: return (T) Boolean.TRUE;\n")
            .addCode("case BOOL_FALSE_ID: return (T) Boolean.FALSE;\n")
            .addCode("case VECTOR_ID: return (T) deserializeUnknownVector(payload);\n");

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        JavacProcessingEnvironment casted = (JavacProcessingEnvironment) processingEnv;
        this.filer = casted.getFiler();
        this.messager = casted.getMessager();
        this.elements = casted.getElementUtils();
        this.types = casted.getTypeUtils();
        this.trees = Trees.instance(casted);

        ObjectMapper mapper = JsonMapper.builder()
                .addModules(new Jdk8Module())
                .visibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
                .visibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.PUBLIC_ONLY)
                .visibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY)
                .build();

        try {
            InputStream api = filer.getResource(StandardLocation.ANNOTATION_PROCESSOR_PATH, "", API_SCHEMA).openInputStream();
            InputStream mtproto = filer.getResource(StandardLocation.ANNOTATION_PROCESSOR_PATH, "", MTPROTO_SCHEMA).openInputStream();

            apiSchema = ImmutableTlSchema.copyOf(mapper.readValue(api, TlSchema.class));

            mtprotoSchema = ImmutableTlSchema.copyOf(mapper.readValue(mtproto, TlSchema.class))
                    .withPackagePrefix(MTPROTO_PACKAGE_PREFIX)
                    .withSuperType(MTProtoObject.class);

            schemas = Arrays.asList(apiSchema, mtprotoSchema);
        } catch (Throwable t) {
            throw Exceptions.propagate(t);
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return true;
        }

        if (annotations.size() > 1) {
            messager.printMessage(Diagnostic.Kind.ERROR, "[TL parser] Generation package must be specified once!", currentElement);
            return true;
        }

        if (currentElement == null) {
            currentElement = (Symbol.PackageSymbol) roundEnv
                    .getElementsAnnotatedWith(GenerateSchema.class)
                    .iterator().next();
        }

        if (typeTree == null) {
            typeTree = collectTypeTree(apiSchema);
            typeTree.putAll(collectTypeTree(mtprotoSchema));

            preparePackages();
        }

        switch (iteration) {
            case 0:
                generatePrimitives();
                iteration++;
                break;
            case 1:
                generateSuperTypes();
                iteration++;
                break;
            case 2:
                generateConstructors();
                iteration++;
                break;
            case 3:
                generateMethods();
                if (++schemaIteration >= schemas.size()) {
                    iteration = 4;
                } else { // *new* generation round
                    schema = schemas.get(schemaIteration);
                    iteration = 1;
                }
                break;
            case 4:
                finalizeSerialization();
                iteration++; // end
                break;
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

    private void finalizeSerialization() {
        Map<String, List<TlEntityObject>> enumTypes = typeTree.entrySet().stream()
                .filter(e -> e.getValue().stream()
                        .mapToInt(c -> c.params().size())
                        .sum() == 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        for (Iterator<Map.Entry<String, List<TlEntityObject>>> it = enumTypes.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, List<TlEntityObject>> e = it.next();
            String packageName = parentPackageName(e.getKey());

            List<TlEntityObject> chunk = e.getValue();
            for (int i = 0; i < chunk.size(); i++) {
                TlEntityObject obj = chunk.get(i);

                String idStr = "0x" + Integer.toHexString(obj.id());
                if (i + 1 == chunk.size()) {
                    if (!it.hasNext()) {
                        serializeMethod.addCode("case $L: return allocator.buffer().writeIntLE(payload.identifier());\n", idStr);
                    }
                    String type = normalizeName(obj.type());
                    deserializeMethod.addCode("case $L: return (T) $T.of(identifier);\n", idStr,
                            ClassName.get(packageName, type));
                } else {
                    deserializeMethod.addCode("case $L:\n", idStr);
                    serializeMethod.addCode("case $L:\n", idStr);
                }
            }
        }

        serializeMethod.addCode("default: throw new IllegalArgumentException($S + Integer.toHexString(payload.identifier()));\n",
                "Incorrect TlObject identifier: 0x");
        serializeMethod.endControlFlow();
        serializer.addMethod(serializeMethod.build());

        writeTo(JavaFile.builder(getBasePackageName(), serializer.build())
                .addStaticImport(ClassName.get(UTIL_PACKAGE, "TlSerialUtil"), "*")
                .addStaticImport(ClassName.get(getBasePackageName(), "TlPrimitives"), "*")
                .indent(INDENT)
                .skipJavaLangImports(true)
                .build());

        deserializeMethod.addCode("default: throw new IllegalArgumentException($S + Integer.toHexString(identifier));\n",
                "Incorrect TlObject identifier: 0x");
        deserializeMethod.endControlFlow();
        deserializer.addMethod(deserializeMethod.build());

        writeTo(JavaFile.builder(getBasePackageName(), deserializer.build())
                .addStaticImport(ClassName.get(UTIL_PACKAGE, "TlSerialUtil"), "*")
                .addStaticImport(ClassName.get(getBasePackageName(), "TlPrimitives"), "*")
                .indent(INDENT)
                .skipJavaLangImports(true)
                .build());
    }

    private void generateMethods() {
        for (TlEntityObject method : schema.methods()) {
            String name = normalizeName(method.name());
            if (ignoredTypes.contains(name.toLowerCase())) {
                continue;
            }

            String packageName = getPackageName(schema, method.name(), true);

            boolean generic = method.params().stream()
                    .anyMatch(p -> p.type().equals("!X"));

            TypeSpec.Builder spec = TypeSpec.interfaceBuilder(name)
                    .addModifiers(Modifier.PUBLIC);

            if (generic) {
                spec.addTypeVariable(TypeVariableName.get("T", schema.superType()));
            }

            TypeName returnType = ParameterizedTypeName.get(
                    ClassName.get(TlMethod.class),
                    parseType(method.type(), schema).box());

            ClassName customType = awareSuperType(name);
            if (customType != null) {
                spec.addSuperinterface(customType);
            }

            spec.addSuperinterface(returnType);
            if (schema.superType() != TlObject.class) {
                spec.addSuperinterface(schema.superType());
            }

            spec.addField(FieldSpec.builder(TypeName.INT, "ID",
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
                builder.addTypeVariable(TypeVariableName.get("T", schema.superType()));
            }

            spec.addMethod(builder.build());

            AnnotationSpec.Builder value = AnnotationSpec.builder(Value.Immutable.class);

            boolean singleton = method.params().stream().allMatch(p -> p.type().startsWith("flags."));
            if (singleton) {
                value.addMember("singleton", "true");

                MethodSpec.Builder instance = MethodSpec.methodBuilder("instance")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(immutableType)
                        .addCode("return $T.of();", immutableTypeRaw);

                if (generic) {
                    instance.addTypeVariable(TypeVariableName.get("T", schema.superType()));
                }

                spec.addMethod(instance.build());
            }

            spec.addAnnotation(value.build());

            spec.addMethod(MethodSpec.methodBuilder("identifier")
                    .addAnnotation(Override.class)
                    .returns(TypeName.INT)
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .addCode("return ID;")
                    .build());

            // serialization
            String methodName0 = "serialize" + name;
            String methodName = methodName0;
            if (computedMethodSerializers.contains(methodName0)) {
                String prx = schema.packagePrefix();
                if (prx.isEmpty()) {
                    String mname = method.name();
                    prx = camelize(mname.substring(0, mname.lastIndexOf('.')));
                }
                char up = prx.charAt(0);
                methodName = "serialize" + Character.toUpperCase(up) + prx.substring(1) + name;
            }

            computedMethodSerializers.add(methodName);

            ClassName typeRaw = ClassName.get(packageName, name);
            TypeName type = generic ? ParameterizedTypeName.get(typeRaw, ClassName.get(schema.superType())) : typeRaw;

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
                serializerBuilder.addTypeVariable(TypeVariableName.get("T", schema.superType()));
            }

            serializerBuilder.addCode("return allocator.buffer()\n");
            serializerBuilder.addCode("\t\t.writeIntLE(payload.identifier())");

            for (TlParam param : method.params()) {
                String paramName = formatFieldName(param);

                String method0 = serializeMethod(method, param);
                if (method0 != null) {
                    serializerBuilder.addCode("\n\t\t." + method0, paramName);
                }

                if (param.type().equals("#")) {
                    continue;
                }

                TypeName paramType = parseType(param.type(), schema);

                MethodSpec.Builder attribute = MethodSpec.methodBuilder(paramName)
                        .addModifiers(Modifier.PUBLIC);

                if (param.type().startsWith("flags.")) {
                    if (param.type().endsWith("true")) {
                        attribute.addModifiers(Modifier.DEFAULT);
                        attribute.addCode("return false;");
                    } else {
                        paramType = paramType.box();
                        attribute.addAnnotation(Nullable.class);
                        attribute.addModifiers(Modifier.ABSTRACT);
                    }
                } else {
                    attribute.addModifiers(Modifier.ABSTRACT);
                }

                spec.addMethod(attribute
                        .returns(paramType)
                        .build());
            }

            serializerBuilder.addCode(";");

            serializer.addMethod(serializerBuilder.build());

            writeTo(JavaFile.builder(packageName, spec.build())
                    .indent(INDENT)
                    .skipJavaLangImports(true)
                    .build());
        }
    }

    private void generateConstructors() {
        Map<String, List<TlEntityObject>> currTypeTree = collectTypeTree(schema);

        for (TlEntityObject constructor : schema.constructors()) {
            String type = normalizeName(constructor.type());
            String name = normalizeName(constructor.name());
            String packageName = getPackageName(schema, constructor.type(), false);
            String qualifiedTypeName = packageName + "." + type;

            boolean multiple = currTypeTree.getOrDefault(qualifiedTypeName, Collections.emptyList()).size() > 1;

            // add Base* prefix to prevent matching with supertype name, e.g. SecureValueError
            if (type.equalsIgnoreCase(name) && multiple) {
                name = "Base" + name;
            } else if (!multiple && !type.equals("Object")) { // use type name if this object type is singleton
                name = type;
            }

            if (ignoredTypes.contains(type.toLowerCase()) || computed.containsKey(packageName + "." + name)) {
                continue;
            }

            TypeSpec.Builder spec = TypeSpec.interfaceBuilder(name)
                    .addModifiers(Modifier.PUBLIC);

            ClassName customType = awareSuperType(name);
            if (customType != null) {
                spec.addSuperinterface(customType);
            }

            if (multiple) {
                spec.addSuperinterface(ClassName.get(packageName, type));
            } else {
                spec.addSuperinterface(schema.superType());
            }

            spec.addField(FieldSpec.builder(TypeName.INT, "ID",
                            Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("0x" + Integer.toHexString(constructor.id()))
                    .build());

            spec.addMethod(MethodSpec.methodBuilder("builder")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(ClassName.get(packageName, "Immutable" + name, "Builder"))
                    .addCode("return Immutable$L.builder();", name)
                    .build());

            Set<TlParam> attributes = new LinkedHashSet<>(constructor.params());
            collectAttributesRecursive(type, attributes);

            boolean singleton = attributes.stream().allMatch(p -> p.type().startsWith("flags."));
            AnnotationSpec.Builder value = AnnotationSpec.builder(Value.Immutable.class);
            if (singleton) {
                value.addMember("singleton", "true");

                spec.addMethod(MethodSpec.methodBuilder("instance")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(ClassName.get(packageName, "Immutable" + name))
                        .addCode("return Immutable$L.of();", name)
                        .build());
            }
            spec.addAnnotation(value.build());

            spec.addMethod(MethodSpec.methodBuilder("identifier")
                    .addAnnotation(Override.class)
                    .returns(TypeName.INT)
                    .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                    .addCode("return ID;")
                    .build());

            // serialization
            String serializeMethodName = "serialize" + name;
            if (computedSerializers.contains(serializeMethodName)) {
                String prx = schema.packagePrefix();
                if (prx.isEmpty()) {
                    prx = camelize(parentPackageName(constructor.name()));
                }
                char up = prx.charAt(0);
                serializeMethodName = "serialize" + Character.toUpperCase(up) + prx.substring(1) + name;
            }

            computedSerializers.add(serializeMethodName);

            TypeName payloadType = ClassName.get(packageName, name);
            serializeMethod.addCode("case $L: return $L(allocator, ($T) payload);\n",
                    "0x" + Integer.toHexString(constructor.id()),
                    serializeMethodName, payloadType);

            MethodSpec.Builder serializerBuilder = MethodSpec.methodBuilder(serializeMethodName)
                    .returns(ByteBuf.class)
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .addParameters(Arrays.asList(
                            ParameterSpec.builder(ByteBufAllocator.class, "allocator").build(),
                            ParameterSpec.builder(payloadType, "payload").build()));

            serializerBuilder.addCode("return allocator.buffer()\n");
            serializerBuilder.addCode("\t\t.writeIntLE(payload.identifier())");

            // deserialization
            String deserializeMethodName = "deserialize" + name;
            if (computedDeserializers.contains(deserializeMethodName)) {
                String prx = schema.packagePrefix();
                if (prx.isEmpty()) {
                    prx = camelize(parentPackageName(constructor.name()));
                }
                char up = prx.charAt(0);
                deserializeMethodName = "deserialize" + Character.toUpperCase(up) + prx.substring(1) + name;
            }

            computedDeserializers.add(deserializeMethodName);

            TypeName typeName = ClassName.get(packageName, "Immutable" + name);
            if (attributes.isEmpty()) {
                deserializeMethod.addCode("case $L: return (T) $T.of();\n",
                        "0x" + Integer.toHexString(constructor.id()), typeName);
            } else {
                deserializeMethod.addCode("case $L: return (T) $L(payload);\n",
                        "0x" + Integer.toHexString(constructor.id()), deserializeMethodName);

                MethodSpec.Builder deserializerBuilder = MethodSpec.methodBuilder(deserializeMethodName)
                        .returns(typeName)
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .addParameter(ParameterSpec.builder(ByteBuf.class, "payload").build());

                boolean withFlags = attributes.contains(flagParameter);
                if (withFlags) {
                    ClassName builder = ClassName.get(packageName, "Immutable" + name, "Builder");
                    deserializerBuilder.addCode("$T builder = $T.builder();\n", builder, typeName);
                    deserializerBuilder.addCode("int flags = payload.readIntLE();\n");
                    deserializerBuilder.addCode("return builder");
                } else {
                    deserializerBuilder.addCode("return $T.builder()", typeName);
                }

                for (TlParam param : attributes) {
                    String paramName = formatFieldName(param);
                    String paramTypeLower = param.type().toLowerCase();

                    // serialization
                    String method0 = serializeMethod(constructor, param);
                    if (method0 != null) {
                        serializerBuilder.addCode("\n\t\t." + method0, paramName);
                    }

                    // deserialization
                    if (param.type().equals("#")) {
                        continue;
                    }

                    String unwrapping = deserializeMethod(paramTypeLower);
                    deserializerBuilder.addCode("\n\t\t.$L(" + unwrapping + ")", paramName);

                    TypeName paramType = parseType(param.type(), schema);

                    MethodSpec.Builder attribute = MethodSpec.methodBuilder(paramName)
                            .addModifiers(Modifier.PUBLIC);

                    boolean optionalInExt = currTypeTree.getOrDefault(qualifiedTypeName, Collections.emptyList()).stream()
                            .filter(c -> normalizeName(c.name()).equals(normalizeName(c.type())))
                            .flatMap(c -> c.params().stream())
                            .anyMatch(p -> p.type().startsWith("flags.") &&
                                    p.name().equals(param.name()));

                    if (param.type().startsWith("flags.")) {
                        if (param.type().endsWith("true")) {
                            attribute.addModifiers(Modifier.DEFAULT);
                            attribute.addCode("return false;");
                        } else {
                            paramType = paramType.box();
                            attribute.addAnnotation(Nullable.class);
                            attribute.addModifiers(Modifier.ABSTRACT);
                        }
                    } else if (optionalInExt) {
                        paramType = paramType.box();
                        attribute.addAnnotation(Nullable.class);
                        attribute.addModifiers(Modifier.ABSTRACT);
                    } else {
                        attribute.addModifiers(Modifier.ABSTRACT);
                    }

                    spec.addMethod(attribute.returns(paramType)
                            .build());
                }

                deserializerBuilder.addCode("\n\t\t.build();");
                deserializer.addMethod(deserializerBuilder.build());
            }

            serializerBuilder.addCode(";");
            serializer.addMethod(serializerBuilder.build());

            writeTo(JavaFile.builder(packageName, spec.build())
                    .indent(INDENT)
                    .skipJavaLangImports(true)
                    .build());

            computed.put(packageName + "." + name, constructor);
        }
    }

    private void generateSuperTypes() {
        Map<String, List<TlEntityObject>> currTypeTree = collectTypeTree(schema);

        Map<String, Set<TlParam>> superTypes = currTypeTree.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        flatMapping(e -> e.getValue().stream()
                                        .flatMap(c -> c.params().stream())
                                        .filter(p -> e.getValue().stream()
                                                .allMatch(c -> c.params().contains(p))),
                                Collectors.toCollection(LinkedHashSet::new))));

        for (Map.Entry<String, Set<TlParam>> e : superTypes.entrySet()) {
            String name = normalizeName(e.getKey());
            Set<TlParam> params = e.getValue();
            String packageName = parentPackageName(e.getKey());
            String qualifiedName = e.getKey();

            boolean canMakeEnum = currTypeTree.get(qualifiedName).stream()
                    .mapToInt(c -> c.params().size()).sum() == 0 &&
                    !name.equals("Object");

            String shortenName = extractEnumName(qualifiedName);

            TypeSpec.Builder spec = canMakeEnum
                    ? TypeSpec.enumBuilder(name)
                    : TypeSpec.interfaceBuilder(name);

            spec.addModifiers(Modifier.PUBLIC);
            spec.addSuperinterface(schema.superType());

            if (canMakeEnum) {

                MethodSpec.Builder ofMethod = MethodSpec.methodBuilder("of")
                        .addParameter(int.class, "identifier")
                        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                        .returns(ClassName.get(packageName, name))
                        .beginControlFlow("switch (identifier)");

                for (TlEntityObject constructor : currTypeTree.get(qualifiedName)) {
                    String subtypeName = normalizeName(constructor.name());
                    String constName = screamilize(subtypeName.substring(shortenName.length()));

                    spec.addEnumConstant(constName, TypeSpec.anonymousClassBuilder(
                                    "$L", "0x" + Integer.toHexString(constructor.id()))
                            .build());

                    ofMethod.addCode("case $L: return $L;\n",
                            "0x" + Integer.toHexString(constructor.id()),
                            constName);

                    computed.put(packageName + "." + subtypeName, constructor);
                }

                spec.addField(int.class, "identifier", Modifier.PRIVATE, Modifier.FINAL);

                spec.addMethod(MethodSpec.constructorBuilder()
                        .addParameter(int.class, "identifier")
                        .addStatement("this.identifier = identifier")
                        .build());

                ofMethod.addCode("default: throw new IllegalArgumentException($S + Integer.toHexString(identifier));\n",
                        "Incorrect type identifier: 0x");
                ofMethod.endControlFlow();

                spec.addMethod(MethodSpec.methodBuilder("identifier")
                        .addAnnotation(Override.class)
                        .returns(TypeName.INT)
                        .addModifiers(Modifier.PUBLIC)
                        .addCode("return identifier;")
                        .build());

                spec.addMethod(ofMethod.build());
            } else {

                for (TlParam param : params) {
                    if (param.type().equals("#")) {
                        continue;
                    }

                    TypeName paramType = parseType(param.type(), schema);
                    boolean optionalInExt = currTypeTree.get(qualifiedName).stream()
                            .flatMap(c -> c.params().stream())
                            .anyMatch(p -> p.type().startsWith("flags.") &&
                                    p.name().equals(param.name()));

                    MethodSpec.Builder attribute = MethodSpec.methodBuilder(formatFieldName(param))
                            .addModifiers(Modifier.PUBLIC);

                    if (param.type().startsWith("flags.")) {
                        if (param.type().endsWith("true")) {
                            attribute.addModifiers(Modifier.DEFAULT);
                            attribute.addCode("return false;");
                        } else {
                            paramType = paramType.box();
                            attribute.addAnnotation(Nullable.class);
                            attribute.addModifiers(Modifier.ABSTRACT);
                        }
                    } else if (optionalInExt) {
                        paramType = paramType.box();
                        attribute.addAnnotation(Nullable.class);
                        attribute.addModifiers(Modifier.ABSTRACT);
                    } else {
                        attribute.addModifiers(Modifier.ABSTRACT);
                    }

                    spec.addMethod(attribute.returns(paramType)
                            .build());
                }
            }

            writeTo(JavaFile.builder(packageName, spec.build())
                    .indent(INDENT)
                    .skipJavaLangImports(true)
                    .build());
        }
    }

    private void generatePrimitives() {

        TypeSpec.Builder spec = TypeSpec.classBuilder("TlPrimitives")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethod(privateConstructor);

        for (TlEntityObject e : apiSchema.constructors()) {
            if (primitiveTypes.contains(normalizeName(e.type()).toLowerCase())) {
                String name = screamilize(e.name()) + "_ID";

                spec.addField(FieldSpec.builder(int.class, name, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer("0x" + Integer.toHexString(e.id()))
                        .build());
            }
        }

        writeTo(JavaFile.builder(getBasePackageName(), spec.build())
                .indent(INDENT)
                .skipJavaLangImports(true)
                .build());
    }

    private void preparePackages() {
        try {

            String processingPackageName = getBasePackageName();
            String template = filer.getResource(StandardLocation.ANNOTATION_PROCESSOR_PATH,
                            "", TEMPLATE_PACKAGE_INFO).getCharContent(true)
                    .toString();

            Set<String> packages = typeTree.keySet().stream()
                    .map(SourceNames::parentPackageName)
                    .filter(s -> !s.equals(processingPackageName))
                    .collect(Collectors.toSet());

            Function<TlSchema, Set<String>> methodPackagesCollector = schema -> schema.methods().stream()
                    .map(e -> getPackageName(schema, e.name(), true))
                    .collect(Collectors.toSet());

            packages.addAll(methodPackagesCollector.apply(apiSchema));
            packages.addAll(methodPackagesCollector.apply(mtprotoSchema));

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

    private TypeName parseType(String type, TlSchema schema) {
        switch (type.toLowerCase()) {
            case "!x":
            case "x": return genericType;
            case "int": return TypeName.INT;
            case "true":
            case "bool": return TypeName.BOOLEAN;
            case "long": return TypeName.LONG;
            case "double": return TypeName.DOUBLE;
            case "bytes":
            case "int128":
            case "int256": return TypeName.get(byte[].class);
            case "string": return TypeName.get(String.class);
            case "object": return ClassName.OBJECT;
            case "jsonvalue": return ClassName.get(JsonNode.class);
            default:
                Matcher flag = FLAG_PATTERN.matcher(type);
                if (flag.matches()) {
                    String innerTypeRaw = flag.group(2);
                    TypeName innerType = parseType(innerTypeRaw, schema);
                    return innerTypeRaw.equals("true") ? innerType : innerType.box();
                }

                Matcher vector = VECTOR_PATTERN.matcher(type);
                if (vector.matches()) {
                    TypeName templateType = parseType(vector.group(1), schema);
                    return ParameterizedTypeName.get(ClassName.get(List.class), templateType.box());
                }

                String packageName = getPackageName(schema, type, false);
                return ClassName.get(packageName, normalizeName(type));
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

    private String getBasePackageName() {
        return currentElement.getQualifiedName().toString();
    }

    private Flag parseFlag(TlParam param) {
        Matcher matcher = FLAG_PATTERN.matcher(param.type());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Incorrect flag type: " + param.name() + "#" + param.type());
        }

        int position = Integer.parseInt(matcher.group(1));
        String type = matcher.group(2);
        return new Flag(position, param, type);
    }

    private void writeTo(JavaFile file) {
        try {
            file.writeTo(filer);
        } catch (Throwable t) {
            throw Exceptions.propagate(t);
        }
    }

    private String extractEnumName(String type) {
        return typeTree.getOrDefault(type, Collections.emptyList()).stream()
                .map(c -> normalizeName(c.name()))
                .reduce(Strings::findCommonPart)
                .orElseGet(() -> normalizeName(type));
    }

    private String deserializeMethod(String type) {
        switch (type.toLowerCase()) {
            case "true": return "true";
            case "bool": return "payload.readIntLE() == BOOL_TRUE_ID";
            case "int": return "payload.readIntLE()";
            case "long": return "payload.readLongLE()";
            case "double": return "payload.readDoubleLE()";
            case "bytes": return "deserializeBytes(payload)";
            case "string": return "deserializeString(payload)";
            case "int128": return "readInt128(payload)";
            case "int256": return "readInt256(payload)";
            case "jsonvalue": return "deserializeJsonNode(payload)";
            default:
                Matcher flag = FLAG_PATTERN.matcher(type);
                if (flag.matches()) {
                    int position = Integer.parseInt(flag.group(1));
                    String typeRaw = flag.group(2);

                    String innerMethod = deserializeMethod(typeRaw);
                    if (typeRaw.equals("true")) {
                        return "(flags & " + (1 << position) + ") != 0";
                    }
                    return "(flags & " + (1 << position) + ") != 0 ? " + innerMethod + " : null";
                }

                Matcher vector = VECTOR_PATTERN.matcher(type);
                if (vector.matches()) {
                    String innerType = vector.group(1).toLowerCase();
                    String specific = "";
                    switch (innerType) {
                        case "int":
                        case "long":
                        case "bytes":
                        case "string":
                            specific = Character.toUpperCase(innerType.charAt(0)) + innerType.substring(1);
                            break;
                    }

                    // NOTE: bare vectors (msg_container, future_salts)
                    if (type.contains("%")) {
                        return "deserializeVector0(payload, true, TlDeserializer::deserializeMessage)";
                    } else if (type.contains("future_salt")) {
                        return "deserializeVector0(payload, true, TlDeserializer::deserializeFutureSalt)";
                    }
                    return "deserialize" + specific + "Vector(payload)";
                }
                return "deserialize(payload)";
        }
    }

    @Nullable
    private String serializeMethod(TlEntityObject object, TlParam param) {
        String wrapping = "payload.$L()";
        String method0;
        String paramTypeLower = param.type().toLowerCase();
        switch (paramTypeLower) {
            case "true":
                method0 = null;
                break;
            case "bool":
                method0 = "writeIntLE";
                wrapping = "payload.$L() ? BOOL_TRUE_ID : BOOL_FALSE_ID";
                break;
            case "#":
                wrapping = object.params().stream()
                        .filter(p -> p.type().startsWith("flags."))
                        .map(this::parseFlag)
                        .map(f -> String.format("(payload.%s()%s ? 1 : 0) << %d", formatFieldName(f.getParam()),
                                f.getType().equals("true") ? "" : " != null",
                                f.getPosition()))
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
                wrapping = "serializeString(allocator, payload.$L())";
                method0 = "writeBytes";
                break;
            case "bytes":
                wrapping = "serializeBytes(allocator, payload.$L())";
                method0 = "writeBytes";
                break;
            case "jsonvalue":
                wrapping = "serializeJsonNode(allocator, payload.$L())";
                method0 = "writeBytes";
                break;
            case "object":
                wrapping = "serializeUnknown(allocator, payload.$L())";
                method0 = "writeBytes";
                break;
            default:
                Matcher vector = VECTOR_PATTERN.matcher(paramTypeLower);
                if (vector.matches()) {
                    String innerType = vector.group(1);
                    String specific = "";
                    switch (innerType.toLowerCase()) {
                        case "int":
                        case "long":
                        case "bytes":
                        case "string":
                            specific = Character.toUpperCase(innerType.charAt(0)) + innerType.substring(1);
                            break;
                    }
                    wrapping = "serialize" + specific + "Vector(allocator, payload.$L())";
                } else if (paramTypeLower.startsWith("flags.")) {
                    if (paramTypeLower.endsWith("true")) {
                        method0 = null;
                        break;
                    }
                    wrapping = "serializeFlags(allocator, payload.$L())";
                } else {
                    wrapping = "serialize(allocator, payload.$L())";
                }
                method0 = "writeBytes";
        }

        if (method0 != null) {
            return method0 + "(" + wrapping + ")";
        }
        return null;
    }

    private String getPackageName(TlSchema schema, String type, boolean method) {
        StringJoiner pckg = new StringJoiner(".");
        pckg.add(getBasePackageName());

        if (method) {
            pckg.add(METHOD_PACKAGE_PREFIX);
        }

        if (!schema.packagePrefix().isEmpty()) {
            pckg.add(schema.packagePrefix());
        }

        int dot = type.lastIndexOf('.');
        if (dot != -1) {
            pckg.add(type.substring(0, dot));
        }

        return pckg.toString();
    }

    private Map<String, List<TlEntityObject>> collectTypeTree(TlSchema schema) {
        return schema.constructors().stream()
                .filter(c -> !ignoredTypes.contains(normalizeName(c.type()).toLowerCase()))
                .collect(Collectors.groupingBy(c -> getPackageName(schema, c.type(), false)
                        + "." + normalizeName(c.type())));
    }

    @Nullable
    private ClassName awareSuperType(String type) {
        switch (type) {
            case "SendMessage":
            case "SendMedia":
                return ClassName.get(UTIL_PACKAGE, "BaseSendMessageRequest");
            case "UpdateNewMessage":
            case "UpdateEditMessage":
            case "UpdateNewChannelMessage":
                return ClassName.get(UTIL_PACKAGE, "PtsUpdate");

            case "MsgDetailedInfo":
            case "MsgResendReq":
            case "MsgsAck":
            case "MsgsAllInfo":
            case "MsgsStateInfo":
            case "MsgsStateReq":
                return ClassName.get(RpcMethod.class);
            default:
                if (type.endsWith("Empty")) {
                    return ClassName.get(EmptyObject.class);
                }

                return null;
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
