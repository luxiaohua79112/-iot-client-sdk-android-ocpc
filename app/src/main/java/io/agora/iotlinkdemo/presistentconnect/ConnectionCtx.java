package io.agora.iotlinkdemo.presistentconnect;


import java.util.UUID;



/**
 * @brief 连接的上下文信息
 */
public class ConnectionCtx  {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    public UUID mConnectId;         ///< 连接Id，是会话的唯一标识
    public long mTraceId;
    public String mUserId;          ///< 本地的 NodeId
    public String mDeviceId;        ///< 设备的 NodeId
    public int mLocalRtcUid;        ///< 本地 RTC uid
    public int mDeviceRtcUid;       ///< 设备端的 Rtc Uid
    public String mChnlName;        ///< 频道名
    public String mRtcToken;        ///< 分配的RTC token
    public String mRtmToken;        ///< 要会话的 RTM Token

    public String mAttachMsg;       ///< 呼叫或者来电时的附带消息

    public PresistentLinkComp.OnDevReqConnectListener mConnectListener;




    ///////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////

    @Override
    public String toString() {
        String infoText = "{ mConnectId=" + mConnectId
                + ", mUserId=" + mUserId
                + ", mDeviceId=" + mDeviceId
                + ", mLocalRtcUid=" + mLocalRtcUid
                + ", mDeviceRtcUid=" + mDeviceRtcUid
                + ", mChnlName=" + mChnlName
                + ", mAttachMsg=" + mAttachMsg
                + ",\n mRtcToken=" + mRtcToken
                + ",\n mRtmToken=" + mRtmToken + " }";
        return infoText;
    }
}
