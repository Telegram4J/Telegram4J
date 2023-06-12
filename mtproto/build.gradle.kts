dependencies {
    api(libs.tl.parser) { isChanging = true }
    api(libs.reactor.core)
    api(libs.reactor.addons.extra)
    api(libs.netty.handler)

    compileOnly(libs.netty.native.epoll) {
        artifact { classifier = "linux-x86_64" }
    }
    compileOnly(libs.netty.native.kqueue) {
        artifact { classifier = "osx-x86_64" }
    }

    api(libs.jackson.databind)
    api(libs.caffeine)
}

description = "TCP client written with Reactor Netty for the Telegram API"
extra["displayName"] = "Telegram4J MTProto"
