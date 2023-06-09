dependencies {
    api(libs.tl.parser) { isChanging = true }
    api(libs.reactor.core)
    api(libs.reactor.addons.extra)
    api(libs.netty.handler)

    compileOnlyApi(libs.netty.native.epoll) {
        artifact { classifier = "linux-x86_64" }
    }
    compileOnlyApi(libs.netty.native.kqueue) {
        artifact { classifier = "osx-x86_64" }
    }

    api(libs.jackson.databind)
    api(libs.caffeine)
}

description = "TCP client written with Reactor Netty for the Telegram API"
extra["displayName"] = "Telegram4J MTProto"
