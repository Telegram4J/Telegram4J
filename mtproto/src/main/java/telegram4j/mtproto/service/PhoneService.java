package telegram4j.mtproto.service;

import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Mono;
import telegram4j.mtproto.MTProtoClientGroup;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.*;
import telegram4j.tl.phone.GroupCall;
import telegram4j.tl.phone.PhoneCall;
import telegram4j.tl.phone.*;
import telegram4j.tl.request.phone.*;

import java.util.List;

public class PhoneService extends RpcService {

    public PhoneService(MTProtoClientGroup groupManager, StoreLayout storeLayout) {
        super(groupManager, storeLayout);
    }

    // phone namespace
    // ======================

    public Mono<String> getCallConfig() {
        return sendMain(GetCallConfig.instance()).map(DataJSON::data);
    }

    public Mono<PhoneCall> requestCall(RequestCall request) {
        return sendMain(request);
    }

    public Mono<PhoneCall> acceptCall(InputPhoneCall peer, ByteBuf gb, PhoneCallProtocol protocol) {
        return Mono.defer(() -> sendMain(ImmutableAcceptCall.of(peer, gb, protocol)));
    }

    public Mono<PhoneCall> confirmCall(InputPhoneCall peer, ByteBuf ga, long keyFingerPrint, PhoneCallProtocol protocol) {
        return Mono.defer(() -> sendMain(ImmutableConfirmCall.of(peer, ga, keyFingerPrint, protocol)));
    }

    public Mono<Boolean> receivedCall(InputPhoneCall peer) {
        return sendMain(ImmutableReceivedCall.of(peer));
    }

    public Mono<Updates> discardCall(DiscardCall request) {
        return sendMain(request);
    }

    public Mono<Updates> setCallRating(SetCallRating request) {
        return sendMain(request);
    }

    public Mono<Boolean> saveCallDebug(InputPhoneCall peer, String debugJson) {
        return sendMain(ImmutableSaveCallDebug.of(peer, ImmutableDataJSON.of(debugJson)));
    }

    public Mono<Boolean> sendSignalingData(InputPhoneCall peer, ByteBuf data) {
        return Mono.defer(() -> sendMain(ImmutableSendSignalingData.of(peer, data)));
    }

    public Mono<Updates> createGroupCall(CreateGroupCall request) {
        return sendMain(request);
    }

    public Mono<Updates> joinGroupCall(JoinGroupCall request) {
        return sendMain(request);
    }

    public Mono<Updates> leaveGroupCall(InputGroupCall peer, int source) {
        return sendMain(ImmutableLeaveGroupCall.of(peer, source));
    }

    public Mono<Updates> inviteToGroupCall(InputGroupCall peer, Iterable<? extends InputUser> ids) {
        return Mono.defer(() -> sendMain(ImmutableInviteToGroupCall.of(peer, ids)));
    }

    public Mono<Updates> discardGroupCall(DiscardCall request) {
        return sendMain(request);
    }

    public Mono<Updates> toggleGroupCallSettings(ToggleGroupCallSettings request) {
        return sendMain(request);
    }

    public Mono<GroupCall> getGroupCall(InputGroupCall peer, int limit) {
        return sendMain(ImmutableGetGroupCall.of(peer, limit));
    }

    public Mono<GroupParticipants> getGroupParticipant(GetGroupParticipants request) {
        return sendMain(request);
    }

    public Mono<List<Integer>> checkGroupCall(InputGroupCall peer, Iterable<Integer> sources) {
        return sendMain(ImmutableCheckGroupCall.of(peer, sources));
    }

    public Mono<Updates> toggleGroupCallRecord(ToggleGroupCallRecord request) {
        return sendMain(request);
    }

    public Mono<Updates> editGroupCallParticipant(EditGroupCallParticipant request) {
        return sendMain(request);
    }

    public Mono<Updates> editGroupCallTitle(InputGroupCall peer, String title) {
        return sendMain(ImmutableEditGroupCallTitle.of(peer, title));
    }

    public Mono<JoinAsPeers> getGroupCallJoinAs(InputPeer peer) {
        return sendMain(ImmutableGetGroupCallJoinAs.of(peer));
    }

    public Mono<ExportedGroupCallInvite> exportGroupCallInvite(ExportGroupCallInvite request) {
        return sendMain(request);
    }

    public Mono<Updates> toggleGroupCallStartSubscription(InputGroupCall peer, boolean subscribed) {
        return sendMain(ImmutableToggleGroupCallStartSubscription.of(peer, subscribed));
    }

    public Mono<Updates> startScheduledGroupCall(InputGroupCall peer) {
        return sendMain(ImmutableStartScheduledGroupCall.of(peer));
    }

    public Mono<Boolean> saveDefaultGroupCallJoinAs(InputPeer peer, InputPeer joinAs) {
        return sendMain(ImmutableSaveDefaultGroupCallJoinAs.of(peer, joinAs));
    }

    public Mono<Updates> joinGroupCallPresentation(InputGroupCall peer, String paramsJson) {
        return sendMain(ImmutableJoinGroupCallPresentation.of(peer, ImmutableDataJSON.of(paramsJson)));
    }

    public Mono<Updates> leaveGroupCallPresentation(InputGroupCall peer) {
        return sendMain(ImmutableLeaveGroupCallPresentation.of(peer));
    }

    public Mono<GroupCallStreamChannels> getGroupCallStreamChannels(InputGroupCall peer) {
        return sendMain(ImmutableGetGroupCallStreamChannels.of(peer));
    }

    public Mono<GroupCallStreamRtmpUrl> getGroupCallStreamRtmpUrl(InputPeer peer, boolean revoke) {
        return sendMain(ImmutableGetGroupCallStreamRtmpUrl.of(peer, revoke));
    }

    public Mono<Boolean> saveCallLog(InputPhoneCall peer, InputFile file) {
        return sendMain(ImmutableSaveCallLog.of(peer, file));
    }
}
