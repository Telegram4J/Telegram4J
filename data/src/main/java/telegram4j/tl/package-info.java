/**
 * Telegram objects created as immutable objects.
 */
@GenerateSchema
@Value.Style(
        depluralize = true,
        jdkOnly = true,
        allMandatoryParameters = true,
        defaultAsDefault = true
)
@NonNullApi
package telegram4j.tl;

import org.immutables.value.Value;
import reactor.util.annotation.NonNullApi;
