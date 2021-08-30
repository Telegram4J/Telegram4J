package telegram4j.core.object;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.TelegramClient;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.util.EntityUtil;
import telegram4j.json.*;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class Message implements TelegramObject {

    private final TelegramClient client;
    private final MessageData data;

    public Message(TelegramClient client, MessageData data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    public Mono<Boolean> delete() {
        return client.getRestClient().getChatService()
                .deleteMessage(getChatId().asLong(), getId().asLong());
    }

    public Id getId() {
        return Id.of(data.messageId());
    }

    public Id getChatId() {
        return Id.of(data.chat().id());
    }

    public MessageData getData() {
        return data;
    }

    public Optional<User> getAuthor() {
        return data.fromUser().map(data -> new User(client, data));
    }

    public Chat getChat() {
        return EntityUtil.getChat(client, data.chat());
    }

    public Instant getTimestamp() {
        return Instant.ofEpochSecond(data.date());
    }

    public Optional<User> getForwardFromUser() {
        return data.forwardFromUser().map(data -> new User(client, data));
    }

    public Optional<Chat> getForwardFromChat() {
        return data.forwardFromChat().map(data -> EntityUtil.getChat(client, data));
    }

    public Optional<Id> getForwardFromMessageId() {
        return data.forwardFromMessageId().map(Id::of);
    }

    public Optional<String> getForwardSignature() {
        return data.forwardSignature();
    }

    public Optional<String> getForwardSenderName() {
        return data.forwardSenderName();
    }

    public Optional<Instant> getForwardTimestamp() {
        return data.forwardDate().map(Instant::ofEpochSecond);
    }

    public Optional<Message> getReplyToMessage() {
        return data.replyToMessage().map(data -> new Message(client, data));
    }

    public Optional<User> getViaBot() {
        return data.viaBot().map(data -> new User(client, data));
    }

    public Optional<Instant> getEditTimestamp() {
        return data.editDate().map(Instant::ofEpochSecond);
    }

    public Optional<Id> getMediaGroupId() {
        return data.mediaGroupId().map(Id::of);
    }

    public Optional<String> getAuthorSignature() {
        return data.authorSignature();
    }

    public Optional<String> getText() {
        return data.text();
    }

    public Optional<List<MessageEntity>> getEntities() {
        return data.entities().map(list -> list.stream()
                .map(data -> new MessageEntity(client, data,
                        getText().orElseThrow(IllegalStateException::new)))
                .collect(Collectors.toList()));
    }

    public Optional<Animation> getAnimation() {
        return data.animation().map(data -> new Animation(client, data));
    }

    public Optional<Audio> getAudio() {
        return data.audio().map(data -> new Audio(client, data));
    }

    public Optional<Document> getDocument() {
        return data.document().map(data -> new Document(client, data));
    }

    public Optional<List<PhotoSize>> getPhoto() {
        return data.photo().map(list -> list.stream()
                .map(data -> new PhotoSize(client, data))
                .collect(Collectors.toList()));
    }

    public Optional<Sticker> getSticker() {
        return data.sticker().map(data -> new Sticker(client, data));
    }

    public Optional<Video> getVideo() {
        return data.video().map(data -> new Video(client, data));
    }

    public Optional<VideoNote> getVideoNote() {
        return data.videoNote().map(data -> new VideoNote(client, data));
    }

    public Optional<Voice> getVoice() {
        return data.voice().map(data -> new Voice(client, data));
    }

    public Optional<String> getCaption() {
        return data.caption();
    }

    public Optional<List<MessageEntity>> getCaptionEntities() {
        return data.captionEntities().map(list -> list.stream()
                .map(data -> new MessageEntity(client, data,
                        getCaption().orElseThrow(IllegalStateException::new)))
                .collect(Collectors.toList()));
    }

    public Optional<Contact> getContact() {
        return data.contact().map(data -> new Contact(client, data));
    }

    public Optional<DiceData> getDice() {
        return data.dice();
    }

    public Optional<GameData> getGame() {
        return data.game();
    }

    public Optional<PollData> getPoll() {
        return data.poll();
    }

    public Optional<VenueData> getVenue() {
        return data.venue();
    }

    public Optional<Location> getLocation() {
        return data.location().map(data -> new Location(client, data));
    }

    public Optional<List<User>> getNewChatMembers() {
        return data.newChatMembers().map(list -> list.stream()
                .map(data -> new User(client, data))
                .collect(Collectors.toList()));
    }

    public Optional<User> getLeftChatMember() {
        return data.leftChatMember().map(data -> new User(client, data));
    }

    public Optional<String> getNewChatTitle() {
        return data.newChatTitle();
    }

    public Optional<List<PhotoSize>> getNewChatPhoto() {
        return data.newChatPhoto().map(list -> list.stream()
                .map(data -> new PhotoSize(client, data))
                .collect(Collectors.toList()));
    }

    public boolean isDeleteChatPhoto() {
        return data.deleteChatPhoto().orElse(false);
    }

    public boolean isGroupChatCreated() {
        return data.groupChatCreated().orElse(false);
    }

    public boolean isSupergroupChatCreated() {
        return data.supergroupChatCreated().orElse(false);
    }

    public boolean isChannelChatCreated() {
        return data.channelChatCreated().orElse(false);
    }

    // TODO: maybe unwrap this?
    public Optional<MessageAutoDeleteTimerChanged> getMessageAutoDeleteTimerChanged() {
        return data.messageAutoDeleteTimerChanged().map(data -> new MessageAutoDeleteTimerChanged(client, data));
    }

    public Optional<Id> getMigrateToChatId() {
        return data.migrateToChatId().map(Id::of);
    }

    public Optional<Id> getMigrateFromChatId() {
        return data.migrateFromChatId().map(Id::of);
    }

    public Optional<Message> getPinnedMessage() {
        return data.pinnedMessage().map(data -> new Message(client, data));
    }

    public Optional<Invoice> getInvoice() {
        return data.invoice().map(data -> new Invoice(client, data));
    }

    public Optional<SuccessfulPayment> getSuccessfulPayment() {
        return data.successfulPayment().map(data -> new SuccessfulPayment(client, data));
    }

    public Optional<String> getConnectedWebsite() {
        return data.connectedWebsite();
    }

    public Optional<Passport> getPassport() {
        return data.passportData().map(data -> new Passport(client, data));
    }

    public Optional<ProximityAlertTriggered> getProximityAlertTriggered() {
        return data.proximityAlertTriggered().map(data -> new ProximityAlertTriggered(client, data));
    }

    public Optional<VoiceChatScheduled> getVoiceChatScheduled() {
        return data.voiceChatScheduled().map(data -> new VoiceChatScheduled(client, data));
    }

    // MessageData#voiceChatStarted not added because it's not needed and empty

    // TODO: maybe unwrap this and return only #duration?
    public Optional<VoiceChatEnded> getVoiceChatEnded() {
        return data.voiceChatEnded().map(data -> new VoiceChatEnded(client, data));
    }

    public Optional<VoiceChatParticipantsInvited> getVoiceChatParticipantsInvited() {
        return data.voiceChatParticipantsInvited().map(data -> new VoiceChatParticipantsInvited(client, data));
    }

    public Optional<InlineKeyboardMarkup> getReplyMarkup() {
        return data.replyMarkup().map(data -> new InlineKeyboardMarkup(client, data));
    }

    public Optional<User> getNewChatParticipant() {
        return data.newChatParticipant().map(data -> new User(client, data));
    }

    public Optional<User> getNewChatMember() {
        return data.newChatMember().map(data -> new User(client, data));
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message that = (Message) o;
        return getId().equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "Message{data=" + data + '}';
    }
}
