package telegram4j.tl.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableTlConstructor.class)
@JsonDeserialize(as = ImmutableTlConstructor.class)
public interface TlConstructor extends TlModelObject {

    @Override
    int id();

    String predicate();

    List<TlParam> params();

    String type();
}
