package telegram4j.tl;

final class Strings {

    private Strings() {}

    static String camelize(String type) {
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
}
