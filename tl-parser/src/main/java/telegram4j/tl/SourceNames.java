package telegram4j.tl;

import telegram4j.tl.model.TlParam;

import javax.lang.model.SourceVersion;
import java.util.regex.Matcher;

import static telegram4j.tl.SchemaGeneratorConsts.*;
import static telegram4j.tl.Strings.camelize;

final class SourceNames {

    private SourceNames() {
    }

    static String normalizeName(String type) {
        type = applyNamingExceptions(type);

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

    static String formatFieldName(TlParam param) {
        String name = camelize(param.name());

        char f = name.charAt(0);
        if (Character.isUpperCase(f)) {
            name = Character.toLowerCase(f) + name.substring(1);
        }

        // This is a strange and in some places illogical problem
        // solution of matching attribute names with java keywords
        if (!SourceVersion.isName(name)) {
            name += "State";
        }

        return name;
    }

    static String applyNamingExceptions(String s) {
        String l = s;
        for (NameTransformer t : namingExceptions) {
            l = t.apply(l);
        }
        return l;
    }

    static String parentPackageName(String qualifiedName) {
        int dot = qualifiedName.lastIndexOf('.');
        if (dot != -1) {
            return qualifiedName.substring(0, dot);
        }
        return "";
    }
}
