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

// Interactive console sample; see telegram4j.example.SecretChatConsole
tasks.register<JavaExec>("runSecretChatConsole") {
    group = "application"
    description = "Signs in with a real account and answers ordinary and secret chats"
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("telegram4j.example.SecretChatConsole")
    standardInput = System.`in`
}
