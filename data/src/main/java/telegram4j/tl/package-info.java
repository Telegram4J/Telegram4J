/**
 * Telegram MTProto requests created as immutable Jackson objects.
 */
@GenerateSchema
@Value.Style(
        depluralize = true,
        jdkOnly = true,
        allParameters = true,
        defaultAsDefault = true
)
@NonNullApi
package telegram4j.tl;

import org.immutables.value.Value;
import reactor.util.annotation.NonNullApi;
