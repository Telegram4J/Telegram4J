package telegram4j.core.spec.media;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.ImmutableInputMediaContact;
import telegram4j.tl.InputMedia;
import telegram4j.tl.InputMediaContact;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class InputMediaContactSpec implements InputMediaSpec {
    private final String phoneNumber;
    private final String firstName;
    private final String lastName;
    private final String vcard;

    private InputMediaContactSpec(String phoneNumber, String firstName, String lastName, String vcard) {
        this.phoneNumber = phoneNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.vcard = vcard;
    }

    public String phoneNumber() {
        return phoneNumber;
    }

    public String firstName() {
        return firstName;
    }

    public String lastName() {
        return lastName;
    }

    public String vcard() {
        return vcard;
    }

    @Override
    public Mono<ImmutableInputMediaContact> resolve(MTProtoTelegramClient client) {
        return Mono.just(InputMediaContact.builder()
                .phoneNumber(phoneNumber())
                .firstName(firstName())
                .lastName(lastName())
                .vcard(vcard())
                .build());
    }

    public InputMediaContactSpec withPhoneNumber(String value) {
        Objects.requireNonNull(value);
        if (phoneNumber.equals(value)) return this;
        return new InputMediaContactSpec(value, firstName, lastName, vcard);
    }

    public InputMediaContactSpec withFirstName(String value) {
        Objects.requireNonNull(value);
        if (firstName.equals(value)) return this;
        return new InputMediaContactSpec(phoneNumber, value, lastName, vcard);
    }

    public InputMediaContactSpec withLastName(String value) {
        Objects.requireNonNull(value);
        if (lastName.equals(value)) return this;
        return new InputMediaContactSpec(phoneNumber, firstName, value, vcard);
    }

    public InputMediaContactSpec withVcard(String value) {
        Objects.requireNonNull(value);
        if (vcard.equals(value)) return this;
        return new InputMediaContactSpec(phoneNumber, firstName, lastName, value);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InputMediaContactSpec that = (InputMediaContactSpec) o;
        return phoneNumber.equals(that.phoneNumber) && firstName.equals(that.firstName) &&
                lastName.equals(that.lastName) && vcard.equals(that.vcard);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + phoneNumber.hashCode();
        h += (h << 5) + firstName.hashCode();
        h += (h << 5) + lastName.hashCode();
        h += (h << 5) + vcard.hashCode();
        return h;
    }

    public static InputMediaContactSpec of(String phoneNumber, String firstName, String lastName, String vcard) {
        Objects.requireNonNull(phoneNumber);
        Objects.requireNonNull(firstName);
        Objects.requireNonNull(lastName);
        Objects.requireNonNull(vcard);
        return new InputMediaContactSpec(phoneNumber, firstName, lastName, vcard);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private static final byte INIT_BIT_PHONE_NUMBER = 0x1;
        private static final byte INIT_BIT_FIRST_NAME = 0x2;
        private static final byte INIT_BIT_LAST_NAME = 0x4;
        private static final byte INIT_BIT_VCARD = 0x8;
        private byte initBits = 0xf;

        private String phoneNumber;
        private String firstName;
        private String lastName;
        private String vcard;

        private Builder() {
        }

        public Builder from(InputMediaContactSpec instance) {
            Objects.requireNonNull(instance);
            phoneNumber(instance.phoneNumber);
            firstName(instance.firstName);
            lastName(instance.lastName);
            vcard(instance.vcard);
            return this;
        }

        public Builder phoneNumber(String phoneNumber) {
            this.phoneNumber = Objects.requireNonNull(phoneNumber);
            initBits &= ~INIT_BIT_PHONE_NUMBER;
            return this;
        }

        public Builder firstName(String firstName) {
            this.firstName = Objects.requireNonNull(firstName);
            initBits &= ~INIT_BIT_FIRST_NAME;
            return this;
        }

        public Builder lastName(String lastName) {
            this.lastName = Objects.requireNonNull(lastName);
            initBits &= ~INIT_BIT_LAST_NAME;
            return this;
        }

        public Builder vcard(String vcard) {
            this.vcard = Objects.requireNonNull(vcard);
            initBits &= ~INIT_BIT_VCARD;
            return this;
        }

        public InputMediaContactSpec build() {
            if (initBits != 0) {
                throw incompleteInitialization();
            }
            return new InputMediaContactSpec(phoneNumber, firstName, lastName, vcard);
        }

        private IllegalStateException incompleteInitialization() {
            List<String> attributes = new ArrayList<>();
            if ((initBits & INIT_BIT_PHONE_NUMBER) != 0) attributes.add("phoneNumber");
            if ((initBits & INIT_BIT_FIRST_NAME) != 0) attributes.add("firstName");
            if ((initBits & INIT_BIT_LAST_NAME) != 0) attributes.add("lastName");
            if ((initBits & INIT_BIT_VCARD) != 0) attributes.add("vcard");
            return new IllegalStateException("Cannot build InputMediaContactSpec, some of required attributes are not set " + attributes);
        }
    }
}
