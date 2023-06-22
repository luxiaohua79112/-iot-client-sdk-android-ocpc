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
 * @brief 云台控制请求命令
 */
public class RtmPlzCtrlReqCmd extends RtmBaseCmd  {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/RtmPlzCtrlReqCmd";



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    public int mAction;     ///< 动作命令：0-开始，1-停止
    public int mDirection;  ///< 方向：0-上、1-下、2-左、3-右、4-镜头拉近、5-镜头拉远
    public int mSpeed;      ///< 速度：0-慢，1-适中（默认），2-快


    ///////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    @Override
    public String toString() {
        String infotext = "{ mSequenceId=" + mSequenceId
                    + ", mDeviceId=" + mDeviceId
                    + ", mCmdId=" + mCmdId
                    + ", mAction=" + mAction
                    + ", mDirection=" + mDirection
                    + ", mSpeed=" + mSpeed
                    + ", mSendTimestamp=" + mSendTimestamp + " }";
        return infotext;
    }


    ///////////////////////////////////////////////////////////////////////
    //////////////////// Override Methods of IRtmCmd //////////////////////
    ///////////////////////////////////////////////////////////////////////
    @Override
    public byte[] getReqCmdDataBytes() {
        JSONObject bodyObj = new JSONObject();

        // body内容
        try {
            bodyObj.put("sequenceId", mSequenceId);
            bodyObj.put("commandId", mCmdId);

            JSONObject paramObj = new JSONObject();
            paramObj.put("action", mAction);
            paramObj.put("direction", mDirection);
            paramObj.put("speed", mSpeed);
            bodyObj.put("param", paramObj);

        } catch (JSONException jsonExp) {
            jsonExp.printStackTrace();
            ALog.getInstance().e(TAG, "<getReqCmdDataBytes> [EXP] jsonExp=" + jsonExp);
            return null;
        }

        String realBody = String.valueOf(bodyObj);
        byte[]  dataBytes = realBody.getBytes(StandardCharsets.UTF_8);
        return dataBytes;
    }


}
