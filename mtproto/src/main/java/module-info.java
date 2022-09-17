module telegram4j.mtproto {
    requires io.netty.buffer;
    requires reactor.core;
    requires reactor.netty.core;
    requires com.github.benmanes.caffeine;
    requires io.netty.codec;
    requires io.netty.common;
    requires org.reactivestreams;
    requires io.netty.transport;
    requires com.fasterxml.jackson.databind;

    requires transitive telegram4j.tl;

    exports telegram4j.mtproto;
    exports telegram4j.mtproto.file;
    exports telegram4j.mtproto.auth;
    exports telegram4j.mtproto.service;
    exports telegram4j.mtproto.store;
    exports telegram4j.mtproto.transport;
    exports telegram4j.mtproto.util;
}
