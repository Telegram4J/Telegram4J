package telegram4j.mtproto.service;

import telegram4j.mtproto.RpcException;

import java.lang.annotation.*;

/**
 * Indicates object or method compatible with bot users.
 * All unmarked methods can be considered user methods.
 * When trying to invoke user compatible method from bot
 * will throw {@link RpcException} exception.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface BotCompatible {
}
