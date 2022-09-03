# Telegram4J

[![Telegram Group](https://img.shields.io/endpoint?color=neon&style=flat-square&label=Telegram%20Channel&url=https%3A%2F%2Ftelegram-badge-4mbpu8e0fit4.runkit.sh%2F%3Furl%3Dhttps%3A%2F%2Ft.me%2Fdiscussion_t4j)](https://t.me/discussion_t4j)
[![Javadoc](https://javadoc.io/badge2/com.telegram4j/telegram4j-core/0.1.0-SNAPSHOT/javadoc.svg)](https://javadoc.io/doc/com.telegram4j/telegram4j-core/0.1.0-SNAPSHOT)
![Build Status](https://github.com/Telegram4J/Telegram4J/actions/workflows/build.yml/badge.svg?branch=master)

Reactive Java library for Telegram [MTProto](https://core.telegram.org/mtproto) API written with Java 11.

## Installation

### Gradle

```groovy
repositories {
    mavenCentral()
    maven { url 'https://s01.oss.sonatype.org/content/repositories/snapshots' }
}

dependencies {
    implementation 'com.telegram4j:telegram4j-core:0.1.0-SNAPSHOT'
}
```

### Maven

```xml
<dependencies>
  <dependency>
    <groupId>com.telegram4j</groupId>
    <artifactId>telegram4j-core</artifactId>
    <version>0.1.0-SNAPSHOT</version>
  </dependency>
</dependencies>
```

## Example usage
Example of a bot that will respond to a message with text !ping reply with the text pong!

```java
public class ExampleReplyBot {
    public static void main(String[] args) {
        // values from https://core.telegram.org/api/obtaining_api_id#obtaining-api-id
        int apiId = "<api id>";
        String apiHash = "<api hash>";
        // value from https://t.me/BotFather DM
        String botAuthToken = "<bot auth token>";
        MTProtoTelegramClient client = MTProtoTelegramClient.create(apiId, apiHash, botAuthToken).connect().block();
        
        client.on(SendMessageEvent.class)
                .filter(e -> e.getMessage().getContent().map("!ping"::equals).orElse(false))
                .flatMap(e -> Mono.justOrEmpty(e.getChat())
                        .flatMap(c -> c.sendMessage(SendMessageSpec.of("pong!")
                                .withReplyToMessageId(e.getMessage().getId()))))
                .subscribe();

        client.onDisconnect().block();
    }
}
```

For additional examples, check the [test packages](https://github.com/Telegram4J/Telegram4J/tree/master/core/src/test/java/telegram4j/core) in the repository
