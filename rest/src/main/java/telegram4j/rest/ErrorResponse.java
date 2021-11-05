package telegram4j.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import reactor.util.annotation.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ErrorResponse {

    private final boolean ok;

    private final int errorCode;

    @Nullable
    private final String description;

    @Nullable
    private final Map<String, Object> parameters;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ErrorResponse(@JsonProperty("ok") boolean ok,
                         @JsonProperty("error_code") int errorCode,
                         @JsonProperty("description") @Nullable String description,
                         @JsonProperty("parameters") Map<String, Object> parameters) {
        this.ok = ok;
        this.errorCode = errorCode;
        this.description = description;
        this.parameters = parameters;
    }

    public boolean isOk() {
        return ok;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public Optional<Map<String, Object>> getParameters() {
        return Optional.ofNullable(parameters);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ErrorResponse that = (ErrorResponse) o;
        return ok == that.ok && errorCode == that.errorCode &&
                Objects.equals(description, that.description) &&
                Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ok, errorCode, description, parameters);
    }

    @Override
    public String toString() {
        return "ErrorResponse{" +
                "ok=" + ok +
                ", errorCode=" + errorCode +
                ", description='" + description + '\'' +
                ", parameters=" + parameters +
                '}';
    }
}
