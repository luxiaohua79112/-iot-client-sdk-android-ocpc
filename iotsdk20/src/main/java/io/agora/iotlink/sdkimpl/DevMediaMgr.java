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
import io.agora.iotlink.base.AtomicInteger;
import io.agora.iotlink.base.BaseThreadComp;
import io.agora.iotlink.callkit.SessionCtx;
import io.agora.iotlink.logger.ALog;
import io.agora.iotlink.rtmsdk.DevFileDelErrInfo;
import io.agora.iotlink.rtmsdk.DevFileInfo;
import io.agora.iotlink.rtmsdk.IRtmCmd;
import io.agora.iotlink.rtmsdk.RtmBaseCmd;
import io.agora.iotlink.rtmsdk.RtmCmdSeqId;
import io.agora.iotlink.rtmsdk.RtmDeleteReqCmd;
import io.agora.iotlink.rtmsdk.RtmDeleteRspCmd;
import io.agora.iotlink.rtmsdk.RtmPlayReqCmd;
import io.agora.iotlink.rtmsdk.RtmPlayRspCmd;
import io.agora.iotlink.rtmsdk.RtmQueryReqCmd;
import io.agora.iotlink.rtmsdk.RtmQueryRspCmd;


/*
 * @brief 设备上媒体文件管理器
 */
public class DevMediaMgr implements IDevMediaMgr {


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/DevMediaMgr";
    private static final int SESSION_TYPE_PLAYBACK = 0x0004;          ///< 会话类型：媒体回放

    //
    // The mesage Id
    //



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private final Object mDataLock = new Object();    ///< 同步访问锁

    private UUID mSessionId;
    private DeviceSessionMgr mSessionMgr;
    private String mUserId;
    private String mDeviceId;

    private View mDisplayView;
    private SessionCtx mPlaybackSession;        ///< 媒体回放会话上下文，不在整个的 DeviceSessionMgr中
    private IPlayingCallback mPlayingCallbk;
    private AtomicInteger mPlayingState = new AtomicInteger();

    ///////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    public DevMediaMgr(final UUID sessionId, final DeviceSessionMgr sessionMgr) {
        mSessionId = sessionId;
        mSessionMgr = sessionMgr;
        IDeviceSessionMgr.SessionInfo sessionInfo = mSessionMgr.getSessionInfo(sessionId);
        mUserId = sessionInfo.mUserId;
        mDeviceId = sessionInfo.mPeerDevId;
        mPlayingState.setValue(DEVPLAYER_STATE_STOPPED);  // 停止播放状态
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

        ALog.getInstance().d(TAG, "<queryMediaList> done, ret=" + ret
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

        ALog.getInstance().d(TAG, "<deleteMediaList> done, ret=" + ret
                + ", deleteReqCmd=" + deleteReqCmd);
        return ret;
    }


    @Override
    public int setDisplayView(final View displayView) {
        mDisplayView = displayView;
        return ErrCode.XOK;
    }

    @Override
    public int play(long globalStartTime, final IPlayingCallback playingCallback) {
        int playingState = mPlayingState.getValue();
        if (playingState != DEVPLAYER_STATE_STOPPED) {
            ALog.getInstance().d(TAG, "<play> bad playing state, state=" + playingState);
            return ErrCode.XERR_BAD_STATE;
        }

        RtmPlayReqCmd playReqCmd = new RtmPlayReqCmd();
        playReqCmd.mGlobalStartTime = globalStartTime;
        playReqCmd.mSequenceId = RtmCmdSeqId.getSeuenceId();
        playReqCmd.mCmdId = IRtmCmd.CMDID_MEDIA_PLAY_TIMELINE;
        playReqCmd.mDeviceId = mDeviceId;
        playReqCmd.mSendTimestamp = System.currentTimeMillis();

        playReqCmd.mRespListener = new IRtmCmd.OnRtmCmdRespListener() {
            @Override
            public void onRtmCmdResponsed(int commandId, int errCode, IRtmCmd reqCmd, IRtmCmd rspCmd) {
                ALog.getInstance().d(TAG, "<play.onRtmCmdResponsed> errCode=" + errCode);
                RtmPlayRspCmd playRspCmd = (RtmPlayRspCmd)rspCmd;
                if (errCode != ErrCode.XOK) {
                    mPlayingState.setValue(IDevMediaMgr.DEVPLAYER_STATE_STOPPED);   // 状态机: 停止播放
                    if (mPlayingCallbk != null) {
                        mPlayingCallbk.onDevMediaOpenDone(null, errCode);
                    }
                    return;
                }

                // 进入RTC频道拉流
                RtcChnlEnter(playRspCmd.mRtcUid, playRspCmd.mChnlName, playRspCmd.mRtcToken);

                if (mPlayingCallbk != null) {
                    mPlayingCallbk.onDevMediaOpenDone(null, ErrCode.XOK);
                }
            }
        };

        mPlayingState.setValue(IDevMediaMgr.DEVPLAYER_STATE_PLAYING); // 状态机: 正在播放
        int ret = mSessionMgr.getRtmMgrComp().sendCommandToDev(playReqCmd);

        ALog.getInstance().d(TAG, "<play> done, ret=" + ret
                + ", playReqCmd=" + playReqCmd);
        return ret;
    }

    @Override
    public int play(long fileId, long startPos, int playSpeed,
                    final IPlayingCallback playingCallback) {
        return ErrCode.XOK;
    }

    @Override
    public int stop() {
        int playingState = mPlayingState.getValue();
        if (playingState == DEVPLAYER_STATE_STOPPED) {
            ALog.getInstance().d(TAG, "<stop> bad state, already stopped!");
            return ErrCode.XERR_BAD_STATE;
        }

        RtmBaseCmd stopReqCmd = new RtmBaseCmd();
        stopReqCmd.mSequenceId = RtmCmdSeqId.getSeuenceId();
        stopReqCmd.mCmdId = IRtmCmd.CMDID_MEDIA_STOP;
        stopReqCmd.mDeviceId = mDeviceId;
        stopReqCmd.mSendTimestamp = System.currentTimeMillis();
        stopReqCmd.mRespListener = null;    // 不需要管设备端是否收到
        int ret = mSessionMgr.getRtmMgrComp().sendCommandToDev(stopReqCmd);

        // 退出频道
        RtcChnlExit();

        mPlayingState.setValue(IDevMediaMgr.DEVPLAYER_STATE_STOPPED); // 状态机: 停止播放

        ALog.getInstance().d(TAG, "<stop> done, ret=" + ret
                + ", stopReqCmd=" + stopReqCmd);
        return ret;
    }

    @Override
    public int resume() {
        return ErrCode.XERR_UNSUPPORTED;
    }

    @Override
    public int pause() {
        return ErrCode.XERR_UNSUPPORTED;
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
        int playingState = mPlayingState.getValue();
        return playingState;
    }

    ////////////////////////////////////////////////////////////////////////////
    /////////////////////////// Methods of RtcEngine ///////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    /**
     * @brief 进行频道进行音视频拉流
     */
    int RtcChnlEnter(int rtcUid, final String chnlName, final String rtcToken) {

        // 进入频道进行音视频拉流
        SessionCtx playbackSession = new SessionCtx();
        playbackSession.mSessionId = UUID.randomUUID();
        playbackSession.mUserId = mUserId;
        playbackSession.mDeviceId = mDeviceId;
        playbackSession.mLocalRtcUid = rtcUid;
        playbackSession.mChnlName = chnlName;
        playbackSession.mRtcToken = rtcToken;
        playbackSession.mType = SESSION_TYPE_PLAYBACK;  // 会话类型
        playbackSession.mUserCount = 1;      // 至少有一个用户
        playbackSession.mSeesionCallback = null;
        playbackSession.mState = IDeviceSessionMgr.SESSION_STATE_CONNECTED;   // 直接连接到设备
        playbackSession.mConnectTimestamp = System.currentTimeMillis();

        // 开始进入频道
        mSessionMgr.talkingPrepare(playbackSession, true, true, false);

        // 设置显示控件
        if (mDisplayView != null) {
            mSessionMgr.setDisplayView(playbackSession, mDisplayView);
        }

        synchronized (mDataLock) {
            mPlaybackSession = playbackSession;
        }

        ALog.getInstance().d(TAG, "<RtcChnlEnter> done, rtcUid=" + rtcUid
                + ", chnlName=" + chnlName + ", rtcToken=" + rtcToken);
        return ErrCode.XOK;
    }

    /**
     * @brief 退出频道
     */
    int RtcChnlExit() {
        SessionCtx playingSession;
        synchronized (mDataLock) {
            playingSession = mPlaybackSession;
        }
        if (playingSession != null) {
            mSessionMgr.talkingStop(playingSession);
        }
        synchronized (mDataLock) {
            mPlaybackSession = null;
        }

        ALog.getInstance().d(TAG, "<RtcChnlExit> done");
        return ErrCode.XOK;
    }
}
