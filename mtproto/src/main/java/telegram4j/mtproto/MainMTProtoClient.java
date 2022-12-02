package telegram4j.mtproto;

import reactor.core.publisher.Sinks;
import telegram4j.tl.Updates;
import telegram4j.tl.request.messages.SendMessage;

public interface MainMTProtoClient extends MTProtoClient {

    /**
     * Create child client, associated with given datacenter.
     *
     * @return The new child client.
     */
    MTProtoClient createChildClient(DataCenter dc);

    MainMTProtoClient create(DataCenter dc);

    /**
     * Gets a {@link Sinks.Many} which redistributes api updates to subscribers and
     * which it can be used to resend updates, as is the case with {@link SendMessage} mapping.
     *
     * @return A {@link Sinks.Many} which redistributes api updates.
     */
    Sinks.Many<Updates> updates();
}
