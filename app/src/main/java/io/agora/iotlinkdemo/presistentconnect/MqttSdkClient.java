package io.agora.iotlinkdemo.presistentconnect;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;


import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.android.service.MqttTraceHandler;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;

import io.agora.iotlink.ErrCode;
import io.agora.iotlink.base.BaseEvent;



public class MqttSdkClient {

    /**
     * @brief MQTT回调接口
     */
    public static interface ICallback {
        /**
         * @brief 连接服务器完成事件
         * @param errCode : 错误码
         */
        default void onMqttConnectDone(int errCode) {}

        /**
         * @brief 订阅完成事件
         * @param errCode : 错误码
         */
        default void onMqttSubscribeDone(int errCode) {}

        /**
         * @brief 消息到来事件
         * @param transPacket : 消息数据
         */
        default void onMqttMsgReceived(final TransPacket transPacket) {}

        /**
         * @brief MQTT链接断开
         * @param cause : 断开原因
         */
        default void onMqttConnectionLost(final String cause) {}
    }

    /**
     * @brief MQTT 初始化参数
     */
    public static class InitParam {
        public Context mContext;
        public ICallback mCallback;
        public String mServerUrl;                   ///< 服务器地址
        public String mUserName;
        public String mPassword;
        public String mClientId;

        @Override
        public String toString() {
            String infoText = "{ mServerUrl=" + mServerUrl
                    + ", mUserName=" + mUserName
                    + ", mPassword=" + mPassword
                    + ", mClientId=" + mClientId + " }";
            return infoText;
        }
    }


    ////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////
    ////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/MQTTTSDK";
    protected static final long EVENT_WAIT_TIMEOUT = 5000;    ///< 事件等待5秒
    protected static final long DISCONNECT_TIMEOUT = 2000;


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private final Object mDataLock = new Object();
    private final BaseEvent mUnsubsribeEvent = new BaseEvent();
    private final BaseEvent mDisonnectEvent = new BaseEvent();

    private InitParam mInitParam;
    private MqttAndroidClient mMqttClient = null;
    private IMqttToken mMqttToken = null;
    private volatile boolean mSubscribed = false;           ///< 主题是否订阅成功了




    ////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////// Methods of Public /////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////
    /**
     * @brief 初始化MQTT，连接到服务器
     * @param initParam : 初始化参数
     * @return 返回错误码
     */
    public int initialize(final InitParam initParam) {
        if ((initParam.mCallback == null) || TextUtils.isEmpty(initParam.mServerUrl)) {
            Log.d(TAG, "<initialize> invalid parameter!");
            return ErrCode.XERR_INVALID_PARAM;
        }

        mInitParam = initParam;
        mMqttClient = new MqttAndroidClient(mInitParam.mContext, mInitParam.mServerUrl, mInitParam.mClientId);
        mMqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Log.d(TAG, "<initialize.connectionLost> cause=" + cause);
                setSubscribed(false);       // 断开连接后，一定要重新订阅主题
                if (cause != null) {
                    mInitParam.mCallback.onMqttConnectionLost(cause.toString());
                } else {
                    mInitParam.mCallback.onMqttConnectionLost("None");
                }
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                Log.d(TAG, "<initialize.messageArrived> topic=" + topic
                        + ", message=" + message.toString());

                TransPacket transPacket = new TransPacket();
                transPacket.mTopic = topic;
                transPacket.mMessageId = message.getId();
                transPacket.mContent = message.toString();
                mInitParam.mCallback.onMqttMsgReceived(transPacket);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                try {
                    MqttMessage mqttMessage = token.getMessage();
                    Log.d(TAG, "<initialize.deliveryComplete> message=" + mqttMessage.toString());

                } catch (MqttException mqttExp) {
                    mqttExp.printStackTrace();
                    Log.e(TAG, "<initialize.deliveryComplete> exp=" + mqttExp.toString());
                }
            }
        });

        mMqttClient.setTraceEnabled(true);
        mMqttClient.setTraceCallback(new MqttTraceHandler() {
            @Override
            public void traceDebug(String tag, String message) {
                Log.d(TAG, "[" + tag + "] " + message);
            }

            @Override
            public void traceError(String tag, String message) {
                Log.e(TAG, "[" + tag + "] " + message);
            }

            @Override
            public void traceException(String tag, String message, Exception e) {
                Log.e(TAG, "[" + tag + "] " + message);
            }
        });



        char[] pswd_bytes = mInitParam.mPassword.toCharArray();
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(mInitParam.mUserName);
        options.setPassword(pswd_bytes);
        options.setAutomaticReconnect(true);        // 配置自动重连
        options.setCleanSession(true);              // 配置每次连接都是新的，不需要旧数据
        try {
            mMqttToken = mMqttClient.connect(options, mInitParam.mContext, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "<initialize.connect.onSuccess>");
                    mInitParam.mCallback.onMqttConnectDone(ErrCode.XOK);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d(TAG, "<initialize.connect.onFailure> exception=" + exception.toString());
                    mInitParam.mCallback.onMqttConnectDone(ErrCode.XERR_NETWORK);
                }
            });

        } catch (MqttException mqttExp) {
            mqttExp.printStackTrace();
            Log.d(TAG, "<initialize.connect> connect, exp=" + mqttExp.toString());
            return ErrCode.XERR_SERVICE;
        }

        Log.d(TAG, "<initialize> done, mInitParam=" + mInitParam.toString());
        return ErrCode.XOK;
    }


    /**
     * @brief 同步释放MQTT客户端，阻塞等待
     * @return 返回错误码
     */
    public int release() {
        if (mMqttClient == null) {
            return ErrCode.XOK;
        }
        if (!isConnected()) {
            mMqttClient.close();
            mMqttClient = null;
            Log.d(TAG, "<release> done, already disconnected!");
            return ErrCode.XOK;
        }


        try {
//            mMqttClient.disconnect(mInitParam.mContext, new IMqttActionListener() {
//                @Override
//                public void onSuccess(IMqttToken asyncActionToken) {
//                    ALog.getInstance().d(TAG, "<release.disconnect.onSuccess>");
//                    mDisonnectEvent.setEvent(ErrCode.XOK);
//                }
//
//                @Override
//                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
//                    ALog.getInstance().d(TAG, "<release.disconnect.onFailure> exception=" + exception.toString());
//                    mDisonnectEvent.setEvent(ErrCode.XERR_NETWORK);
//                }
//            });
//
//            mDisonnectEvent.waitEvent(EVENT_WAIT_TIMEOUT);

            mMqttClient.disconnect(DISCONNECT_TIMEOUT);

        } catch (MqttException mqttExp) {
            mqttExp.printStackTrace();
            Log.e(TAG, "<release> disconnect EXCEPTION, exp=" + mqttExp.toString());
        }

        // 关闭客户端并且完全释放
        //mMqttClient.close();
        mMqttClient = null;
        setSubscribed(false);

        int errCode = ErrCode.XOK; // mDisonnectEvent.getAttachValue();
        Log.d(TAG, "<release> done, errCode=" + errCode);
        return errCode;
    }


    /**
     * @brief MQTT订阅消息
     */
    public int subscribe(final String[] topicArray, final int[] qosArray) {
        if (mMqttClient == null) {
            Log.e(TAG, "<subscribe> bad state, mqtt already released");
            return ErrCode.XERR_BAD_STATE;
        }
        if (!isConnected()) {
            Log.e(TAG, "<subscribe> bad state, mqtt disconnected");
            return ErrCode.XERR_BAD_STATE;
        }

        try {
            mMqttClient.subscribe(topicArray, qosArray, mInitParam.mContext, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "<subscribe.onSuccess> ");
                    setSubscribed(true);
                    mInitParam.mCallback.onMqttSubscribeDone(ErrCode.XOK);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "<subscribe.onFailure> exception=" + exception.toString());
                    setSubscribed(false);
                    mInitParam.mCallback.onMqttSubscribeDone(ErrCode.XERR_NETWORK);
                }
            });


        } catch (MqttException mqttExp) {
            mqttExp.printStackTrace();
            Log.e(TAG, "<subscribe> exp=" + mqttExp.toString());
            return ErrCode.XERR_SERVICE;
        }

        Log.d(TAG, "<subscribe> done");
        return ErrCode.XOK;
    }

    /**
     * @brief 同步取消MQTT的消息订阅，阻塞等待
     */
    public int unsubscribe(final String[] topicArray) {
        if (mMqttClient == null) {
            Log.e(TAG, "<unsubscribe> bad state, mqtt already released");
            return ErrCode.XERR_BAD_STATE;
        }
        if (!isConnected()) {
            Log.e(TAG, "<unsubscribe> bad state, mqtt disconnected");
            return ErrCode.XERR_BAD_STATE;
        }

        try {
            mMqttClient.unsubscribe(topicArray, mInitParam.mContext, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "<unsubscribe.onSuccess> ");
                    setSubscribed(false);
                    mUnsubsribeEvent.setEvent(ErrCode.XOK);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "<unsubscribe.onFailure> exception=" + exception);
                    mUnsubsribeEvent.setEvent(ErrCode.XERR_NETWORK);
                }
            });

            mUnsubsribeEvent.waitEvent(EVENT_WAIT_TIMEOUT);

        } catch (MqttException mqttExp) {
            mqttExp.printStackTrace();
            Log.e(TAG, "<unsubscribe> [EXCEPTION] exp=" + mqttExp.toString());
            return ErrCode.XERR_SERVICE;
        }

        int errCode = mUnsubsribeEvent.getAttachValue();
        Log.d(TAG, "<unsubscribe> done, errCode=" + errCode);
        return errCode;
    }


    /**
     * @brief MQTT发送数据包
     * @param sendingPkt : 要发送的数据包
     */
    public int sendPacket(final TransPacket sendingPkt) {
        if (mMqttClient == null) {
            Log.e(TAG, "<sendPacket> bad state, mqtt already released");
            return ErrCode.XERR_BAD_STATE;
        }
        if (!isConnected()) {
            Log.e(TAG, "<sendPacket> bad state, mqtt disconnected");
            return ErrCode.XERR_BAD_STATE;
        }

        try {
            MqttMessage mqttMessage = new MqttMessage(sendingPkt.mContent.getBytes(StandardCharsets.UTF_8));
            mMqttClient.publish(sendingPkt.mTopic, mqttMessage, mInitParam.mContext, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "<sendPacket.onSuccess> ");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "<sendPacket.onFailure> exception=" + exception);
                }
            });

        } catch (MqttException mqttExp) {
            mqttExp.printStackTrace();
            Log.e(TAG, "<sendPacket> [EXCEPTION] exp=" + mqttExp.toString());
            return ErrCode.XERR_SERVICE;
        }

        Log.d(TAG, "<sendPacket> done, topic=" + sendingPkt.mTopic
                    + ", content=" + sendingPkt.mContent);
        return ErrCode.XOK;
    }

    /**
     * @brief 返回当前是否已经连接
     */
    public boolean isConnected() {
        if (mMqttClient == null) {
            return false;
        }

        boolean connected = mMqttClient.isConnected();
        return connected;
    }

    /**
     * @brief 设置主题是否已经订阅
     */
    void setSubscribed(boolean subed) {
        synchronized (mDataLock) {
            mSubscribed = subed;
        }
    }

    /**
     * @brief 返回主题是否已经订阅
     */
    boolean isSubscribed() {
        synchronized (mDataLock) {
            return mSubscribed;
        }
    }

}