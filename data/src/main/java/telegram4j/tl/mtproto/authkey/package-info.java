/**
 * Telegram MTProto requests used in auth-key generation created as immutable Jackson objects.
 */
@Value.Style(
        depluralize = true,
        jdkOnly = true,
        allParameters = true,
        defaultAsDefault = true
)
@NonNullApi
@MetaEncodingEnabled
package telegram4j.tl.mtproto.authkey;

import org.immutables.value.Value;
import reactor.util.annotation.NonNullApi;
import telegram4j.json.encoding.MetaEncodingEnabled;
