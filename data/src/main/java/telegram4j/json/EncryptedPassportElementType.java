package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonValue;

public enum EncryptedPassportElementType {

    PERSONAL_DETAILS,
    PASSPORT,
    DRIVER_LICENSE,
    IDENTITY_CARD,
    INTERNAL_PASSPORT,
    ADDRESS,
    UTILITY_BILL,
    BANK_STATEMENT,
    RENTAL_AGREEMENT,
    PASSPORT_REGISTRATION,
    TEMPORARY_REGISTRATION,
    PHONE_NUMBER,
    EMAIL;

    @JsonValue
    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
