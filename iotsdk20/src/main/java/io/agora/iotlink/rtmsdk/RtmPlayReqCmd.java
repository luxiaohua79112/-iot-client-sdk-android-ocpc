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
 * @brief 播放请求命令
 */
public class RtmPlayReqCmd extends RtmBaseCmd  {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/RtmPlayReqCmd";



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    public long mGlobalStartTime;       ///< 全局的播放开始时间戳

    public String mFileId;              ///< 要播放的 fileId
    public long mOffset;                ///< 播放开始时间戳

    public int mRate;                   ///< 播放倍速


    ///////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    @Override
    public String toString() {
        String infoText = "{ mSequenceId=" + mSequenceId
                + ", mDeviceId=" + mDeviceId
                + ", mCmdId=" + mCmdId
                + ", mSendTimestamp=" + mSendTimestamp
                + ", mGlobalStartTime=" + mGlobalStartTime
                + ", mFileId=" + mFileId
                + ", mOffset=" + mOffset
                + ", mRate=" + mRate + " }";
        return infoText;
    }



    ///////////////////////////////////////////////////////////////////////
    //////////////////// Override Methods of IRtmCmd //////////////////////
    ///////////////////////////////////////////////////////////////////////

    @Override
    public String getReqCmdData() {
        JSONObject bodyObj = new JSONObject();

        // body内容
        try {
            bodyObj.put("sequenceId", mSequenceId);
            bodyObj.put("commandId", mCmdId);

            JSONObject paramObj = new JSONObject();
            if (!TextUtils.isEmpty(mFileId)) {
                paramObj.put("id", mFileId);
                paramObj.put("offset", mOffset);
            } else {
                paramObj.put("begin", mGlobalStartTime);
            }
            paramObj.put("rate", mRate);
            bodyObj.put("param", paramObj);

        } catch (JSONException jsonExp) {
            jsonExp.printStackTrace();
            ALog.getInstance().e(TAG, "<getReqCmdData> [EXP] jsonExp=" + jsonExp);
            return null;
        }

        String realBody = String.valueOf(bodyObj);
        return realBody;
    }





}
