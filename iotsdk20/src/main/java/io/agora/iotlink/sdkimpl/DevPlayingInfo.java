/**
 * @file IAgoraIotAppSdk.java
 * @brief This file define the SDK interface for Agora Iot AppSdk 2.0
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 2.0.0.1
 * @date 2023-04-12
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink.sdkimpl;


import android.os.Message;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IDeviceSessionMgr;
import io.agora.iotlink.IDevController;
import io.agora.iotlink.IDevMediaMgr;
import io.agora.iotlink.IDevPreviewMgr;
import io.agora.iotlink.base.BaseThreadComp;
import io.agora.iotlink.callkit.DisplayViewMgr;
import io.agora.iotlink.callkit.SessionCtx;
import io.agora.iotlink.callkit.SessionMgr;
import io.agora.iotlink.logger.ALog;
import io.agora.iotlink.rtcsdk.TalkingEngine;
import io.agora.iotlink.rtmsdk.RtmMgrComp;
import io.agora.rtc2.Constants;
import io.agora.rtm.RtmClient;
import io.agora.rtm.RtmClientListener;
import io.agora.rtm.RtmMessage;
import io.agora.rtm.jni.PEER_ONLINE_STATE;


/**
 * @brief 设备SD卡播放状态信息
 */
public class DevPlayingInfo  {
    private UUID mPlaingId;          ///< 本次播放唯一标识

    private DevPlayerChnlInfo mPlayChnlInfo = new DevPlayerChnlInfo();  ///< 设备播放器频道信息

    private UUID mPlaySessionId;         ///< 设备播放器的会话Id
    private int mPlayingState = IDevMediaMgr.DEVPLAYER_STATE_STOPPED;          ///< 播放状态机
    private long mPlayStartTime = 0;               ///< 播放开始时间
    private MediaPlayingClock mPlayingClock = new MediaPlayingClock();  ///< 播放器时钟


    ///////////////////////////////////////////////////////////////////////////
    /////////////////////////////// Public Methods ////////////////////////////
    ///////////////////////////////////////////////////////////////////////////

    /**
     * @brief 重置播放器到关闭状态
     */
    public synchronized void reset() {
        mPlaingId = null;
        mPlayingState = IDevMediaMgr.DEVPLAYER_STATE_STOPPED;
        mPlayStartTime = 0;
        mPlayingClock.stopWithProgress(0);
        mPlaySessionId = null;
        mPlayChnlInfo.clear();
    }

    /**
     * @brief 设置当前播放Id
     */
    public synchronized void setPlayingId(final UUID playingId) {
        mPlaingId = playingId;
    }

    /**
     * @brief 获取当前播放Id
     */
    public synchronized UUID getPlayingId() {
        return mPlaingId;
    }

    /**
     * @brief 判断是否当前的播放Id
     */
    public synchronized boolean isCurrPlayingId(final UUID compPlayingId) {
        if ((mPlaingId == null) || (compPlayingId == null)) {
            return false;
        }
        boolean bSame = mPlaingId.equals(compPlayingId);
        return bSame;
    }


    /**
     * @brief 设置播放器状态
     */
    public synchronized void setPlayingState(int newState) {
        mPlayingState = newState;
    }

    /**
     * @brief 获取播放器状态
     */
    public synchronized int getPlayingState() {
       return mPlayingState;
    }

    /**
     * @brief 设置播放文件信息
     */
    public synchronized void setPlayFileInfo(final String fileId,
                                             final IDevMediaMgr.IPlayingCallback playingCallback) {
        mPlayChnlInfo.setPlayingInfo(fileId, playingCallback);
    }

    /**
     * @brief 获取播放器回调接口
     */
    public synchronized IDevMediaMgr.IPlayingCallback getPlayingCallback() {
        return mPlayChnlInfo.getPlayingCallback();
    }

    /**
     * @brief 获取当前播放FileId
     */
    public synchronized String getPlayingFileId() {
        return mPlayChnlInfo.getPlayingFileId();
    }

    /**
     * @brief 设置播放启动总时间
     */
    public synchronized void setStartTimestamp(long globalStartTime, int playSpeed) {
       //  播放器时钟不走，固定在启动时刻点
        mPlayStartTime = globalStartTime;
        mPlayingClock.setRunSpeed(playSpeed);
        mPlayingClock.stopWithProgress(globalStartTime);
    }

    /**
     * @brief 获取播放启动总时间
     */
    public synchronized long getStartTimestamp() {
        return mPlayStartTime;
    }

    /**
     * @brief 从指定进度启动播放时钟
     */
    public synchronized void clockStartWithCurr() {
        mPlayingClock.startWithProgress(mPlayStartTime);
    }

    /**
     * @brief 启动播放时钟
     */
    public synchronized void clockStart() {
        mPlayingClock.start();
    }

    /**
     * @brief 停止播放时钟
     */
    public synchronized void clockStop() {
        mPlayingClock.stop();
    }

    /**
     * @brief 设置时钟速度
     */
    public synchronized void clockSetSpeed(int speed) {
        mPlayingClock.setRunSpeed(speed);
    }

    /**
     * @brief 获取时钟进度
     */
    public synchronized long getClockProgress() {
        return mPlayingClock.getProgress();
    }

    /**
     * @brief 设置当前播放通道的sessionId
     */
    public synchronized void setPlaySessionId(final UUID playSessionId) {
        mPlaySessionId = playSessionId;
    }

    /**
     * @brief 获取当前播放通道的sessionId
     */
    public synchronized UUID getPlaySessionId() {
        return mPlaySessionId;
    }

    /**
     * @brief 设置播放频道信息
     */
    public synchronized void setPlayChnlInfo(final String deviceId, int uid, final String chnlName,
                                             final String rtcToken, int devUid, final View displayView,
                                             final DevMediaMgr mediaMgr) {
        mPlayChnlInfo.clear();
        mPlayChnlInfo.setInfo(deviceId, uid, chnlName, rtcToken, devUid, displayView, mediaMgr);
    }

    /**
     * @brief 获取播放频道信息
     */
    public synchronized DevPlayerChnlInfo getPlayChnlInfo() {
        return mPlayChnlInfo;
    }

}
