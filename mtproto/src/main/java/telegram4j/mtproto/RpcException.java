/*
 * Copyright 2023 Telegram4J
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package telegram4j.mtproto;

import telegram4j.tl.api.TlMethod;
import telegram4j.tl.mtproto.RpcError;

import java.io.Serial;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.IntStream;

/** Subtype of {@link MTProtoException} which receives on RPC response. */
public final class RpcException extends MTProtoException {
    @Serial
    private static final long serialVersionUID = 909166276209425603L;

    private final RpcError error;
    private final TlMethod<?> method;

    public RpcException(String message, RpcError error, TlMethod<?> method) {
        super(message);
        this.error = Objects.requireNonNull(error);
        this.method = Objects.requireNonNull(method);
    }

    /**
     * Create {@link Predicate} for throwable which matches on
     * {@code RpcException} with one of specified error codes.
     *
     * @param codes The array of rpc method errors codes
     * @return A {@link Predicate} for throwable which matches
     * on {@code RpcException} with one of specified error codes.
     */
    public static Predicate<Throwable> isErrorCode(int... codes) {
        return t -> t instanceof RpcException t0 && IntStream.of(codes).anyMatch(c -> t0.error.errorCode() == c);
    }

    /**
     * Create {@link Predicate} for throwable which matches on flood wait.
     *
     * @return A {@link Predicate} for throwable which matches on flood wait errors.
     */
    public static Predicate<Throwable> isFloodWait() {
        return t -> t instanceof RpcException t0 && t0.error.errorCode() == 420 &&
                t0.error.errorMessage().startsWith("FLOOD_WAIT_");
    }

    /**
     * Create {@link Predicate} for throwable which matches on specified error message.
     *
     * @param message The error message to match.
     * @return A {@link Predicate} for throwable which matches on specified error message.
     */
    public static Predicate<Throwable> isErrorMessage(String message) {
        return t -> t instanceof RpcException t0 && t0.error.errorMessage().equals(message);
    }

    /**
     * Gets original {@link RpcError} received as method response.
     *
     * @return The original {@link RpcError}.
     */
    public RpcError getError() {
        return error;
    }

    /**
     * Gets original request which received this error.
     *
     * @return The original {@link TlMethod} request.
     */
    public TlMethod<?> getMethod() {
        return method;
    }
}
