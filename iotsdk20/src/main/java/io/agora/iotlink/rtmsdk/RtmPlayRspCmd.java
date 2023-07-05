package io.agora.iotlink.rtmsdk;


import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import io.agora.iotlink.ErrCode;
import io.agora.iotlink.logger.ALog;
import io.agora.iotlink.utils.JsonUtils;

/**
 * @brief 播放响应命令
 */
public class RtmPlayRspCmd extends RtmBaseCmd  {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/RtmPlayRspCmd";



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    public int mRtcUid;         ///< RTC uid
    public String mChnlName;    ///< 播放的频道名
    public String mRtcToken;    ///< token



    ///////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    @Override
    public String toString() {
        String infoText = "{ mSequenceId=" + mSequenceId
                + ", mDeviceId=" + mDeviceId
                + ", mCmdId=" + mCmdId
                + ", mRtcUid=" + mRtcUid
                + ", mChnlName=" + mChnlName
                + ", mRtcToken=" + mRtcToken
                + ", mIsRespCmd=" + mIsRespCmd
                + ", mErrCode=" + mErrCode + " }";
        return infoText;
    }



}
