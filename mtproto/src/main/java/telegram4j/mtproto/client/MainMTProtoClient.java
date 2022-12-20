package telegram4j.mtproto.client;

import reactor.core.publisher.Sinks;
import telegram4j.tl.Updates;
import telegram4j.tl.request.messages.SendMessage;

public interface MainMTProtoClient extends MTProtoClient {

    /**
     * Gets a multicast {@link Sinks.Many} which redistributes api updates to subscribers and
     * which it can be used to resend updates, as is the case with {@link SendMessage} mapping.
     *
     * @return A {@link Sinks.Many} which redistributes api updates.
     */
    // TODO: This should not be tied to the client.
    //  It will need to be moved to some global location
    Sinks.Many<Updates> updates();
}
