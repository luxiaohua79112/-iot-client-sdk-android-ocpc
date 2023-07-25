package io.agora.iotlink.callkit;

import android.view.View;

import java.util.UUID;

import io.agora.iotlink.IDevPreviewMgr;
import io.agora.iotlink.IDeviceSessionMgr;
import io.agora.iotlink.sdkimpl.DevController;
import io.agora.iotlink.sdkimpl.DevMediaMgr;
import io.agora.iotlink.sdkimpl.DevPreviewMgr;


/**
 * @brief 记录通话相关信息
 */
public class SessionCtx  {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    public UUID mSessionId;         ///< 会话Id，是会话的唯一标识
    public String mUserId;          ///< 本地的 NodeId
    public String mDeviceId;        ///< 设备的 NodeId
    public int mLocalRtcUid;        ///< 本地 RTC uid
    public int mDeviceRtcUid;       ///< 设备端的 Rtc Uid
    public String mChnlName;        ///< 频道名
    public String mRtcToken;        ///< 分配的RTC token
    public String mRtmToken;        ///< 要会话的 RTM Token

    public int mState;              ///< 会话状态机
    public long mConnectTimestamp;  ///< 开始连接的时间戳
    public String mAttachMsg;       ///< 呼叫或者来电时的附带消息
    public int mType;               ///< 会话类型
    public int mUserCount;          ///< 在线的用户数量，默认至少有一个用户

    public DevPreviewMgr mDevPreviewMgr;    ///< 预览管理器
    public DevController mDevController;    ///< 设备信令控制器
    public DevMediaMgr mDevMediaMgr;        ///< 设备媒体管理器

    public IDeviceSessionMgr.ISessionCallback mSeesionCallback;       ///< 会话相关的回调
    public IDevPreviewMgr.OnPreviewListener mPreviewListener;
    public IDevPreviewMgr.OnCaptureFrameListener mCaptureListener;

    public boolean mPubLocalAudio;  ///< 是否推送本地音频流
    public boolean mSubDevAudio;    ///< 当前是否订阅设备端音频流
    public boolean mSubDevVideo;    ///< 当前是否订阅设备端视频流


    ///////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////

    @Override
    public String toString() {
        String infoText = "{ mSessionId=" + mSessionId
                + ", mUserId=" + mUserId
                + ", mDeviceId=" + mDeviceId
                + ", mLocalRtcUid=" + mLocalRtcUid
                + ", mDeviceRtcUid=" + mDeviceRtcUid
                + ", mChnlName=" + mChnlName
                + ", mAttachMsg=" + mAttachMsg
                + ", mType=" + mType
                + ", mUserCount=" + mUserCount
                + ", mPubLocalAudio=" + mPubLocalAudio
                + ", mSubDevVideo=" + mSubDevVideo
                + ", mSubDevAudio=" + mSubDevAudio
                + ",\n mRtcToken=" + mRtcToken
                + ",\n mRtmToken=" + mRtmToken + " }";
        return infoText;
    }
}
