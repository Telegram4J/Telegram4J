package telegram4j.tl;

import java.util.function.UnaryOperator;

class NameTransformer implements UnaryOperator<String> {

    private final String s;
    private final String r;

    NameTransformer(String s, String r) {
        this.s = s;
        this.r = r;
    }

    static NameTransformer create(String s, String r) {
        return new NameTransformer(s, r);
    }

    @Override
    public String apply(String s) {
        if (s.endsWith(this.s)) {
            return s.replace(this.s, this.r);
        }
        return s;
    }
}
