dependencies {
    api(libs.tl.parser)
    api(libs.reactor.core)
    api(libs.reactor.addons.extra)
    api(libs.netty.handler)

    compileOnly(libs.netty.native.epoll)
    compileOnly(libs.netty.native.kqueue)

    api(libs.jackson.databind)
    api(libs.caffeine)
}

description = "TCP client written with Reactor Netty for the Telegram API"
extra["displayName"] = "Telegram4J MTProto"
