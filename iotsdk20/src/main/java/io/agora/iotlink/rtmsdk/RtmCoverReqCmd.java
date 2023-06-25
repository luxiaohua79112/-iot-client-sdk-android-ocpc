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
 * @brief 获取视频封面请求命令
 *
 */
public class RtmCoverReqCmd extends RtmBaseCmd  {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/RtmCoverReqCmd";



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    public String mImgUrl;            ///< 要查询封面的图片文件路径




    ///////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    @Override
    public String toString() {
        String infoText = "{ mSequenceId=" + mSequenceId
                + ", mDeviceId=" + mDeviceId
                + ", mCmdId=" + mCmdId
                + ", mSendTimestamp=" + mSendTimestamp
                + ", mImgUrl=" + mImgUrl + " }";
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
            paramObj.put("imgUrl", mImgUrl);
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
