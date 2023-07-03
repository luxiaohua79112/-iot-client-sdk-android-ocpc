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
import io.agora.iotlink.IDeviceSessionMgr;
import io.agora.iotlink.IVodPlayer;
import io.agora.iotlink.logger.ALog;
import io.agora.iotlink.rtmsdk.IRtmCmd;
import io.agora.iotlink.rtmsdk.RtmBaseCmd;
import io.agora.iotlink.rtmsdk.RtmCmdSeqId;
import io.agora.iotlink.rtmsdk.RtmQueryReqCmd;


/*
 * @brief 设备上媒体文件管理器
 */
public class DevMediaMgr  implements IDevMediaMgr {


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/DevMediaMgr";


    //
    // The mesage Id
    //



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////

    private UUID mSessionId;
    private DeviceSessionMgr mSessionMgr;
    private String mDeviceId;

    ///////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    public DevMediaMgr(final UUID sessionId, final DeviceSessionMgr sessionMgr) {
        mSessionId = sessionId;
        mSessionMgr = sessionMgr;
        IDeviceSessionMgr.SessionInfo sessionInfo = mSessionMgr.getSessionInfo(sessionId);
        mDeviceId = sessionInfo.mPeerDevId;
    }


    ///////////////////////////////////////////////////////////////////////
    ///////////////// Methods of Override IDevMediaMgr  ///////////////////
    ///////////////////////////////////////////////////////////////////////

    @Override
    public int queryMediaList(final QueryParam queryParam, final OnQueryListener queryListener) {

        RtmQueryReqCmd queryReqCmd = new RtmQueryReqCmd();
        queryReqCmd.mQueryParam.mFileId = queryParam.mFileId;
        queryReqCmd.mQueryParam.mBeginTime = queryParam.mBeginTimestamp;
        queryReqCmd.mQueryParam.mEndTime = queryParam.mEndTimestamp;
        queryReqCmd.mQueryParam.mPageIndex = queryParam.mPageIndex;
        queryReqCmd.mQueryParam.mPageSize = queryParam.mPageSize;

        queryReqCmd.mSequenceId = RtmCmdSeqId.getSeuenceId();
        queryReqCmd.mCmdId = IRtmCmd.CMDID_MEDIA_QUERY;
        queryReqCmd.mDeviceId = mDeviceId;
        queryReqCmd.mSendTimestamp = System.currentTimeMillis();

        queryReqCmd.mRespListener = new IRtmCmd.OnRtmCmdRespListener() {
            @Override
            public void onRtmCmdResponsed(int commandId, int errCode, IRtmCmd reqCmd, IRtmCmd rspCmd) {
                ALog.getInstance().d(TAG, "<queryMediaList.onRtmCmdResponsed> errCode=" + errCode);

                queryListener.onDevMediaQueryDone(errCode, null);
            }
        };

        int ret = mSessionMgr.getRtmMgrComp().sendCommandToDev(queryReqCmd);

        ALog.getInstance().d(TAG, "<sendCmdPtzReset> done, ret=" + ret
                + ", queryReqCmd=" + queryReqCmd);
        return ret;
    }

    @Override
    public int deleteMediaList(final List<Long> deletingList, final OnDeleteListener deleteListener) {
        return ErrCode.XOK;
    }


    @Override
    public int setDisplayView(final View displayView) {
        return ErrCode.XOK;
    }

    @Override
    public int play(long globalStartTime, final IPlayingCallback playingCallback) {
        return ErrCode.XOK;
    }

    @Override
    public int play(long fileId, long startPos, int playSpeed,
                    final IPlayingCallback playingCallback) {
        return ErrCode.XOK;
    }

    @Override
    public int stop() {
        return ErrCode.XOK;
    }

    @Override
    public int resume() {
        return ErrCode.XOK;
    }

    @Override
    public int pause() {
        return ErrCode.XOK;
    }

    @Override
    public int seek(long seekPos) {
        return ErrCode.XOK;
    }

    @Override
    public int setPlayingSpeed(int speed) {
        return ErrCode.XOK;
    }


    @Override
    public long getPlayingProgress()  {
        return ErrCode.XOK;
    }

    @Override
    public int getPlayingState() {
        return ErrCode.XOK;
    }

}
