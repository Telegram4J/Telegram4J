dependencies {
    api(rootProject.libs.tl.parser)
    api(rootProject.libs.reactor.netty.core)
    api(rootProject.libs.reactor.netty.core)
    api(rootProject.libs.reactor.addons.extra)

    api(rootProject.libs.jackson.databind)
    api(rootProject.libs.caffeine)
}

description = "TCP client written with Reactor Netty for the Telegram API"
extra["displayName"] = "Telegram4J MTProto"
