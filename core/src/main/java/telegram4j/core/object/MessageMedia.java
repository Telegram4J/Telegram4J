package telegram4j.core.object;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.internal.EntityFactory;
import telegram4j.core.object.media.GeoPoint;
import telegram4j.core.object.media.PollResults;
import telegram4j.core.util.Id;
import telegram4j.mtproto.file.Context;
import telegram4j.tl.*;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public sealed class MessageMedia implements TelegramObject {

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
        DICE,

        STORY
    }

    public static final class Geo extends MessageMedia {

        private final MessageMediaGeo data;

        public Geo(MTProtoTelegramClient client, MessageMediaGeo data) {
            super(client, Type.GEO);
            this.data = Objects.requireNonNull(data);
        }

        public Optional<GeoPoint> getGeo() {
            return data.geo() instanceof BaseGeoPoint g
                    ? Optional.of(new GeoPoint(g))
                    : Optional.empty();
        }

        @Override
        public String toString() {
            return "MessageMediaGeo{" +
                    "data=" + data +
                    '}';
        }
    }

    public static final class Document extends MessageMedia {

        private final telegram4j.tl.MessageMedia data; // MessageMediaDocument/MessageMediaPhoto
        private final Context context;

        public Document(MTProtoTelegramClient client, telegram4j.tl.MessageMediaDocument data, Context context) {
            super(client, Type.DOCUMENT);
            this.data = Objects.requireNonNull(data);
            this.context = Objects.requireNonNull(context);
        }

        public Document(MTProtoTelegramClient client, telegram4j.tl.MessageMediaPhoto data, Context context) {
            super(client, Type.DOCUMENT);
            this.data = Objects.requireNonNull(data);
            this.context = Objects.requireNonNull(context);
        }

        public boolean isNoPremium() {
            return data instanceof MessageMediaDocument d && d.nopremium();
        }

        /**
         * Gets whether this media file hidden by spoiler.
         *
         * @return {@code true} if media file hidden by spoiler.
         */
        public boolean hasSpoiler() {
            if (data instanceof MessageMediaDocument d) {
                return d.spoiler();
            } else if (data instanceof MessageMediaPhoto p) {
                return p.spoiler();
            } else {
                throw new IllegalStateException();
            }
        }

        /**
         * Gets document of the message, if it hasn't expired by timer.
         *
         * @return The {@link telegram4j.core.object.Document} of the message, if it hasn't expired by timer.
         */
        public Optional<telegram4j.core.object.Document> getDocument() {
            if (data instanceof MessageMediaDocument d) {
                return d.document() instanceof BaseDocument b
                        ? Optional.of(EntityFactory.createDocument(client, b, context))
                        : Optional.empty();
            } else if (data instanceof MessageMediaPhoto p) {
                return p.photo() instanceof BasePhoto b
                        ? Optional.of(new Photo(client, b, context))
                        : Optional.empty();
            } else {
                throw new IllegalStateException();
            }
        }

        /**
         * Gets {@link Duration} of the document self-destruction, if present.
         *
         * @return The {@link Duration} of the document self-destruction, if present.
         */
        public Optional<Duration> getAutoDeleteDuration() {
            Integer ttlSeconds;
            if (data instanceof MessageMediaDocument d) {
                ttlSeconds = d.ttlSeconds();
            } else if (data instanceof MessageMediaPhoto p) {
                ttlSeconds = p.ttlSeconds();
            } else {
                throw new IllegalStateException();
            }

            return Optional.ofNullable(ttlSeconds)
                    .map(Duration::ofSeconds);
        }

        @Override
        public String toString() {
            return "MessageMediaDocument{" +
                    "data=" + data +
                    '}';
        }
    }

    public static final class Venue extends MessageMedia {

        private final MessageMediaVenue data;

        public Venue(MTProtoTelegramClient client, MessageMediaVenue data) {
            super(client, Type.VENUE);
            this.data = Objects.requireNonNull(data);
        }

        public Optional<GeoPoint> getGeo() {
            return data.geo() instanceof BaseGeoPoint g
                    ? Optional.of(new GeoPoint(g))
                    : Optional.empty();
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
        public String toString() {
            return "MessageMediaVenue{" +
                    "data=" + data +
                    '}';
        }
    }

    public static final class Game extends MessageMedia {

        private final MessageMediaGame data;
        private final Context context;

        public Game(MTProtoTelegramClient client, MessageMediaGame data, Context context) {
            super(client, Type.GAME);
            this.data = Objects.requireNonNull(data);
            this.context = Objects.requireNonNull(context);
        }

        public telegram4j.core.object.media.Game getGame() {
            return new telegram4j.core.object.media.Game(client, data.game(), context);
        }

        @Override
        public String toString() {
            return "MessageMediaGame{" +
                    "data=" + data +
                    '}';
        }
    }

    public static final class WebPage extends MessageMedia {

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
        public String toString() {
            return "MessageMediaWebPage{" +
                    "data=" + data +
                    '}';
        }
    }

    public static final class Contact extends MessageMedia {

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
            return Id.ofUser(data.userId());
        }

        @Override
        public String toString() {
            return "MessageMediaContact{" +
                    "data=" + data +
                    '}';
        }
    }

    public static final class GeoLive extends MessageMedia {

        private final MessageMediaGeoLive data;

        public GeoLive(MTProtoTelegramClient client, MessageMediaGeoLive data) {
            super(client, Type.GEO_LIVE);
            this.data = Objects.requireNonNull(data);
        }

        public Optional<GeoPoint> getGeo() {
            return data.geo() instanceof BaseGeoPoint g
                    ? Optional.of(new GeoPoint(g))
                    : Optional.empty();
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
        public String toString() {
            return "MessageMediaGeoLive{" +
                    "data=" + data +
                    '}';
        }
    }

    public static final class Poll extends MessageMedia {

        private final MessageMediaPoll data;
        private final Id peer;

        public Poll(MTProtoTelegramClient client, MessageMediaPoll data, Id peer) {
            super(client, Type.POLL);
            this.data = Objects.requireNonNull(data);
            this.peer = Objects.requireNonNull(peer);
        }

        /**
         * Gets information about poll.
         *
         * @return The {@link telegram4j.core.object.media.Poll} object.
         */
        public telegram4j.core.object.media.Poll getPoll() {
            return new telegram4j.core.object.media.Poll(client, data.poll(), peer);
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
        public String toString() {
            return "MessageMediaPoll{" +
                    "data=" + data +
                    '}';
        }
    }

    public static final class Invoice extends MessageMedia {

        private final MessageMediaInvoice data;
        private final Context context;

        public Invoice(MTProtoTelegramClient client, MessageMediaInvoice data, Context context) {
            super(client, Type.INVOICE);
            this.data = Objects.requireNonNull(data);
            this.context = Objects.requireNonNull(context);
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
                    .map(d -> EntityFactory.createDocument(client, d, context));
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
        public String toString() {
            return "MessageMediaInvoice{" +
                    "data=" + data +
                    '}';
        }
    }

    public static final class Dice extends MessageMedia {

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
        public String toString() {
            return "MessageMediaDice{" +
                    "data=" + data +
                    '}';
        }
    }

    public static final class Story extends MessageMedia {
        private final MessageMediaStory data;

        public Story(MTProtoTelegramClient client, MessageMediaStory data) {
            super(client, Type.STORY);
            this.data = data;
        }
    }
}
