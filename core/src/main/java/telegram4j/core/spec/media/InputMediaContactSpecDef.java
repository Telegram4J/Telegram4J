package telegram4j.core.spec.media;

import org.immutables.value.Value;
import telegram4j.tl.InputMediaContact;

@Value.Immutable(builder = false)
interface InputMediaContactSpecDef extends InputMediaSpec {

    @Override
    default Type type() {
        return Type.CONTACT;
    }

    String phoneNumber();

    String firstName();

    String lastName();

    String vcard();

    @Override
    default InputMediaContact asData() {
        return InputMediaContact.builder()
                .phoneNumber(phoneNumber())
                .firstName(firstName())
                .lastName(lastName())
                .vcard(vcard())
                .build();
    }
}
