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
package telegram4j.example;

import java.util.Objects;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import reactor.core.scheduler.Schedulers;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.auth.AuthorizationHandler.Resources;
import telegram4j.core.auth.CodeAuthorizationHandler;
import telegram4j.core.auth.CodeAuthorizationHandler.Code;
import telegram4j.core.auth.CodeAuthorizationHandler.CodeAction;
import telegram4j.core.auth.CodeAuthorizationHandler.PhoneCodeContext;
import telegram4j.core.auth.CodeAuthorizationHandler.PhoneNumberAction;
import telegram4j.core.auth.CodeAuthorizationHandler.PhoneNumberContext;
import telegram4j.core.auth.TwoFactorHandler;
import telegram4j.core.event.domain.message.SendMessageEvent;
import telegram4j.core.object.Message;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.util.Id;
import telegram4j.core.util.PeerId;
import telegram4j.mtproto.secret.SecretChatState;
import telegram4j.mtproto.resource.ProxyResources;
import telegram4j.mtproto.resource.TcpClientResources;
import telegram4j.mtproto.secret.SecretChatKeyExchange;
import telegram4j.mtproto.secret.SecretChatManager;
import telegram4j.mtproto.secret.SecretChatUtils;
import telegram4j.mtproto.secret.SeqNoState;
import telegram4j.mtproto.store.FileSecretChatOutbox;
import telegram4j.mtproto.store.SecretChatOutbox;
import telegram4j.mtproto.store.SecretChatOutboxImpl;
import telegram4j.mtproto.store.FileStoreLayout;
import telegram4j.mtproto.store.StoreLayoutImpl;
import telegram4j.tl.*;
import telegram4j.tl.secret.BaseDecryptedMessage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Properties;
import java.util.Scanner;
import java.util.StringJoiner;

/**
 * Controls a real account's secret chats from the console.
 *
 * <p>Responds to any messages, both in ordinary and secret chats, and accepts secret chat
 * invitations. Type {@code /help} once it is running to find out what else it can do.
 *
 * <h2>Configuration</h2>
 *
 * Every setting is resolved in four places, in order of priority; the first one found is used:
 *
 * <ol>
 *   <li>the environment, as {@code T4J_API_ID, T4J_API_HASH} and so on</li>
 *   <li>{@code ./secret-chat-console.properties}, which is ignored by git</li>
 *   <li>{@code ./secret-chat-console.example.properties}, normally should not be used</li>
 *   <li>{@code secret-chat-console.example.properties} on the classpath</li>
 * </ol>
 *
 * Copy the example that can be found at {@code core/src/test/resources/} and populate it with
 * your credentials. It ships with no credentials on purpose: an api_id stored in a committed
 * file becomes part of the repository. Using an api_id that belongs to an official client is one
 * of the documented reasons why accounts get limited.
 * Take your own from <a href="https://my.telegram.org">my.telegram.org</a>.
 *
 * <h2>The session file</h2>
 *
 * It stores the account's authorization key and every secret chat's key in plain text. Unlike
 * other Telegram account data, there is no server-side copy of this file. If you lose it, the
 * conversations become unrecoverable; if you share it, they become readable.
 * It is covered by {@code .gitignore}.
 *
 * <h2>Notes</h2>
 *
 * Chat creation and message sending have been tested on both Android and iPhone using the
 * official applications. Support for MadelineProto is implemented but untested — it is
 * expected to work with {@code accept.legacy.mtproto=true}, but this has not been confirmed.
 */
public final class SecretChatConsole {

    // region configuration

    /**
     * Where settings come from, in the order they are consulted: the environment first, then a
     * local file which is not committed, then the example file, which is committed.
     *
     * <p>The example carries no credentials of its own. Filling it in and committing it would
     * put an api_id in the repository, and an api_id belonging to someone else is one of the
     * documented reasons why accounts get limited.
     */
    static final String LOCAL_FILE = "secret-chat-console.properties";
    static final String EXAMPLE_FILE = "secret-chat-console.example.properties";
    static final String EXAMPLE_RESOURCE = "/" + EXAMPLE_FILE;

    static final Properties LOCAL = SecretChatUtils.loadFile(LOCAL_FILE);
    static final Properties EXAMPLE = SecretChatUtils.loadFile(EXAMPLE_FILE);
    static final Properties EXAMPLE_FROM_PATH = SecretChatUtils.loadResource(EXAMPLE_RESOURCE);

    static final int API_ID = Integer.parseInt(required("T4J_API_ID", "api.id"));
    static final String API_HASH = required("T4J_API_HASH", "api.hash");

    /** The account to use, in international format. */
    static final String PHONE_NUMBER = required("T4J_PHONE", "phone");

    /** Where the authorization key, the secret chats and their keys are kept between runs. */
    static final Path SESSION_FILE = Path.of(getSetting("T4J_SESSION_FILE", "session.file",
            "secret-chat-console-" + PHONE_NUMBER.replaceAll("\\D", "") + ".bin"));

    /**
     * Messages older than this are ignored.
     *
     * <p>On the first connection, the server replays everything missed while the account was
     * offline. Processing a week's worth of backlog is not what anyone wants from a test run.
     */
    static final Duration IGNORE_OLDER_THAN = Duration.ofSeconds(Long.parseLong(
            getSetting("T4J_IGNORE_OLDER_THAN_SECONDS", "ignore.older.than.seconds", "3600")));

    /** Whether to read peers which start a chat in MTProto 1.0, such as MadelineProto. */
    static final boolean ACCEPT_LEGACY_MTPROTO = Boolean.parseBoolean(
            getSetting("T4J_ACCEPT_LEGACY_MTPROTO", "accept.legacy.mtproto", "false"));

    /** How many sent messages each chat keeps so it can send them again when the peer asks. */
    static final int OUTBOX_CAPACITY = Integer.parseInt(getSetting("T4J_OUTBOX_CAPACITY",
            "outbox.capacity", String.valueOf(SecretChatOutboxImpl.DEFAULT_CAPACITY)));

    /** Whether sent messages outlive the process, so we are able to resend them. */
    static final boolean OUTBOX_PERSIST = Boolean.parseBoolean(
            getSetting("T4J_OUTBOX_PERSIST", "outbox.persist", "true"));

    /**
     * Where a persisted outbox keeps its files, one for each chat.
     *
     * <p>Kept apart per account for the same reason the session file is: the messages are
     * encrypted under the keys of their chats, and those live in that account's session file.
     */
    static final Path OUTBOX_DIRECTORY = Path.of(
        getSetting("T4J_OUTBOX_DIRECTORY", "outbox.directory",
            "encrypted-messages/" + PHONE_NUMBER.replaceAll("\\D", "")));

    /**
     * A SOCKS5 proxy to reach Telegram through, as {@code host:port}, or blank to connect directly.
     *
     * <p>Telegram is unreachable from some networks, which manifests as a
     * {@code ConnectTimeoutException} against 149.154.x.x rather than as an account-related error.
     * A system-wide VPN is not enough if it merely sets the machine's proxy settings: those are
     * respected by browsers and curl, but not by raw sockets, so the proxy address must point to
     * the port it listens on.
     */
    @Nullable
    static final InetSocketAddress SOCKS5_PROXY = address(
        getSetting("T4J_SOCKS5_PROXY", "proxy.socks5", ""));
    @Nullable
    static final String PROXY_USERNAME = emptyToNull(
        getSetting("T4J_PROXY_USERNAME", "proxy.username", ""));
    @Nullable
    static final String PROXY_PASSWORD = emptyToNull(
        getSetting("T4J_PROXY_PASSWORD", "proxy.password", ""));

    /**
     * How many answers this may send in {@link #ANSWER_WINDOW}.
     *
     * <p>Nothing stops two instances from being pointed at each other, and if each answers every
     * message, they will talk to each other as fast as the network allows. The limit is on answers
     * rather than on everything: read receipts follow messages that were actually received and
     * cannot generate new messages on their own.
     */
    static final int ANSWER_LIMIT = 5;
    static final Duration ANSWER_WINDOW = Duration.ofSeconds(6);

    /** Send times of the recent answers, oldest first. */
    static final Deque<Instant> RECENT_ANSWERS = new ArrayDeque<>();

    static final Scanner STDIN = new Scanner(new BufferedReader(new InputStreamReader(System.in)));

    /** What the rows of a key visualisation line up under here. */
    static final String KEY_INDENT = "        ";

    static final DateTimeFormatter TIMESTAMP =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    static final String HELP = """
            commands:
              /chats                 what secret chats exist and where their counters are
              /newchat <who>         ask a user for a secret chat: @username, or
                                     a phone number as +XXXXXXXXXXX
              /send <chatId> <text>  send into a secret chat
              /rekey <chatId>        replace the key of a secret chat now
              /discard <chatId>      tear a secret chat down and forget its key
              /skipgap <chatId>      give up on messages the chat is stuck waiting for
              /resend <chatId>       send the peer again what it has not confirmed
              /standin <chatId> [n]  occupy the number of a message we no longer have, which is
                                     the only thing left when /resend cannot. Safe where a
                                     neighbour of the hole is still kept, MAY END THE CHAT
                                     otherwise. Without n, the number the peer last asked for
              /help                  this
              /quit                  stop""";

    // endregion

    public static void main(String[] args) {
        quietLogs();

        var store = new FileStoreLayout(new StoreLayoutImpl(c -> c), SESSION_FILE);

        var bootstrap = MTProtoTelegramClient
                .create(API_ID, API_HASH, new CodeAuthorizationHandler(new ConsoleCallback()))
                .setStoreLayout(store);

        if (SOCKS5_PROXY != null) {
            log("connecting through SOCKS5 " + SOCKS5_PROXY);
            bootstrap.setTcpClientResources(TcpClientResources.builder()
                    .proxyResources(ProxyResources.ofSocks5()
                            .address(SOCKS5_PROXY)
                            .username(PROXY_USERNAME)
                            .password(PROXY_PASSWORD)
                            .build())
                    .build());
        }

        bootstrap
                .withConnection(client -> {
                    var secretChatsManager = new SecretChatManager(
                            client.getServiceHolder().getChatService(), store,
                            telegram4j.mtproto.auth.DhPrimeCheckerCache.instance(), ACCEPT_LEGACY_MTPROTO,
                            outbox());

                    log("signed in as " + client.getSelfId().asLong()
                            + ", waiting for messages (ignoring anything older than " + IGNORE_OLDER_THAN + ")");

                    return Mono.when(
                            resumeSecretChats(secretChatsManager),
                            handleOrdinaryMessages(client),
                            handleRawUpdates(client, secretChatsManager),
                            readCommands(client, secretChatsManager));
                })
                .block();
    }

    @Nullable
    static String getSetting(String environment, String key, @Nullable String fallback) {
        var value = System.getenv(environment);
        if (value == null) {
            value = LOCAL.getProperty(key);
        }
        if (value == null) {
            value = EXAMPLE.getProperty(key);
        }
        if (value == null) {
            value = EXAMPLE_FROM_PATH.getProperty(key);
        }
        return (value != null && !value.isBlank()) ? value.trim() : fallback;
    }

    /** Like {@link #getSetting}, for what the program cannot start without. */
    static String required(String environment, String key) {
        var value = getSetting(environment, key, null);
        if (value == null) {
            // said once and in full: the alternative is three separate runs to discover three
            // missing settings
            throw new IllegalStateException("""
                    Missing configuration. Set %s in the environment, or %s in ./%s.
                    Start from core/src/test/resources%s, filling in credentials of your own
                    from https://my.telegram.org rather than borrowing another client's.
                    The local file is read from the working directory, which is %s"""
                .formatted(environment, key, LOCAL_FILE, EXAMPLE_RESOURCE,
                    Path.of("").toAbsolutePath()));
        }
        return value;
    }

    @Nullable
    static InetSocketAddress address(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        int colon = value.lastIndexOf(':');
        if (colon < 0) {
            throw new IllegalStateException("Expected host:port for the proxy, got: " + value);
        }
        return new InetSocketAddress(value.substring(0, colon).trim(),
            Integer.parseInt(value.substring(colon + 1).trim()));
    }

    @Nullable
    static String emptyToNull(@Nullable String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    /**
     * {@return Where sent messages are kept so that they can be sent again when the peer asks}
     *
     * <p>A peer that loses one of our messages shows nothing we send after it, and says so only
     * by asking; only a side that kept the message can answer. A persisted outbox can still
     * answer after a restart, which is when it matters most, since a hole opened while we were
     * away is exactly the one that outlives us.
     */
    static SecretChatOutbox outbox() {
        return OUTBOX_PERSIST
                ? new FileSecretChatOutbox(OUTBOX_DIRECTORY, OUTBOX_CAPACITY, true)
                : new SecretChatOutboxImpl(OUTBOX_CAPACITY);
    }

    /**
     * {@return whether one more answer may be sent right now}
     *
     * <p>The counter applies across all chats: the point is not to be the source of a flood,
     * and which conversation an answer comes from does not change that.
     */
    static synchronized boolean allowAnswer() {
        var now = Instant.now();
        var oldest = now.minus(ANSWER_WINDOW);
        while (!RECENT_ANSWERS.isEmpty() && RECENT_ANSWERS.peekFirst().isBefore(oldest)) {
            RECENT_ANSWERS.removeFirst();
        }
        if (RECENT_ANSWERS.size() >= ANSWER_LIMIT) {
            return false;
        }
        RECENT_ANSWERS.addLast(now);
        return true;
    }

    /**
     * The shared test logging config puts the root logger at TRACE, which buries the prompts this
     * needs to show. Set here rather than in that file, which is not ours to retune.
     */
    static void quietLogs() {
        ((ch.qos.logback.classic.Logger)
            org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(
            ch.qos.logback.classic.Level.INFO);
    }

    /** Reports what survived the last run, since none of it can be fetched from the server. */
    static Mono<Void> resumeSecretChats(SecretChatManager secretChatManager) {
        return secretChatManager.getSecretChats()
                .doOnNext(chat -> log("restored secret chat " + chat.id() + " with " + chat.peerId()
                        + " (" + chat.status() + ", layer " + chat.layer()
                        + ", in " + chat.seqNo().inSeqNo() + " out " + chat.seqNo().outSeqNo() + ")"))
                .then();
    }

    // region ordinary chats

    static Mono<Void> handleOrdinaryMessages(MTProtoTelegramClient client) {
        return client.on(SendMessageEvent.class)
                .filter(event -> !event.getMessage().getFlags().contains(Message.Flag.OUT))
                .filter(event -> isRecent(event.getMessage().getCreateTimestamp()))
                .flatMap(event -> {
                    var message = event.getMessage();
                    String text = message.getContent();
                    var receivedAt = message.getCreateTimestamp();
                    String authorId = Objects.toString(message.getAuthorId().orElse(null));
                    log("received unencrypted from " + authorId + ": " + text);

                    String answer = "Received unencrypted '" + text + "' at " + TIMESTAMP.format(receivedAt);
                    // the event carries the chat the update came with, which is the only source
                    // on first contact: getChat() on the message asks the store, and the peer is
                    // not in it until an update has introduced it
                    Mono<Chat> chat = Mono.justOrEmpty(event.getChat())
                            .switchIfEmpty(message.getChat())
                            .cache();

                    Mono<Void> markRead = chat
                            .flatMap(c -> client.getServiceHolder().getChatService()
                                    .readHistory(client.asResolvedInputPeer(c.getId()), message.getId()))
                            .doOnNext(ignored -> log("marked as read up to message id "
                                + message.getId() + " from " + authorId))
                            .onErrorResume(e -> {
                                log("failed to mark as read message from " + authorId + ": " + e);
                                return Mono.empty();
                            })
                            .then();

                    Mono<Void> reply = Mono.defer(() -> {
                                if (!allowAnswer()) {
                                    log("not answering: more than " + ANSWER_LIMIT
                                            + " answers in the last " + ANSWER_WINDOW
                                            + " for " + authorId);
                                    return Mono.<Chat>empty();
                                }
                                return chat;
                            })
                            .flatMap(c -> c.sendMessage(answer))
                            .doOnNext(sent -> log("sent unencrypted answer, message id " + sent.getId()))
                            .switchIfEmpty(Mono.fromRunnable(
                                    () -> log("no answer sent, could not resolve the chat")))
                            .onErrorResume(e -> {
                                log("failed to answer in chat: " + e);
                                return Mono.empty();
                            })
                            .then();

                    // the receipt is subscribed to first and neither waits on the other
                    return Mono.when(markRead, reply);
                })
                .then();
    }

    // endregion
    // region secret chats

    /**
     * Secret chats are not part of the event layer yet, so they are driven from the raw updates.
     */
    static Mono<Void> handleRawUpdates(MTProtoTelegramClient client,
        SecretChatManager secretChatsManager) {
        return client.getMtProtoClientGroup().updates().all()
                .concatMapIterable(SecretChatConsole::flattenUpdates)
                // strictly in order: a chat becomes usable through updateEncryption, and the
                // peer's first message follows immediately, sometimes in the same batch. Handled
                // concurrently, the message overtakes the acceptance and is dropped as arriving
                // for a chat that is not ready yet, which leaves a gap nothing can fill
                .concatMap(update -> handleUpdate(secretChatsManager, update)
                        .onErrorResume(e -> {
                            log("failed to handle " + update.getClass().getSimpleName() + ": " + e);
                            return Mono.empty();
                        }))
                .then();
    }

    static List<Update> flattenUpdates(Updates updates) {
        return switch (updates.identifier()) {
            case UpdateShort.ID -> List.of(((UpdateShort) updates).update());
            case BaseUpdates.ID -> ((BaseUpdates) updates).updates();
            case UpdatesCombined.ID -> ((UpdatesCombined) updates).updates();
            default -> List.of();
        };
    }

    static Mono<Void> handleUpdate(SecretChatManager secretChats, Update update) {
        return switch (update.identifier()) {
            case UpdateEncryption.ID -> onEncryption(secretChats, (UpdateEncryption) update);
            case UpdateNewEncryptedMessage.ID ->
                    onEncryptedMessage(secretChats, ((UpdateNewEncryptedMessage) update).message());
            case UpdateEncryptedMessagesRead.ID -> {
                var read = (UpdateEncryptedMessagesRead) update;
                log("peer read our secret messages in chat " + read.chatId()
                        + " up to " + TIMESTAMP.format(Instant.ofEpochSecond(read.maxDate())));
                yield Mono.empty();
            }
            case UpdateReadHistoryOutbox.ID -> {
                var read = (UpdateReadHistoryOutbox) update;
                log("peer read our unencrypted messages up to id " + read.maxId());
                yield Mono.empty();
            }
            default -> Mono.empty();
        };
    }

    static Mono<Void> onEncryption(SecretChatManager secretChatsManager, UpdateEncryption update) {
        var chat = update.chat();
        if (chat instanceof EncryptedChatRequested requested) {
            log("secret chat " + requested.id() + " requested by " + requested.adminId() + ", accepting");
        }

        return secretChatsManager.onUpdate(chat)
                .doOnNext(state -> log("secret chat " + state.id() + " is now " + state.status()
                        + (state.status() == SecretChatState.Status.READY
                        ? ", key 0x" + Long.toHexString(state.key().id()) : "")))
                .then();
    }

    static Mono<Void> onEncryptedMessage(SecretChatManager secretChats, EncryptedMessage message) {
        var receivedAt = Instant.ofEpochSecond(message.date());
        if (!isRecent(receivedAt)) {
            log("ignoring a secret message from " + TIMESTAMP.format(receivedAt) + ", too old");
            return Mono.empty();
        }

        return secretChats.onNewMessage(message)
                // service messages and anything withheld as a repeat or a gap emit nothing,
                // and a message that fills a gap releases everything held behind it
                .ofType(BaseDecryptedMessage.class)
                .concatMap(decrypted -> {
                    String text = decrypted.message();
                    int chatId = message.chatId();
                    log("received encrypted in chat " + chatId + ": " + text);

                    String answer = "Received encrypted '" + text + "' at " + TIMESTAMP.format(receivedAt);

                    Mono<Void> markRead = secretChats.markHistoryWasRead(chatId, message.date())
                            .doOnSuccess(ignored -> log("marked encrypted history read in chat "
                                    + chatId + " up to " + TIMESTAMP.format(receivedAt)))
                            .onErrorResume(e -> {
                                log("failed to mark encrypted history read: " + e);
                                return Mono.empty();
                            });

                    Mono<Void> reply = Mono.defer(() -> {
                        if (!allowAnswer()) {
                            log("not answering in chat " + chatId + ": more than "
                                    + ANSWER_LIMIT + " answers in the last " + ANSWER_WINDOW);
                            return Mono.empty();
                        }
                        return secretChats.sendMessage(chatId, answer)
                                .doOnSuccess(ignored -> log("sent encrypted answer in chat "
                                        + chatId));
                    });

                    // the receipt is subscribed to first and neither waits on the other
                    return Mono.when(markRead, reply);
                })
                .then();
    }

    // endregion

    // region commands

    static Mono<Void> readCommands(MTProtoTelegramClient client, SecretChatManager secretChats) {
        log(HELP);

        return Flux.<String>generate(sink -> sink.next(STDIN.nextLine().trim()))
                .subscribeOn(Schedulers.boundedElastic())
                .filter(line -> !line.isEmpty())
                .takeWhile(line -> !line.equalsIgnoreCase("/quit"))
                .concatMap(line -> runCommand(client, secretChats, line)
                        .onErrorResume(e -> {
                            log("command failed: " + e);
                            return Mono.empty();
                        }))
                // the command loop ending is not enough on its own: the event subscriptions
                // never complete by themselves, so the connection has to be taken down
                .then(Mono.defer(() -> {
                    log("disconnecting");
                    return client.disconnect();
                }));
    }

    static Mono<Void> runCommand(MTProtoTelegramClient client, SecretChatManager secretChats, String line) {
        String[] parts = line.split("\\s+", 3);
        return switch (parts[0]) {
            case "/help" -> Mono.fromRunnable(() -> log(HELP));
            case "/chats" -> secretChats.getSecretChats()
                    .concatMap(chat -> describePeer(client, chat.peerId())
                            .map(who -> describeChat(chat, who)))
                    .doOnNext(SecretChatConsole::log)
                    .switchIfEmpty(Mono.fromRunnable(() -> log("  no secret chats")))
                    .then();
            case "/newchat" -> {
                if (parts.length < 2) {
                    yield Mono.fromRunnable(() -> log("usage: /newchat <@username|+phone>"));
                }
                yield resolveUser(client, parts[1])
                    .flatMap(secretChats::requestSecretChat)
                    .doOnNext(state -> log("requested secret chat " + state.id()
                            + " with " + state.peerId() + ", waiting for it to be accepted"))
                    .then();
            }
            case "/send" -> {
                if (parts.length < 3) {
                    yield Mono.fromRunnable(() -> log("usage: /send <chatId> <text>"));
                }
                int chatId = Integer.parseInt(parts[1]);
                yield secretChats.sendMessage(chatId, parts[2])
                        .doOnSuccess(ignored -> log("sent into secret chat " + chatId));
            }
            case "/rekey" -> {
                if (parts.length < 2) {
                    yield Mono.fromRunnable(() -> log("usage: /rekey <chatId>"));
                }
                int chatId = Integer.parseInt(parts[1]);
                yield secretChats.rekey(chatId)
                        .doOnSuccess(ignored -> log("asked to replace the key of secret chat " + chatId
                                + "; the answer arrives as an update"));
            }
            case "/discard" -> {
                if (parts.length < 2) {
                    yield Mono.fromRunnable(() -> log("usage: /discard <chatId>"));
                }
                int chatId = Integer.parseInt(parts[1]);
                yield secretChats.discardSecretChat(chatId, false)
                    .doOnSuccess(ignored -> log("discarded secret chat " + chatId));
            }
            // commands below attempt to fix stuck chats
            case "/skipgap" -> {
                if (parts.length < 2) {
                    yield Mono.fromRunnable(() -> log("usage: /skipgap <chatId>"));
                }
                int chatId = Integer.parseInt(parts[1]);
                yield secretChats.skipGap(chatId)
                        .doOnNext(released -> log("released a message that was held: " + released))
                        .switchIfEmpty(Mono.fromRunnable(
                                () -> log("nothing was being held in chat " + chatId)))
                        .then();
            }
            case "/resend" -> {
                if (parts.length < 2) {
                    yield Mono.fromRunnable(() -> log("usage: /resend <chatId>"));
                }
                int chatId = Integer.parseInt(parts[1]);
                yield secretChats.resendUnconfirmed(chatId)
                        .doOnSuccess(ignored -> log("sent secret chat " + chatId
                                + " what it had not confirmed and we still have"));
            }
            case "/standin" -> {
                if (parts.length < 2) {
                    yield Mono.fromRunnable(() -> log("usage: /standin <chatId> [seqNo]"));
                }
                int chatId = Integer.parseInt(parts[1]);
                // without a number, the one the peer keeps quoting as far as it has got
                var standIn = parts.length < 3
                        ? secretChats.standInFor(chatId)
                        : secretChats.standInFor(chatId, Integer.parseInt(parts[2]));
                // how risky it was depends on what numbers were available to copy, which the
                // manager says plainly: at info when it found a neighbour of the hole, at warning
                // when it had to guess
                yield standIn.doOnSuccess(ignored -> log("stood in for a message of secret chat "
                        + chatId + ", see the log above for the numbers it carried"));
            }
            default -> Mono.fromRunnable(() -> log("unknown command: " + parts[0] + ", try /help"));
        };
    }

    /**
     * {@return The user that a {@code /newchat} argument names}
     *
     * <p>A leading {@code +} marks a phone number, resolved with {@code contacts.resolvePhone}.
     * That answers only for a number whose owner can be found that way — we are in their
     * contacts, or their privacy settings allow it — and it adds nobody to our own contacts.
     * A number nobody can be found by fails with {@code PHONE_NOT_OCCUPIED}, which says nothing
     * about whether the number exists. Anything else is taken for a username.
     */
    static Mono<InputUser> resolveUser(MTProtoTelegramClient client, String who) {
        if (!who.startsWith("+")) {
            return client.resolvePeer(PeerId.of(who))
                    .switchIfEmpty(Mono.error(new IllegalArgumentException("no such user: " + who)))
                    .flatMap(peer -> client.asInputUserExact(peer.getId()));
        }

        // digits only, which is the form the API takes, and it lets the number be typed with
        // whatever punctuation it is usually written with
        return client.getServiceHolder().getUserService().resolvePhone(who.replaceAll("\\D", ""))
                .flatMap(resolved -> resolved.users().stream()
                        .filter(BaseUser.class::isInstance)
                        .map(BaseUser.class::cast)
                        .findFirst()
                        // the answer carries the user in full, access hash included, so there is
                        // nothing left to look up in the store
                        .map(user -> Mono.<InputUser>just(ImmutableBaseInputUser.of(user.id(),
                                Objects.requireNonNullElse(user.accessHash(), 0L))))
                        .orElseGet(() -> Mono.error(new IllegalArgumentException(
                                "no user came back for " + who))));
    }

    static Mono<String> describePeer(MTProtoTelegramClient client, long peerId) {
        return client.getUserById(Id.ofUser(peerId))
                .map(user -> {
                    var parts = new StringJoiner(", ", " (", ")").setEmptyValue("");
                    if (!user.getFullName().isBlank()) {
                        parts.add(user.getFullName());
                    }
                    user.getUsername().ifPresent(name -> parts.add("@" + name));
                    user.getPhone().ifPresent(phone -> parts.add("+" + phone));
                    return peerId + parts.toString();
                })
                .onErrorReturn(String.valueOf(peerId))
                .defaultIfEmpty(String.valueOf(peerId));
    }

    static String describeChat(SecretChatState chat, String who) {
        SeqNoState seqNo = chat.seqNo();
        var out = new StringBuilder("  ").append(chat.id()).append(" with ").append(who)
                .append(": ").append(chat.status())
                .append(", layer ").append(chat.layer())
                .append(", in ").append(seqNo.inSeqNo())
                .append(" out ").append(seqNo.outSeqNo())
                // What the peer last reported processing of ours, and the number /standin aims
                // at. It says nothing about delivery: it moves only when the peer sends a message,
                // never when it reads one, and the official Android client reports one less than
                // it has. Standing still while `in` keeps growing is the one thing it does say,
                // and that is a peer holding everything behind a message it never got
                .append(" peer has ").append(seqNo.remoteInSeqNo())
                .append(", ").append(chat.remainingMessages()).append(" messages before rekey")
                .append(", rekey ").append(chat.rekey().status());

        var keyHash = chat.keyHash();
        if (keyHash != null) {
            // what the apps show, from the key the chat started with: this is the one two people
            // compare, and it deliberately does not move when the key is replaced
            out.append("\n      encryption key").append(SecretChatUtils.visualisation(keyHash, KEY_INDENT));
        }

        var key = chat.key();
        if (key != null) {
            // and the key actually in use, which does move, so a replacement can be seen landing
            out.append("\n      current key id 0x").append(Long.toHexString(key.id()))
                    .append(SecretChatUtils.visualisation(SecretChatKeyExchange.keyHash(key), KEY_INDENT));
        }
        var oldKey = chat.oldKey();
        if (oldKey != null) {
            out.append("\n      previous key id 0x").append(Long.toHexString(oldKey.id()));
        }
        return out.toString();
    }

    // endregion

    /**
     * Is needed because we get a bunch of updates, including old ones.
     * @param timestamp update (message) timestamp
     * @return {@code true} if update is not older that {@code IGNORE_OLDER_THAN} from
     * the current moment
     */
    static boolean isRecent(Instant timestamp) {
        return timestamp.isAfter(Instant.now().minus(IGNORE_OLDER_THAN));
    }

    static void log(String message) {
        System.out.println("[" + TIMESTAMP.format(Instant.now()) + "] " + message);
    }

    /** Reads what the sign-in needs from the terminal. */
    static final class ConsoleCallback implements CodeAuthorizationHandler.Callback {
        @Override
        public Mono<PhoneNumberAction> onPhoneNumber(Resources res, PhoneNumberContext ctx) {
            return Mono.just(PhoneNumberAction.of(PHONE_NUMBER));
        }

        @Override
        public Mono<CodeAction> onSentCode(Resources res, PhoneCodeContext ctx) {
            // blocking on the terminal, so off the event loop
            return Mono.fromCallable(() -> {
                        System.out.print("Verification code sent to " + PHONE_NUMBER
                                + " (blank to resend): ");
                        System.out.flush();
                        String line = STDIN.nextLine().trim();
                        return line.isEmpty() ? CodeAction.resend() : (CodeAction) new Code(line);
                    })
                    .subscribeOn(Schedulers.boundedElastic());
        }

        @Override
        public Mono<String> on2FAPassword(Resources res, TwoFactorHandler.Context ctx) {
            return Mono.fromCallable(() -> {
                        System.out.print("Two-factor password: ");
                        System.out.flush();
                        return STDIN.nextLine().trim();
                    })
                    .subscribeOn(Schedulers.boundedElastic());
        }
    }
}
