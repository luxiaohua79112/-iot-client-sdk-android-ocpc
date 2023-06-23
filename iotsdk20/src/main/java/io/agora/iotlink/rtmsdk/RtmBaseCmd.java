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
 * @brief 基本命令，可以是请求命令，也可以是响应命令，通过 mIsRespCmd 区分
 *        如果超时没有接收到响应数据，则返回超时
 */
public class RtmBaseCmd implements IRtmCmd  {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/RtmBaseCmd";



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    public long  mSequenceId;       ///< 序列号，request--response的序列号一一对应
    public String mDeviceId;        ///< 命令到达的设备Id
    public int mCmdId;              ///< 命令Id

    public long mSendTimestamp;     ///< 命令发送的时间戳，用于超时判断
    public OnRtmCmdRespListener mRespListener;  ///< 命令响应监听器

    public boolean mIsRespCmd;      ///< 是否是响应命令包，true
    public int mErrCode;            ///< 回应命令中：错误码





    ///////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    @Override
    public String toString() {
        String infoText = "{ mSequenceId=" + mSequenceId
                + ", mDeviceId=" + mDeviceId
                + ", mCmdId=" + mCmdId
                + ", mSendTimestamp=" + mSendTimestamp
                + ", mIsRespCmd=" + mIsRespCmd
                + ", mErrCode=" + mErrCode + " }";
        return infoText;
    }



    ///////////////////////////////////////////////////////////////////////
    //////////////////// Override Methods of IRtmCmd //////////////////////
    ///////////////////////////////////////////////////////////////////////
    @Override
    public long getSequenceId() {
        return mSequenceId;
    }

    @Override
    public int getCommandId() {
        return mCmdId;
    }

    @Override
    public String getDeviceId() {
        return mDeviceId;
    }

    @Override
    public long getSendTimestamp() {
        return mSendTimestamp;
    }

    @Override
    public OnRtmCmdRespListener getRespListener() {
        return mRespListener;
    }

    @Override
    public boolean isResponseCmd() {
        return mIsRespCmd;
    }

    @Override
    public byte[] getReqCmdDataBytes() {
        JSONObject body = new JSONObject();

        // body内容
        try {
            body.put("sequenceId", mSequenceId);
            body.put("commandId", mCmdId);

        } catch (JSONException jsonExp) {
            jsonExp.printStackTrace();
            ALog.getInstance().e(TAG, "<getReqCmdDataBytes> [EXP] jsonExp=" + jsonExp);
            return null;
        }

        String realBody = String.valueOf(body);
        byte[]  dataBytes = realBody.getBytes(StandardCharsets.UTF_8);
        return dataBytes;
    }

    @Override
    public int getRespErrCode() {
        return mErrCode;
    }



}
