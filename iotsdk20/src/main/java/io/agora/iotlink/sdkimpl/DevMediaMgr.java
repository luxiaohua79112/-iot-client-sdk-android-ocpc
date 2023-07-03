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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IDevMediaMgr;
import io.agora.iotlink.IDeviceSessionMgr;
import io.agora.iotlink.IVodPlayer;
import io.agora.iotlink.logger.ALog;
import io.agora.iotlink.rtmsdk.DevFileDelErrInfo;
import io.agora.iotlink.rtmsdk.DevFileInfo;
import io.agora.iotlink.rtmsdk.IRtmCmd;
import io.agora.iotlink.rtmsdk.RtmBaseCmd;
import io.agora.iotlink.rtmsdk.RtmCmdSeqId;
import io.agora.iotlink.rtmsdk.RtmDeleteReqCmd;
import io.agora.iotlink.rtmsdk.RtmDeleteRspCmd;
import io.agora.iotlink.rtmsdk.RtmPlayReqCmd;
import io.agora.iotlink.rtmsdk.RtmQueryReqCmd;
import io.agora.iotlink.rtmsdk.RtmQueryRspCmd;


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
                RtmQueryRspCmd queryRspCmd = (RtmQueryRspCmd)rspCmd;
                ArrayList<DevMediaItem> mediaList = new ArrayList<>();
                if (queryRspCmd != null) {
                    int count = queryRspCmd.mFileList.size();
                    for (int i = 0; i < count; i++) {
                        DevFileInfo fileInfo = queryRspCmd.mFileList.get(i);

                        DevMediaItem mediaItem = new DevMediaItem();
                        mediaItem.mFileId = fileInfo.mFileId;
                        mediaItem.mStartTimestamp = fileInfo.mStartTime;
                        mediaItem.mStopTimestamp = fileInfo.mStopTime;
                        mediaItem.mType = fileInfo.mFileType;
                        mediaItem.mEvent = fileInfo.mEvent;
                        mediaItem.mImgUrl = fileInfo.mImgUrl;
                        mediaItem.mVideoUrl = fileInfo.mVideoUrl;
                        mediaList.add(mediaItem);
                    }
                }

                queryListener.onDevMediaQueryDone(errCode, mediaList);
            }
        };

        int ret = mSessionMgr.getRtmMgrComp().sendCommandToDev(queryReqCmd);

        ALog.getInstance().d(TAG, "<sendCmdPtzReset> done, ret=" + ret
                + ", queryReqCmd=" + queryReqCmd);
        return ret;
    }

    @Override
    public int deleteMediaList(final List<String> deletingList, final OnDeleteListener deleteListener) {
        RtmDeleteReqCmd deleteReqCmd = new RtmDeleteReqCmd();
        int deletingCount = deletingList.size();
        for (int i = 0; i < deletingCount; i++) {
            String fileId = deletingList.get(i);
            deleteReqCmd.mFileIdList.add(fileId);
        }

        deleteReqCmd.mSequenceId = RtmCmdSeqId.getSeuenceId();
        deleteReqCmd.mCmdId = IRtmCmd.CMDID_MEDIA_DELETE;
        deleteReqCmd.mDeviceId = mDeviceId;
        deleteReqCmd.mSendTimestamp = System.currentTimeMillis();

        deleteReqCmd.mRespListener = new IRtmCmd.OnRtmCmdRespListener() {
            @Override
            public void onRtmCmdResponsed(int commandId, int errCode, IRtmCmd reqCmd, IRtmCmd rspCmd) {
                ALog.getInstance().d(TAG, "<deleteMediaList.onRtmCmdResponsed> errCode=" + errCode);
                RtmDeleteRspCmd deleteRspCmd = (RtmDeleteRspCmd)rspCmd;
                ArrayList<DevMediaDelResult> delRsltList = new ArrayList<>();
                if (deleteRspCmd != null) {
                    int count = deleteRspCmd.mErrorList.size();
                    for (int i = 0; i < count; i++) {
                        DevFileDelErrInfo delErrInfo = deleteRspCmd.mErrorList.get(i);

                        DevMediaDelResult delResult = new DevMediaDelResult();
                        delResult.mFileId = delErrInfo.mFileId;
                        delResult.mErrCode = delErrInfo.mDelErrCode;
                        delRsltList.add(delResult);
                    }
                }

                deleteListener.onDevMediaDeleteDone(errCode, delRsltList);
            }
        };

        int ret = mSessionMgr.getRtmMgrComp().sendCommandToDev(deleteReqCmd);

        ALog.getInstance().d(TAG, "<sendCmdPtzReset> done, ret=" + ret
                + ", deleteReqCmd=" + deleteReqCmd);
        return ret;
    }


    @Override
    public int setDisplayView(final View displayView) {
        return ErrCode.XOK;
    }

    @Override
    public int play(long globalStartTime, final IPlayingCallback playingCallback) {
//        RtmPlayReqCmd playReqCmd = new RtmPlayReqCmd();
//        playReqCmd.mGlobalStartTime = globalStartTime;
//        playReqCmd.mSequenceId = RtmCmdSeqId.getSeuenceId();
//        playReqCmd.mCmdId = IRtmCmd.CMDID_MEDIA_PLAY_TIMELINE;
//        playReqCmd.mDeviceId = mDeviceId;
//        playReqCmd.mSendTimestamp = System.currentTimeMillis();
//
//        playReqCmd.mRespListener = new IRtmCmd.OnRtmCmdRespListener() {
//            @Override
//            public void onRtmCmdResponsed(int commandId, int errCode, IRtmCmd reqCmd, IRtmCmd rspCmd) {
//                ALog.getInstance().d(TAG, "<play.onRtmCmdResponsed> errCode=" + errCode);
//                if (errCode != ErrCode.XOK) {
//                    return;
//                }
//
//                playingCallback.on(errCode, delRsltList);
//            }
//        };
//
//        int ret = mSessionMgr.getRtmMgrComp().sendCommandToDev(deleteReqCmd);
//
//        ALog.getInstance().d(TAG, "<sendCmdPtzReset> done, ret=" + ret
//                + ", deleteReqCmd=" + deleteReqCmd);
//        return ret;
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
