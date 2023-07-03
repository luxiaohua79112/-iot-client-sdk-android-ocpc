package io.agora.iotlinkdemo.presistentconnect;


import android.content.Context;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.UUID;

import io.agora.iotlink.ErrCode;
import io.agora.iotlink.base.BaseThreadComp;
import io.agora.iotlink.logger.ALog;
import io.agora.iotlink.utils.JsonUtils;


/**
 * @brief 长联接组件，有自己独立的运行线程
 */
public class PresistentLinkComp extends BaseThreadComp
        implements MqttTransport.ICallback {

    private static final int DEFAULT_DEV_UID = 10;               ///< 设备端uid，固定为10

    //
    // The method of all callkit
    //
    private static final String METHOD_USER_START_CALL = "user-start-call";
    private static final String METHOD_DEVICE_START_CALL = "device-start-call";

    //
    // 长联接 状态机
    //
    public static final int PRESISTENTLINK_STATE_INVALID = 0x0000;             ///< 长联接未初始化
    public static final int PRESISTENTLINK_STATE_INITIALIZED = 0x0001;         ///< 长联接初始化完成，但还未就绪
    public static final int PRESISTENTLINK_STATE_PREPARING = 0x0002;           ///< 长联接正在就绪中
    public static final int PRESISTENTLINK_STATE_RUNNING = 0x0003;             ///< 长联接就绪完成，可以正常使用
    public static final int PRESISTENTLINK_STATE_RECONNECTING = 0x0004;        ///< 长联接正在内部重连中，暂时不可用
    public static final int PRESISTENTLINK_STATE_UNPREPARING = 0x0005;         ///< 长联接正在注销处理，完成后切换到初始化完成状态


    /**
     * @brief 长联接初始化参数
     */
    public static class InitParam {
        public Context mContext;
        public String mAppId;                       ///< 项目的 AppId
        public String mProjectID;                   ///< 申请的项目Id
        public String mPusherId;                    ///< 离线推送的pusherId
        public String mMasterServerUrl;             ///< 主服务器URL
        public String mSlaveServerUrl;              ///< 辅服务器URL
    }


    /**
     * @brief SDK就绪监听器，errCode=0表示就绪成功
     */
    public interface OnPrepareListener {
        void onSdkPrepareDone(final PrepareParam paramParam, int errCode);
    }

    /**
     * @brief 长联接就绪参数，其中 mClientType值如下：
     *        1: Web;  2: Phone;  3: Pad;  4: TV;  5: PC;  6: Mini_app
     */
    public static class PrepareParam {
        public String mAppId;
        public String mUserId;
        public int mClientType;
        public String mPusherId;                    ///< 离线推送的pusherId


        @Override
        public String toString() {
            String infoText = "{ mAppId=" + mAppId
                    + ", mUserId=" + mUserId + ", mClientType=" + mClientType
                    + ", mPusherId=" + mPusherId + " }";
            return infoText;
        }
    }

    /**
     * @brief 连接请求返回数据包
     */
    public static class ReqConnectResult {
        public int mErrCode;            ///< 错误码
        public UUID mConnectId;         ///< 连接唯一标识
    }


    /**
     * @brief 连接请求监听器
     */
    public static interface OnDevReqConnectListener {
        /**
         * @brief 请求设备连接完成
         * @param connectId : 连接唯一标识
         * @param deviceId : 对端设备 NodeId
         * @param localRtcUid : 本地RtcUid
         * @param rtcToken
         * @param rtmToken
         */
        default void onDevReqConnectDone(int errCode, final UUID connectId,
                                         final String deviceId, int localRtcUid,
                                         final String chnlName,
                                         final String rtcToken, final String rtmToken) { }
    }


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/PresLinkComp";
    private static final int MQTT_TIMEOUT = 10000;
    private static final int UNPREPARE_WAIT_TIMEOUT = 3500;



    //
    // The message Id
    //
    private static final int MSGID_PREPARE_NODEACTIVE = 0x0001;
    private static final int MSGID_PREPARE_INIT_DONE = 0x0002;
    private static final int MSGID_MQTT_STATE_CHANGED = 0x0003;
    private static final int MSGID_PACKET_SEND = 0x0004;
    private static final int MSGID_PACKET_RECV = 0x0005;
    private static final int MSGID_UNPREPARE = 0x0006;


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private final Object mUnprepareEvent = new Object();
    private InitParam mInitParam;



    public static final Object mDataLock = new Object();    ///< 同步访问锁,类中所有变量需要进行加锁处理
    private LocalNode mLocalNode = new LocalNode();
    private volatile int mStateMachine = PRESISTENTLINK_STATE_INVALID;     ///< 当前呼叫状态机

    private PrepareParam mPrepareParam;
    private OnPrepareListener mPrepareListener;
    private MqttTransport mMqttTransport = new MqttTransport();

    private String mMqttTopicSub;                                   ///< MQTT订阅的主题
    private String mMqttTopicPub;                                   ///< MQTT发布的主题
    private TransPktQueue mRecvPktQueue = new TransPktQueue();      ///< 接收数据包队列
    private TransPktQueue mSendPktQueue = new TransPktQueue();      ///< 发送数据包队列

    private ConnectionMgr mConnectMgr = new ConnectionMgr();     ///< 连接请求管理

    ///////////////////////////////////////////////////////////////////////
    //////////////// Override Methods of IAgoraIotAppSdk //////////////////
    ///////////////////////////////////////////////////////////////////////
    private static PresistentLinkComp mPresistentLinkInstance = null;

    public static PresistentLinkComp getInstance() {
        if(mPresistentLinkInstance == null) {
            synchronized (PresistentLinkComp.class) {
                if(mPresistentLinkInstance == null) {
                    mPresistentLinkInstance = new PresistentLinkComp();
                }
            }
        }
        return mPresistentLinkInstance;
    }

     /**
     * @brief 初始化长联接组件，启动组件线程
     */
    public int initialize(InitParam initParam) {
        mInitParam = initParam;

        synchronized (mDataLock) {
            mLocalNode.mReady = false;
        }


        // 设置 HTTP服务器地址
        HttpTransport.getInstance().setBaseUrl(initParam.mMasterServerUrl);

        // 启动组件线程
        runStart(TAG);
        mSendPktQueue.clear();
        mRecvPktQueue.clear();


        // SDK初始化完成，状态机切换到 初始化完成状态
        setStateMachine(PRESISTENTLINK_STATE_INITIALIZED);

        Log.d(TAG, "<initialize> done");
        return ErrCode.XOK;
    }

    /**
     * @brief 长联接组件释放，停止组件线程，释放所有资源
     */
    public void release() {
        // 停止组件线程
        runStop();
        synchronized (mDataLock) {
            mLocalNode.mReady = false;
        }
        mSendPktQueue.clear();
        mRecvPktQueue.clear();

        // 状态机切换到 无效状态
        setStateMachine(PRESISTENTLINK_STATE_INVALID);

        Log.d(TAG, "<release> done");
    }

    /**
     * @brief 获取长连接组件的状态机
     */
    public int getStateMachine() {
        synchronized (mDataLock) {
            return mStateMachine;
        }
    }

    private void setStateMachine(int newState) {
        synchronized (mDataLock) {
            mStateMachine = newState;
        }
    }

    /**
     * @brief 准备长联接，相当于账号登录操作
     */
    public int prepare(final PrepareParam prepareParam, final OnPrepareListener prepareListener) {
        int state = getStateMachine();
        if (state != PRESISTENTLINK_STATE_INITIALIZED) {
            Log.e(TAG, "<prepare> bad status, state=" + state);
            return ErrCode.XERR_BAD_STATE;
        }

        setStateMachine(PRESISTENTLINK_STATE_PREPARING);    // 设置状态机正在准备操作

        // 发送消息进行操作
        synchronized (mDataLock) {
            mPrepareParam = prepareParam;
            mPrepareListener = prepareListener;

            // 设置本地节点信息
            mLocalNode.mReady = false;
            mLocalNode.mUserId = prepareParam.mUserId;
        }
        sendSingleMessage(MSGID_PREPARE_NODEACTIVE, 0, 0, null, 0);

        Log.d(TAG, "<prepare> prepareParam=" + prepareParam.toString());
        return ErrCode.XOK;
    }

    /**
     * @brief 取消长联接，相当于账号登出操作
     */
    public int unprepare() {
        int state = getStateMachine();
        if (state == PRESISTENTLINK_STATE_INVALID) {
            Log.e(TAG, "<unprepare> bad status, state=" + state);
            return ErrCode.XERR_BAD_STATE;
        }
        if (state == PRESISTENTLINK_STATE_INITIALIZED) {
            Log.e(TAG, "<unprepare> already unprepared!");
            return ErrCode.XOK;
        }
        Log.d(TAG, "<unprepare> ==>Enter");

        // 设置状态机到 注销状态
        setStateMachine(PRESISTENTLINK_STATE_UNPREPARING);

        // 删除队列中所有消息, 仅发送注销消息
        removeAllMessages();
        sendSingleMessage(MSGID_UNPREPARE, 0, 0, null, 0);

        synchronized (mUnprepareEvent) {
            try {
                mUnprepareEvent.wait(UNPREPARE_WAIT_TIMEOUT);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.e(TAG, "<unprepare> exception=" + e.getMessage());
            }
        }

        synchronized (mDataLock) {
            mLocalNode.mReady = false;
            mLocalNode.mUserId = null;
            mLocalNode.mNodeId = null;
            mLocalNode.mRegion = null;
            mLocalNode.mToken = null;
        }
        removeAllMessages();

        // 设置状态机到 初始化完成
        setStateMachine(PRESISTENTLINK_STATE_INITIALIZED);

        Log.d(TAG, "<unprepare> <==Exit");
        return ErrCode.XOK;
    }

    /**
     * @brief 获取当前用户账号
     */
    public String getLocalUserId() {
        synchronized (mDataLock) {
            return mLocalNode.mUserId;
        }
    }

    /**
     * @brief 获取就绪后的 NodeId
     */
    public String getLocalNodeId() {
        synchronized (mDataLock) {
            return mLocalNode.mNodeId;
        }
    }


    /**
     * @brief 获取长联接的初始化参数
     */
    public PresistentLinkComp.InitParam getInitParam() {
        return mInitParam;
    }

    /**
     * @brief 发起设备连接请求
     */
    public ReqConnectResult devReqConnect(final String deviceId, final String attachMsg,
                                          final OnDevReqConnectListener connectListener ) {
        long t1 = System.currentTimeMillis();
        ReqConnectResult result = new ReqConnectResult();

        if (!mLocalNode.mReady) {
            Log.e(TAG, "<devReqConnect> bad state, sdkState=" + mStateMachine);
            result.mErrCode = ErrCode.XERR_SDK_NOT_READY;
            return result;
        }
        ConnectionCtx connectCtx = mConnectMgr.findConnectionByDeviceId(deviceId);
        if (connectCtx != null) {
            Log.e(TAG, "<devReqConnect> bad state, already in connection");
            result.mErrCode = ErrCode.XERR_SDK_NOT_READY;
            return result;
        }

        // 发送请求消息
        Log.d(TAG, "<devReqConnect> ==> BEGIN, deviceId=" + deviceId);
        UUID connectId = UUID.randomUUID();
        long traceId = System.currentTimeMillis();

        // body内容
        JSONObject body = new JSONObject();
        try {
            JSONObject header = new JSONObject();
            header.put("traceId", traceId );
            header.put("timestamp", System.currentTimeMillis());
            header.put("nodeToken", mLocalNode.mToken);
            header.put("method", METHOD_USER_START_CALL);
            body.put("header", header);

            JSONObject payloadObj = new JSONObject();
            payloadObj.put("appId", mInitParam.mAppId);
            payloadObj.put("deviceId", deviceId);
            payloadObj.put("extraMsg", attachMsg);
            body.put("payload", payloadObj);

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "<devReqConnect>> [Exit] failure with JSON exp!");
            result.mErrCode = ErrCode.XERR_JSON_WRITE;
            return result;
        }


        ConnectionCtx newConnect = new ConnectionCtx();
        newConnect.mConnectId = connectId;
        newConnect.mUserId = mLocalNode.mNodeId;
        newConnect.mDeviceId = deviceId;
        newConnect.mDeviceRtcUid = DEFAULT_DEV_UID;
        newConnect.mAttachMsg = attachMsg;
        newConnect.mTraceId = traceId;
        newConnect.mConnectListener = connectListener;

        // 添加到连接管理器中
        mConnectMgr.addConnection(newConnect);

        // 发送主叫的 MQTT呼叫请求数据包
        TransPacket transPacket = new TransPacket();
        transPacket.mTopic = getMqttPublishTopic();
        transPacket.mContent = body.toString();
        transPacket.mConnectId = connectId;
        sendPacket(transPacket);

        long t2 = System.currentTimeMillis();
        Log.d(TAG, "<devReqConnect> <==End done" + ", costTime=" + (t2-t1));
        result.mConnectId = connectId;
        result.mErrCode = ErrCode.XOK;
        return result;
    }

    /**
     * @brief 发起设备断开请求
     */
    public int devReqDisconnect(final UUID connectId) {
        long t1 = System.currentTimeMillis();
        if (!mLocalNode.mReady) {
            Log.e(TAG, "<devReqDisconnect> bad state, sdkState=" + mStateMachine);
            return ErrCode.XERR_SDK_NOT_READY;
        }

        Log.d(TAG, "<devReqDisconnect> ==> BEGIN");

        // 在MQTT发送队列中删除主叫呼叫请求
        removePacketByConnectId(connectId);

        // 会话管理器中直接删除该会话
        ConnectionCtx connectionCtx = mConnectMgr.removeConnection(connectId);
        if (connectionCtx == null) {
            Log.e(TAG, "<devReqDisconnect> <==END, already disconnect, not found");
            return ErrCode.XOK;
        }

        long t2 = System.currentTimeMillis();
        Log.d(TAG, "<devReqDisconnect> <==END, costTime=" + (t2-t1));
        return ErrCode.XOK;
    }

    /**
     * @brief 获取本地节点信息
     */
    LocalNode getLoalNode() {
        synchronized (mDataLock) {
            return mLocalNode;
        }
    }


    /**
     * @brief 发送数据包，被其他组件模块调用
     */
    int sendPacket(final TransPacket sendPacket) {
        if (getStateMachine() != PRESISTENTLINK_STATE_RUNNING) {
            return ErrCode.XERR_BAD_STATE;
        }

        // 插入发送队列
        mSendPktQueue.inqueue(sendPacket);

        // 消息通知组件线程进行发送
        sendSingleMessage(MSGID_PACKET_SEND, 0, 0, null, 0);

        return ErrCode.XOK;
    }

    /**
     * @brief 根据 sessionId 从队列中删除要发送的数据包
     * @param connectId : 查找的sessionId
     * @return 返回删除的 packet，如果没有对应sessionId的发送包则返回null
     */
    TransPacket removePacketByConnectId(final UUID connectId) {
        if (getStateMachine() != PRESISTENTLINK_STATE_RUNNING) {
            return null;
        }

        // 从发送队列中进行删除
        TransPacket packet = mSendPktQueue.removeByConnectId(connectId);
        return packet;
    }

    /**
     * @brief 获取MQTT订阅的主题
     */
    String getMqttSubscribedTopic() {
        return mMqttTopicSub;
    }

    /**
     * @brief 获取MQTT发布的主题
     */
    String getMqttPublishTopic() {
        return mMqttTopicPub;
    }



    ///////////////////////////////////////////////////////////////////////////
    //////////////// Methods for Override BaseThreadComp ///////////////////////
    //////////////////////////////////////////////////////////////////////////
    @Override
    protected void processWorkMessage(Message msg)   {
        switch (msg.what) {
            case MSGID_PREPARE_NODEACTIVE:
                    onMessagePrepareNodeActive(msg);
                break;

            case MSGID_PREPARE_INIT_DONE:
                    onMessageInitDone(msg);
                break;

            case MSGID_MQTT_STATE_CHANGED:
                    onMessageMqttStateChanged(msg);
                break;

            case MSGID_PACKET_SEND:
                    onMessagePacketSend(msg);
                break;

            case MSGID_PACKET_RECV:
                    onMessagePacketRecv(msg);
                break;

            case MSGID_UNPREPARE:
                    onMessageUnprepare(msg);
                break;
        }

         mMqttTransport.processWorkMessage(msg);
    }

    @Override
    protected void removeAllMessages() {
        synchronized (mMsgQueueLock) {
            mWorkHandler.removeMessages(MSGID_PREPARE_NODEACTIVE);
            mWorkHandler.removeMessages(MSGID_PREPARE_INIT_DONE);
            mWorkHandler.removeMessages(MSGID_MQTT_STATE_CHANGED);
            mWorkHandler.removeMessages(MSGID_PACKET_SEND);
            mWorkHandler.removeMessages(MSGID_PACKET_RECV);
            mWorkHandler.removeMessages(MSGID_UNPREPARE);
        }

        if (mMqttTransport != null) {
            mMqttTransport.removeAllMessages();
        }
    }

    @Override
    protected void processTaskFinsh() {
        if (mMqttTransport != null) {
            mMqttTransport.release();
        }
        Log.d(TAG, "<processTaskFinsh> done!");
    }


    ///////////////////////////////////////////////////////////////////////////
    //////////////// Methods for thread message handler ///////////////////////
    //////////////////////////////////////////////////////////////////////////
    /**
     * @brief 组件线程消息处理：Node节点激活
     */
    void onMessagePrepareNodeActive(Message msg) {
        int sdkState = getStateMachine();
        if (sdkState != PRESISTENTLINK_STATE_PREPARING) {
            Log.e(TAG, "<onMessagePrepareNodeActive> bad state, sdkState=" + sdkState);
            return;
        }
        removeMessage(MSGID_PREPARE_NODEACTIVE);

        // 激活节点
        PrepareParam prepareParam;
        synchronized (mDataLock) {
            prepareParam = mPrepareParam;
        }
        HttpTransport.NodeActiveResult result = HttpTransport.getInstance().nodeActive(prepareParam);
        if (result.mErrCode != ErrCode.XOK) {
            Log.e(TAG, "<onMessagePrepareNodeActive> fail to nodeActive, ret=" + result.mErrCode);
            sendSingleMessage(MSGID_PREPARE_INIT_DONE, result.mErrCode, 0, null, 0);
            return;
        }
        if (getStateMachine() != PRESISTENTLINK_STATE_PREPARING) {
            Log.e(TAG, "<onMessagePrepareNodeActive> bad state2, sdkState=" + getStateMachine());
            return;
        }

            // 更新本地节点信息
        synchronized (mDataLock) {
            mLocalNode.mReady = true;
            mLocalNode.mUserId = prepareParam.mUserId;
            mLocalNode.mNodeId = result.mNodeId;
            mLocalNode.mRegion = result.mNodeRegion;
            mLocalNode.mToken = result.mNodeToken;
        }

        // 设置 MQTT订阅和发布的主题
        mMqttTopicSub = "$falcon/callkit/" + result.mNodeId + "/sub";
        mMqttTopicPub = "$falcon/callkit/" + result.mNodeId + "/pub";


        //
        // 创建并初始化 Mqtt Transport
        //
        MqttTransport.InitParam mqttParam = new MqttTransport.InitParam();
        mqttParam.mContext = mInitParam.mContext;
        mqttParam.mThreadComp = this;
        mqttParam.mCallback = this;
        mqttParam.mServerUrl = String.format("tcp://%s:%d", result.mMqttServer, result.mMqttPort);
        mqttParam.mUserName = result.mMqttUserName;
        mqttParam.mPassword = result.mNodeId + "/" + result.mUserId;
        mqttParam.mClientId = result.mNodeId;

        mqttParam.mSubTopicArray = new String[1];
        mqttParam.mSubTopicArray[0] = mMqttTopicSub;
        mqttParam.mSubQosArray = new int[1];
        mqttParam.mSubQosArray[0] = 2;

        int ret = mMqttTransport.initialize(mqttParam);
        if (ret != ErrCode.XOK) {
            synchronized (mDataLock) {
                mLocalNode.mReady = false;
                mLocalNode.mUserId = null;
                mLocalNode.mNodeId = null;
            }
            mMqttTransport.release();
            Log.e(TAG, "<onMessagePrepareNodeActive> fail to mqtt init, ret=" + ret);
            sendSingleMessage(MSGID_PREPARE_INIT_DONE, ret, 0, null, 0);
            return;
        }

        // 发送一个超时延时的事件，这样如果没有 MQTT初始化回调回来，也能继续后续处理
        sendSingleMessage(MSGID_PREPARE_INIT_DONE, ErrCode.XERR_TIMEOUT, 0, null, MQTT_TIMEOUT);
        Log.d(TAG, "<onMessagePrepareNodeActive> done");
    }


    /**
     * @brief 组件线程消息处理：MQTT初始化完成
     */
    void onMessageInitDone(Message msg) {
        int sdkState = getStateMachine();
        if (sdkState != PRESISTENTLINK_STATE_PREPARING) {
            Log.e(TAG, "<onMessageInitDone> bad state, sdkState=" + sdkState);
            return;
        }
        removeMessage(MSGID_PREPARE_NODEACTIVE);
        removeMessage(MSGID_PREPARE_INIT_DONE);

        // 获取回调数据
        PrepareParam prepareParam;
        OnPrepareListener prepareListener;
        synchronized (mDataLock) {
            prepareParam = mPrepareParam;
            prepareListener = mPrepareListener;
        }

        if (msg.arg1 != ErrCode.XOK) {  // prepare() 操作有错误
            // 清除数据
            synchronized (mDataLock) {
                mLocalNode.mReady = false;
                mLocalNode.mNodeId = prepareParam.mUserId;
            }
            if (mMqttTransport != null) {
                mMqttTransport.release();
            }
            setStateMachine(PRESISTENTLINK_STATE_INITIALIZED);  // 设置状态机到初始化状态

        } else {
            setStateMachine(PRESISTENTLINK_STATE_RUNNING);  // 设置状态机到正常运行状态
        }

        Log.d(TAG, "<onMessageInitDone> done, errCode=" + msg.arg1);
        prepareListener.onSdkPrepareDone(prepareParam, msg.arg1);
    }

    /**
     * @brief 组件线程消息处理：MQTT状态发生变化
     */
    void onMessageMqttStateChanged(Message msg) {
        int sdkState = getStateMachine();
        if (sdkState == PRESISTENTLINK_STATE_UNPREPARING || sdkState == PRESISTENTLINK_STATE_INITIALIZED){
            Log.e(TAG, "<onMessageMqttStateChanged> bad state");
            return;
        }
        int newState = msg.arg1;
        Log.d(TAG, "<onMessageMqttStateChanged> newState=" + newState);

        boolean ready = false;
        if (newState == MqttTransport.MQTT_TRANS_STATE_CONNECTED) {
            ready = true;
            setStateMachine(PRESISTENTLINK_STATE_RUNNING);

        } else if (newState == MqttTransport.MQTT_TRANS_STATE_RECONNECTING) {
            setStateMachine(PRESISTENTLINK_STATE_RECONNECTING);
        }

        // 更新当前本地节点就绪状态
        synchronized (mDataLock) {
            mLocalNode.mReady = ready;
        }
    }

    /**
     * @brief 组件线程消息处理：发送数据包
     */
    void onMessagePacketSend(Message msg) {
        int sdkState = getStateMachine();
        if (sdkState != PRESISTENTLINK_STATE_RUNNING) {
            Log.e(TAG, "<onMessagePacketSend> bad state, sdkState=" + sdkState);
            return;
        }

        //
        // 将发送队列中的数据包依次发送出去
        //
        for (;;) {
            TransPacket sendingPkt = mSendPktQueue.dequeue();
            if (sendingPkt == null) {  // 发送队列为空，没有要发送的数据包了
                break;
            }

            mMqttTransport.sendPacket(sendingPkt);
        }

    }

    /**
     * @brief 组件线程消息处理：处理接收到的数据包
     */
    void onMessagePacketRecv(Message msg) {
        int sdkState = getStateMachine();
        if (sdkState != PRESISTENTLINK_STATE_RUNNING) {
            Log.e(TAG, "<onMessagePacketRecv> bad state, sdkState=" + sdkState);
            return;
        }

        //
        // 将发送队列中的数据包依次发送出去
        //
        for (;;) {
            TransPacket recvedPkt = mRecvPktQueue.dequeue();
            if (recvedPkt == null) {  // 接收队列为空，没有要接收到的数据包要分发了
                break;
            }

            // TODO: 根据数据包不同的topic进行分发处理，当前全部都是呼叫系统的数据包
            DoMqttParsePacket(recvedPkt);
        }
    }

    /**
     * @brief 组件线程消息处理：处理接收到的数据包
     */
    void DoMqttParsePacket(final TransPacket recvedPkt) {

        JSONObject recvJsonObj = JsonUtils.generateJsonObject(recvedPkt.mContent);
        if (recvJsonObj == null) {
            Log.e(TAG, "<DoMqttParsePacket> Invalid json=" + recvedPkt.mContent);
            return;
        }

        JSONObject headJsonObj = JsonUtils.parseJsonObject(recvJsonObj, "header", null);
        JSONObject payloadJsonObj = JsonUtils.parseJsonObject(recvJsonObj,"payload", null);
        int respCode = JsonUtils.parseJsonIntValue(recvJsonObj, "code", 0);

        // 解析 Header 头信息
        if (headJsonObj == null) {
            Log.e(TAG, "<DoMqttParsePacket> NO header json obj");
            return;
        }
        String headerMethod = JsonUtils.parseJsonStringValue(headJsonObj, "method", null);
        if (TextUtils.isEmpty(headerMethod)) {
            Log.e(TAG, "<DoMqttParsePacket> NO method field in header");
            return;
        }
        long traceId = JsonUtils.parseJsonLongValue(headJsonObj, "traceId", -1);
        if (traceId < 0) {
            Log.e(TAG, "<DoMqttParsePacket> NO traceId field in header");
            return;
        }

        if (headerMethod.compareToIgnoreCase(METHOD_USER_START_CALL) == 0) {
            // APP主叫设备回应数据包
            ConnectionCtx connectionCtx = mConnectMgr.findConnectionByTraceId(traceId);
            if (connectionCtx == null) {  // 主叫会话已经不存在了，丢弃该包
                Log.e(TAG, "<DoMqttParsePacket> [DIALING] connection NOT found, drop packet!");
                return;
            }

            if (respCode == ErrCode.XOK) {
                connectionCtx.mRtcToken = JsonUtils.parseJsonStringValue(payloadJsonObj, "token", null);
                connectionCtx.mChnlName = JsonUtils.parseJsonStringValue(payloadJsonObj, "cname", null);
                connectionCtx.mLocalRtcUid = JsonUtils.parseJsonIntValue(payloadJsonObj, "uid", -1);
            }

//            // 这里模拟获取RTM的token
//            HttpTransport.DevConnectRslt result = HttpTransport.getInstance().connectDevice(
//                    mInitParam.mAppId, connectionCtx.mUserId, connectionCtx.mDeviceId);
//            if (result.mErrCode != ErrCode.XOK) {
//                ALog.getInstance().e(TAG, "<DoMqttParsePacket> fail to request token");
//            } else {
//                connectionCtx.mRtmToken = result.mRtmToken;
//            }

            // 更新连接数据
            mConnectMgr.updateConnection(connectionCtx);

            // 回调给应用层
            if (connectionCtx.mConnectListener != null) {
                connectionCtx.mConnectListener.onDevReqConnectDone(respCode, connectionCtx.mConnectId,
                        connectionCtx.mDeviceId, connectionCtx.mLocalRtcUid,
                        connectionCtx.mChnlName, connectionCtx.mRtcToken, connectionCtx.mRtmToken);
            }

        } else if (headerMethod.compareToIgnoreCase(METHOD_DEVICE_START_CALL) == 0) {
            // 设备来电数据包
            if (respCode != ErrCode.XOK) {
                Log.d(TAG, "<DoMqttParsePacket> [INCOMING] response error, respCode=" + respCode);
                return;
            }

            // 判断参数字段，注意: 协议包中这里 peerNodeId 是 APP账号的 NodeId，不是设备的NodeId
            //  这里 peerNodeId 就用不到了，判断哪个设备直接用 cname 来判断
            String rtcToken = JsonUtils.parseJsonStringValue(payloadJsonObj, "token", null);
            String chnlName = JsonUtils.parseJsonStringValue(payloadJsonObj, "cname", null);
            String peerNodeId = JsonUtils.parseJsonStringValue(payloadJsonObj, "peerId", null);
            String attachMsg = JsonUtils.parseJsonStringValue(payloadJsonObj, "extraMsg", null);
            int localUid = JsonUtils.parseJsonIntValue(payloadJsonObj, "uid", -1);
            long timestamp = JsonUtils.parseJsonLongValue(payloadJsonObj, "timestamp", -1);
            long version = JsonUtils.parseJsonLongValue(payloadJsonObj, "version", -1);
            if (TextUtils.isEmpty(rtcToken) || TextUtils.isEmpty(chnlName) || TextUtils.isEmpty(peerNodeId)) {
                Log.d(TAG, "<DoMqttParsePacket> [INCOMING] invalid parameter, drop packet");
                return;
            }

//            // 通过频道名判断该设备是否已经在通话
//            SessionCtx sessionCtx = mConnectMgr.findSessionByChannelName(chnlName);
//            if (sessionCtx != null) {
//                Log.e(TAG, "<DoMqttParsePacket> [INCOMING] session already exist, drop packet!");
//                return;
//            }
//
//            // 处理MQTT来电消息
//            DoMqttEventIncoming(traceId, attachMsg, chnlName, rtcToken, localUid);
        }
    }


    /**
     * @brief 组件线程消息处理：unprepare操作
     */
    void onMessageUnprepare(Message msg) {
        Log.d(TAG, "<onMessageUnprepare> BEGIN");
        if (mMqttTransport != null) {
            mMqttTransport.release();
        }
        mRecvPktQueue.clear();
        mSendPktQueue.clear();
        Log.d(TAG, "<onMessageUnprepare> END");

        synchronized (mUnprepareEvent) {
            mUnprepareEvent.notify();    // 事件通知
        }
    }


    ///////////////////////////////////////////////////////////////////////////
    //////////// Methods for Override MqttTransport.ICallback ////////////////
    //////////////////////////////////////////////////////////////////////////
    @Override
    public void onMqttTransInitDone(int errCode) {
        int sdkState = getStateMachine();
        if (sdkState != PRESISTENTLINK_STATE_PREPARING) {
            Log.d(TAG, "<onMqttTransInitDone> bad state, sdkState=" + sdkState);
            return;
        }

        removeMessage(MSGID_PREPARE_INIT_DONE);  // 移除已有的超时消息
        sendSingleMessage(MSGID_PREPARE_INIT_DONE, errCode, 0, null, 0);
    }

    @Override
    public void onMqttTransStateChanged(int newState) {
        sendSingleMessage(MSGID_MQTT_STATE_CHANGED, newState, 0, null, 0);
    }

    @Override
    public void onMqttTransReceived(final TransPacket transPacket) {
        // 插入数据包到接收队列中
        mRecvPktQueue.inqueue(transPacket);

        // 消息通知组件线程进行接收包分发处理
        sendSingleMessage(MSGID_PACKET_RECV, 0, 0, null, 0);
    }

    @Override
    public void onMqttTransError(int errCode) {
        Log.e(TAG, "<onMqttTransError> errCode=" + errCode);
    }



}
