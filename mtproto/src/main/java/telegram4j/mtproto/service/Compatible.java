package telegram4j.mtproto.service;

import java.lang.annotation.*;

/**
 * Indicates service or method to compatible with different
 * account types. By default, if this annotation isn't present
 * method should consider as user-compatible.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Compatible {

    Type value();

    enum Type {
        /** Method compatible with any users. */
        USER,

        /** Method compatible with any bots. */
        BOT,

        /** Method compatible with both account types. */
        BOTH
    }
}
