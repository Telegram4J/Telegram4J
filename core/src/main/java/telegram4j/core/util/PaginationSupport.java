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
package telegram4j.core.util;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

/**
 * Template methods for various types of pagination.
 *
 * @see <a href="https://core.telegram.org/api/offsets">Pagionation</a>
 */
public final class PaginationSupport {

    private PaginationSupport() {}

    /**
     * Computes loop-like {@link Flux} for offset-based pagination.
     *
     * @param <T> The element type for retrieving.
     * @param prod A function to exchange updated offset to container of entities.
     * @param countExtractor A function to get count of entities in the produced container.
     * @param offset The first offset for pagination.
     * @param limit The constant limit for retrieving objects.
     * @return A {@link Flux}, emitting retrieved containers with entities.
     */
    public static <T> Flux<T> paginate(IntFunction<? extends Publisher<T>> prod, ToIntFunction<T> countExtractor, int offset, int limit) {
        AtomicInteger localOffset = new AtomicInteger(offset);
        AtomicInteger count = new AtomicInteger(-1);

        return Flux.defer(() -> prod.apply(localOffset.get()))
                .doOnNext(c -> {
                    count.set(countExtractor.applyAsInt(c));
                    localOffset.addAndGet(limit);
                })
                .repeat(() -> count.get() > localOffset.get());
    }
}
