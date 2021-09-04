package telegram4j.json.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import telegram4j.json.api.ChatId;

import java.io.IOException;

public class ChatIdSerializer extends StdSerializer<ChatId> {

    public ChatIdSerializer() {
        super(ChatId.class);
    }

    @Override
    public void serialize(ChatId value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        String str = value.asString();
        if (!str.startsWith("@")) {
            gen.writeNumber(Long.parseLong(str));
        } else {
            gen.writeString(str);
        }
    }
}
