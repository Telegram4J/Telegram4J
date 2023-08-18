/*
 * Copyright 2023 Telegram4J
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package telegram4j.core.object.markup;

import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.internal.EntityFactory;
import telegram4j.core.internal.MappingUtil;
import telegram4j.core.object.TelegramObject;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.AdminRight;
import telegram4j.core.object.chat.PrivateChat;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.spec.markup.KeyboardButtonSpec;
import telegram4j.core.util.Id;
import telegram4j.tl.*;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Representation of various types of markup button.
 *
 * @see KeyboardButtonSpec
 */
public final class KeyboardButton implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.KeyboardButton data;

    public KeyboardButton(MTProtoTelegramClient client, telegram4j.tl.KeyboardButton data) {
        this.client = Objects.requireNonNull(client);
        this.data = Objects.requireNonNull(data);
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    /**
     * Gets type of button.
     *
     * @return The {@link Type} of button.
     */
    public Type getType() {
        return Type.of(data);
    }

    /**
     * Gets text of button.
     *
     * @return The text of button.
     */
    public String getText() {
        return data.text();
    }

    /**
     * Gets callback data, if {@link #getType() type} is {@link Type#CALLBACK}.
     *
     * @return The callback data, if {@link #getType() type} is {@link Type#CALLBACK}.
     */
    public Optional<ByteBuf> getData() {
        return data instanceof KeyboardButtonCallback c
                ? Optional.of(c.data())
                : Optional.empty();
    }

    /**
     * Gets whether button allows creating quiz polls.
     * Can have 3 states: can be absent if {@link #getType() type} is not {@link Type#REQUEST_POLL}
     * or no these restriction (poll can be either quiz or regular);
     * {@code true} for quiz-only polls and {@code false} for regular polls.
     *
     * @return {@code true} if button allows creating quiz polls.
     */
    public Optional<Boolean> isQuiz() {
        return data instanceof KeyboardButtonRequestPoll p
                ? Optional.ofNullable(p.quiz())
                : Optional.empty();
    }

    /**
     * Gets inline query, if {@link #getType() type} is {@link Type#SWITCH_INLINE}.
     *
     * @return The inline query, if {@link #getType() type} is {@link Type#SWITCH_INLINE}.
     */
    public Optional<String> getQuery() {
        return data instanceof KeyboardButtonSwitchInline i
                ? Optional.of(i.query())
                : Optional.empty();
    }

    /**
     * Gets whether button uses current chat/channel for inline query, if {@link #getType() type} is {@link Type#SWITCH_INLINE}.
     *
     * @return {@code true} if button uses current chat/channel for inline query.
     */
    public boolean isSamePeer() {
        return data instanceof KeyboardButtonSwitchInline i && i.samePeer();
    }

    /**
     * Gets url that will be opened on press, if {@link #getType() type} is one of this:
     * {@link Type#URL}, {@link Type#URL_AUTH}, {@link Type#WEB_VIEW}, {@link Type#SIMPLE_WEB_VIEW}.
     *
     * @return The url that will be opened on press, if {@link #getType() type} is one of this:
     * {@link Type#URL}, {@link Type#URL_AUTH}, {@link Type#WEB_VIEW}, {@link Type#SIMPLE_WEB_VIEW}
     */
    public Optional<String> getUrl() {
        return switch (data.identifier()) {
            case KeyboardButtonWebView.ID -> Optional.of(((KeyboardButtonWebView) data).url());
            case KeyboardButtonSimpleWebView.ID -> Optional.of(((KeyboardButtonSimpleWebView) data).url());
            case KeyboardButtonUrl.ID -> Optional.of(((KeyboardButtonUrl) data).url());
            case KeyboardButtonUrlAuth.ID -> Optional.of(((KeyboardButtonUrlAuth) data).url());
            case InputKeyboardButtonUrlAuth.ID -> Optional.of(((InputKeyboardButtonUrlAuth) data).url());
            default -> Optional.empty();
        };
    }

    /**
     * Gets text that will be displayed in forwarded messages, if {@link #getType() type} is {@link Type#URL_AUTH}.
     *
     * @return The text that will be displayed in forwarded messages, if {@link #getType() type} is {@link Type#URL_AUTH}.
     */
    public Optional<String> getForwardText() {
        return data instanceof KeyboardButtonUrlAuth a
                ? Optional.ofNullable(a.fwdText())
                : Optional.empty();
    }

    /**
     * Gets id of login or request peer button, if {@link #getType() type} is
     * {@link Type#URL_AUTH} or {@link Type#REQUEST_PEER}.
     *
     * @return The id of login or request peer button, if {@link #getType() type} is
     * {@link Type#URL_AUTH} or {@link Type#REQUEST_PEER}.
     */
    public Optional<Integer> getButtonId() {
        return switch (data.identifier()) {
            case KeyboardButtonUrlAuth.ID -> Optional.of(((KeyboardButtonUrlAuth) data).buttonId());
            case KeyboardButtonRequestPeer.ID -> Optional.of(((KeyboardButtonRequestPeer) data).buttonId());
            default -> Optional.empty();
        };
    }

    /**
     * Gets whether bot requests the permission to send messages to user, if {@link #getType() type} is {@link Type#URL_AUTH}.
     *
     * @return {@code true} if bot requests the permission to send messages to user.
     */
    public boolean isRequestWriteAccess() {
        return data instanceof InputKeyboardButtonUrlAuth a && a.requestWriteAccess();
    }

    /**
     * Gets whether user should verify his identity by password, if {@link #getType() type} is {@link Type#CALLBACK}.
     *
     * @return {@code true} if user should verify his identity by password.
     */
    public boolean isRequiresPassword() {
        return data instanceof KeyboardButtonCallback a && a.requiresPassword();
    }

    /**
     * Gets id of bot which will be used for user authorization, if {@link #getType() type} is {@link Type#URL_AUTH}.
     *
     * @return The id of bot which will be used for user authorization, if {@link #getType() type} is {@link Type#URL_AUTH}.
     */
    public Optional<Id> getBotId() {
        return data instanceof InputKeyboardButtonUrlAuth a
                ? Optional.of(Id.of(a.bot(), client.getSelfId()))
                : Optional.empty();
    }

    /**
     * Requests to retrieve bot which will be used for user authorization.
     *
     * @return An {@link Mono} emitting on successful completion the {@link User user}.
     */
    public Mono<User> getBot() {
        return getBot(MappingUtil.IDENTITY_RETRIEVER);
    }

    /**
     * Requests to retrieve bot which will be used for user authorization using specified retrieval strategy.
     *
     * @param strategy The strategy to apply.
     * @return An {@link Mono} emitting on successful completion the {@link User user}.
     */
    public Mono<User> getBot(EntityRetrievalStrategy strategy) {
        return Mono.justOrEmpty(getBotId())
                .flatMap(id -> client.withRetrievalStrategy(strategy).getUserById(id));
    }

    /**
     * Gets id of user their profile will be opened on press, if {@link #getType() type} is {@link Type#USER_PROFILE}.
     *
     * @return The id of user their profile will be opened on press, if {@link #getType() type} is {@link Type#USER_PROFILE}.
     */
    public Optional<Id> getUserId() {
        return switch (data.identifier()) {
            case KeyboardButtonUserProfile.ID -> Optional.of(Id.ofUser(((KeyboardButtonUserProfile) data).userId()));
            case InputKeyboardButtonUserProfile.ID -> Optional.of(Id.of(
                    ((InputKeyboardButtonUserProfile) data).userId(), client.getSelfId()));
            default -> Optional.empty();
        };
    }

    /**
     * Requests to retrieve user their profile will be opened on press.
     *
     * @return An {@link Mono} emitting on successful completion the {@link User user}.
     */
    public Mono<User> getUser() {
        return getUser(MappingUtil.IDENTITY_RETRIEVER);
    }

    /**
     * Requests to retrieve user their profile will be opened on press using specified retrieval strategy.
     *
     * @param strategy The strategy to apply.
     * @return An {@link Mono} emitting on successful completion the {@link User user}.
     */
    public Mono<User> getUser(EntityRetrievalStrategy strategy) {
        return Mono.justOrEmpty(getUserId())
                .flatMap(id -> client.withRetrievalStrategy(strategy).getUserById(id));
    }

    /**
     * Gets {@code RequestPeer} parameters if {@link #getType()} is {@link Type#REQUEST_PEER}.
     *
     * @return The {@code RequestPeer} parameters if {@link #getType()} is {@link Type#REQUEST_PEER}.
     */
    public Optional<RequestPeer> getRequestPeer() {
        return data instanceof KeyboardButtonRequestPeer d
                ? Optional.of(EntityFactory.createRequestPeer(d.peerType()))
                : Optional.empty();

    }

    @Override
    public String toString() {
        return "KeyboardButton{" +
                "data=" + data +
                '}';
    }

    public enum Type {
        /** Bot keyboard button. */
        DEFAULT,

        /** Button to buy a product. */
        BUY,

        /** Callback button. */
        CALLBACK,

        /** Button to start a game. */
        GAME,

        /** Button to request a user's geolocation. */
        REQUEST_GEO_LOCATION,

        /** Button to request a user's phone number. */
        REQUEST_PHONE,

        /**
         * A button that allows the user to create and
         * send a poll when pressed; Available only in {@link PrivateChat DMs}.
         */
        REQUEST_POLL,

        /**
         * Button to force a user to switch to inline mode Pressing the button
         * will prompt the user to select one of their chats, open that chat and
         * insert the bot's username and the specified inline query in the input field.
         */
        SWITCH_INLINE,

        /** URL embedded button. */
        URL,

        /** An inline button that opens user profile which specified by {@link #getUserId()}. */
        USER_PROFILE,

        /**
         * Button to request a user to authorize via URL
         * using <a href="https://telegram.org/blog/privacy-discussions-web-bots#meet-seamless-web-bots">Seamless Telegram Login</a>.
         * When the user clicks on such a button, {@link telegram4j.tl.request.messages.RequestUrlAuth}
         * should be called, providing the {@link KeyboardButtonUrlAuth#buttonId()} and the ID of the container message.
         * The returned {@link UrlAuthResultRequest} object will contain more details
         * about the authorization request ({@link UrlAuthResultRequest#requestWriteAccess()}
         * if the bot would like to send messages to the user along with the username of
         * the bot which will be used for user authorization).
         * Finally, the user can choose to call {@link telegram4j.tl.request.messages.AcceptUrlAuth} to
         * get a {@link UrlAuthResultAccepted} with the URL to open instead of the {@code url}
         * of this constructor, or a {@link UrlAuthResultDefault}, in which case the {@code url}
         * of this constructor must be opened, instead. If the user refuses the
         * authorization request but still wants to open the link, the {@code url} of this constructor must be used.
         */
        URL_AUTH,

        SIMPLE_WEB_VIEW,

        WEB_VIEW,

        REQUEST_PEER;

        /**
         * Gets type of raw {@link telegram4j.tl.KeyboardButton} object.
         * Input button also will be handled but as resolved objects.
         *
         * @param data The button data.
         * @return The {@code Type} of raw button object.
         */
        public static Type of(telegram4j.tl.KeyboardButton data) {
            return switch (data.identifier()) {
                case BaseKeyboardButton.ID -> DEFAULT;
                case InputKeyboardButtonUrlAuth.ID, KeyboardButtonUrlAuth.ID -> URL_AUTH;
                case InputKeyboardButtonUserProfile.ID, KeyboardButtonUserProfile.ID -> USER_PROFILE;
                case KeyboardButtonBuy.ID -> BUY;
                case KeyboardButtonCallback.ID -> CALLBACK;
                case KeyboardButtonGame.ID -> GAME;
                case KeyboardButtonRequestGeoLocation.ID -> REQUEST_GEO_LOCATION;
                case KeyboardButtonRequestPhone.ID -> REQUEST_PHONE;
                case KeyboardButtonRequestPoll.ID -> REQUEST_POLL;
                case KeyboardButtonSwitchInline.ID -> SWITCH_INLINE;
                case KeyboardButtonUrl.ID -> URL;
                case KeyboardButtonSimpleWebView.ID -> SIMPLE_WEB_VIEW;
                case KeyboardButtonWebView.ID -> WEB_VIEW;
                case KeyboardButtonRequestPeer.ID -> REQUEST_PEER;
                default -> throw new IllegalStateException("Unexpected keyboard button type: " + data);
            };
        }

        /**
         * Gets whether this button supports only in the {@link ReplyMarkup.Type#INLINE inline} keyboards.
         *
         * @return {@code true} if button type is supports in the inline keyboards.
         */
        public boolean isInlineOnly() {
            return switch (this) {
                case BUY, CALLBACK, GAME, SWITCH_INLINE, URL_AUTH, USER_PROFILE, URL -> true;
                default -> false;
            };
        }
    }

    public static final class RequestChannel implements RequestPeer {

        private final RequestPeerTypeBroadcast data;

        public RequestChannel(RequestPeerTypeBroadcast data) {
            this.data = data;
        }

        public boolean isOwnedByUser() {
            return data.creator();
        }

        public Optional<Boolean> hasUsername() {
            return Optional.ofNullable(data.hasUsername());
        }

        public Optional<Set<AdminRight>> getUserAdminRights() {
            return Optional.ofNullable(data.userAdminRights()).map(AdminRight::of);
        }

        public Optional<Set<AdminRight>> getBotAdminRights() {
            return Optional.ofNullable(data.botAdminRights()).map(AdminRight::of);
        }

        @Override
        public String toString() {
            return "RequestChannel{" +
                    "data=" + data +
                    '}';
        }
    }

    public static final class RequestChat implements RequestPeer {

        private final RequestPeerTypeChat data;

        public RequestChat(RequestPeerTypeChat data) {
            this.data = data;
        }

        public boolean isOwnedByUser() {
            return data.creator();
        }

        public boolean isBotParticipant() {
            return data.botParticipant();
        }

        public Optional<Boolean> hasUsername() {
            return Optional.ofNullable(data.hasUsername());
        }

        public Optional<Boolean> isForum() {
            return Optional.ofNullable(data.forum());
        }

        public Optional<Set<AdminRight>> getUserAdminRights() {
            return Optional.ofNullable(data.userAdminRights()).map(AdminRight::of);
        }

        public Optional<Set<AdminRight>> getBotAdminRights() {
            return Optional.ofNullable(data.botAdminRights()).map(AdminRight::of);
        }

        @Override
        public String toString() {
            return "RequestChat{" +
                    "data=" + data +
                    '}';
        }
    }

    public static final class RequestUser implements RequestPeer {

        private final RequestPeerTypeUser data;

        public RequestUser(RequestPeerTypeUser data) {
            this.data = data;
        }

        public Optional<Boolean> isBot() {
            return Optional.ofNullable(data.bot());
        }

        public Optional<Boolean> isPremium() {
            return Optional.ofNullable(data.premium());
        }

        @Override
        public String toString() {
            return "RequestUser{" +
                    "data=" + data +
                    '}';
        }
    }

    public sealed interface RequestPeer {}
}
