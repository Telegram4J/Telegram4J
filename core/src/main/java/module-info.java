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
