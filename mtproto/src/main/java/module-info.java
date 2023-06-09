module telegram4j.mtproto {
    requires reactor.core;
    requires reactor.extra;
    requires org.reactivestreams;
    requires io.netty.buffer;
    requires io.netty.handler;
    requires io.netty.codec;
    requires io.netty.common;
    requires io.netty.transport;
    requires com.github.benmanes.caffeine;
    requires com.fasterxml.jackson.databind;

    requires static io.netty.transport.classes.epoll;
    requires static io.netty.transport.classes.kqueue;

    requires transitive telegram4j.tl;

    exports telegram4j.mtproto;
    exports telegram4j.mtproto.file;
    exports telegram4j.mtproto.auth;
    exports telegram4j.mtproto.service;
    exports telegram4j.mtproto.store;
    exports telegram4j.mtproto.transport;
    exports telegram4j.mtproto.util;
    exports telegram4j.mtproto.store.object;
    exports telegram4j.mtproto.client;
    exports telegram4j.mtproto.resource;
}
