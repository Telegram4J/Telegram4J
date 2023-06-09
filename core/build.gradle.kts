dependencies {
    api(project(":mtproto"))

    // I run test code and reproduce tests on linux,
    // and therefore it would be convenient for me to use epoll
    testImplementation(libs.netty.native.epoll) {
        artifact { classifier = "linux-x86_64" }
    }
}

description = "Java MTProto library for the Telegram API"
extra["displayName"] = "Telegram4J Core"
