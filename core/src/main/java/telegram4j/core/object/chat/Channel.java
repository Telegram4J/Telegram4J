package telegram4j.core.object.chat;

import telegram4j.core.object.BotInfo;
import telegram4j.core.object.RestrictionReason;
import telegram4j.core.object.StickerSet;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface Channel extends Chat {

    String getTitle();

    Optional<String> getUsername();

    Optional<String> getAbout();

    Optional<List<BotInfo>> getBotInfo();

    Instant getCreateTimestamp();

    Optional<StickerSet> getStickerSet();

    Optional<List<RestrictionReason>> getRestrictionReason();
}
