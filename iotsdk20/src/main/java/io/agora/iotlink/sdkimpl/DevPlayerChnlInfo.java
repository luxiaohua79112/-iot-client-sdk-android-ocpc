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
 * @brief 设备播放器的频道参数
 */
public class DevPlayerChnlInfo  {

    private String mFileId;
    private IDevMediaMgr.IPlayingCallback mPlayingCallback;

    private String mDeviceId;
    private int mRtcUid;
    private String mChnlName;
    private String mRtcToken;
    private View mDisplayView;

    @Override
    public String toString() {
        String infoText = "{ mDeviceId=" + mDeviceId
                + ", mRtcUid=" + mRtcUid
                + ", mChannelName=" + mChnlName
                + ", mDisplayView=" + mDisplayView
                + ",\n mRtcToken=" + mRtcToken + " }";
        return infoText;
    }

    public synchronized void setPlayingInfo(final String fileId,
                                         final IDevMediaMgr.IPlayingCallback playingCallback) {
        mFileId = fileId;
        mPlayingCallback = playingCallback;
    }

    public synchronized String getPlayingFileId() {
        return mFileId;
    }

    public synchronized IDevMediaMgr.IPlayingCallback getPlayingCallback() {
        return mPlayingCallback;
    }



    public synchronized void setInfo(final String deviceId, int uid, final String chnlName,
                                     final String rtcToken, final View displayView ) {
        mDeviceId = deviceId;
        mRtcUid = uid;
        mChnlName = chnlName;
        mRtcToken = rtcToken;
        mDisplayView = displayView;
    }

    public synchronized String getDeviceId() {
        return mDeviceId;
    }

    public synchronized int getRtcUid() {
        return mRtcUid;
    }

    public synchronized String getChannelName() {
        return mChnlName;
    }

    public synchronized String getRtcToken() {
        return mRtcToken;
    }

    public synchronized View getDisplayView() {
        return mDisplayView;
    }

    public synchronized void clear() {
        mDeviceId = null;
        mRtcUid = 0;
        mChnlName = null;
        mRtcToken = null;
        mDisplayView = null;
    }

}
