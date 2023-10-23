/**
 * @file IAgoraIotAppSdk.java
 * @brief This file define the SDK interface for Agora Iot AppSdk 2.0
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2022-01-26
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink;


import android.content.Context;
import android.os.Bundle;

import java.util.List;
import java.util.UUID;

import io.agora.iotlink.callkit.SessionCtx;



/*
 * @brief 设备会话管理接口
 */

public interface IDeviceSessionMgr  {

    /**
     * @brief SDK初始化参数
     */
    public static class InitParam {
        public Context mContext;
        public String mAppId;                       ///< 项目的 AppId
        public String mProjectID;                   ///< 申请的项目Id
        public String mUserId;                      ///< 对应账号Id
        public String mLogFilePath;                 ///< 日志文件路径
    }



    /**
     * @brief 会话回调接口
     */
    public static interface ISessionCallback {

        /**
         * @brief 设备连接连接完成回调事件
         * @param sessionId : 会话唯一标识
         * @param connectParam : 设备连接参数
         * @param errCode : 0表示设备连接成功；否则表示连接结果错误码
         */
        default void onSessionConnectDone(final UUID sessionId, final ConnectParam connectParam,
                                      int errCode) { }

        /**
         * @brief 设备断开连接回调事件，设备断开连接后，所有的预览和控制都不能进行，只能再次重连
         * @param sessionId : 会话唯一标识
         */
        default void onSessionDisconnected(final UUID sessionId) { }

        /**
         * @brief 有其他用户上线进入通话事件 (非当前用户)
         * @param onlineUserCount : 当前在线的总用户数量
         */
        default void onSessionOtherUserOnline(final UUID sessionId, int onlineUserCount) {}

        /**
         * @brief 有其他用户退出进入通话事件 （非当前用户）
         * @param onlineUserCount : 当前仍然在线的总用户数量
         */
        default void onSessionOtherUserOffline(final UUID sessionId, int onlineUserCount) {}

        /**
         * @brief 会话的Token过期，此时应用层需要重新请求token信息，然后调用 renewToken()方法
         * @param sessionId : 会话唯一标识
         */
        default void onSessionTokenWillExpire(final UUID sessionId) {}

        /**
         * @brief 会话错误回调事件，产生该错误后只能断开连接，重新连接设备
         * @param sessionId : 会话唯一标识
         * @param errCode : 错误代码
         */
        default void onSessionError(final UUID sessionId, int errCode) {}

    }


    /**
     * @brief 设备连接参数
     */
    public static class ConnectParam {
        public String mPeerDevId;           ///< 要连接设备的 DeviceId
        public int mLocalRtcUid;            ///< 本地 RTC uid
        public String mChannelName;         ///< 要会话的RTC频道名
        public String mRtcToken;            ///< 要会话的RTC Token
        public String mRtmUid;              ///< 要会话的 RTM uid
        public String mRtmToken;            ///< 要会话的 RTM Token

        @Override
        public String toString() {
            String infoText = "{ mPeerDevId=" + mPeerDevId
                    + ", mLocalRtcUid=" + mLocalRtcUid
                    + ", mChannelName=" + mChannelName
                    + ",\n mRtcToken=" + mRtcToken
                    + ",\n mRtmUid=" + mRtmUid
                    + ",\n mRtmToken=" + mRtmToken + " }";
            return infoText;
        }
    }

    /**
     * @brief 连接返回
     */
    public static class ConnectResult {
        public UUID mSessionId;         ///< 连接时，分配的唯一的 sessionId
        public int mErrCode;            ///< 处理结果错误码
    }


    /**
     * @brief 设备Token的Renew参数
     */
    public static class TokenRenewParam {
        public String mRtcToken;            ///< 要会话的RTC Token
        public String mRtmToken;            ///< 要会话的 RTM Token

        @Override
        public String toString() {
            String infoText = "{ mRtcToken=" + mRtcToken
                    + ",\n mRtmToken=" + mRtmToken + " }";
            return infoText;
        }
    }

    /**
     * @brief 会话类型
     */
    public static final int SESSION_TYPE_UNKNOWN = 0x0000;           ///< 会话类型：未知
    public static final int SESSION_TYPE_DIAL = 0x0001;              ///< 会话类型：主叫
    public static final int SESSION_TYPE_INCOMING = 0x0002;          ///< 会话类型：来电被叫
    public static final int SESSION_TYPE_PLAYBACK = 0x0003;          ///< 会话类型：媒体回放

    /**
     * @brief 会话状态机
     */
    public static final int SESSION_STATE_DISCONNECTED = 0x0000;     ///< 还没有连接到设备
    public static final int SESSION_STATE_CONNECTING = 0x0001;       ///< 正在连接到设备
    public static final int SESSION_STATE_CONNECTED = 0x0002;         ///< 已经连接到设备


    /**
     * @brief 会话信息
     */
    public static class SessionInfo {
        public UUID mSessionId;         ///< 会话的唯一标识
        public String mPeerDevId;       ///< 对端设备的 DeviceId
        public int mLocalRtcUid;        ///< 本地 RTC uid
        public String mChannelName;     ///< 要会话的RTC频道名
        public String mRtcToken;        ///< 要会话的RTC Token

        public String mRtmUid;          ///< 本地 RTM uid
        public String mRtmToken;        ///< 要会话的 RTM Token
        public int mState;              ///< 会话的状态机

        public String mAttachMsg;       ///< 呼叫或者来电时的附带消息
        public int mType;               ///< 会话类型
        public int mUserCount;          ///< 在线的用户数量，默认至少有一个用户

        @Override
        public String toString() {
            String infoText = "{ mSessionId=" + mSessionId
                    + ", mPeerDevId=" + mPeerDevId
                    + ", mLocalRtcUid=" + mLocalRtcUid
                    + ", mChannelName=" + mChannelName
                    + ", mState=" + mState
                    + ", mAttachMsg=" + mAttachMsg
                    + ", mType=" + mType
                    + ", mUserCount=" + mUserCount
                    + ", mRtmUid=" + mRtmUid
                    + ",\n mRtcToken=" + mRtcToken
                    + ",\n mRtmToken=" + mRtmToken + " }";

            return infoText;
        }
    }

    ////////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /**
     * @brief 初始化Sdk,初始化成功后，通过listener异步返回
     * @param initParam : 初始化参数
     * @return 返回错误码
     */
    int initialize(final InitParam initParam);

    /**
     * @brief 释放SDK所有资源，所有的组件模块也会被释放
     */
    void release();



    /**
     * @brief 获取当前所有的会话列表
     * @return 返回当前活动的会话列表
     */
    List<SessionInfo> getSessionList();

    /**
     * @brief 连接设备，每次连接设备会产生一个会话，并且自动分配sessionId，作为连接的唯一标识
     * @param connectParam : 设备连接参数
     * @param sessionCallback : 设备回调
     */
    ConnectResult connect(final ConnectParam connectParam,
                          final ISessionCallback sessionCallback);


    /**
     * @brief 断开设备连接，同步调用，会断开设备所有的连接并且停止所有的预览、控制处理等
     * @param sessionId : 设备连接会话Id
     * @return 返回错误码
     */
    int disconnect(final UUID sessionId);

    /**
     * @brief 根据 sessionId 获取会话状态信息
     * @param sessionId : 会话唯一标识
     * @return 返回会话信息，如果没有查询到会话，则返回null
     */
    SessionInfo getSessionInfo(final UUID sessionId);

    /**
     * @brief 对会话进行 renewToken操作
     * @param sessionId : 设备连接会话Id
     * @param renewParam : token参数
     * @return 返回错误码
     */
    int renewToken(final UUID sessionId, final TokenRenewParam renewParam);

    /**
     * @brief 获取设备预览的组件接口
     * @param sessionId : 会话唯一标识
     * @return 返回该会话的预览控制接口
     */
    IDevPreviewMgr getDevPreviewMgr(final UUID sessionId);

    /**
     * @brief 获取设备媒体文件管理器组件接口
     */
    IDevMediaMgr getDevMediaMgr(final UUID sessionId);

    /**
     * @brief 获取设备控制器组件接口
     */
    IDevController getDevController(final UUID sessionId);

}
