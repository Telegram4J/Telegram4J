package telegram4j.mtproto.payload;

import telegram4j.mtproto.MTProtoSession;

import java.util.function.Function;

@FunctionalInterface
public interface PayloadMapperStrategy extends Function<MTProtoSession, PayloadMapper> {

    PayloadMapperStrategy ENCRYPTED = EncryptedPayloadMapper::new;

    PayloadMapperStrategy UNENCRYPTED = UnencryptedPayloadMapper::new;
}
