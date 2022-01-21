package telegram4j.core.spec;

import org.immutables.value.Value;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PACKAGE)
@Retention(RetentionPolicy.CLASS)
@Value.Style(
        typeAbstract = "*Def",
        typeImmutable = "*",
        visibility = Value.Style.ImplementationVisibility.PUBLIC,
        deepImmutablesDetection = true,
        allMandatoryParameters = true,
        depluralize = true,
        instance = "create",
        defaultAsDefault = true,
        jdkOnly = true
)
public @interface SpecStyle {
}
