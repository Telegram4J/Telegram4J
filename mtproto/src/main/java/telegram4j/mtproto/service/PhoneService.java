package telegram4j.mtproto.service;

import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Mono;
import telegram4j.mtproto.MTProtoClient;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.*;
import telegram4j.tl.phone.GroupCall;
import telegram4j.tl.phone.PhoneCall;
import telegram4j.tl.phone.*;
import telegram4j.tl.request.phone.*;

import java.util.List;

public class PhoneService extends RpcService {

    public PhoneService(MTProtoClient client, StoreLayout storeLayout) {
        super(client, storeLayout);
    }

    // phone namespace
    // ======================

    public Mono<String> getCallConfig() {
        return client.sendAwait(GetCallConfig.instance()).map(DataJSON::data);
    }

    public Mono<PhoneCall> requestCall(RequestCall request) {
        return client.sendAwait(request);
    }

    public Mono<PhoneCall> acceptCall(InputPhoneCall peer, ByteBuf gb, PhoneCallProtocol protocol) {
        return Mono.defer(() -> client.sendAwait(ImmutableAcceptCall.of(peer, gb, protocol)));
    }

    public Mono<PhoneCall> confirmCall(InputPhoneCall peer, ByteBuf ga, long keyFingerPrint, PhoneCallProtocol protocol) {
        return Mono.defer(() -> client.sendAwait(ImmutableConfirmCall.of(peer, ga, keyFingerPrint, protocol)));
    }

    public Mono<Boolean> receivedCall(InputPhoneCall peer) {
        return client.sendAwait(ImmutableReceivedCall.of(peer));
    }

    public Mono<Updates> discardCall(DiscardCall request) {
        return client.sendAwait(request);
    }

    public Mono<Updates> setCallRating(SetCallRating request) {
        return client.sendAwait(request);
    }

    public Mono<Boolean> saveCallDebug(InputPhoneCall peer, String debugJson) {
        return client.sendAwait(ImmutableSaveCallDebug.of(peer, ImmutableDataJSON.of(debugJson)));
    }

    public Mono<Boolean> sendSignalingData(InputPhoneCall peer, ByteBuf data) {
        return Mono.defer(() -> client.sendAwait(ImmutableSendSignalingData.of(peer, data)));
    }

    public Mono<Updates> createGroupCall(CreateGroupCall request) {
        return client.sendAwait(request);
    }

    public Mono<Updates> joinGroupCall(JoinGroupCall request) {
        return client.sendAwait(request);
    }

    public Mono<Updates> leaveGroupCall(InputGroupCall peer, int source) {
        return client.sendAwait(ImmutableLeaveGroupCall.of(peer, source));
    }

    public Mono<Updates> inviteToGroupCall(InputGroupCall peer, Iterable<? extends InputUser> ids) {
        return Mono.defer(() -> client.sendAwait(ImmutableInviteToGroupCall.of(peer, ids)));
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

    public Mono<GroupParticipants> getGroupParticipant(GetGroupParticipants request) {
        return client.sendAwait(request);
    }

    public Mono<List<Integer>> checkGroupCall(InputGroupCall peer, Iterable<Integer> sources) {
        return client.sendAwait(ImmutableCheckGroupCall.of(peer, sources));
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

    public Mono<GroupCallStreamChannels> getGroupCallStreamChannels(InputGroupCall peer) {
        return client.sendAwait(ImmutableGetGroupCallStreamChannels.of(peer));
    }

    public Mono<GroupCallStreamRtmpUrl> getGroupCallStreamRtmpUrl(InputPeer peer, boolean revoke) {
        return client.sendAwait(ImmutableGetGroupCallStreamRtmpUrl.of(peer, revoke));
    }

    public Mono<Boolean> saveCallLog(InputPhoneCall peer, InputFile file) {
        return client.sendAwait(ImmutableSaveCallLog.of(peer, file));
    }
}
