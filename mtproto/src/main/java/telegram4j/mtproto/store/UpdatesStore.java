package telegram4j.mtproto.store;

import reactor.core.publisher.Mono;
import telegram4j.mtproto.store.object.ResolvedDeletedMessages;
import telegram4j.tl.*;

public interface UpdatesStore {

    Mono<Void> onNewMessage(Message update);

    Mono<Message> onEditMessage(Message update);

    Mono<ResolvedDeletedMessages> onDeleteMessages(UpdateDeleteMessages update);
    Mono<ResolvedDeletedMessages> onDeleteMessages(UpdateDeleteScheduledMessages update);
    Mono<ResolvedDeletedMessages> onDeleteMessages(UpdateDeleteChannelMessages update);

    Mono<Void> onUpdatePinnedMessages(UpdatePinnedMessages payload);
    Mono<Void> onUpdatePinnedMessages(UpdatePinnedChannelMessages payload);

    Mono<Void> onChatParticipant(UpdateChatParticipant payload);

    Mono<Void> onChannelParticipant(UpdateChannelParticipant payload);

    Mono<Void> onChatParticipants(ChatParticipants payload);
}
