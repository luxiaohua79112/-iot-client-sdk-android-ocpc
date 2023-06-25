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
 * @brief 上层定制化请求命令
 *
 */
public class RtmCustomizeReqCmd extends RtmBaseCmd  {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/RtmCustomizeReqCmd";



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    public String mSendData;            ///< 定制化发送的数据




    ///////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    @Override
    public String toString() {
        String infoText = "{ mSequenceId=" + mSequenceId
                + ", mDeviceId=" + mDeviceId
                + ", mCmdId=" + mCmdId
                + ", mSendTimestamp=" + mSendTimestamp
                + ", mSendData=" + mSendData + " }";
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
            paramObj.put("sendData", mSendData);
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
