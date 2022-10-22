package telegram4j.core.internal;

import reactor.core.publisher.Mono;
import telegram4j.core.object.chat.UnresolvedPeerException;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.util.BitFlag;
import telegram4j.core.util.Id;
import telegram4j.core.util.ImmutableEnumSet;

import java.util.Set;

public class MappingUtil {
    private MappingUtil() {}

    public static final EntityRetrievalStrategy IDENTITY_RETRIEVER = client -> client;

    public static <E extends Enum<E> & BitFlag> int getMaskValue(Set<E> values) {
        if (values.getClass() == ImmutableEnumSet.class)
            return ((ImmutableEnumSet<E>) values).getValue();
        return values.stream()
                .map(E::mask)
                .reduce(0, (l, r) -> l | r);
    }

    public static <T> Mono<T> unresolvedPeer(Id peerId) {
        return Mono.error(() -> new UnresolvedPeerException(peerId));
    }
}
