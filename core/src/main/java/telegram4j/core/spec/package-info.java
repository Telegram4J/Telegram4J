/**
 * High-level request builders.
 */
@Value.Style(
        typeAbstract = "*Generator",
        typeImmutable = "*",
        visibility = Value.Style.ImplementationVisibility.PUBLIC,
        deepImmutablesDetection = true,
        allMandatoryParameters = true,
        depluralize = true,
        defaultAsDefault = true
)
@MetaEncodingEnabled
package telegram4j.core.spec;

import org.immutables.value.Value;
import telegram4j.json.encoding.MetaEncodingEnabled;
