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

import reactor.core.publisher.Mono;
import telegram4j.tl.api.TlMethod;

import java.util.function.Function;
import java.util.function.Predicate;

/** Interface for mapping rpc responses. */
public interface ResponseTransformer {

    /**
     * Create new {@code ResponseTransformer} which returns
     * on matched exceptions {@link Mono#empty()} as response for given methods scope.
     *
     * @param methodPredicate The method scope.
     * @param predicate The predicate for exceptions.
     * @return The new {@code ResponseTransformer} which returns {@link Mono#empty()} on matched errors.
     */
    static ResponseTransformer empty(MethodPredicate methodPredicate, Predicate<? super Throwable> predicate) {
        return new ResponseTransformer() {
            @Override
            public <R> Mono<R> transform(Mono<R> mono, TlMethod<? extends R> method) {
                if (methodPredicate.test(method)) {
                    return mono.onErrorResume(predicate, t -> Mono.empty());
                }
                return mono;
            }
        };
    }

    /**
     * Create new {@code ResponseTransformer} which reties signals
     * on flood wait errors for given methods scope.
     *
     * @param methodPredicate The method scope.
     * @param retrySpec The retry spec for flood wait errors.
     * @return The new {@code ResponseTransformer} which retries signals on flood wait for matched methods.
     */
    static ResponseTransformer retryFloodWait(MethodPredicate methodPredicate, MTProtoRetrySpec retrySpec) {
        return new ResponseTransformer() {
            @Override
            public <R> Mono<R> transform(Mono<R> mono, TlMethod<? extends R> method) {
                if (methodPredicate.test(method)) {
                    return mono.retryWhen(retrySpec);
                }
                return mono;
            }
        };
    }

    /**
     * Modifies specified reactive sequence with rpc response.
     *
     * @param <R> The type of method response.
     * @param mono The mono for transformation.
     * @param method The method which returns this response.
     * @return A {@link Function} which modifies response sequence.
     */
    <R> Mono<R> transform(Mono<R> mono, TlMethod<? extends R> method);
}
