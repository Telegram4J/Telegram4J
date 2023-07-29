package telegram4j.mtproto.client.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import reactor.util.concurrent.Queues;
import telegram4j.mtproto.*;
import telegram4j.mtproto.auth.DhPrimeCheckerCache;
import telegram4j.mtproto.client.DefaultReconnectionStrategy;
import telegram4j.mtproto.client.MTProtoClient;
import telegram4j.mtproto.client.MTProtoOptions;
import telegram4j.mtproto.resource.TcpClientResources;
import telegram4j.mtproto.store.StoreLayoutImpl;
import telegram4j.mtproto.transport.IntermediateTransport;
import telegram4j.tl.TlInfo;
import telegram4j.tl.api.TlObject;
import telegram4j.tl.mtproto.ImmutableMsgsAck;
import telegram4j.tl.request.ImmutableInitConnection;
import telegram4j.tl.request.ImmutableInvokeWithLayer;
import telegram4j.tl.request.help.GetConfig;
import telegram4j.tl.request.mtproto.ImmutableDestroySession;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ClientTest {

    MTProtoClientImpl client;

    @BeforeEach
    void clientInit() {
        final var DC_2 = DcOptions.createDefault(false)
                .find(DcId.Type.MAIN, 2)
                .orElseThrow();

        final var mtprotoOptions = new MTProtoOptions(
                TcpClientResources.create(true),
                PublicRsaKeyRegister.createDefault(),
                DhPrimeCheckerCache.instance(),
                new StoreLayoutImpl(Function.identity()),
                ForkJoinPool.commonPool(), false
        );

        final var clientOptions = new MTProtoClient.Options(
                dc -> new IntermediateTransport(true),
                ImmutableInvokeWithLayer.of(TlInfo.LAYER, ImmutableInitConnection.of(1337,
                        "N/A", "N/A", "N/A", "N/A", "N/A", "N/A", GetConfig.instance())),
                Duration.ofSeconds(10), DefaultReconnectionStrategy.create(3, 5, Duration.ofSeconds(1)),
                16 * 1024, List.of(),
                Duration.ofDays(1)
        );

        client = new MTProtoClientImpl(
                null, DcId.Type.MAIN,
                DC_2, mtprotoOptions, clientOptions);
    }

    @Test
    void testOnClose() {
        StepVerifier.create(client.onClose())
                .then(() -> assertDoesNotThrow(() -> client.close().block()))
                .verifyComplete();
    }

    @Test
    void testCloseForDisconnected() {
        assertDoesNotThrow(() -> client.close().block());
        assertDoesNotThrow(() -> client.close().block());
    }

    @Test
    void testSendForDisconnected() {
        client.send(GetConfig.instance()).subscribe();
        assertEquals(1, client.pendingRequests.size());
    }

    @Test
    void testSendForClosed() {
        client.close().block();
        StepVerifier.create(client.send(GetConfig.instance()))
                .verifyErrorMatches(t -> t instanceof MTProtoException m &&
                        m.getMessage().equals("Client has been closed"));
    }

    @Test
    void illegalMethod() {
        Function<TlObject, Predicate<Throwable>> expectedError = req ->
                t -> t instanceof MTProtoException m &&
                        m.getMessage().equals("Illegal method was sent: " + req);

        var msgsAck = ImmutableMsgsAck.of(List.of());
        StepVerifier.create(client.send(msgsAck)).verifyErrorMatches(expectedError.apply(msgsAck));
        var destroySession = ImmutableDestroySession.of(1337);
        StepVerifier.create(client.send(destroySession)).verifyErrorMatches(expectedError.apply(destroySession));
    }

    @Test
    void testDiscard() {
        for (int i = 0; i < Queues.SMALL_BUFFER_SIZE; i++) {
            client.send(GetConfig.instance()).subscribe();
        }
        StepVerifier.create(client.send(GetConfig.instance()))
                .verifyErrorMatches(t -> t instanceof DiscardedRpcRequestException d
                        && d.getMethod() == GetConfig.instance());
    }
}
