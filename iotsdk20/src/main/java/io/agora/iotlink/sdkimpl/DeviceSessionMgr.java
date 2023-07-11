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
 * @brief 设备会话管理器
 */
public class DeviceSessionMgr extends BaseThreadComp
        implements IDeviceSessionMgr, TalkingEngine.ICallback {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/DeviceSessionMgr";
    private static final int DEFAULT_DEV_UID = 10;               ///< 设备端uid，固定为10
    private static final long TIMER_INTERVAL = 2000;             ///< 定时器间隔 2秒
    private static final long CONNECT_TIMEOUT = 30000;           ///< 设备连接超时30秒



    //
    // The message Id
    //
    private static final int MSGID_SDK_BASE = 0x1000;
    private static final int MSGID_SDK_CONNECT_DONE = 0x1002;    ///< 连接设备完成消息
    private static final int MSGID_SDK_DEV_OFFLINE= 0x1003;      ///< 设备掉线消息
    private static final int MSGID_SDK_DEV_FIRSTFRAME = 0x1004;  ///< 设备首帧出图消息
    private static final int MSGID_SDK_TIMER = 0x1005;           ///< 定时器

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private InitParam mInitParam;



    public static final Object mDataLock = new Object();    ///< 同步访问锁,类中所有变量需要进行加锁处理

    private DisplayViewMgr mViewMgr = new DisplayViewMgr();
    private SessionMgr mSessionMgr = new SessionMgr();          ///< 设备会话管理器
    private SessionMgr mDevPlayerMgr = new SessionMgr();        ///< 设备播放器管理器，用来管理SD卡播放的

    private TalkingEngine mTalkEngine = new TalkingEngine();    ///< 通话引擎
    private static final Object mTalkEngLock = new Object();    ///< 通话引擎同步访问锁

    private RtmMgrComp mRtmComp;                                ///< RTM组件


    ///////////////////////////////////////////////////////////////////////
    //////////////// Override Methods of IDeviceSessionMgr //////////////////
    ///////////////////////////////////////////////////////////////////////

    @Override
    public int initialize(InitParam initParam) {
        mInitParam = initParam;

        // 初始化日志系统
        if ((initParam.mLogFilePath != null) && (!initParam.mLogFilePath.isEmpty())) {
            boolean logRet = ALog.getInstance().initialize(initParam.mLogFilePath);
            if (!logRet) {
                Log.e(TAG, "<initialize > [ERROR] fail to initialize logger");
            }
        }

        mSessionMgr.clear();
        mViewMgr.clear();

        // 创建 RTM组件
        mRtmComp = new RtmMgrComp();
        int ret = mRtmComp.initialize(this);
        if (ret != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<initialize> fail to init RTM comp, ret=" + ret);
            return ErrCode.XERR_UNSUPPORTED;
        }


        // 启动组件线程
        runStart(TAG);

        sendSingleMessage(MSGID_SDK_TIMER, 0, 0, null,0);
        ALog.getInstance().d(TAG, "<initialize> done");
        return ErrCode.XOK;
    }

    @Override
    public void release() {
        // 停止组件线程
        runStop();

        mSessionMgr.clear();
        mViewMgr.clear();

        // 销毁RTM组件
        if (mRtmComp != null) {
            mRtmComp.release();
            mRtmComp = null;
        }


        ALog.getInstance().d(TAG, "<release> done");
        ALog.getInstance().release();
    }

    @Override
    public List<SessionInfo> getSessionList() {
        List<SessionInfo> sessionInfoList = new ArrayList<>();

        List<SessionCtx> sessionCtxList = mSessionMgr.getAllSessionList();
        int sessionCount = sessionCtxList.size();
        int i;
        for (i = 0; i < sessionCount; i++) {
            SessionCtx  sessionCtx = sessionCtxList.get(i);

            SessionInfo sessionInfo = new SessionInfo();
            sessionInfo.mSessionId = sessionCtx.mSessionId;
            sessionInfo.mUserId = sessionCtx.mUserId;
            sessionInfo.mPeerDevId = sessionCtx.mDeviceId;
            sessionInfo.mLocalRtcUid = sessionCtx.mLocalRtcUid;
            sessionInfo.mChannelName = sessionCtx.mChnlName;
            sessionInfo.mRtcToken = sessionCtx.mRtcToken;
            sessionInfo.mRtmToken = sessionCtx.mRtmToken;
            sessionInfo.mAttachMsg = sessionCtx.mAttachMsg;
            sessionInfo.mType = sessionCtx.mType;
            sessionInfo.mUserCount = sessionCtx.mUserCount;
            sessionInfoList.add(sessionInfo);
        }

        return sessionInfoList;
    }

    @Override
    public ConnectResult connect(final ConnectParam connectParam,
                                        final ISessionCallback sessionCallback) {
        long t1 = System.currentTimeMillis();
        ConnectResult result = new ConnectResult();

        SessionCtx sessionCtx = mSessionMgr.findSessionByDeviceId(connectParam.mPeerDevId);
        if (sessionCtx != null) {
            ALog.getInstance().e(TAG, "<connect> bad state, device already in session"
                    + ", deviceId=" + connectParam.mPeerDevId);
            result.mErrCode = ErrCode.XERR_SDK_NOT_READY;
            return result;
        }

        // 发送请求消息
        ALog.getInstance().d(TAG, "<connect> ==> BEGIN, connectParam=" + connectParam);
        SessionCtx newSession = new SessionCtx();
        newSession.mSessionId = UUID.randomUUID();
        newSession.mUserId = connectParam.mUserId;
        newSession.mDeviceId = connectParam.mPeerDevId;
        newSession.mLocalRtcUid = connectParam.mLocalRtcUid;
        newSession.mDeviceRtcUid = DEFAULT_DEV_UID;
        newSession.mChnlName = connectParam.mChannelName;
        newSession.mRtcToken = connectParam.mRtcToken;
        newSession.mRtmToken = connectParam.mRtmToken;
        newSession.mType = SESSION_TYPE_DIAL;  // 会话类型
        newSession.mUserCount = 1;      // 至少有一个用户
        newSession.mSeesionCallback = sessionCallback;
        newSession.mState = SESSION_STATE_CONNECTING;   // 正在连接中状态机
        newSession.mConnectTimestamp = System.currentTimeMillis();

        // 添加到会话管理器中
        mSessionMgr.addSession(newSession);

        // 加入频道处理
        talkingPrepare(newSession, false, false, false);

        // RTM组件中连接设备处理
        mRtmComp.connectToDevice(connectParam.mPeerDevId, connectParam.mRtmToken);

        long t2 = System.currentTimeMillis();
        ALog.getInstance().d(TAG, "<connect> <==End done" + ", costTime=" + (t2-t1));
        result.mSessionId = newSession.mSessionId;
        result.mErrCode = ErrCode.XOK;
        return result;
    }

    @Override
    public int disconnect(final UUID sessionId) {
        long t1 = System.currentTimeMillis();
        ALog.getInstance().d(TAG, "<disconnect> ==> BEGIN, sessionId=" + sessionId);

        // 会话管理器中直接删除该会话
        SessionCtx sessionCtx = mSessionMgr.removeSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().e(TAG, "<disconnect> <==END, already disconnected, not found");
            return ErrCode.XOK;
        }

        // 停止预览或者录像
        if (sessionCtx.mDevPreviewMgr != null) {
            sessionCtx.mDevPreviewMgr.previewStop();
            sessionCtx.mDevPreviewMgr.recordingStop();
        }

        // 停止设备端播放
        if (sessionCtx.mDevMediaMgr != null) {
            sessionCtx.mDevMediaMgr.stop();
        }


        // 离开通话频道
        talkingStop(sessionCtx);

        long t2 = System.currentTimeMillis();
        ALog.getInstance().d(TAG, "<disconnect> <==END, costTime=" + (t2-t1));
        return ErrCode.XOK;
    }

    @Override
    public SessionInfo getSessionInfo(final UUID sessionId) {
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().e(TAG, "<getSessionInfo> not found session, sessionId=" + sessionId);
            return null;
        }

        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.mSessionId = sessionCtx.mSessionId;
        sessionInfo.mUserId = sessionCtx.mUserId;
        sessionInfo.mPeerDevId = sessionCtx.mDeviceId;
        sessionInfo.mLocalRtcUid = sessionCtx.mLocalRtcUid;
        sessionInfo.mChannelName = sessionCtx.mChnlName;
        sessionInfo.mRtcToken = sessionCtx.mRtcToken;
        sessionInfo.mRtmToken = sessionCtx.mRtmToken;
        sessionInfo.mAttachMsg = sessionCtx.mAttachMsg;
        sessionInfo.mType = sessionCtx.mType;
        sessionInfo.mUserCount = sessionCtx.mUserCount;
        sessionInfo.mState = sessionCtx.mState;

        ALog.getInstance().d(TAG, "<getSessionInfo> sessionInfo=" + sessionInfo);
        return sessionInfo;
    }

    @Override
    public IDevPreviewMgr getDevPreviewMgr(final UUID sessionId) {
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().e(TAG, "<getDevPreviewMgr> not found session, sessionId=" + sessionId);
            return null;
        }

        return sessionCtx.mDevPreviewMgr;
    }

    @Override
    public IDevMediaMgr getDevMediaMgr(final UUID sessionId) {
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().e(TAG, "<getDevMediaMgr> not found session, sessionId=" + sessionId);
            return null;
        }

        return sessionCtx.mDevMediaMgr;
    }

    @Override
    public IDevController getDevController(final UUID sessionId) {
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().e(TAG, "<getDevController> not found session, sessionId=" + sessionId);
            return null;
        }

        return sessionCtx.mDevController;
    }


    public IDeviceSessionMgr.InitParam getInitParam() {
        return mInitParam;
    }

    public RtmMgrComp getRtmMgrComp() {
        return mRtmComp;
    }

    ///////////////////////////////////////////////////////////////////////////
    //////////////// Methods for Override BaseThreadComp //////////////////////
    //////////////////////////////////////////////////////////////////////////
    @Override
    public void processWorkMessage(Message msg) {
        switch (msg.what) {
            case MSGID_SDK_CONNECT_DONE:
                onMessageConnectDone(msg);
                break;
            case MSGID_SDK_DEV_OFFLINE:
                onMessageDeviceOffline(msg);
                break;
            case MSGID_SDK_DEV_FIRSTFRAME:
                onMessageDeviceFirstFrame(msg);
                break;

            case MSGID_SDK_TIMER:
                DoTimer(msg);
                break;
        }
    }

    @Override
    protected void removeAllMessages() {
        synchronized (mMsgQueueLock) {
            mWorkHandler.removeMessages(MSGID_SDK_CONNECT_DONE);
            mWorkHandler.removeMessages(MSGID_SDK_DEV_OFFLINE);
            mWorkHandler.removeMessages(MSGID_SDK_DEV_FIRSTFRAME);
            mWorkHandler.removeMessages(MSGID_SDK_TIMER);
        }
    }

    @Override
    protected void processTaskFinsh() {

        // 退出所有频道，释放通话引擎
        talkingRelease();

        ALog.getInstance().d(TAG, "<processTaskFinsh> done");
    }


    /**
     * @brief 工作线程中运行，定时器
     */
    void DoTimer(Message msg) {
        List<SessionCtx> timeoutSessionList = mSessionMgr.queryTimeoutSessionList(CONNECT_TIMEOUT);

        //
        // 处理连接超时的会话
        //
        for (SessionCtx sessionCtx : timeoutSessionList) {
            talkingStop(sessionCtx);    // 退出通话
            mSessionMgr.removeSession(sessionCtx.mSessionId);   // 从会话管理器中删除本次会话

            // 回调呼叫超时失败
            ALog.getInstance().d(TAG, "<DoTimer> callback connecting timeout, sessionCtx=" + sessionCtx);
            CallbackSessionConnectDone(sessionCtx, ErrCode.XERR_TIMEOUT);
        }

        sendSingleMessage(MSGID_SDK_TIMER, 0, 0, null, TIMER_INTERVAL);
    }


    /**
     * @brief 工作线程中运行，设备上线
     */
    void onMessageConnectDone(Message msg) {
        int errCode = msg.arg1;
        UUID sessionId = (UUID)(msg.obj);
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().w(TAG, "<onMessageConnectDone> session removed, sessionId=" + sessionId);
            return;
        }
        if (sessionCtx.mState != SESSION_STATE_CONNECTING) {
            ALog.getInstance().d(TAG, "<onMessageConnectDone> bad session state, mState=" + sessionCtx.mState);
            return;
        }

        if (errCode == ErrCode.XOK) {
            // 连接成功，则更新状态机
            sessionCtx.mState = SESSION_STATE_CONNECTED;
            sessionCtx.mDevPreviewMgr = new DevPreviewMgr(sessionCtx.mSessionId, this);
            sessionCtx.mDevMediaMgr = new DevMediaMgr(sessionCtx.mSessionId, this);
            sessionCtx.mDevController = new DevController(sessionCtx.mSessionId, this);
            mSessionMgr.updateSession(sessionCtx);

        } else {
            // 连接失败，则直接从会话管理器中删除会话
            talkingStop(sessionCtx);
            sessionCtx.mState = SESSION_STATE_DISCONNECTED;
            mSessionMgr.removeSession(sessionCtx.mSessionId);
        }

        // 回调连接成功
        ALog.getInstance().d(TAG, "<onMessageConnectDone> errCode=" + errCode);
        CallbackSessionConnectDone(sessionCtx, errCode);
    }

    /**
     * @brief 工作线程中运行，设备掉线
     */
    void onMessageDeviceOffline(Message msg) {
        UUID sessionId = (UUID)(msg.obj);
        SessionCtx sessionCtx = mSessionMgr.removeSession(sessionId);  // 会话列表中删除会话
        if (sessionCtx == null) {
            ALog.getInstance().w(TAG, "<onMessageDeviceOffline> session removed, sessionId=" + sessionId);
            return;
        }
        ALog.getInstance().d(TAG, "<onMessageDeviceOffline> sessionCtx=" + sessionCtx);

        // 停止预览或者录像
        if (sessionCtx.mDevPreviewMgr != null) {
            sessionCtx.mDevPreviewMgr.previewStop();
            sessionCtx.mDevPreviewMgr.recordingStop();
        }

        // 停止设备端播放
        if (sessionCtx.mDevMediaMgr != null) {
            sessionCtx.mDevMediaMgr.stop();
        }

        // 结束通话
        talkingStop(sessionCtx);

        // 回调设备端断开连接
        CallbackSessionDisconnected(sessionCtx);
    }

    /**
     * @brief 工作线程中运行，设备端视频首帧
     */
    void onMessageDeviceFirstFrame(Message msg) {
        int width = msg.arg1;
        int height = msg.arg2;
        UUID sessionId = (UUID)(msg.obj);
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().w(TAG, "<onMessageDeviceFirstFrame> session removed, sessionId=" + sessionId);
            return;
        }
        ALog.getInstance().d(TAG, "<onMessageDeviceFirstFrame> sessionCtx=" + sessionCtx
                + ", width=" + width + ", height=" + height);

        if (sessionCtx.mState == SESSION_STATE_CONNECTED || sessionCtx.mState == SESSION_STATE_CONNECTING) {
            // 回调对端首帧出图
            CallbackPeerFirstVideo(sessionCtx, width, height);
        }
    }


    /////////////////////////////////////////////////////////////////////////////
    //////////////////// TalkingEngine.ICallback 回调处理 ////////////////////////
    /////////////////////////////////////////////////////////////////////////////
    @Override
    public void onTalkingJoinDone(final UUID sessionId, final String channel, int uid) {
        SessionCtx playerSession = mDevPlayerMgr.getSession(sessionId);
        if ((playerSession != null) && (playerSession.mDevMediaMgr != null)) {
            playerSession.mDevMediaMgr.onTalkingJoinDone(sessionId, channel, uid);
        }
    }

    @Override
    public void onTalkingLeftDone(final UUID sessionId) {
        SessionCtx playerSession = mDevPlayerMgr.getSession(sessionId);
        if ((playerSession != null) && (playerSession.mDevMediaMgr != null)) {
            playerSession.mDevMediaMgr.onTalkingLeftDone(sessionId);
        }
    }

    @Override
    public void onUserOnline(final UUID sessionId, int uid, int elapsed) {

        // 先处理设备播放的会话
        SessionCtx playerSession = mDevPlayerMgr.getSession(sessionId);
        if ((playerSession != null) && (playerSession.mDevMediaMgr != null)) {
            playerSession.mDevMediaMgr.onUserOnline(sessionId, uid, elapsed);
            return;
        }

        // 再处理设备通话的会话
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().w(TAG, "<onUserOnline> session removed, sessionId=" + sessionId
                    + ", uid=" + uid);
            return;
        }
        ALog.getInstance().d(TAG, "<onUserOnline> uid=" + uid + ", sessionCtx=" + sessionCtx);

        if (uid == sessionCtx.mDeviceRtcUid) {  // 对端设备加入频道
            // 发送连接完成事件
            sendSingleMessage(MSGID_SDK_CONNECT_DONE, ErrCode.XOK, 0, sessionId, 0);
            return;
        }

        // 回调其他用户上线事件
        if (uid != sessionCtx.mLocalRtcUid) {
            // 更新session上下文中 用户数量
            sessionCtx.mUserCount++;
            mSessionMgr.updateSession(sessionCtx);

            ALog.getInstance().d(TAG, "<onUserOnline> callback online event");
            CallbackOtherUserOnline(sessionCtx, uid);
        }
    }

    @Override
    public void onUserOffline(final UUID sessionId, int uid, int reason) {

        // 先处理设备播放的会话
        SessionCtx playerSession = mDevPlayerMgr.getSession(sessionId);
        if ((playerSession != null) && (playerSession.mDevMediaMgr != null)) {
            playerSession.mDevMediaMgr.onUserOffline(sessionId, uid, reason);
            return;
        }

        // 再处理设备通话的会话
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().w(TAG, "<onUserOffline> session removed, sessionId=" + sessionId
                    + ", uid=" + uid + ", reason=" + reason);
            return;
        }
        ALog.getInstance().d(TAG, "<onUserOffline> uid=" + uid
                + ", reason=" + reason + ", sessionCtx=" + sessionCtx);

        if (uid == sessionCtx.mDeviceRtcUid) {  // 对端设备退出频道
            // 发送对端设备TC掉线事件
            sendSingleMessage(MSGID_SDK_DEV_OFFLINE, 0, 0, sessionId, 0);
            return;
        }

        // 回调其他用户上线事件
        if (uid != sessionCtx.mLocalRtcUid) {
            // 更新session上下文中 用户数量
            sessionCtx.mUserCount--;
            mSessionMgr.updateSession(sessionCtx);

            ALog.getInstance().d(TAG, "<onUserOffline> callback online event");
            CallbackOtherUserOffline(sessionCtx, uid);
        }
    }

    @Override
    public void onPeerFirstVideoDecoded(final UUID sessionId, int peerUid, int videoWidth, int videoHeight) {
        // 先处理设备播放的会话
        SessionCtx playerSession = mDevPlayerMgr.getSession(sessionId);
        if ((playerSession != null) && (playerSession.mDevMediaMgr != null)) {
            playerSession.mDevMediaMgr.onPeerFirstVideoDecoded(sessionId, peerUid, videoWidth, videoHeight);
            return;
        }

        // 再处理设备通话的会话
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().d(TAG, "<onPeerFirstVideoDecoded> session removed"
                    + ", peerUid=" + peerUid + ", width=" + videoWidth + ", height=" + videoHeight );
            return;
        }
        ALog.getInstance().d(TAG, "<onPeerFirstVideoDecoded> sessionCtx=" + sessionCtx
                + ", peerUid=" + peerUid + ", width=" + videoWidth + ", height=" + videoHeight );

        // 发送对端RTC首帧出图事件
        sendSingleMessage(MSGID_SDK_DEV_FIRSTFRAME, videoWidth, videoHeight, sessionId, 0);
    }

    @Override
    public void onRecordingError(final UUID sessionId, int errCode) {
        // 先处理设备播放的会话
        SessionCtx playerSession = mDevPlayerMgr.getSession(sessionId);
        if ((playerSession != null) && (playerSession.mDevMediaMgr != null)) {
            playerSession.mDevMediaMgr.onRecordingError(sessionId, errCode);
            return;
        }

        // 再处理设备通话的会话
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().d(TAG, "<onRecordingError> session removed"
                    + ", sessionId=" + sessionId + ", errCode=" + errCode);
            return;
        }
        ALog.getInstance().d(TAG, "<onRecordingError> sessionCtx=" + sessionCtx
                + ", errCode=" + errCode);

        // 发送录像错误回调消息
        //sendSingleMessage(MSGID_RECORDING_ERROR, errCode, 0, sessionId, 0);
    }


    ///////////////////////////////////////////////////////////////////////
    ///////////////////////////// 通话处理方法 /////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    /**
     * @brief 通话准备处理，创建RTC并且进入频道
     */
    int talkingPrepare(final SessionCtx sessionCtx, boolean subPeerVideo, boolean subPeerAudio,
                        boolean pubLocalAudio) {
        boolean bRet = true;

        synchronized (mTalkEngLock) {

            // 如果RtcSdk还未创建，则进行创建并且初始化
            if (!mTalkEngine.isReady()) {
                mTalkEngine = new TalkingEngine();
                TalkingEngine.InitParam talkInitParam = mTalkEngine.new InitParam();
                talkInitParam.mContext = mInitParam.mContext;
                talkInitParam.mAppId = mInitParam.mAppId;
                talkInitParam.mCallback = this;
                mTalkEngine.initialize(talkInitParam);
            }

            // 加入频道
            bRet = mTalkEngine.joinChannel(sessionCtx, subPeerVideo, subPeerAudio, pubLocalAudio);

            // 设置音频效果
            //mTalkEngine.setAudioEffect(mAudioEffect);

            // 设置设备视频帧显示控件
            View displayView = mViewMgr.getDisplayView(sessionCtx.mDeviceId);
            if (displayView != null)  {
                mTalkEngine.setRemoteVideoView(sessionCtx, displayView);
            }
        }

        return (bRet ? ErrCode.XOK : ErrCode.XERR_INVALID_PARAM);
    }

    /**
     * @brief 应答对方或者对方应答后，开始通话，根据配置决定是否推送本地音频流
     */
    void talkingStart(final SessionCtx sessionCtx, boolean pubLocalAudio) {
        boolean muteLocalAudio = (!pubLocalAudio);

        synchronized (mTalkEngLock) {
            mTalkEngine.muteLocalAudioStream(sessionCtx, muteLocalAudio);
        }
    }

    /**
     * @brief 停止通话，状态机切换到空闲，清除对端设备和peerUid
     */
    void talkingStop(final SessionCtx sessionCtx) {
        int sessionCount = mSessionMgr.size();

        synchronized (mTalkEngLock) {
            mTalkEngine.leaveChannel(sessionCtx);

            // 如果当前没有会话了，直接释放整个RtcSDK
            if (sessionCount <= 0) {
                mTalkEngine.release();
            }
        }
    }

    /**
     * @brief 退出所有的频道，并且释放整个通话引擎
     */
    void talkingRelease() {
        List<SessionCtx> sessionList = mSessionMgr.getAllSessionList();
        int sessionCount = sessionList.size();
        int i;

        synchronized (mTalkEngLock) {
            for (i = 0; i < sessionCount; i++) {
                SessionCtx sessionCtx = sessionList.get(i);
                mTalkEngine.leaveChannel(sessionCtx);
            }

            mTalkEngine.release();
        }

        ALog.getInstance().d(TAG, "<talkingRelease> done, sessionCount=" + sessionCount);
    }


    /**
     * @brief 设备播放器的频道加入结果
     */
    public static class DevPlayerChnlRslt {
        public int mErrCode;
        public UUID mSessionId;

        @Override
        public String toString() {
            String infoText = "{ mErrCode=" + mErrCode + ", mSessionId=" + mSessionId + " }";
            return infoText;
        }
    }

    /**
     * @brief 进入 设备播放器频道
     */
    DevPlayerChnlRslt devPlayerChnlEnter(final DevPlayerChnlInfo chnlInfo) {
        DevPlayerChnlRslt result = new DevPlayerChnlRslt();

        SessionCtx playerSession = new SessionCtx();
        playerSession.mSessionId = UUID.randomUUID();
        playerSession.mUserId = mInitParam.mUserId;
        playerSession.mDeviceId = chnlInfo.getDeviceId();
        playerSession.mLocalRtcUid = chnlInfo.getRtcUid();
        playerSession.mChnlName = chnlInfo.getChannelName();
        playerSession.mRtcToken = chnlInfo.getRtcToken();
        playerSession.mType = SESSION_TYPE_PLAYBACK;  // 会话类型
        playerSession.mUserCount = 1;      // 至少有一个用户
        playerSession.mSeesionCallback = null;
        playerSession.mState = IDeviceSessionMgr.SESSION_STATE_CONNECTED;   // 直接连接到设备
        playerSession.mConnectTimestamp = System.currentTimeMillis();
        playerSession.mDevMediaMgr = chnlInfo.getMediaMgr();
        mDevPlayerMgr.addSession(playerSession);

        // 开始进入频道
        int ret = talkingPrepare(playerSession, true, true, false);
        if (ret != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<devPlayerChnlEnter> fail to prepare, ret=" + ret);
            mDevPlayerMgr.removeSession(playerSession.mSessionId);
            result.mErrCode = ret;
            return result;
        }

        // 设置显示控件
        View displayView = chnlInfo.getDisplayView();
        if (displayView != null) {
            setDisplayView(playerSession, displayView);
        }

        result.mSessionId = playerSession.mSessionId;
        ALog.getInstance().d(TAG, "<devPlayerChnlEnter> done, chnlInfo=" + chnlInfo
            + ", result=" + result);
        return result;
    }

    /**
     * @brief 退出 设备播放器频道
     */
    int devPlayerChnlExit(final UUID playerSessionId) {
        SessionCtx playerSession = mDevPlayerMgr.removeSession(playerSessionId);
        if (playerSession == null) {
            ALog.getInstance().e(TAG, "<devPlayerChnlExit> not found playerSession"
                    + ", sessionId=" + playerSessionId);
            return ErrCode.XERR_INVALID_PARAM;
        }

        talkingStop(playerSession);

        ALog.getInstance().d(TAG, "<devPlayerChnlExit> done");
        return ErrCode.XOK;
    }



    /////////////////////////////////////////////////////////////////////////////
    /////////////////////// 提供给其他模块调用的 RTC相关方法 /////////////////////////
    /////////////////////////////////////////////////////////////////////////////

    int setDisplayView(final UUID sessionId, final View displayView) {
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().e(TAG, "<setDisplayView> not found session, sessionId=" + sessionId);
            return ErrCode.XERR_INVALID_PARAM;
        }

        boolean ret = true;
        if (sessionCtx != null) {  // 当前有相应的会话，直接更新设备显示控件
            synchronized (mTalkEngLock) {
                if (mTalkEngine.isReady()) {
                    ret = mTalkEngine.setRemoteVideoView(sessionCtx, displayView);
                }
            }
        }

        ALog.getInstance().d(TAG, "<setDisplayView> done, sessionId=" + sessionId + ", ret=" + ret);
        return (ret ? ErrCode.XOK : ErrCode.XERR_UNSUPPORTED);
    }


    int setDisplayView(final SessionCtx sessionCtx, final View displayView) {
        boolean ret = true;
        synchronized (mTalkEngLock) {
            if (mTalkEngine.isReady()) {
                ret = mTalkEngine.setRemoteVideoView(sessionCtx, displayView);
            }
        }

        ALog.getInstance().d(TAG, "<setDisplayView> done, sessionId=" + sessionCtx.mSessionId
                + ", ret=" + ret);
        return (ret ? ErrCode.XOK : ErrCode.XERR_UNSUPPORTED);
    }

    public int previewStart(final UUID sessionId, final IDevPreviewMgr.OnPreviewListener previewListener) {
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().e(TAG, "<previewStart> not found session, sessionId=" + sessionId);
            return ErrCode.XERR_INVALID_PARAM;
        }

        boolean ret = true;
        if (sessionCtx != null) {  // 当前有相应的会话，直接更新设备显示控件
            synchronized (mTalkEngLock) {
                if (mTalkEngine.isReady()) {
                    ret = mTalkEngine.subscribeStart(sessionCtx);
                }
            }

            // 保存预览监听器
            sessionCtx.mPreviewListener = previewListener;
            mSessionMgr.updateSession(sessionCtx);
        }

        ALog.getInstance().d(TAG, "<previewStart> done, sessionId=" + sessionId + ", ret=" + ret);
        return (ret ? ErrCode.XOK : ErrCode.XERR_UNSUPPORTED);
    }


    public int previewStop(final UUID sessionId) {
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().e(TAG, "<previewStop> not found session, sessionId=" + sessionId);
            return ErrCode.XERR_INVALID_PARAM;
        }

        boolean ret = true;
        if (sessionCtx != null) {  // 当前有相应的会话，直接更新设备显示控件
            synchronized (mTalkEngLock) {
                if (mTalkEngine.isReady()) {
                    ret = mTalkEngine.subscribeStop(sessionCtx);
                }
            }

            // 清除预览监听器
            sessionCtx.mPreviewListener = null;
            mSessionMgr.updateSession(sessionCtx);
        }

        ALog.getInstance().d(TAG, "<previewStop> done, sessionId=" + sessionId + ", ret=" + ret);
        return (ret ? ErrCode.XOK : ErrCode.XERR_UNSUPPORTED);
    }


    int muteLocalAudio(final UUID sessionId, boolean mute) {
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().e(TAG, "<muteLocalAudio> not found session, sessionId=" + sessionId);
            return ErrCode.XERR_INVALID_PARAM;
        }

        boolean ret;
        synchronized (mTalkEngLock) {
            ret = mTalkEngine.muteLocalAudioStream(sessionCtx, mute);
        }

        ALog.getInstance().d(TAG, "<muteLocalAudio> done, sessionId=" + sessionId + ", ret=" + ret);
        return (ret ? ErrCode.XOK : ErrCode.XERR_UNSUPPORTED);
    }

    int muteDeviceVideo(final UUID sessionId, boolean mute) {
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().e(TAG, "<muteDeviceVideo> not found session, sessionId=" + sessionId);
            return ErrCode.XERR_INVALID_PARAM;
        }

        boolean ret;
        synchronized (mTalkEngLock) {
            ret = mTalkEngine.mutePeerVideoStream(sessionCtx, mute);
        }

        ALog.getInstance().d(TAG, "<muteDeviceVideo> done, sessionId=" + sessionId + ", ret=" + ret);
        return (ret ? ErrCode.XOK : ErrCode.XERR_UNSUPPORTED);
    }


    public int muteDeviceAudio(final UUID sessionId, boolean mute) {
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().e(TAG, "<muteDeviceAudio> not found session, sessionId=" + sessionId);
            return ErrCode.XERR_INVALID_PARAM;
        }

        boolean ret;
        synchronized (mTalkEngLock) {
            ret = mTalkEngine.mutePeerAudioStream(sessionCtx, mute);
        }

        ALog.getInstance().d(TAG, "<muteDeviceAudio> done, sessionId=" + sessionId + ", ret=" + ret);
        return (ret ? ErrCode.XOK : ErrCode.XERR_UNSUPPORTED);
    }


    public int captureVideoFrame(final UUID sessionId, final String saveFilePath) {
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().e(TAG, "<captureVideoFrame> not found session, sessionId=" + sessionId);
            return ErrCode.XERR_INVALID_PARAM;
        }

        boolean ret;
        synchronized (mTalkEngLock) {
            ret = mTalkEngine.takeSnapshot(sessionCtx, saveFilePath);
        }

        ALog.getInstance().d(TAG, "<captureVideoFrame> done, sessionId=" + sessionId
                + ", ret=" + ret + ", saveFilePath=" + saveFilePath);
        return (ret ? ErrCode.XOK : ErrCode.XERR_UNSUPPORTED);
    }

    public int recordingStart(final UUID sessionId, final String outFilePath) {
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().e(TAG, "<recordingStart> not found session, sessionId=" + sessionId);
            return ErrCode.XERR_INVALID_PARAM;
        }

        int ret;
        synchronized (mTalkEngLock) {
            ret = mTalkEngine.recordingStart(sessionCtx, outFilePath);
        }

        ALog.getInstance().d(TAG, "<recordingStart> done, sessionId=" + sessionId
                + ", ret=" + ret + ", outFilePath=" + outFilePath);
        return (ret == Constants.ERR_OK) ? ErrCode.XOK : ErrCode.XERR_UNSUPPORTED;
    }

    public int recordingStop(final UUID sessionId) {
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().e(TAG, "<recordingStop> not found session, sessionId=" + sessionId);
            return ErrCode.XERR_INVALID_PARAM;
        }

        int ret;
        synchronized (mTalkEngLock) {
            ret = mTalkEngine.recordingStop(sessionCtx);
        }

        ALog.getInstance().d(TAG, "<recordingStop> done, sessionId=" + sessionId
                + ", ret=" + ret );
        return (ret == Constants.ERR_OK) ? ErrCode.XOK : ErrCode.XERR_UNSUPPORTED;
    }

    public boolean isRecording(final UUID sessionId) {
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().e(TAG, "<isRecording> not found session, sessionId=" + sessionId);
            return false;
        }

        boolean recording;
        synchronized (mTalkEngLock) {
            recording = mTalkEngine.isRecording(sessionCtx);
        }

        ALog.getInstance().d(TAG, "<isRecording> done, sessionId=" + sessionId
                + ", recording=" + recording );
        return recording;
    }


    public IDevPreviewMgr.RtcNetworkStatus getNetworkStatus(final UUID sessionId) {
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().e(TAG, "<getNetworkStatus> not found session, sessionId=" + sessionId);
            return null;
        }

        IDevPreviewMgr.RtcNetworkStatus networkStatus;
        synchronized (mTalkEngLock) {
            networkStatus = mTalkEngine.getNetworkStatus();
        }

        ALog.getInstance().d(TAG, "<getNetworkStatus> done, sessionId=" + sessionId);
        return networkStatus;
    }

    public int setPlaybackVolume(final UUID sessionId, int volumeLevel) {
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().e(TAG, "<setPlaybackVolume> not found session, sessionId=" + sessionId);
            return ErrCode.XERR_INVALID_PARAM;
        }

        boolean ret;
        synchronized (mTalkEngLock) {
            ret = mTalkEngine.setPlaybackVolume(volumeLevel);
        }

        ALog.getInstance().d(TAG, "<setPlaybackVolume> done, sessionId=" + sessionId
                + ", ret=" + ret + ", volumeLevel=" + volumeLevel);
        return (ret ? ErrCode.XOK : ErrCode.XERR_UNSUPPORTED);
    }


    public int setAudioEffect(final UUID sessionId, final IDevPreviewMgr.AudioEffectId effectId) {
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().e(TAG, "<setAudioEffect> not found session, sessionId=" + sessionId);
            return ErrCode.XERR_INVALID_PARAM;
        }
        int voice_changer = Constants.AUDIO_EFFECT_OFF;
        switch (effectId) {
            case OLDMAN:
                voice_changer = Constants.VOICE_CHANGER_EFFECT_OLDMAN;
                break;

            case BABYBOY:
                voice_changer = Constants.VOICE_CHANGER_EFFECT_BOY;
                break;

            case BABYGIRL:
                voice_changer = Constants.VOICE_CHANGER_EFFECT_GIRL;
                break;

            case ZHUBAJIE:
                voice_changer = Constants.VOICE_CHANGER_EFFECT_PIGKING;
                break;

            case ETHEREAL:
                voice_changer = Constants.VOICE_CHANGER_EFFECT_SISTER;
                break;

            case HULK:
                voice_changer = Constants.VOICE_CHANGER_EFFECT_HULK;
                break;
        }

        boolean ret;
        synchronized (mTalkEngLock) {
            ret = mTalkEngine.setAudioEffect(voice_changer);
        }

        ALog.getInstance().d(TAG, "<setAudioEffect> done, sessionId=" + sessionId
                + ", ret=" + ret + ", voice_changer=" + voice_changer);
        return (ret ? ErrCode.XOK : ErrCode.XERR_UNSUPPORTED);
    }

    public IDevPreviewMgr.AudioEffectId getAudioEffect(final UUID sessionId) {
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().e(TAG, "<getAudioEffect> not found session, sessionId=" + sessionId);
            return IDevPreviewMgr.AudioEffectId.NORMAL;
        }

        int voice_changer;
        synchronized (mTalkEngLock) {
            voice_changer = mTalkEngine.getAudioEffect();
        }

        IDevPreviewMgr.AudioEffectId effectId = IDevPreviewMgr.AudioEffectId.NORMAL;
        switch (voice_changer) {
            case Constants.VOICE_CHANGER_EFFECT_OLDMAN:
                effectId = IDevPreviewMgr.AudioEffectId.OLDMAN;
                break;

            case Constants.VOICE_CHANGER_EFFECT_BOY:
                effectId = IDevPreviewMgr.AudioEffectId.BABYBOY;
                break;

            case Constants.VOICE_CHANGER_EFFECT_GIRL:
                effectId = IDevPreviewMgr.AudioEffectId.BABYGIRL;
                break;

            case Constants.VOICE_CHANGER_EFFECT_PIGKING:
                effectId = IDevPreviewMgr.AudioEffectId.ZHUBAJIE;
                break;

            case Constants.VOICE_CHANGER_EFFECT_SISTER:
                effectId = IDevPreviewMgr.AudioEffectId.ETHEREAL;
                break;

            case Constants.VOICE_CHANGER_EFFECT_HULK:
                effectId = IDevPreviewMgr.AudioEffectId.HULK;
                break;
        }

        ALog.getInstance().d(TAG, "<getAudioEffect> done, sessionId=" + sessionId
                + ", voice_changer=" + voice_changer);
        return effectId;
    }

    public int setRtcPrivateParam(final UUID sessionId, final String privateParam) {
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().e(TAG, "<setRtcPrivateParam> not found session, sessionId=" + sessionId);
            return ErrCode.XERR_INVALID_PARAM;
        }

        int ret;
        synchronized (mTalkEngLock) {
            ret = mTalkEngine.setParameters(privateParam);
        }

        ALog.getInstance().d(TAG, "<setRtcPrivateParam> done, sessionId=" + sessionId
                + ", ret=" + ret + ", privateParam=" + privateParam);
        return (ret == Constants.ERR_OK) ? ErrCode.XOK : ErrCode.XERR_INVALID_PARAM;
    }

    /////////////////////////////////////////////////////////////////////////////
    /////////////////////////////// 所有的对上层回调处理 //////////////////////////
    /////////////////////////////////////////////////////////////////////////////
    void CallbackSessionConnectDone(final SessionCtx sessionCtx, int errCode) {
        if (sessionCtx.mSeesionCallback != null) {
            ConnectParam connectParam = new ConnectParam();
            connectParam.mUserId = sessionCtx.mUserId;
            connectParam.mPeerDevId = sessionCtx.mDeviceId;
            connectParam.mChannelName = sessionCtx.mChnlName;
            connectParam.mLocalRtcUid = sessionCtx.mLocalRtcUid;
            connectParam.mRtcToken = sessionCtx.mRtcToken;
            connectParam.mRtmToken = sessionCtx.mRtmToken;
            sessionCtx.mSeesionCallback.onSessionConnectDone(sessionCtx.mSessionId, connectParam, errCode);
        }
    }

    void CallbackSessionDisconnected(final SessionCtx sessionCtx) {
        if (sessionCtx.mSeesionCallback != null) {
            sessionCtx.mSeesionCallback.onSessionDisconnected(sessionCtx.mSessionId);
        }
    }

    void CallbackOtherUserOnline(final SessionCtx sessionCtx, int uid) {
        if (sessionCtx.mSeesionCallback != null) {
            sessionCtx.mSeesionCallback.onSessionOtherUserOnline(sessionCtx.mSessionId, sessionCtx.mUserCount);
        }
    }

    void CallbackOtherUserOffline(final SessionCtx sessionCtx, int uid) {
        if (sessionCtx.mSeesionCallback != null) {
            sessionCtx.mSeesionCallback.onSessionOtherUserOffline(sessionCtx.mSessionId, sessionCtx.mUserCount);
        }
    }

    void CallbackPeerFirstVideo(final SessionCtx sessionCtx, int width, int height) {
        if (sessionCtx.mPreviewListener != null) {
            sessionCtx.mPreviewListener.onDeviceFirstVideo(sessionCtx.mSessionId, width, height);
        }
    }


}
