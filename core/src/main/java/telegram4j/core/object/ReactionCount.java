package telegram4j.core.object;

import telegram4j.core.internal.EntityFactory;

import java.util.Objects;
import java.util.Optional;

public class ReactionCount {

    private final telegram4j.tl.ReactionCount data;

    public ReactionCount(telegram4j.tl.ReactionCount data) {
        this.data = Objects.requireNonNull(data);
    }

    public Optional<Integer> getChosenOrder() {
        return Optional.ofNullable(data.chosenOrder());
    }

    public Reaction getReaction() {
        return EntityFactory.createReaction(data.reaction());
    }

    public int getCount() {
        return data.count();
    }

    @Override
    public String toString() {
        return "ReactionCount{" +
                "data=" + data +
                '}';
    }
}
