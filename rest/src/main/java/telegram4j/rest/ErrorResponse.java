package telegram4j.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class ErrorResponse {

    private final boolean ok;

    @JsonProperty("error_code")
    private final int errorCode;

    private final String description;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public ErrorResponse(@JsonProperty("ok") boolean ok,
                         @JsonProperty("error_code") int errorCode,
                         @JsonProperty("description") String description) {
        this.ok = ok;
        this.errorCode = errorCode;
        this.description = Objects.requireNonNull(description, "description");
    }

    public boolean isOk() {
        return ok;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ErrorResponse that = (ErrorResponse) o;
        return ok == that.ok && errorCode == that.errorCode && description.equals(that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ok, errorCode, description);
    }

    @Override
    public String toString() {
        return "ErrorResponse{" +
                "ok=" + ok +
                ", errorCode=" + errorCode +
                ", description='" + description + '\'' +
                '}';
    }
}
