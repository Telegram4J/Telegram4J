rootProject.name = "Telegram4J"

include("core")
include("mtproto")

val version: String by settings
val isJitpack = System.getenv("JITPACK") == "true"
val isSnapshot = version.endsWith("-SNAPSHOT")

dependencyResolutionManagement {

    repositories {
        mavenLocal()
        mavenCentral()

        if (isJitpack || isSnapshot) {
            maven("https://s01.oss.sonatype.org/content/repositories/snapshots")
        }
    }
}
