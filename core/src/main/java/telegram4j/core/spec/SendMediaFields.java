package telegram4j.core.spec;

import org.immutables.value.Value;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import telegram4j.json.InputFile;

@FieldStyle
@Value.Enclosing
public final class SendMediaFields {

    private SendMediaFields() {}

    @Value.Immutable
    public interface File extends Spec<Tuple2<String, InputFile>> {

        static File of(String name, InputFile inputFile) {
            return ImmutableSendMedia.File.of(name, inputFile);
        }

        String name();

        InputFile inputFile();

        @Override
        default Tuple2<String, InputFile> asRequest() {
            return Tuples.of(name(), inputFile());
        }
    }
}
