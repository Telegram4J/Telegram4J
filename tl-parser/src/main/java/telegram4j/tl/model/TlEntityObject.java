package telegram4j.tl.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableTlEntityObject.class)
@JsonDeserialize(as = ImmutableTlEntityObject.class)
public interface TlEntityObject {

    int id();

    @JsonAlias({"predicate", "method"})
    String name();

    List<TlParam> params();

    String type();
}
