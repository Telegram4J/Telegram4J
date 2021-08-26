package telegram4j.core.object;

public interface Attachment extends TelegramObject {

    String getFileId();

    String getFileUniqueId();
}
