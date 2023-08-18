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
module telegram4j.core {
    requires reactor.core;
    requires org.reactivestreams;
    requires io.netty.buffer;
    requires io.netty.common;
    requires io.netty.transport;
    requires reactor.extra;
    requires com.fasterxml.jackson.databind;
    requires com.github.benmanes.caffeine;

    requires transitive telegram4j.mtproto;

    requires static com.google.errorprone.annotations;

    exports telegram4j.core;
    exports telegram4j.core.auxiliary;
    exports telegram4j.core.handle;
    exports telegram4j.core.event;
    exports telegram4j.core.event.domain;
    exports telegram4j.core.event.domain.chat;
    exports telegram4j.core.event.domain.inline;
    exports telegram4j.core.event.domain.message;
    exports telegram4j.core.object;
    exports telegram4j.core.object.chat;
    exports telegram4j.core.object.markup;
    exports telegram4j.core.object.media;
    exports telegram4j.core.retriever;
    exports telegram4j.core.spec;
    exports telegram4j.core.spec.inline;
    exports telegram4j.core.spec.markup;
    exports telegram4j.core.spec.media;
    exports telegram4j.core.util;
    exports telegram4j.core.util.parser;
    exports telegram4j.core.auth;
}
