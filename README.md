# Telegram4J

[![Telegram Group](https://img.shields.io/endpoint?color=neon&style=flat-square&label=Telegram%20Channel&url=https%3A%2F%2Ftg.sumanjay.workers.dev%2Fdiscussion_t4j)](https://t.me/discussion_t4j)
[![Javadoc](https://javadoc.io/badge2/com.telegram4j/telegram4j-core/0.1.0/javadoc.svg)](https://javadoc.io/doc/com.telegram4j/telegram4j-core/0.1.0)
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
                .filter(e -> e.getMessage().getContent().equals("!ping"))
                .flatMap(e -> Mono.justOrEmpty(e.getChat())
                        // telegram api may not deliver chat info and in this situation it's necessary to retrieve chat
                        .switchIfEmpty(e.getMessage().getChat())
                        .flatMap(c -> c.sendMessage(SendMessageSpec.of("pong!")
                                .withReplyTo(e.getMessage()))))
                .subscribe();

        client.onDisconnect().block();
    }
}
```

For additional examples, check [this packages](https://github.com/Telegram4J/Telegram4J/tree/master/core/src/test/java/telegram4j/core) in the repository
