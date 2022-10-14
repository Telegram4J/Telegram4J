package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.internal.EntityFactory;
import telegram4j.core.object.media.GeoPoint;
import telegram4j.core.object.media.PollResults;
import telegram4j.core.util.Id;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.*;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public class MessageMedia implements TelegramObject {

    protected final MTProtoTelegramClient client;
    protected final Type type;

    public MessageMedia(MTProtoTelegramClient client, Type type) {
        this.client = Objects.requireNonNull(client);
        this.type = Objects.requireNonNull(type);
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    public Type getType() {
        return type;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageMedia that = (MessageMedia) o;
        return type == that.type;
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public String toString() {
        return "MessageMedia{" +
                "type=" + type +
                '}';
    }

    public enum Type {

        /** Attached map. */
        GEO,

        /** Attached contact. */
        CONTACT,

        /** Current version of the client does not support this media type. */
        UNSUPPORTED,

        /** Any type of files. */
        DOCUMENT,

        /** Preview of webpage. */
        WEB_PAGE,

        /** Message venue. */
        VENUE,

        /** Telegram game. */
        GAME,

        /** Payment invoice. */
        INVOICE,

        /** Indicates a live geolocation. */
        GEO_LIVE,

        /** Message poll. */
        POLL,

        /** Message dice. */
        DICE
    }

    public static class Geo extends MessageMedia {

        private final MessageMediaGeo data;

        public Geo(MTProtoTelegramClient client, MessageMediaGeo data) {
            super(client, Type.GEO);
            this.data = Objects.requireNonNull(data);
        }

        public Optional<GeoPoint> getGeo() {
            return Optional.ofNullable(TlEntityUtil.unmapEmpty(data.geo(), BaseGeoPoint.class)).map(GeoPoint::new);
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Geo that = (Geo) o;
            return data.equals(that.data);
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return "MessageMediaGeo{" +
                    "data=" + data +
                    '}';
        }
    }

    public static class Document extends MessageMedia {

        private final telegram4j.tl.MessageMedia data; // MessageMediaDocument/MessageMediaPhoto
        private final int messageId;
        private final InputPeer peer;

        public Document(MTProtoTelegramClient client, telegram4j.tl.MessageMedia data, int messageId, InputPeer peer) {
            super(client, Type.DOCUMENT);
            this.data = Objects.requireNonNull(data);
            this.messageId = messageId;
            this.peer = Objects.requireNonNull(peer);
        }

        public boolean isNoPremium() {
            return data.identifier() == MessageMediaDocument.ID && ((MessageMediaDocument) data).nopremium();
        }

        /**
         * Gets document of the message, if it hasn't expired by timer.
         *
         * @return The {@link telegram4j.core.object.Document} of the message, if it hasn't expired by timer.
         */
        public Optional<telegram4j.core.object.Document> getDocument() {
            switch (data.identifier()) {
                case MessageMediaDocument.ID:
                    return Optional.ofNullable(TlEntityUtil.unmapEmpty(((MessageMediaDocument) data).document(), BaseDocument.class))
                            .map(d -> EntityFactory.createDocument(client, d, messageId, peer));
                case MessageMediaPhoto.ID:
                    return Optional.ofNullable(TlEntityUtil.unmapEmpty(((MessageMediaPhoto) data).photo(), BasePhoto.class))
                            .map(d -> new telegram4j.core.object.Photo(client, d, peer, messageId));
                default: throw new IllegalStateException();
            }
        }

        /**
         * Gets {@link Duration} of the document self-destruction, if present.
         *
         * @return The {@link Duration} of the document self-destruction, if present.
         */
        public Optional<Duration> getAutoDeleteDuration() {
            switch (data.identifier()) {
                case MessageMediaDocument.ID:
                    return Optional.ofNullable(((MessageMediaDocument) data).ttlSeconds()).map(Duration::ofSeconds);
                case MessageMediaPhoto.ID:
                    return Optional.ofNullable(((MessageMediaPhoto) data).ttlSeconds()).map(Duration::ofSeconds);
                default: throw new IllegalStateException();
            }
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Document that = (Document) o;
            return data.equals(that.data);
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return "MessageMediaDocument{" +
                    "data=" + data +
                    '}';
        }
    }

    public static class Venue extends MessageMedia {

        private final MessageMediaVenue data;

        public Venue(MTProtoTelegramClient client, MessageMediaVenue data) {
            super(client, Type.VENUE);
            this.data = Objects.requireNonNull(data);
        }

        public Optional<GeoPoint> getGeo() {
            return Optional.ofNullable(TlEntityUtil.unmapEmpty(data.geo(), BaseGeoPoint.class)).map(GeoPoint::new);
        }

        public String getTitle() {
            return data.title();
        }

        public String getAddress() {
            return data.address();
        }

        public String getProvider() {
            return data.provider();
        }

        public String getVenueId() {
            return data.venueId();
        }

        public String getVenueType() {
            return data.venueType();
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Venue that = (Venue) o;
            return data.equals(that.data);
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return "MessageMediaVenue{" +
                    "data=" + data +
                    '}';
        }
    }

    public static class Game extends MessageMedia {

        private final MessageMediaGame data;
        private final int messageId;
        private final InputPeer peer;

        public Game(MTProtoTelegramClient client, MessageMediaGame data, int messageId, InputPeer peer) {
            super(client, Type.GAME);
            this.data = Objects.requireNonNull(data);
            this.messageId = messageId;
            this.peer = Objects.requireNonNull(peer);
        }

        public telegram4j.core.object.media.Game getGame() {
            return new telegram4j.core.object.media.Game(client, data.game(), messageId, peer);
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Game that = (Game) o;
            return data.equals(that.data);
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return "MessageMediaGame{" +
                    "data=" + data +
                    '}';
        }
    }

    public static class WebPage extends MessageMedia {

        private final MessageMediaWebPage data;

        public WebPage(MTProtoTelegramClient client, MessageMediaWebPage data) {
            super(client, Type.WEB_PAGE);
            this.data = Objects.requireNonNull(data);
        }

        // TODO
        public telegram4j.tl.WebPage webpage() {
            return data.webpage();
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            WebPage that = (WebPage) o;
            return data.equals(that.data);
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return "MessageMediaWebPage{" +
                    "data=" + data +
                    '}';
        }
    }

    public static class Contact extends MessageMedia {

        private final MessageMediaContact data;

        public Contact(MTProtoTelegramClient client, MessageMediaContact data) {
            super(client, Type.CONTACT);
            this.data = Objects.requireNonNull(data);
        }

        public String getPhoneNumber() {
            return data.phoneNumber();
        }

        public String getFirstName() {
            return data.firstName();
        }

        public String getLastName() {
            return data.lastName();
        }

        public String getVcard() {
            return data.vcard();
        }

        public Id getUserId() {
            return Id.ofUser(data.userId(), null);
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Contact that = (Contact) o;
            return data.equals(that.data);
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return "MessageMediaContact{" +
                    "data=" + data +
                    '}';
        }
    }

    public static class GeoLive extends MessageMedia {

        private final MessageMediaGeoLive data;

        public GeoLive(MTProtoTelegramClient client, MessageMediaGeoLive data) {
            super(client, Type.GEO_LIVE);
            this.data = Objects.requireNonNull(data);
        }

        public Optional<GeoPoint> getGeo() {
            return Optional.ofNullable(TlEntityUtil.unmapEmpty(data.geo(), BaseGeoPoint.class)).map(GeoPoint::new);
        }

        public Optional<Integer> getHeading() {
            return Optional.ofNullable(data.heading());
        }

        public int getPeriod() {
            return data.period();
        }

        public Optional<Integer> getProximityNotificationRadius() {
            return Optional.ofNullable(data.proximityNotificationRadius());
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GeoLive that = (GeoLive) o;
            return data.equals(that.data);
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return "MessageMediaGeoLive{" +
                    "data=" + data +
                    '}';
        }
    }

    public static class Poll extends MessageMedia {

        private final MessageMediaPoll data;

        public Poll(MTProtoTelegramClient client, MessageMediaPoll data) {
            super(client, Type.POLL);
            this.data = Objects.requireNonNull(data);
        }

        /**
         * Gets information about poll.
         *
         * @return The {@link telegram4j.core.object.media.Poll} object.
         */
        public telegram4j.core.object.media.Poll getPoll() {
            return new telegram4j.core.object.media.Poll(data.poll());
        }

        /**
         * Gets information about poll results.
         *
         * @return The {@link PollResults} object.
         */
        public PollResults getResults() {
            return new PollResults(client, data.results());
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Poll that = (Poll) o;
            return data.equals(that.data);
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return "MessageMediaPoll{" +
                    "data=" + data +
                    '}';
        }
    }

    public static class Invoice extends MessageMedia {

        private final MessageMediaInvoice data;
        private final int messageId;
        private final InputPeer peer;

        public Invoice(MTProtoTelegramClient client, MessageMediaInvoice data, int messageId, InputPeer peer) {
            super(client, Type.INVOICE);
            this.data = Objects.requireNonNull(data);
            this.messageId = messageId;
            this.peer = Objects.requireNonNull(peer);
        }

        public boolean isShippingAddressRequested() {
            return data.shippingAddressRequested();
        }

        public boolean isTest() {
            return data.test();
        }

        public String getTitle() {
            return data.title();
        }

        public String getDescription() {
            return data.description();
        }

        public Optional<telegram4j.core.object.Document> getPhoto() {
            return Optional.ofNullable(data.photo())
                    .map(d -> EntityFactory.createDocument(client, (BaseDocumentFields) d, messageId, peer));
        }

        public Optional<Integer> getReceiptMessageId() {
            return Optional.ofNullable(data.receiptMsgId());
        }

        public String getCurrency() {
            return data.currency();
        }

        public long getTotalAmount() {
            return data.totalAmount();
        }

        public String getStartParam() {
            return data.startParam();
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Invoice that = (Invoice) o;
            return data.equals(that.data);
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return "MessageMediaInvoice{" +
                    "data=" + data +
                    '}';
        }
    }

    public static class Dice extends MessageMedia {

        private final MessageMediaDice data;

        public Dice(MTProtoTelegramClient client, MessageMediaDice data) {
            super(client, Type.DICE);
            this.data = Objects.requireNonNull(data);
        }

        /**
         * Gets value of dice.
         *
         * @return The value of dice.
         * @see <a href="https://core.telegram.org/api/dice">Dice</a>
         */
        public int getValue() {
            return data.value();
        }

        /**
         * Gets dice unicode emoji.
         *
         * @return The dice unicode emoji.
         */
        public String getEmoticon() {
            return data.emoticon();
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Dice that = (Dice) o;
            return data.equals(that.data);
        }

        @Override
        public int hashCode() {
            return data.hashCode();
        }

        @Override
        public String toString() {
            return "MessageMediaDice{" +
                    "data=" + data +
                    '}';
        }
    }
}
