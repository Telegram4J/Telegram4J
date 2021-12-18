package telegram4j.mtproto.store;

public class UserNameFields {
    private final String userName;
    private final String firstName;
    private final String lastName;

    public UserNameFields(String user_name, String first_name, String last_name) {
        userName = user_name;
        firstName = first_name;
        lastName = last_name;
    }

    public String getUserName() {
        return userName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }
}
