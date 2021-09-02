package telegram4j.json.encoding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@ChatIdEncodingEnabled
@IdEncodingEnabled
@OptionalIdEncodingEnabled
@Target(ElementType.PACKAGE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MetaEncodingEnabled {
}
