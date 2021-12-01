package telegram4j.tl;

import java.lang.annotation.*;

/**
 * Mark package as place where parsed
 * <a href="https://core.telegram.org/mtproto/TL">TL objects</a>
 * will be generated.
 */
@Documented
@Target(ElementType.PACKAGE)
@Retention(RetentionPolicy.SOURCE)
public @interface GenerateSchema {
}
