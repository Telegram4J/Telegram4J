package telegram4j.core;

import reactor.core.publisher.Mono;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.*;
import telegram4j.tl.messages.BaseMessages;
import telegram4j.tl.messages.ChannelMessages;
import telegram4j.tl.messages.Messages;

import java.util.List;

public class FileReferenceManager {

    private final MTProtoTelegramClient client;

    public FileReferenceManager(MTProtoTelegramClient client) {
        this.client = client;
    }

    public Mono<FileReferenceId> refresh(String fileReferenceId) {
        return Mono.fromCallable(() -> FileReferenceId.deserialize(fileReferenceId))
                .flatMap(f -> {
                    switch (f.getFileType()) {
                        case CHAT_PHOTO:
                            switch (f.getPeer().identifier()) {
                                case InputPeerChannel.ID:
                                case InputPeerChannelFromMessage.ID:
                                    InputChannel channel = TlEntityUtil.toInputChannel(f.getPeer());
                                    return client.getServiceHolder().getMessageService()
                                            .getMessages(channel, List.of(ImmutableInputMessageID.of(f.getMessageId())))
                                            .ofType(ChannelMessages.class)
                                            .flatMap(b -> findMessageAction(b, f));
                                case InputPeerChat.ID:
                                    return client.getServiceHolder()
                                            .getMessageService()
                                            .getMessages(List.of(ImmutableInputMessageID.of(f.getMessageId())))
                                            .ofType(BaseMessages.class)
                                            .flatMap(b -> findMessageAction(b, f));
                                case InputPeerSelf.ID:
                                case InputPeerUser.ID:
                                case InputPeerUserFromMessage.ID:
                                    return client.getServiceHolder()
                                            .getUserService()
                                            .getUserPhotos(TlEntityUtil.toInputUser(f.getPeer()),
                                                    0, -f.getDocumentId(), 1)
                                            .map(p -> p.photos().get(0))
                                            .ofType(BasePhoto.class)
                                            .map(p -> FileReferenceId.ofChatPhoto(p, -1, f.getPeer()));
                                default:
                                    return Mono.error(new IllegalArgumentException("Unknown input peer type: " + f.getPeer()));
                            }

                        case DOCUMENT:
                        case PHOTO:
                            switch (f.getPeer().identifier()) {
                                case InputPeerChannel.ID:
                                case InputPeerChannelFromMessage.ID:
                                    InputChannel channel = TlEntityUtil.toInputChannel(f.getPeer());
                                    return client.getServiceHolder().getMessageService()
                                            .getMessages(channel, List.of(ImmutableInputMessageID.of(f.getMessageId())))
                                            .ofType(ChannelMessages.class)
                                            .flatMap(b -> findMessageMedia(b, f));
                                case InputPeerSelf.ID:
                                case InputPeerUser.ID:
                                case InputPeerUserFromMessage.ID:
                                case InputPeerChat.ID:
                                    return client.getServiceHolder().getMessageService()
                                            .getMessages(List.of(ImmutableInputMessageID.of(f.getMessageId())))
                                            .ofType(BaseMessages.class)
                                            .flatMap(b -> findMessageMedia(b, f));
                                default:
                                    return Mono.error(new IllegalArgumentException("Unknown input peer type: " + f.getPeer()));
                            }
                        // No need refresh
                        case STICKER_SET_THUMB:
                            return Mono.just(f);
                        default:
                            return Mono.error(new IllegalStateException());
                    }
                });
    }

    private Mono<FileReferenceId> findMessageAction(Messages messages, FileReferenceId orig) {
        var list = messages.identifier() == BaseMessages.ID
                ? ((BaseMessages) messages).messages()
                : ((ChannelMessages) messages).messages();

        var service = list.stream()
                .filter(m -> m.id() == orig.getMessageId() &&
                        m.identifier() == MessageService.ID)
                .map(m -> (MessageService) m)
                .findFirst()
                .orElseThrow();

        switch (service.action().identifier()) {
            case MessageActionChatEditPhoto.ID:
                MessageActionChatEditPhoto a = (MessageActionChatEditPhoto) service.action();
                return Mono.justOrEmpty(TlEntityUtil.unmapEmpty(a.photo(), BasePhoto.class))
                        .map(p -> FileReferenceId.ofChatPhoto(p, orig.getMessageId(), orig.getPeer()));
            default:
                return Mono.error(new IllegalStateException("Unexpected message action type: " + service.action()));
        }
    }

    private Mono<FileReferenceId> findMessageMedia(Messages messages, FileReferenceId orig) {
        var list = messages.identifier() == BaseMessages.ID
                ? ((BaseMessages) messages).messages()
                : ((ChannelMessages) messages).messages();
        var message = list.stream()
                .filter(m -> m.id() == orig.getMessageId() &&
                        m.identifier() != MessageEmpty.ID)
                .map(m -> (BaseMessage) m)
                .findFirst()
                .orElseThrow();

        var media = message.media();
        if (media == null) {
            return Mono.empty();
        }

        switch (media.identifier()) {
            case MessageMediaDocument.ID:
                return Mono.justOrEmpty(((MessageMediaDocument) media).document())
                        .ofType(BaseDocument.class)
                        .map(d -> FileReferenceId.ofDocument(d,
                                orig.getMessageId(), orig.getPeer()));
            case MessageMediaPhoto.ID:
                return Mono.justOrEmpty(((MessageMediaPhoto) media).photo())
                        .ofType(BasePhoto.class)
                        .map(d -> FileReferenceId.ofPhoto(d,
                                orig.getMessageId(), orig.getPeer()));
            default:
                return Mono.error(new IllegalStateException("Unexpected message media type: " + media));
        }
    }
}
