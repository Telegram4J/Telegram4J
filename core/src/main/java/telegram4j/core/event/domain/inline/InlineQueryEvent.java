package telegram4j.core.event.domain.inline;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.GeoPoint;
import telegram4j.core.object.User;
import telegram4j.core.spec.AnswerInlineCallbackQuerySpec;
import telegram4j.core.util.EntityFactory;
import telegram4j.tl.InlineQueryPeerType;
import telegram4j.tl.request.messages.SetInlineBotResults;

import java.util.Optional;

public class InlineQueryEvent extends BotEvent {

    private final long queryId;
    private final User user;
    private final String query;
    @Nullable
    private final GeoPoint geo;
    @Nullable
    private final InlineQueryPeerType peerType;
    private final String offset;

    public InlineQueryEvent(MTProtoTelegramClient client, long queryId, User user, String query,
                            @Nullable GeoPoint geo, @Nullable InlineQueryPeerType peerType, String offset) {
        super(client);
        this.queryId = queryId;
        this.user = user;
        this.query = query;
        this.geo = geo;
        this.peerType = peerType;
        this.offset = offset;
    }

    @Override
    public long getQueryId() {
        return queryId;
    }

    @Override
    public User getUser() {
        return user;
    }

    /**
     * Gets query text.
     *
     * @return The query text.
     */
    public String getQuery() {
        return query;
    }

    /**
     * Gets attached to query {@link GeoPoint geo position}, if present.
     *
     * @return The attached {@link GeoPoint geo position}, if present.
     */
    public Optional<GeoPoint> getGeo() {
        return Optional.ofNullable(geo);
    }

    /**
     * Gets type of chat where this query was created, if present.
     *
     * @return The {@link InlineQueryPeerType} of query creation location, if present.
     */
    public Optional<InlineQueryPeerType> getPeerType() {
        return Optional.ofNullable(peerType);
    }

    /**
     * Gets textually offset for query results, might be empty string.
     *
     * @return The textually offset for query results.
     */
    public String getOffset() {
        return offset;
    }

    /**
     * Request to set answer for this inline query.
     *
     * @param spec The specification for inline query answer.
     * @return A {@link Mono} which emitting on completion boolean which displays completion state.
     */
    public Mono<Boolean> answer(AnswerInlineCallbackQuerySpec spec) {
        return Mono.defer(() -> Flux.fromIterable(spec.results())
                .flatMap(e -> EntityFactory.createInlineResult(client, e))
                .collectList()
                .flatMap(list -> client.getServiceHolder().getMessageService()
                        .setInlineBotResults(SetInlineBotResults.builder()
                                .queryId(getQueryId())
                                .gallery(spec.gallery())
                                .isPrivate(spec.privacy())
                                .results(list)
                                .nextOffset(spec.nextOffset().orElse(null))
                                .switchPm(spec.switchPm().orElse(null))
                                .cacheTime(Math.toIntExact(spec.cacheTime().getSeconds()))
                                .build())));
    }

    @Override
    public String toString() {
        return "InlineQueryEvent{" +
                "queryId=" + queryId +
                ", user=" + user +
                ", query='" + query + '\'' +
                ", geo=" + geo +
                ", peerType=" + peerType +
                ", offset='" + offset + '\'' +
                '}';
    }
}
