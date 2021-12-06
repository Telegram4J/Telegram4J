package telegram4j.tl.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableTlParam.class)
@JsonDeserialize(as = ImmutableTlParam.class)
public interface TlParam {

    String name();

    String type();
}
