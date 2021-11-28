package telegram4j.tl;

final class Strings {

    private Strings() {}

    static String camelize(String type) {
        if (!type.contains("_") && !type.contains(".")) {
            return type;
        }

        StringBuilder builder = new StringBuilder(type.length());
        for (int i = 0; i < type.length(); i++) {
            char c = type.charAt(i);
            if (c == '_' || c == '.') {
                char n = Character.toUpperCase(type.charAt(i++ + 1));
                builder.append(n);
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    static String screamilize(String type) {
        StringBuilder buf = new StringBuilder(type.length());
        for (int i = 0; i < type.length(); i++) {
            char p = i - 1 != -1 ? type.charAt(i - 1) : Character.MIN_VALUE;
            char c = type.charAt(i);

            if (Character.isLetter(c) && Character.isLetter(p) &&
                    Character.isLowerCase(p) && Character.isUpperCase(c)) {
                buf.append('_');
            }

            if (c == '.' || c == '-' || c == '_' || Character.isWhitespace(c) &&
                    Character.isLetterOrDigit(p) && i + 1 < type.length() &&
                    Character.isLetterOrDigit(type.charAt(i + 1))) {

                buf.append('_');
            } else {
                buf.append(Character.toUpperCase(c));
            }
        }
        return buf.toString();
    }

    static String findCommonPart(String s1, String s2) {
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
}
