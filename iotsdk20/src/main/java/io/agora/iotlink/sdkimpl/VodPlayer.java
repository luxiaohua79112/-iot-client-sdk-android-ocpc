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

import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IVodPlayer;


/*
 * @brief 云录视频播放器
 */
public class VodPlayer implements IVodPlayer {


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/VodPlayer";


    //
    // The mesage Id
    //


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////



    ///////////////////////////////////////////////////////////////////////
    /////////////////// Methods of Override IVodPlayer  ///////////////////
    ///////////////////////////////////////////////////////////////////////

    @Override
    public int setDisplayView(final View displayView) {
        return ErrCode.XOK;
    }

    @Override
    public int open(final String mediaUrl, final ICallback callback) {
        return ErrCode.XOK;
    }

    @Override
    public void close() {

    }

    @Override
    public VodMediaInfo getMediaInfo() {
        return null;
    }

    @Override
    public long getPlayingProgress() {
        return ErrCode.XOK;
    }

    @Override
    public int getPlayingState() {
        return ErrCode.XOK;
    }

    @Override
    public int play() {
        return ErrCode.XOK;
    }

    @Override
    public int pause() {
        return ErrCode.XOK;
    }

    @Override
    public int stop() {
        return ErrCode.XOK;
    }

    @Override
    public long seek(long seekPos) {
        return seekPos;
    }

}
