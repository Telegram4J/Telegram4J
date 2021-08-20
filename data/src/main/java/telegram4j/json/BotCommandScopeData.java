package telegram4j.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

public interface BotCommandScopeData {

    BotCommandScopeType type();

    @Value.Immutable
    @JsonSerialize(as = ImmutableBotCommandScopeDefaultData.class)
    @JsonDeserialize(as = ImmutableBotCommandScopeDefaultData.class)
    interface BotCommandScopeDefaultData extends BotCommandScopeData {

        static ImmutableBotCommandScopeDefaultData.Builder builder() {
            return ImmutableBotCommandScopeDefaultData.builder();
        }

        @Override
        default BotCommandScopeType type() {
            return BotCommandScopeType.DEFAULT;
        }
    }

    @Value.Immutable
    @JsonSerialize(as = ImmutableBotCommandScopeAllPrivateChatsData.class)
    @JsonDeserialize(as = ImmutableBotCommandScopeAllPrivateChatsData.class)
    interface BotCommandScopeAllPrivateChatsData extends BotCommandScopeData {

        static ImmutableBotCommandScopeAllPrivateChatsData.Builder builder() {
            return ImmutableBotCommandScopeAllPrivateChatsData.builder();
        }

        @Override
        default BotCommandScopeType type() {
            return BotCommandScopeType.ALL_PRIVATE_CHATS;
        }
    }

    @Value.Immutable
    @JsonSerialize(as = ImmutableBotCommandScopeAllGroupChatsData.class)
    @JsonDeserialize(as = ImmutableBotCommandScopeAllGroupChatsData.class)
    interface BotCommandScopeAllGroupChatsData extends BotCommandScopeData {

        static ImmutableBotCommandScopeAllGroupChatsData.Builder builder() {
            return ImmutableBotCommandScopeAllGroupChatsData.builder();
        }

        @Override
        default BotCommandScopeType type() {
            return BotCommandScopeType.ALL_GROUP_CHATS;
        }
    }

    @Value.Immutable
    @JsonSerialize(as = ImmutableBotCommandScopeAllChatAdministratorsData.class)
    @JsonDeserialize(as = ImmutableBotCommandScopeAllChatAdministratorsData.class)
    interface BotCommandScopeAllChatAdministratorsData extends BotCommandScopeData {

        static ImmutableBotCommandScopeAllChatAdministratorsData.Builder builder() {
            return ImmutableBotCommandScopeAllChatAdministratorsData.builder();
        }

        @Override
        default BotCommandScopeType type() {
            return BotCommandScopeType.ALL_CHAT_ADMINISTRATORS;
        }
    }

    @Value.Immutable
    @JsonSerialize(as = ImmutableBotCommandScopeChatData.class)
    @JsonDeserialize(as = ImmutableBotCommandScopeChatData.class)
    interface BotCommandScopeChatData extends BotCommandScopeData {

        static ImmutableBotCommandScopeChatData.Builder builder() {
            return ImmutableBotCommandScopeChatData.builder();
        }

        @Override
        default BotCommandScopeType type() {
            return BotCommandScopeType.CHAT;
        }

        @JsonProperty("chat_id")
        String chatId();
    }

    @Value.Immutable
    @JsonSerialize(as = ImmutableBotCommandScopeChatAdministratorsData.class)
    @JsonDeserialize(as = ImmutableBotCommandScopeChatAdministratorsData.class)
    interface BotCommandScopeChatAdministratorsData extends BotCommandScopeData {

        static ImmutableBotCommandScopeChatAdministratorsData.Builder builder() {
            return ImmutableBotCommandScopeChatAdministratorsData.builder();
        }

        @Override
        default BotCommandScopeType type() {
            return BotCommandScopeType.CHAT_ADMINISTRATORS;
        }

        @JsonProperty("chat_id")
        String chatId();
    }

    @Value.Immutable
    @JsonSerialize(as = ImmutableBotCommandScopeChatMemberData.class)
    @JsonDeserialize(as = ImmutableBotCommandScopeChatMemberData.class)
    interface BotCommandScopeChatMemberData extends BotCommandScopeData {

        static ImmutableBotCommandScopeChatMemberData.Builder builder() {
            return ImmutableBotCommandScopeChatMemberData.builder();
        }

        @Override
        default BotCommandScopeType type() {
            return BotCommandScopeType.CHAT_MEMBER;
        }

        @JsonProperty("chat_id")
        String chatId();

        @JsonProperty("user_id")
        String userId();
    }
}
