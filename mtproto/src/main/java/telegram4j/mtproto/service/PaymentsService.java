package telegram4j.mtproto.service;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.mtproto.MTProtoClient;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.ImmutableDataJSON;
import telegram4j.tl.InputPeer;
import telegram4j.tl.payments.*;
import telegram4j.tl.request.payments.*;

public class PaymentsService extends RpcService {

    public PaymentsService(MTProtoClient client, StoreLayout storeLayout) {
        super(client, storeLayout);
    }

    public Mono<PaymentForm> getPaymentForm(InputPeer peer, int msgId, @Nullable String themeParams) {
        return client.sendAwait(GetPaymentForm.builder()
                .peer(peer)
                .msgId(msgId)
                .themeParams(themeParams != null ? ImmutableDataJSON.of(themeParams) : null)
                .build());
    }

    public Mono<PaymentReceipt> getPaymentReceipt(InputPeer peer, int msgId) {
        return client.sendAwait(ImmutableGetPaymentReceipt.of(peer, msgId));
    }

    public Mono<ValidatedRequestedInfo> validateRequestedInfo(ValidateRequestedInfo request) {
        return client.sendAwait(request);
    }

    public Mono<PaymentResult> sendPaymentForm(SendPaymentForm request) {
        return client.sendAwait(request);
    }

    public Mono<SavedInfo> getSavedInfo() {
        return client.sendAwait(GetSavedInfo.instance());
    }

    public Mono<Boolean> clearSavedInfo(ClearSavedInfo request) {
        return client.sendAwait(request);
    }

    public Mono<BankCardData> getBankCardData(String number) {
        return client.sendAwait(ImmutableGetBankCardData.of(number));
    }
}
