dependencies {
    api(libs.tl.parser)
    api(libs.reactor.addons.extra)
    api(libs.netty.handler)

    compileOnly(libs.netty.native.epoll) {
        artifact {
            classifier = osdetector.classifier
        }
    }

    compileOnly(libs.netty.native.kqueue) /*{
        artifact {
            classifier = osdetector.classifier
        }
    }*/

    api(libs.jackson.databind)
    api(libs.caffeine)
}

description = "TCP client written with Reactor Netty for the Telegram API"
extra["displayName"] = "Telegram4J MTProto"
