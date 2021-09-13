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
import org.immutables.value.Value;
import reactor.util.annotation.Nullable;
import telegram4j.json.api.tl.TlObject;
import telegram4j.tl.model.TlConstructor;
import telegram4j.tl.model.TlParam;
import telegram4j.tl.model.TlSchema;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
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

    private static final List<String> ignoredTypes = Collections.unmodifiableList(Arrays.asList(
            "bool", "true", "false", "null", "int", "long",
            "string", "flags", "vector", "#"));

    private static final ClassName LIST_CLASS_NAME = ClassName.get(List.class);
    private static final ClassName OPTIONAL_CLASS_NAME = ClassName.get(Optional.class);

    private static final String API_SCHEMA = "api.json";
    private static final String INDENT = "    ";
    private static final Pattern FLAG_PATTERN = Pattern.compile("^flags\\.\\d+\\?(.+)");
    private static final Pattern VECTOR_PATTERN = Pattern.compile("^([vV]ector<)([A-Za-z0-9._<>]+)>$");

    private final Map<String, TlConstructor> computed = new HashMap<>();

    private Filer filer;
    private Messager messager;
    private Elements elements;
    private Types types;
    private Trees trees;
    private RoundEnvironment roundEnv;
    private ObjectMapper mapper;
    private Symbol.PackageSymbol currentElement;

    private int progress = 0x1;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
        this.elements = processingEnv.getElementUtils();
        this.types = processingEnv.getTypeUtils();
        this.trees = Trees.instance(processingEnv);

        this.mapper = JsonMapper.builder()
                .addModules(new Jdk8Module())
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .visibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
                .visibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.PUBLIC_ONLY)
                .visibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY)
                .serializationInclusion(JsonInclude.Include.NON_ABSENT)
                .build();
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
            return false;
        }

        this.roundEnv = roundEnv;

        try {
            String packageName = currentElement.getQualifiedName().toString();

            InputStream in = filer.getResource(StandardLocation.ANNOTATION_PROCESSOR_PATH, "", API_SCHEMA).openInputStream();
            TlSchema schema = mapper.readValue(in, TlSchema.class);

            if ((progress & STAGE_GENERAL_SUPERCLASSES) != 0) {
                Map<String, Integer> counter = new HashMap<>();
                Set<TlConstructor> generalSuperclasses = schema.constructors().stream()
                        .filter(c -> !ignoredTypes.contains(normalizeName(c.type()).toLowerCase()))
                        .peek(c -> counter.compute(normalizeName(c.type()),
                                (s1, integer) -> integer == null ? 1 : integer + 1))
//                        .filter(c -> counter.get(normalizeName(c.type())) > 1)
                        .sorted(Comparator.comparingInt(tl -> tl.params().stream()
                                .mapToInt(param -> counter.getOrDefault(normalizeName(param.type()), 0))
                                .min().orElse(0)))
                        .filter(c -> normalizeName(c.type()).equalsIgnoreCase(normalizeName(c.predicate())))
                        .collect(Collectors.toCollection(LinkedHashSet::new));

                counter.clear();

                for (TlConstructor constructor : generalSuperclasses) {
                    String name = normalizeName(constructor.type());
                    if (computed.containsKey(name)) {
                        continue;
                    }

                    TypeSpec.Builder superType = TypeSpec.interfaceBuilder(name)
                            .addModifiers(Modifier.PUBLIC);

                    AnnotationSpec.Builder immutable = AnnotationSpec.builder(Value.Immutable.class);
                    if (constructor.params().stream().allMatch(param -> FLAG_PATTERN.matcher(param.type()).matches())) {
                        immutable.addMember("singleton", "true");
                    }
                    superType.addAnnotation(immutable.build());

                    for (TlParam param : constructor.params()) {
                        String paramType = normalizeName(param.type());
                        if (ignoredTypes.contains(paramType.toLowerCase())) {
                            continue;
                        }
                        String qualifiedName = param.type().startsWith("flags.")
                                ? param.type()
                                : packageName + "." + normalizeName(param.type());

                        TypeName fieldType = unwrapType(qualifiedName);
                        if(fieldType == null) {
                            messager.printMessage(Diagnostic.Kind.NOTE,
                                    "Unknown field type: " + constructor.predicate() +
                                            "#" + param.name() + " (" + param.type() + ")");

                            //return false;
                            continue;
                        }

                        superType.addMethod(MethodSpec.methodBuilder(formatFieldName(param.name()))
                                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                .returns(fieldType)
                                .build());
                    }

                    JavaFile.builder(packageName, superType.build())
                            .indent(INDENT)
                            .build()
                            .writeTo(filer);

                    computed.put(name, constructor);
                }

                progress &= ~STAGE_GENERAL_SUPERCLASSES;
                return false;
            }

            for (TlConstructor constructor : schema.constructors()) {
                String type = normalizeName(constructor.type());
                if (ignoredTypes.contains(type.toLowerCase())) {
                    continue;
                }

                String alias = normalizeName(constructor.predicate());
                if (computed.containsKey(type) && computed.containsKey(alias)) {
                    continue;
                }

                String name = type;
                if (computed.containsKey(type)) { // use alias if class with this type already generated
                    name = alias;
                }

                TypeSpec.Builder builder = TypeSpec.interfaceBuilder(name)
                        .addModifiers(Modifier.PUBLIC);

                if(!alias.equals(type)){
                    roundEnv.getRootElements().stream()
                            .filter(e -> e.getSimpleName().contentEquals(type))
                            .map(Element::asType)
                            .map(TypeName::get)
                            .forEach(builder::addSuperinterface);
                }

                builder.addSuperinterface(TlObject.class);

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

                Set<TlParam> collectedFields = new LinkedHashSet<>(constructor.params());
                TlConstructor supertype = computed.get(normalizeName(constructor.type()));
                if (supertype != null) {
                    collectedFields.addAll(supertype.params());
                }

                AnnotationSpec.Builder immutable = AnnotationSpec.builder(Value.Immutable.class);
                if (collectedFields.stream().allMatch(param -> FLAG_PATTERN.matcher(param.type()).matches())) {
                    immutable.addMember("singleton", "true");
                }
                builder.addAnnotation(immutable.build());

                for (TlParam param : collectedFields) {
                    String paramType = normalizeName(param.type());
                    if (ignoredTypes.contains(paramType.toLowerCase())) {
                        continue;
                    }
                    String qualifiedName = param.type().startsWith("flags.")
                            ? param.type()
                            : packageName + "." + normalizeName(param.type());

                    TypeName fieldType = unwrapType(qualifiedName);
                    if(fieldType == null) {
                        messager.printMessage(Diagnostic.Kind.NOTE,
                                "Unknown field type: " + constructor.predicate() +
                                "#" + param.name() + " (" + param.type() + ")");

                        return false;
                    }

                    builder.addMethod(MethodSpec.methodBuilder(formatFieldName(param.name()))
                            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                            .returns(fieldType)
                            .build());
                }

                JavaFile.builder(packageName, builder.build())
                        .indent(INDENT)
                        .build()
                        .writeTo(filer);

                computed.put(name, constructor);
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

    private String formatFieldName(String type) {
        type = camelize(type);

        // keyword handling
        if (!SourceVersion.isName(type)) {
            type += "State";
        }

        return type;
    }

    @Nullable
    private TypeName unwrapType(String type) {
        Matcher flag = FLAG_PATTERN.matcher(type);
        if(flag.matches()){
            TypeName innerType = mapType(flag.group(1));
            System.out.println("innerType = " + innerType);
            if (innerType == null) {
                return null;
            }

            return ParameterizedTypeName.get(OPTIONAL_CLASS_NAME, innerType.box());
        }

        int idx = type.lastIndexOf('.');
        String unpacked = idx != -1 ? type.substring(idx + 1) : type;
        return mapType(unpacked);
    }

    @Nullable
    private TypeName mapType(String type) {
        if (type.equalsIgnoreCase("int")) {
            return TypeName.INT;
        }
        if (type.equalsIgnoreCase("bool") || type.equalsIgnoreCase("true")) {
            return TypeName.BOOLEAN;
        }
        if (type.equalsIgnoreCase("long")) {
            return TypeName.LONG;
        }
        if (type.equalsIgnoreCase("bytes")) {
            return TypeName.get(byte[].class);
        }
        if (type.equalsIgnoreCase("string")) {
            return TypeName.get(String.class);
        }
        Matcher vector = VECTOR_PATTERN.matcher(type);
        if (vector.matches()) {
            String template = normalizeName(vector.group(2));
            TypeName templateType = mapType(template);
            if(templateType == null){
                return null;
            }
            return ParameterizedTypeName.get(LIST_CLASS_NAME, templateType.box());
        }
        String qualifiedName = getPackageName() + "." + normalizeName(type);
        return roundEnv.getRootElements().stream()
                .filter(e -> e.toString().equals(qualifiedName))
                .findFirst()
                .map(Element::asType)
                .map(TypeName::get)
                .orElse(null);
    }

    private String getPackageName() {
        return currentElement.getQualifiedName().toString();
    }
}
