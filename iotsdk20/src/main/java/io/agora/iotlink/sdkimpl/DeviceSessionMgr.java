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
    private static final int MSGID_SDK_CONNECT_DONE = 0x1002;  ///< 连接RTC完成消息
    private static final int MSGID_SDK_DEV_OFFLINE= 0x1003;      ///< 设备掉线消息
    private static final int MSGID_SDK_DEV_FIRSTFRAME = 0x1004;  ///< 设备首帧出图消息
    private static final int MSGID_SDK_DEV_SHOTTAKEN = 0x1005;   ///< 截图完成回调
    private static final int MSGID_SDK_TIMER = 0x1006;           ///< 定时器
    private static final int MSGID_SDK_CONNECT_DEV = 0x1007;     ///< 连接到设备端
    private static final int MSGID_SDK_DISCONNECT_DEV = 0x1008;  ///< 从设备断开连接
    private static final int MSGID_SDK_RENEW_TOKEN = 0x1009;     ///< renew token处理

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
        long t1 = System.currentTimeMillis();
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

        long t2 = System.currentTimeMillis();
        ALog.getInstance().d(TAG, "<initialize> done, costTime=" + (t2-t1));
        return ErrCode.XOK;
    }

    @Override
    public void release() {
        long t1 = System.currentTimeMillis();
        // 停止组件线程
        runStop();

        mSessionMgr.clear();
        mViewMgr.clear();

        // 销毁RTM组件
        if (mRtmComp != null) {
            mRtmComp.release();
            mRtmComp = null;
        }

        long t2 = System.currentTimeMillis();
        ALog.getInstance().d(TAG, "<release> done, costTime=" + (t2-t1));
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
            sessionInfo.mPeerDevId = sessionCtx.mDeviceId;
            sessionInfo.mLocalRtcUid = sessionCtx.mLocalRtcUid;
            sessionInfo.mChannelName = sessionCtx.mChnlName;
            sessionInfo.mRtcToken = sessionCtx.mRtcToken;
            sessionInfo.mRtmUid = sessionCtx.mRtmUid;
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
        newSession.mDeviceId = connectParam.mPeerDevId;
        newSession.mLocalRtcUid = connectParam.mLocalRtcUid;
        newSession.mDeviceRtcUid = DEFAULT_DEV_UID;
        newSession.mChnlName = connectParam.mChannelName;
        newSession.mRtcToken = connectParam.mRtcToken;
        newSession.mRtmUid = connectParam.mRtmUid;
        newSession.mRtmToken = connectParam.mRtmToken;
        newSession.mType = SESSION_TYPE_DIAL;  // 会话类型
        newSession.mUserCount = 1;      // 至少有一个用户
        newSession.mSeesionCallback = sessionCallback;
        newSession.mState = SESSION_STATE_CONNECTING;   // 正在连接中状态机
        newSession.mPubLocalAudio = false;  // 连接时默认不推本地音频流
        newSession.mSubDevVideo = false;    // 连接时默认不订阅设备音频流
        newSession.mSubDevAudio = false;    // 连接时默认不订阅设备视频流
        newSession.mConnectTimestamp = System.currentTimeMillis();
        newSession.mRtcState = SessionCtx.STATE_CONNECTING;
        newSession.mRtmState = SessionCtx.STATE_CONNECTING;

        // 添加到会话管理器中
        mSessionMgr.addSession(newSession);

        // 发送连接消息
        sendSingleMessage(MSGID_SDK_CONNECT_DEV, 0, 0, newSession, 0);

        long t2 = System.currentTimeMillis();
        ALog.getInstance().d(TAG, "<connect> <==End done" + ", costTime=" + (t2-t1));
        result.mSessionId = newSession.mSessionId;
        result.mErrCode = ErrCode.XOK;
        return result;
    }

    @Override
    public int disconnect(final UUID sessionId,
                          final OnSessionDisconnectListener disconnectListener) {
        long t1 = System.currentTimeMillis();
        ALog.getInstance().d(TAG, "<disconnect> ==> BEGIN, sessionId=" + sessionId);

        // 找到该会话
        SessionCtx removeSession = mSessionMgr.getSession(sessionId);
        if (removeSession == null) {
            ALog.getInstance().e(TAG, "<disconnect> <==END, not found");
            return ErrCode.XOK;
        }
        if (removeSession.mState != SESSION_STATE_CONNECTED) {
            ALog.getInstance().e(TAG, "<disconnect> <==END, bad state, mState=" + removeSession.mState);
            return ErrCode.XERR_BAD_STATE;
        }

        // 发送断连消息
        Object[] params = { removeSession, disconnectListener};
        sendSingleMessage(MSGID_SDK_DISCONNECT_DEV, 0, 0, params, 0);

        long t2 = System.currentTimeMillis();
        ALog.getInstance().d(TAG, "<disconnect> <==END, costTime=" + (t2-t1));
        return ErrCode.XOK;
    }

    @Override
    public int renewToken(final UUID sessionId, final TokenRenewParam renewParam) {
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().e(TAG, "<renewToken> not found session, sessionId=" + sessionId);
            return ErrCode.XERR_INVALID_PARAM;
        }
        if (sessionCtx.mState != SESSION_STATE_CONNECTED) {
            ALog.getInstance().e(TAG, "<renewToken> bad state, mState=" + sessionCtx.mState);
            return ErrCode.XERR_BAD_STATE;
        }

        // 发送 Renew Token 处理消息
        Object[] params = { sessionId, renewParam};
        sendSingleMessage(MSGID_SDK_RENEW_TOKEN, 0, 0, params, 0);

        ALog.getInstance().d(TAG, "<renewToken> done");
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
        sessionInfo.mPeerDevId = sessionCtx.mDeviceId;
        sessionInfo.mLocalRtcUid = sessionCtx.mLocalRtcUid;
        sessionInfo.mChannelName = sessionCtx.mChnlName;
        sessionInfo.mRtcToken = sessionCtx.mRtcToken;
        sessionInfo.mRtmUid = sessionCtx.mRtmUid;
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
            case MSGID_SDK_DEV_OFFLINE:
                onMessageDeviceOffline(msg);
                break;
            case MSGID_SDK_DEV_FIRSTFRAME:
                onMessageDeviceFirstFrame(msg);
                break;
            case MSGID_SDK_DEV_SHOTTAKEN:
                onMessageSnapshotTaken(msg);
                break;


            case MSGID_SDK_CONNECT_DEV:
                onMessageConnectDev(msg);
                break;
            case MSGID_SDK_CONNECT_DONE:
                onMessageConnectDone(msg);
                break;
            case MSGID_SDK_DISCONNECT_DEV:
                onMessageDisconnectDev(msg);
                break;

            case MSGID_SDK_RENEW_TOKEN:
                onMessageRenewToken(msg);
                break;

            case MSGID_SDK_TIMER:
                DoTimer(msg);
                break;
        }
    }

    @Override
    protected void removeAllMessages() {
        synchronized (mMsgQueueLock) {
            if (mWorkHandler != null) {
                mWorkHandler.removeMessages(MSGID_SDK_CONNECT_DONE);
                mWorkHandler.removeMessages(MSGID_SDK_DEV_OFFLINE);
                mWorkHandler.removeMessages(MSGID_SDK_DEV_FIRSTFRAME);
                mWorkHandler.removeMessages(MSGID_SDK_DEV_SHOTTAKEN);
                mWorkHandler.removeMessages(MSGID_SDK_TIMER);
                mWorkHandler.removeMessages(MSGID_SDK_CONNECT_DEV);
                mWorkHandler.removeMessages(MSGID_SDK_DISCONNECT_DEV);
                mWorkHandler.removeMessages(MSGID_SDK_RENEW_TOKEN);
            }
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
            mSessionMgr.removeSession(sessionCtx.mSessionId);   // 从会话管理器中删除本次会话
            talkingStop(sessionCtx);    // 退出通话
            mRtmComp.disconnectFromDevice(sessionCtx);  // RTM断开设备

            // 回调呼叫超时失败
            ALog.getInstance().d(TAG, "<DoTimer> callback connecting timeout, sessionCtx=" + sessionCtx);
            CallbackSessionConnectDone(sessionCtx, ErrCode.XERR_TIMEOUT);
        }

        sendSingleMessage(MSGID_SDK_TIMER, 0, 0, null, TIMER_INTERVAL);
    }

    /**
     * @brief 工作线程中运行，连接设备操作
     */
    void onMessageConnectDev(Message msg) {
        SessionCtx newSession = (SessionCtx)(msg.obj);

        // 加入频道处理
        int errCode = talkingPrepare(newSession);
        if (errCode != ErrCode.XOK) {
            ALog.getInstance().e(TAG, "<onMessageConnectDev> failure to talkingPrepare(), errCode=" + errCode
                    + ", deviceId=" + newSession.mDeviceId);
            // 更新 RTC连接状态
            newSession.mRtcState = SessionCtx.STATE_DISCONNECTED;
            mSessionMgr.updateSession(newSession);

            // 发送消息，通知连接完成
            sendSingleMessage(MSGID_SDK_CONNECT_DONE, errCode, 0, newSession.mSessionId, 0);
        }

        // RTM组件中连接设备处理
        mRtmComp.connectToDevice(newSession, new RtmMgrComp.OnRtmConnectDevListener() {
            @Override
            public void onRtmConnectDevDone(final SessionCtx rtmSessionCtx, int errCode) {
                ALog.getInstance().d(TAG, "<onMessageConnectDev.onRtmConnectDevDone> deviceId=" + rtmSessionCtx.mDeviceId
                        + ", errCode=" + errCode);
                SessionCtx findSessionCtx = mSessionMgr.getSession(rtmSessionCtx.mSessionId);
                if (findSessionCtx == null) {
                    ALog.getInstance().d(TAG, "<onMessageConnectDev.onRtmConnectDevDone> NOT found sessionId=" + rtmSessionCtx.mSessionId);
                    return;
                }

                // 更新 RTM连接状态
                if (errCode == ErrCode.XOK) {
                    findSessionCtx.mRtmState = SessionCtx.STATE_CONNECTED;
                } else {
                    findSessionCtx.mRtmState = SessionCtx.STATE_DISCONNECTED;
                }
                mSessionMgr.updateSession(findSessionCtx);

                // 发送消息通知连接完成
                sendSingleMessage(MSGID_SDK_CONNECT_DONE, errCode, 0, findSessionCtx.mSessionId, 0);
            }
        });

        ALog.getInstance().d(TAG, "<onMessageConnectDev> done, newSession=" + newSession);
    }


    /**
     * @brief 工作线程中运行，连接完成
     * @param msg : 消息对象；  msg.arg1是错误码；  msg.obj是 sessionId
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

        int rtcState = sessionCtx.mRtcState;
        int rtmState = sessionCtx.mRtmState;
        if ((rtcState == SessionCtx.STATE_CONNECTING) || (rtmState == SessionCtx.STATE_CONNECTING)) {
            ALog.getInstance().d(TAG, "<onMessageConnectDone> RTC or RTM connect is ongoing!");
            return;
        }

        if ((rtcState == SessionCtx.STATE_CONNECTED) && (rtmState == SessionCtx.STATE_CONNECTED)) {
            // RTC和 RTM都 连接成功，则更新状态机
            sessionCtx.mState = SESSION_STATE_CONNECTED;
            sessionCtx.mDevPreviewMgr = new DevPreviewMgr(sessionCtx.mSessionId, this);
            sessionCtx.mDevMediaMgr = new DevMediaMgr(sessionCtx.mSessionId, this);
            sessionCtx.mDevController = new DevController(sessionCtx.mSessionId, this);
            mSessionMgr.updateSession(sessionCtx);
            errCode = ErrCode.XOK;

        } else {
            // 有一个连接失败，则直接从会话管理器中删除会话
            sessionCtx.mState = SESSION_STATE_DISCONNECTED;
            mSessionMgr.removeSession(sessionCtx.mSessionId);
            talkingStop(sessionCtx);  // 退出通话
            mRtmComp.disconnectFromDevice(sessionCtx); // RTM断开设备
            errCode = ErrCode.XERR_NETWORK;
        }

        // 回调设备连接结果
        ALog.getInstance().d(TAG, "<onMessageConnectDone> sessionId=" + sessionId
                + ", errCode=" + errCode);
        CallbackSessionConnectDone(sessionCtx, errCode);
    }

    /**
     * @brief 工作线程中运行，断连设备操作
     */
    void onMessageDisconnectDev(Message msg) {
        Object[] params = (Object[])msg.obj;
        SessionCtx removeSession = (SessionCtx) (params[0]);
        OnSessionDisconnectListener disconnectListener = (OnSessionDisconnectListener) (params[1]);
        long t1 = System.currentTimeMillis();

        // 停止预览或者录像
        if (removeSession.mDevPreviewMgr != null) {
            removeSession.mDevPreviewMgr.previewStop();
            removeSession.mDevPreviewMgr.recordingStop();
        }

        // 停止设备端播放
        if (removeSession.mDevMediaMgr != null) {
            removeSession.mDevMediaMgr.stop();
        }

        // RTM断开设备
        mRtmComp.disconnectFromDevice(removeSession);

        // 离开通话频道
        talkingStop(removeSession);

        // 从会话管理器中删除
        mSessionMgr.removeSession(removeSession.mSessionId);

        // 回调设备断连结果
        long t2 = System.currentTimeMillis();

        ALog.getInstance().d(TAG, "<onMessageDisconnectDev> removeSession=" + removeSession
                + ", costTime=" + (t2-t1));
        if (disconnectListener != null) {
            disconnectListener.onSessionDisconnectDone(removeSession.mSessionId, ErrCode.XOK);
        }
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

        // RTM断开设备
        mRtmComp.disconnectFromDevice(sessionCtx);

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

    /**
     * @brief 工作线程中运行，截图完成
     */
    void onMessageSnapshotTaken(Message msg) {
        Object[] params = (Object[])msg.obj;
        UUID sessionId = (UUID)(params[0]);
        String filePath = (String) (params[1]);
        Integer width = (Integer) (params[2]);
        Integer height = (Integer) (params[3]);
        Integer errCode = (Integer) (params[4]);

        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().w(TAG, "<onMessageSnapshotTaken> session removed, sessionId=" + sessionId);
            return;
        }
        ALog.getInstance().d(TAG, "<onMessageSnapshotTaken> sessionCtx=" + sessionCtx
                + ", width=" + width + ", height=" + height
                + ", filePath=" + filePath + ", errCode=" + errCode);

        if (sessionCtx.mState == SESSION_STATE_CONNECTED || sessionCtx.mState == SESSION_STATE_CONNECTING) {
            // 回调截图完成
            int respCode = ErrCode.XOK;
            if (errCode == -1) { // 文件写入失败
                respCode = ErrCode.XERR_FILE_WRITE;
            } else if (errCode == -2) { // 没有收到指定的适配帧
                respCode = ErrCode.XERR_FILE_WRITE;
            } else if (errCode == -3) { // 调用太频繁
                respCode = ErrCode.XERR_INVOKE_TOO_OFTEN;

            } else if (errCode != 0) {
                respCode = ErrCode.XERR_UNKNOWN;
            }
            CallbackShotTakeDone(sessionCtx, respCode, filePath, width, height);
        }
    }

    /**
     * @brief 工作线程中运行，Renew Token处理
     */
    void onMessageRenewToken(Message msg) {
        Object[] params = (Object[])msg.obj;
        UUID sessionId = (UUID)(params[0]);
        TokenRenewParam renewParam = (TokenRenewParam) (params[1]);

        // 更新 sessionCtx中已有的 rtcToken 和 rtmToken 两个值
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().w(TAG, "<onMessageRenewToken> session removed, sessionId=" + sessionId);
            return;
        }
        sessionCtx.mRtcToken = renewParam.mRtcToken;
        sessionCtx.mRtmToken = renewParam.mRtmToken;
        mSessionMgr.updateSession(sessionCtx);

        // 更新 RTC 的 token
        talkingRenewToken(sessionCtx, renewParam.mRtcToken);

        // 更新 RTM的token
        mRtmComp.renewToken(renewParam.mRtmToken);

        ALog.getInstance().d(TAG, "<onMessageRenewToken> done, sessionCtx=" + sessionCtx);
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
            // 更新 RTC连接状态
            sessionCtx.mRtcState = SessionCtx.STATE_CONNECTED;
            mSessionMgr.updateSession(sessionCtx);

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
    public void onSnapshotTaken(final UUID sessionId, int uid,
                                final String filePath, int width, int height, int errCode) {
        // 再处理设备通话的会话
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().d(TAG, "<onSnapshotTaken> session removed"
                    + ", uid=" + uid + ", filePath=" + filePath
                    + ", width=" + width + ", height=" + height + ", errCode=" + errCode );
            return;
        }
        ALog.getInstance().d(TAG, "<onSnapshotTaken> sessionCtx=" + sessionCtx
                    + ", uid=" + uid + ", filePath=" + filePath
                    + ", width=" + width + ", height=" + height + ", errCode=" + errCode );

        // 发送截图完成回调事件
        Object[] params = { sessionId, filePath, width, height, errCode};
        sendSingleMessage(MSGID_SDK_DEV_SHOTTAKEN, 0, 0, params, 0);

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

    @Override
    public void onRtcTokenWillExpire(final UUID sessionId, final String token) {
        // 再处理设备通话的会话
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().d(TAG, "<onRtcTokenWillExperie> session removed, sessionId=" + sessionId);
            return;
        }
        ALog.getInstance().d(TAG, "<onRtcTokenWillExperie> sessionCtx=" + sessionCtx
                + ", token=" + token );

        // 直接回调 Token过期
        CallbackTokenWillExpire(sessionCtx);
    }


    ///////////////////////////////////////////////////////////////////////
    ///////////////////////////// RTM处理 /////////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    public void onRtmTokenWillExpire(final UUID sessionId) {
        // 再处理设备通话的会话
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().d(TAG, "<onRtmTokenWillExpire> session removed, sessionId=" + sessionId);
            return;
        }
        ALog.getInstance().d(TAG, "<onRtmTokenWillExpire> sessionCtx=" + sessionCtx);

        // 直接回调 Token过期
        CallbackTokenWillExpire(sessionCtx);
    }


    ///////////////////////////////////////////////////////////////////////
    ///////////////////////////// 通话处理方法 /////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    /**
     * @brief 通话准备处理，创建RTC并且进入频道
     */
    int talkingPrepare(final SessionCtx sessionCtx) {
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
            bRet = mTalkEngine.joinChannel(sessionCtx);

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
     * @brief token更新
     */
    void talkingRenewToken(final SessionCtx sessionCtx, final String rtcNewToken) {
        synchronized (mTalkEngLock) {
            mTalkEngine.renewToken(sessionCtx, rtcNewToken);
        }
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
        playerSession.mDeviceId = chnlInfo.getDeviceId();
        playerSession.mLocalRtcUid = chnlInfo.getRtcUid();
        playerSession.mDeviceRtcUid = chnlInfo.getDevRtcUid();
        playerSession.mChnlName = chnlInfo.getChannelName();
        playerSession.mRtcToken = chnlInfo.getRtcToken();
        playerSession.mType = SESSION_TYPE_PLAYBACK;  // 会话类型
        playerSession.mUserCount = 1;      // 至少有一个用户
        playerSession.mSeesionCallback = null;
        playerSession.mState = IDeviceSessionMgr.SESSION_STATE_CONNECTED;   // 直接连接到设备
        playerSession.mConnectTimestamp = System.currentTimeMillis();
        playerSession.mDevMediaMgr = chnlInfo.getMediaMgr();
        playerSession.mPubLocalAudio = false;   // SD卡播放时 默认不推本地音频流
        playerSession.mSubDevVideo = true;      // SD卡播放时 默认订阅设备音频流
        playerSession.mSubDevAudio = true;      // SD卡播放时 默认订阅设备视频流
        mDevPlayerMgr.addSession(playerSession);

        // 开始进入频道
        int ret = talkingPrepare(playerSession);
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

    public int previewStart(final UUID sessionId, boolean bSubAudio, final IDevPreviewMgr.OnPreviewListener previewListener) {
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().e(TAG, "<previewStart> not found session, sessionId=" + sessionId);
            return ErrCode.XERR_INVALID_PARAM;
        }

        // 更新 sessionCtx 内容
        sessionCtx.mSubDevAudio = bSubAudio;    // 根据参数确定是否订阅设备音频流
        sessionCtx.mSubDevVideo = true;         // 默认订阅设备视频流
        sessionCtx.mPubLocalAudio = false;      // 默认不推本地音频流

        mSessionMgr.updateSession(sessionCtx);

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

        // 更新 sessionCtx 内容
        sessionCtx.mSubDevAudio = false;        // 停止订阅设备音频流
        sessionCtx.mSubDevVideo = false;        // 停止订阅设备视频流
        sessionCtx.mPubLocalAudio = false;      // 停止推送本地音频流
        mSessionMgr.updateSession(sessionCtx);

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

        // 更新 sessionCtx 内容
        sessionCtx.mPubLocalAudio = (!mute);
        mSessionMgr.updateSession(sessionCtx);

        boolean ret;
        synchronized (mTalkEngLock) {
            ret = mTalkEngine.muteLocalAudioStream(sessionCtx);
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

        // 更新 sessionCtx 内容
        sessionCtx.mSubDevVideo = (!mute);
        mSessionMgr.updateSession(sessionCtx);

        boolean ret;
        synchronized (mTalkEngLock) {
            ret = mTalkEngine.mutePeerVideoStream(sessionCtx);
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

        // 更新 sessionCtx 内容
        sessionCtx.mSubDevAudio = (!mute);
        mSessionMgr.updateSession(sessionCtx);

        boolean ret;
        synchronized (mTalkEngLock) {
            ret = mTalkEngine.mutePeerAudioStream(sessionCtx);
        }

        ALog.getInstance().d(TAG, "<muteDeviceAudio> done, sessionId=" + sessionId + ", ret=" + ret);
        return (ret ? ErrCode.XOK : ErrCode.XERR_UNSUPPORTED);
    }


    public int captureVideoFrame(final UUID sessionId, final String saveFilePath,
                                 final IDevPreviewMgr.OnCaptureFrameListener captureListener) {
        SessionCtx sessionCtx = mSessionMgr.getSession(sessionId);
        if (sessionCtx == null) {
            ALog.getInstance().e(TAG, "<captureVideoFrame> not found session, sessionId=" + sessionId);
            return ErrCode.XERR_INVALID_PARAM;
        }

        // 更新 session中截图回调
        sessionCtx.mCaptureListener = captureListener;
        mSessionMgr.updateSession(sessionCtx);

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
            connectParam.mPeerDevId = sessionCtx.mDeviceId;
            connectParam.mChannelName = sessionCtx.mChnlName;
            connectParam.mLocalRtcUid = sessionCtx.mLocalRtcUid;
            connectParam.mRtcToken = sessionCtx.mRtcToken;
            connectParam.mRtmUid = sessionCtx.mRtmUid;
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

    void CallbackTokenWillExpire(final SessionCtx sessionCtx) {
        if (sessionCtx.mSeesionCallback != null) {
            sessionCtx.mSeesionCallback.onSessionTokenWillExpire(sessionCtx.mSessionId);
        }
    }

    void CallbackPeerFirstVideo(final SessionCtx sessionCtx, int width, int height) {
        if (sessionCtx.mPreviewListener != null) {
            sessionCtx.mPreviewListener.onDeviceFirstVideo(sessionCtx.mSessionId, width, height);
        }
    }

    void CallbackShotTakeDone(final SessionCtx sessionCtx, int errCode, final String filePath,
                              int width, int height) {
        if (sessionCtx.mCaptureListener != null) {
            sessionCtx.mCaptureListener.onSnapshotDone(sessionCtx.mSessionId, errCode, filePath, width, height);
        }
    }

}
