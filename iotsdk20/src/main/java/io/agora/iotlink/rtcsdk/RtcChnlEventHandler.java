/**
 * @file TalkingEngine.java
 * @brief This file implement audio and video communication
 *        作为IoT设备端进入通话后，需要将本地的音视频往频道内推流，同时订阅对端的音频流，并进行播放
 *        作为移动应端进入通话后，需要将本地的音频往频道内推流，同时订阅对端的音视频流，并进行播放
 *        因为在服务器端针对每个账户分配了固定的 RtcUserId，因此频道的加入、退出等不用同步等待了
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2021-10-15
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink.rtcsdk;




import java.util.Locale;
import java.util.UUID;

import io.agora.iotlink.logger.ALog;
import io.agora.rtc2.ClientRoleOptions;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;




public class RtcChnlEventHandler extends IRtcEngineEventHandler {
    private final String TAG = "IOTSDK/RtcEHandler";

    private TalkingEngine  mTalkingEng;
    private UUID mSessionId;



    public RtcChnlEventHandler(final TalkingEngine talkingEngine, final UUID sessionId) {
        mTalkingEng = talkingEngine;
        mSessionId = sessionId;
    }

    @Override
    public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
//        String strLog = String.format(Locale.getDefault(),"<onJoinChannelSuccess> channel=%s, uid=%d, elapsed=%d",
//                channel, uid, elapsed);
//        ALog.getInstance().d(TAG, strLog);
        mTalkingEng.onJoinChannelSuccess(mSessionId, channel, uid, elapsed);
    }

    @Override
    public void onRejoinChannelSuccess(String channel, int uid, int elapsed) {
//        String strLog = String.format(Locale.getDefault(),"<onRejoinChannelSuccess> channel=%s, uid=%d, elapsed=%d",
//                channel, uid, elapsed);
//        ALog.getInstance().d(TAG, strLog);
        mTalkingEng.onRejoinChannelSuccess(mSessionId, channel, uid, elapsed);
    }

    @Override
    public void onLeaveChannel(RtcStats stats) {
//        ALog.getInstance().d(TAG, "<onLeaveChannel> stats=" + stats);
        mTalkingEng.onLeaveChannelSuccess(mSessionId);
    }

    @Override
    public void onUserJoined(int uid, int elapsed) {
//        String strLog = String.format(Locale.getDefault(), "<onUserJoined> uid=%d, elapsed=%d", uid, elapsed);
//        ALog.getInstance().d(TAG, strLog);
        mTalkingEng.onUserJoined(mSessionId, uid, elapsed);
    }

    @Override
    public void onUserOffline(int uid, int reason) {
//        String strLog = String.format(Locale.getDefault(),"<onUserOffline> uid=%d, reason=%d", uid, reason);
//        ALog.getInstance().d(TAG, strLog);
        mTalkingEng.onUserOffline(mSessionId, uid, reason);
    }

    @Override
    public void onFirstLocalVideoFrame(Constants.VideoSourceType source, int width, int height, int elapsed) {
//        String strLog = String.format(Locale.getDefault(),"<onFirstLocalVideoFrame> width=%d, height=%d, elapsed=%d",
//                width, height, elapsed);
//        ALog.getInstance().d(TAG, strLog);
        mTalkingEng.onFirstLocalVideoFrame(mSessionId, width, height, elapsed);
    }

    @Override
    public void onFirstRemoteVideoFrame(int uid, int width, int height, int elapsed) {
//        String strLog = String.format(Locale.getDefault(), "<onFirstRemoteVideoFrame> uid=%d, width=%d, height=%d, elapsed=%d",
//                uid, width, height, elapsed);
//        ALog.getInstance().d(TAG, strLog);
        mTalkingEng.onFirstRemoteVideoFrame(mSessionId, uid, width, height, elapsed);
    }


    @Override
    public void onLastmileQuality(int quality) {
//        ALog.getInstance().d(TAG, "<onLastmileQuality> quality=" + quality);
        mTalkingEng.onLastmileQuality(mSessionId, quality);
    }

    @Override
    public void onNetworkQuality(int uid, int txQuality, int rxQuality) {
//           String strLog = String.format(Locale.getDefault(),"<onNetworkQuality> channel=%s, uid=%d, muted=%d",
//                    uid, txQuality, rxQuality);
//            ALog.getInstance().d(TAG, strLog);
        mTalkingEng.onNetworkQuality(mSessionId, uid, txQuality, rxQuality);
    }

    @Override
    public void onRtcStats(IRtcEngineEventHandler.RtcStats stats) {
//           ALog.getInstance().d(TAG, "<onRtcStats> stats=" + stats);
        mTalkingEng.onRtcStats(mSessionId, stats);
    }


    @Override
    public void onLocalVideoStats(Constants.VideoSourceType source,
                                  IRtcEngineEventHandler.LocalVideoStats stats) {
//            String strLog = String.format(Locale.getDefault(),"<onLocalVideoStats> stats=%s, encodedFrameWidth=%d, encodedFrameHeight=%d",
//                    stats.toString(), stats.encodedFrameWidth, stats.encodedFrameHeight);
//            ALog.getInstance().d(TAG, strLog);
        mTalkingEng.onLocalVideoStats(mSessionId, source, stats);
    }


    @Override
    public void onRemoteVideoStats(IRtcEngineEventHandler.RemoteVideoStats stats) {
//            String strLog = String.format(Locale.getDefault(),"<onRemoteVideoStats> stats=%s, width=%d, height=%d",
//                    stats.toString(), stats.width, stats.height);
//            ALog.getInstance().d(TAG, strLog);
        mTalkingEng.onRemoteVideoStats(mSessionId, stats);
    }

    @Override
    public void onClientRoleChanged(int oldRole, int newRole, ClientRoleOptions newRoleOptions) {
        ALog.getInstance().d(TAG, "<onClientRoleChanged> oldRole=" + oldRole
            + ", newRole=" + newRole + ", newRoleOptions=" + newRoleOptions);
    }

    @Override
    public void onClientRoleChangeFailed(int reason, int currentRole) {
        ALog.getInstance().d(TAG, "<onClientRoleChangeFailed> reason=" + reason
                + ", currentRole=" + currentRole);
    }

};
