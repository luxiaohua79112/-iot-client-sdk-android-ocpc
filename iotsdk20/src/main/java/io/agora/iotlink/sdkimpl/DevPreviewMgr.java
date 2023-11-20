/**
 * @file AccountMgr.java
 * @brief This file implement the call kit and RTC management
 *
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2023-05-19
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink.sdkimpl;



import android.view.View;

import java.util.List;
import java.util.UUID;

import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IDevMediaMgr;
import io.agora.iotlink.IDevPreviewMgr;
import io.agora.iotlink.IVodPlayer;
import io.agora.iotlink.base.AtomicBoolean;


/*
 * @brief 设备预览管理器
 */
public class DevPreviewMgr  implements IDevPreviewMgr {


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/DevPreviewMgr";


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private UUID mSessionId;
    private DeviceSessionMgr mSessionMgr;

    private View mDisplayView;
    private AtomicBoolean mIsPreviewing = new AtomicBoolean();        ///< 是否正在预览


    ///////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    public DevPreviewMgr(final UUID sessionId, final DeviceSessionMgr sessionMgr) {
        mSessionId = sessionId;
        mSessionMgr = sessionMgr;
        mIsPreviewing.setValue(false);
    }

    ///////////////////////////////////////////////////////////////////////
    ///////////////// Methods of Override IDevPreviewMgr  ///////////////////
    ///////////////////////////////////////////////////////////////////////
    @Override
    public int setDisplayView(final View displayView) {
        mDisplayView = displayView;
        int ret = mSessionMgr.setDisplayView(mSessionId, displayView);
        return ret;
    }

    @Override
    public int previewStart(boolean bSubAudio, final OnPreviewListener previewListener) {
        int ret = mSessionMgr.previewStart(mSessionId, bSubAudio, previewListener);
        if (ret == ErrCode.XOK) {
            mIsPreviewing.setValue(true);
        }
        return ret;
    }

    @Override
    public int previewStop() {
        int ret = mSessionMgr.previewStop(mSessionId);
        if (ret == ErrCode.XOK) {
            mIsPreviewing.setValue(false);
        }
        return ret;
    }

    @Override
    public boolean isPreviewing() {
        return mIsPreviewing.getValue();
    }

    @Override
    public int recordingStart(final String outFilePath) {
        int ret = mSessionMgr.recordingStart(mSessionId, outFilePath);
        return ret;
    }

    @Override
    public int recordingStop() {
        int ret = mSessionMgr.recordingStop(mSessionId);
        return ret;
    }

    @Override
    public boolean isRecording() {
        boolean recording = mSessionMgr.isRecording(mSessionId);
        return recording;
    }

    @Override
    public int captureVideoFrame(final String saveFilePath, final OnCaptureFrameListener captureListener) {
        int ret = mSessionMgr.captureVideoFrame(mSessionId, saveFilePath, captureListener);
        return ret;
    }

    @Override
    public int muteLocalAudio(boolean mute) {
        int ret = mSessionMgr.muteLocalAudio(mSessionId, mute);
        return ret;
    }

    @Override
    public int setLocalAudioVolume(int volume) {
        int ret = mSessionMgr.setLocalAudioVolume(mSessionId, volume);
        return ret;
    }

    @Override
    public int muteDeviceVideo(boolean mute) {
        int ret = mSessionMgr.muteDeviceVideo(mSessionId, mute);
        return ret;
    }

    @Override
    public int muteDeviceAudio(boolean mute) {
        int ret = mSessionMgr.muteDeviceAudio(mSessionId, mute);
        return ret;
    }

    @Override
    public RtcNetworkStatus getNetworkStatus() {
        RtcNetworkStatus networkStatus = mSessionMgr.getNetworkStatus(mSessionId);
        return networkStatus;
    }

    @Override
    public int setPlaybackVolume(int volumeLevel) {
        int ret = mSessionMgr.setPlaybackVolume(mSessionId, volumeLevel);
        return ret;
    }

    @Override
    public int setAudioEffect(final AudioEffectId effectId) {
        int ret = mSessionMgr.setAudioEffect(mSessionId, effectId);
        return ret;
    }

    @Override
    public AudioEffectId getAudioEffect() {
        AudioEffectId effectId = mSessionMgr.getAudioEffect(mSessionId);
        return effectId;
    }

    @Override
    public int setRtcPrivateParam(String privateParam) {
        int ret = mSessionMgr.setRtcPrivateParam(mSessionId, privateParam);
        return ret;
    }

}
