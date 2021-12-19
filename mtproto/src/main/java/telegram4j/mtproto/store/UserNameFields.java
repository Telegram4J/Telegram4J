package telegram4j.mtproto.store;

import reactor.util.annotation.Nullable;
import telegram4j.tl.BaseUser;

import java.util.Objects;

public class UserNameFields {
    @Nullable
    private final String userName;
    @Nullable
    private final String firstName;
    @Nullable
    private final String lastName;

    public UserNameFields(@Nullable String userName, @Nullable String firstName, @Nullable String lastName) {
        this.userName = userName;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @Nullable
    public String getUserName() {
        return userName;
    }

    @Nullable
    public String getFirstName() {
        return firstName;
    }

    @Nullable
    public String getLastName() {
        return lastName;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserNameFields that = (UserNameFields) o;
        return Objects.equals(userName, that.userName) && Objects.equals(firstName, that.firstName) && Objects.equals(lastName, that.lastName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName, firstName, lastName);
    }

    @Override
    public String toString() {
        return "UserNameFields{" +
                "userName='" + userName + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                '}';
    }
}
