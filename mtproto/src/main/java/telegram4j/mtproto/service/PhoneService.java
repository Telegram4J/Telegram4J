package telegram4j.mtproto.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.mtproto.MTProtoClient;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.*;
import telegram4j.tl.phone.*;
import telegram4j.tl.phone.GroupCall;
import telegram4j.tl.phone.PhoneCall;
import telegram4j.tl.request.phone.*;

import java.util.function.Function;

public class PhoneService extends RpcService {

    public PhoneService(MTProtoClient client, StoreLayout storeLayout) {
        super(client, storeLayout);
    }

    public Mono<String> getCallConfig() {
        return client.sendAwait(GetCallConfig.instance())
                .map(DataJSON::data);
    }

    public Mono<PhoneCall> requestCall(RequestCall request) {
        return client.sendAwait(request);
    }

    public Mono<PhoneCall> acceptCall(InputPhoneCall peer, byte[] gB, PhoneCallProtocol protocol) {
        return client.sendAwait(ImmutableAcceptCall.of(peer, gB, protocol));
    }

    public Mono<PhoneCall> confirmCall(InputPhoneCall peer, byte[] gB, long keyFingerPrint, PhoneCallProtocol protocol) {
        return client.sendAwait(ImmutableConfirmCall.of(peer, gB, keyFingerPrint, protocol));
    }

    public Mono<Boolean> receivedCall(InputPhoneCall peer) {
        return client.sendAwait(ImmutableReceivedCall.of(peer));
    }

    // TODO: check updates type
    public Mono<Updates> discardCall(DiscardCall request) {
        return client.sendAwait(request);
    }

    public Mono<Updates> setCallRating(SetCallRating request) {
        return client.sendAwait(request);
    }

    public Mono<Boolean> saveCallDebug(InputPhoneCall peer, String debugJson) {
        return client.sendAwait(ImmutableSaveCallDebug.of(peer, ImmutableDataJSON.of(debugJson)));
    }

    public Mono<Boolean> sendSignalingData(InputPhoneCall peer, byte[] data) {
        return client.sendAwait(ImmutableSendSignalingData.of(peer, data));
    }

    public Mono<Updates> createGroupCall(InputPeer peer, int randomId, String title, int scheduleDate) {
        return client.sendAwait(CreateGroupCall.builder()
                .peer(peer)
                .randomId(randomId)
                .title(title)
                .scheduleDate(scheduleDate)
                .build());
    }

    // TODO: unwrap DataJSON param
    public Mono<Updates> joinGroupCall(JoinGroupCall request) {
        return client.sendAwait(request);
    }

    public Mono<Updates> leaveGroupCall(InputGroupCall peer, int source) {
        return client.sendAwait(ImmutableLeaveGroupCall.of(peer, source));
    }

    public Mono<Updates> inviteToGroupCall(InputGroupCall peer, Iterable<? extends InputUser> ids) {
        return client.sendAwait(InviteToGroupCall.builder()
                .call(peer)
                .users(ids)
                .build());
    }

    public Mono<Updates> discardGroupCall(DiscardCall request) {
        return client.sendAwait(request);
    }

    public Mono<Updates> toggleGroupCallSettings(ToggleGroupCallSettings request) {
        return client.sendAwait(request);
    }

    public Mono<GroupCall> getGroupCall(InputGroupCall peer, int limit) {
        return client.sendAwait(ImmutableGetGroupCall.of(peer, limit));
    }

    public Mono<GroupParticipants> getGroupParticipant(InputGroupCall peer, Iterable<? extends InputPeer> ids,
                                                       Iterable<Integer> sources, String offset, int limit) {
        return client.sendAwait(GetGroupParticipants.builder()
                .call(peer)
                .ids(ids)
                .sources(sources)
                .offset(offset)
                .limit(limit)
                .build());
    }

    public Flux<Integer> checkGroupCall(InputGroupCall peer, Iterable<Integer> sources) {
        return client.sendAwait(CheckGroupCall.builder()
                .call(peer)
                .sources(sources)
                .build())
                .flatMapIterable(Function.identity());
    }

    public Mono<Updates> toggleGroupCallRecord(ToggleGroupCallRecord request) {
        return client.sendAwait(request);
    }

    public Mono<Updates> editGroupCallParticipant(EditGroupCallParticipant request) {
        return client.sendAwait(request);
    }

    public Mono<Updates> editGroupCallTitle(InputGroupCall peer, String title) {
        return client.sendAwait(ImmutableEditGroupCallTitle.of(peer, title));
    }

    public Mono<JoinAsPeers> getGroupCallJoinAs(InputPeer peer) {
        return client.sendAwait(ImmutableGetGroupCallJoinAs.of(peer));
    }

    public Mono<ExportedGroupCallInvite> exportGroupCallInvite(ExportGroupCallInvite request) {
        return client.sendAwait(request);
    }

    public Mono<Updates> toggleGroupCallStartSubscription(InputGroupCall peer, boolean subscribed) {
        return client.sendAwait(ImmutableToggleGroupCallStartSubscription.of(peer, subscribed));
    }

    public Mono<Updates> startScheduledGroupCall(InputGroupCall peer) {
        return client.sendAwait(ImmutableStartScheduledGroupCall.of(peer));
    }

    public Mono<Boolean> saveDefaultGroupCallJoinAs(InputPeer peer, InputPeer joinAs) {
        return client.sendAwait(ImmutableSaveDefaultGroupCallJoinAs.of(peer, joinAs));
    }

    public Mono<Updates> joinGroupCallPresentation(InputGroupCall peer, String paramsJson) {
        return client.sendAwait(ImmutableJoinGroupCallPresentation.of(peer, ImmutableDataJSON.of(paramsJson)));
    }

    public Mono<Updates> leaveGroupCallPresentation(InputGroupCall peer) {
        return client.sendAwait(ImmutableLeaveGroupCallPresentation.of(peer));
    }
}
