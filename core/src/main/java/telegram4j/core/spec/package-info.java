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
@NonNullApi
package telegram4j.core.spec;

import org.immutables.value.Value;
import reactor.util.annotation.NonNullApi;
