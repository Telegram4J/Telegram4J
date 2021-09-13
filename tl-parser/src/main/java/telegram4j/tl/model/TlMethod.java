package telegram4j.tl.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableTlMethod.class)
@JsonDeserialize(as = ImmutableTlMethod.class)
public interface TlMethod extends TlModelObject {

    @Override
    int id();

    String method();

    List<TlParam> params();

    String type();
}
