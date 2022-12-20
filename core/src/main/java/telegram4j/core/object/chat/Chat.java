package telegram4j.core.object.chat;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.core.auxiliary.AuxiliaryMessages;
import telegram4j.core.internal.MappingUtil;
import telegram4j.core.object.*;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.spec.ForwardMessagesSpec;
import telegram4j.core.spec.SendMessageSpec;
import telegram4j.core.util.Id;
import telegram4j.core.util.PeerId;
import telegram4j.tl.messages.AffectedHistory;

import java.time.Duration;
import java.util.Optional;

/** The Telegram <a href="https://core.telegram.org/api/channel">chat</a> representation. */
public interface Chat extends PeerEntity {

    /**
     * Gets the type of chat.
     *
     * @return The {@link Type type} of chat.
     */
    Type getType();

    /**
     * Gets title (i.e. name) of chat.
     *
     * @return The name of chat.
     */
    String getName();

    /**
     * Gets the low quality chat photo, if present.
     *
     * @return The {@link ProfilePhoto photo} of chat, if present.
     */
    Optional<ProfilePhoto> getMinPhoto();

    /**
     * Gets the normal chat photo, if present
     * and if detailed information about chat is available.
     *
     * @return The {@link Photo photo} of chat, if present and
     * if detailed information about chat is available
     */
    Optional<Photo> getPhoto();

    /**
     * Gets a message auto delete duration which
     * can be added to {@link Message#getCreateTimestamp()} to get
     * delete timestamp, if present.
     *
     * @return The {@link Duration} of message auto delete timer, if present.
     */
    Optional<Duration> getMessageAutoDeleteDuration();

    /**
     * Gets id of pinned message, if present
     * and if detailed information about chat is available.
     *
     * @return The {@link Id} of pinned message, if present and available.
     */
    Optional<Integer> getPinnedMessageId();

    /**
     * Requests to retrieve pinned message.
     *
     * @return A {@link Mono} emitting on successful completion {@link AuxiliaryMessages pinned message}.
     */
    default Mono<AuxiliaryMessages> getPinnedMessage() {
        return getPinnedMessage(MappingUtil.IDENTITY_RETRIEVER);
    }

    /**
     * Requests to retrieve pinned message using specified retrieval strategy.
     *
     * @param strategy The strategy to apply
     * @return A {@link Mono} emitting on successful completion {@link AuxiliaryMessages pinned message}.
     */
    Mono<AuxiliaryMessages> getPinnedMessage(EntityRetrievalStrategy strategy);

    /**
     * Gets notify settings of chat, if present
     * and if detailed information about chat is available.
     *
     * @return The {@link PeerNotifySettings} of chat, if present and available.
     */
    Optional<PeerNotifySettings> getNotifySettings();

    /**
     * Gets text of channel description, if present.
     *
     * @return The text of channel description, if present.
     */
    Optional<String> getAbout();

    /**
     * Gets id of the dialog folder, where this chat/channel placed, if present.
     *
     * @see <a href="https://core.telegram.org/api/folders">Folders</a>
     * @return The id of the dialog folder, where this chat/channel placed, if present.
     */
    Optional<Integer> getFolderId();

    /**
     * Gets unicode emoji representation of a specific channel theme.
     *
     * @return The unicode emoji representation of a specific channel theme.
     */
    Optional<String> getThemeEmoticon();

    // Interaction methods

    /**
     * Requests to create new message with specified text.
     *
     * @param text The text of message.
     * @return A {@link Mono} emitting on successful completion created message.
     */
    default Mono<Message> sendMessage(String text) {
        return sendMessage(SendMessageSpec.of(text));
    }

    /**
     * Requests to create new message or media by specified edit specification in this chat.
     *
     * @param spec an immutable object that specifies how to create the message.
     * @return A {@link Mono} emitting on successful completion created message.
     */
    Mono<Message> sendMessage(SendMessageSpec spec);

    /**
     * Requests to forward message(s) by forward specification in this chat.
     *
     * @param spec an immutable object that specifies how to forward the message(s).
     * @param toPeer The {@link PeerId} of chat where to forward message(s).
     * @return A {@link Flux} emitting created messages.
     */
    Flux<Message> forwardMessages(ForwardMessagesSpec spec, PeerId toPeer);

    /**
     * Requests to unpin all messages in chat.
     *
     * @return A {@link Mono} emitting on successful completion {@link AffectedHistory} with affected history range.
     */
    Mono<AffectedHistory> unpinAllMessages();

    /** All types of telegram chat. */
    enum Type {

        /** Represents a {@link PrivateChat}. */
        PRIVATE,

        /** Represents a {@link GroupChat}. */
        GROUP,

        /** Represents a {@link SupergroupChat}. */
        SUPERGROUP,

        /** Represents a {@link BroadcastChannel}. */
        CHANNEL
    }
}
