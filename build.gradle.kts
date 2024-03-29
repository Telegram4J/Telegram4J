import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    `java-library`
    `maven-publish`
    signing
    idea
    alias(libs.plugins.versions)
}

val isJitpack = System.getenv("JITPACK") == "true"
val isSnapshot = version.toString().endsWith("-SNAPSHOT")

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = "com.github.ben-manes.versions")
    apply(plugin = "idea")

    if (!isJitpack && !isSnapshot) {
        apply(plugin = "signing")
    }

    val archiveBaseName = "telegram4j-$name"

    idea {
        module {
            isDownloadSources = true
        }
    }

    dependencies {
        api(platform(rootProject.libs.reactor.bom))
        api(platform(rootProject.libs.netty.bom))

        compileOnly(rootProject.libs.jsr305)

        testImplementation(rootProject.libs.logback)
        testImplementation(rootProject.libs.junit)
    }

    java {
        withJavadocJar()
        withSourcesJar()
    }

    configurations.all {
        resolutionStrategy.cacheChangingModulesFor(1, TimeUnit.HOURS)
    }

    tasks.withType<JavaCompile> {
        options.javaModuleVersion.set(version.toString())
        options.encoding = "UTF-8"
        options.release.set(17)
    }

    tasks.withType<AbstractArchiveTask> {
        this.archiveBaseName.set(archiveBaseName)
    }

    tasks.test {
        useJUnitPlatform()

        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
        }
    }

    tasks.withType<Javadoc> {
        afterEvaluate {
            val displayName: String by extra
            title = "$displayName API reference ($version)"
        }

        options {
            encoding = "UTF-8"
            locale = "en_US"
            val opt = this as StandardJavadocDocletOptions
            opt.addBooleanOption("html5", true)
            opt.addStringOption("encoding", "UTF-8")

            tags = listOf(
                "apiNote:a:API Note:",
                "implSpec:a:Implementation Requirements:",
                "implNote:a:Implementation Note:"
            )
            links = listOf(
                "https://projectreactor.io/docs/core/release/api/",
                "https://fasterxml.github.io/jackson-databind/javadoc/2.14/",
                "https://www.reactive-streams.org/reactive-streams-1.0.3-javadoc/",
                "https://netty.io/4.1/api/"
            )
        }
    }

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                groupId = "com.telegram4j"
                artifactId = archiveBaseName

                versionMapping {
                    usage("java-api") {
                        fromResolutionOf("runtimeClasspath")
                    }
                    usage("java-runtime") {
                        fromResolutionResult()
                    }
                }

                pom {
                    val prov = providers.provider {
                        val displayName: String by extra
                        return@provider displayName
                    }

                    name.set(prov)
                    description.set(project.description)
                    url.set("https://github.com/Telegram4J/Telegram4J")
                    inceptionYear.set("2021")

                    developers {
                        developer { name.set("The Telegram4J") }
                    }

                    licenses {
                        license {
                            name.set("Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                            distribution.set("repo")
                        }
                    }

                    scm {
                        url.set("https://github.com/Telegram4J/Telegram4J")
                        connection.set("scm:git:git://github.com/Telegram4J/Telegram4J.git")
                        developerConnection.set("scm:git:ssh://git@github.com:Telegram4J/Telegram4J.git")
                    }
                }

                if (!isJitpack) {
                    repositories {
                        maven {
                            if (isSnapshot) {
                                url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
                            } else {
                                url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2")
                            }

                            val sonatypeUsername: String? by project
                            val sonatypePassword: String? by project
                            if (sonatypeUsername != null && sonatypePassword != null) {
                                credentials {
                                    username = sonatypeUsername
                                    password = sonatypePassword
                                }
                            }
                        }
                    }
                }
            }

            if (!isJitpack && !isSnapshot) {
                signing {
                    val signingKey: String? by project
                    val signingPassword: String? by project
                    if (signingKey != null && signingPassword != null) {
                        useInMemoryPgpKeys(signingKey, signingPassword)
                    }
                    sign(this@publications["mavenJava"])
                }
            }
        }
    }
}
