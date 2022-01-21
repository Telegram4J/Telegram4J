package telegram4j.core.spec;

import org.immutables.value.Value;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
@Value.Style(
        depluralize = true,
        allMandatoryParameters = true,
        jdkOnly = true
)
@interface FieldsStyle {
}
