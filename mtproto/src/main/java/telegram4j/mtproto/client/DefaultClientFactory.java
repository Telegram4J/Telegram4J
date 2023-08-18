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
package telegram4j.mtproto.client;

import telegram4j.mtproto.DataCenter;
import telegram4j.mtproto.DcId;
import telegram4j.mtproto.client.impl.MTProtoClientImpl;

import java.util.Objects;

public class DefaultClientFactory implements ClientFactory {
    protected final MTProtoOptions mtprotoOptions;
    protected final MTProtoClient.Options clientOptions;

    public DefaultClientFactory(MTProtoOptions mtprotoOptions, MTProtoClient.Options clientOptions) {
        this.mtprotoOptions = Objects.requireNonNull(mtprotoOptions);
        this.clientOptions = Objects.requireNonNull(clientOptions);
    }

    @Override
    public MTProtoClient create(MTProtoClientGroup group, DcId.Type type, DataCenter dc) {
        return new MTProtoClientImpl(group, type, dc, mtprotoOptions, clientOptions);
    }
}
