/*
 * Copyright 2023 Telegram4J
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package telegram4j.mtproto.store;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListMap;
import telegram4j.mtproto.secret.SecretChatUtils;

/**
 * An outbox which keeps what it holds on disk, so that a hole opened before a restart can still
 * be answered afterwards.
 *
 * <p> The files hold the messages exactly as they were sent, which is to say still encrypted under
 * the key of their chat. That key lives in the session file rather than here, so these files on
 * their own are of no use to anyone; they are worth no less care than the session file for it,
 * since together the two are the conversation in the clear.
 *
 * <p> Writing is best-effort and never fails a send: an outbox that lost a write can answer one
 * fewer resend request, which is the same position as having no outbox at all. Failures are
 * logged and swallowed.
 */
public class FileSecretChatOutbox extends SecretChatOutboxImpl {
    protected static final Logger log = Loggers.getLogger(FileSecretChatOutbox.class);

    /** Where the files go when the caller expresses no preference. */
    protected static final Path DEFAULT_DIRECTORY = Path.of("./encrypted-messages");

    protected static final ObjectMapper MAPPER = new ObjectMapper();
    protected static final String FILE_PREFIX = "messages";
    protected static final String FILE_SUFFIX = ".json";

    protected final Path directory;
    protected final boolean fileForEachChat;

    public FileSecretChatOutbox() {
        this(DEFAULT_DIRECTORY);
    }

    /**
     * @param directory Where the files live. Nothing is assumed about it: a caller keeping several
     * accounts apart is the one that knows what to call them apart by.
     */
    public FileSecretChatOutbox(Path directory) {
        this(directory, DEFAULT_CAPACITY, true);
    }

    /**
     * @param fileForEachChat Whether each chat gets its own {@code messages<chatId>.json}, rather
     * than all of them sharing one {@code messages.json}. One file for each means a write touches
     * only the chat that changed, and a file that goes missing or unreadable costs that one chat.
     */
    public FileSecretChatOutbox(Path directory, int capacity, boolean fileForEachChat) {
        super(capacity);
        this.directory = Objects.requireNonNull(directory);
        this.fileForEachChat = fileForEachChat;
        load();
    }

    public Path getDirectory() {
        return directory;
    }

    @Override
    public Mono<Void> add(int chatId, Sent message) {
        return Mono.<Void>fromRunnable(() -> {
            put(chatId, message);
            save(chatId);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> deleteAll(int chatId) {
        return Mono.<Void>fromRunnable(() -> {
            remove(chatId);
            save(chatId);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    // region persistence

    protected Path fileFor(int chatId) {
        return directory.resolve(fileForEachChat
                ? FILE_PREFIX + chatId + FILE_SUFFIX
                : FILE_PREFIX + FILE_SUFFIX);
    }

    protected void load() {
        // reading would put messages straight into the map, past the check put() makes, and an
        // outbox told to keep nothing must keep nothing after a restart too
        if (capacity == 0 || !Files.isDirectory(directory)) {
            return;
        }

        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory,
                FILE_PREFIX + "*" + FILE_SUFFIX)) {
            // 2L because UTF-16 symbols are said to be allowed, 10x -- for overhead in check
            long maxExpectedSize = SecretChatUtils.MAX_ENCRYPTED_MESSAGE_SIZE * 2L * capacity * 10;
            if (!fileForEachChat) {
                maxExpectedSize *= 100;
            }
            for (Path file : files) {
                if (!Files.isRegularFile(file)) {
                    log.debug("Skipping non-regular file: {}", file);
                    continue;
                }
                long size = Files.size(file);
                if (size > maxExpectedSize) {
                    log.warn("File size {} > {} (expected maximum for storage), "
                        + "skipping file {}", size, maxExpectedSize, file);
                    continue;
                }
                read(file);
            }
        } catch (IOException e) {
            log.warn("Failed to list the secret chat outbox in {}", directory, e);
        }
    }

    protected void read(Path file) {
        try {
            JsonNode root = MAPPER.readTree(Files.readString(file));
            var chats = root.fields();
            while (chats.hasNext()) {
                var chat = chats.next();
                int chatId = Integer.parseInt(chat.getKey());
                var kept = new ConcurrentSkipListMap<Integer, Sent>();
                for (JsonNode message : chat.getValue()) {
                    var sent = deserialize(message);
                    kept.put(sent.outSeqNo(), sent);
                }
                if (!kept.isEmpty()) {
                    messages.put(chatId, kept);
                }
            }
        } catch (IOException | RuntimeException e) {
            // a file we cannot read is a file we cannot resend from, which the chat survives
            log.warn("Failed to read the secret chat outbox {}", file, e);
        }
    }

    /**
     * Writes what is kept for one chat, or for all of them when they share a file.
     *
     * <p> Serialized against itself: writing truncates the file first, so two of these running at
     * once can interleave into something that parses as nothing at all, which would cost the whole
     * chat's outbox rather than the one message that raced. Sends are what trigger it, and they
     * arrive at whichever thread the caller subscribed on.
     *
     * <p> Ordering between two of them does not matter: each writes the state it finds rather than
     * one captured earlier, so whichever writes last writes something at least as fresh.
     */
    protected synchronized void save(int chatId) {
        try {
            Files.createDirectories(directory);
            var file = fileFor(chatId);

            ObjectNode root = JsonNodeFactory.instance.objectNode();
            if (fileForEachChat) {
                var kept = messages.get(chatId);
                if (kept == null || kept.isEmpty()) {
                    Files.deleteIfExists(file);
                    return;
                }
                serialize(root, chatId, kept);
            } else {
                for (var chat : messages.entrySet()) {
                    serialize(root, chat.getKey(), chat.getValue());
                }
            }

            Files.writeString(file, MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root));
        } catch (IOException | RuntimeException e) {
            log.warn("Failed to write the secret chat outbox of chat {}", chatId, e);
        }
    }

    protected static void serialize(ObjectNode root, int chatId, Map<Integer, Sent> kept) {
        var array = root.putArray(String.valueOf(chatId));
        for (var sent : kept.values()) {
            array.addObject()
                    .put("outSeqNo", sent.outSeqNo())
                    .put("inSeqNo", sent.inSeqNo())
                    .put("layer", sent.layer())
                    .put("service", sent.service())
                    .put("payload", Base64.getEncoder().encodeToString(sent.payload()));
        }
    }

    protected static Sent deserialize(JsonNode message) {
        return new Sent(message.get("outSeqNo").asInt(), message.get("inSeqNo").asInt(),
                message.get("layer").asInt(), message.get("service").asBoolean(),
                Base64.getDecoder().decode(message.get("payload").asText()));
    }

    // endregion
}
