/**
 * Telegram types created as immutable Jackson objects.
 */
@Value.Style(
        depluralize = true,
        jdkOnly = true,
        allParameters = true,
        defaultAsDefault = true
)
package telegram4j.json;

import org.immutables.value.Value;
