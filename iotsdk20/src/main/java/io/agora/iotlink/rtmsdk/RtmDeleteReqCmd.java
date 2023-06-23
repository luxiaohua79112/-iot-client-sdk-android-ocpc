package io.agora.iotlink.rtmsdk;


import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.agora.iotlink.ErrCode;
import io.agora.iotlink.logger.ALog;
import io.agora.iotlink.utils.JsonUtils;

/**
 * @brief 设备端媒体文件删除请求命令
 *
 */
public class RtmDeleteReqCmd extends RtmBaseCmd  {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/RtmDeleteReqCmd";



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    public ArrayList<String> mFileIdList = new ArrayList<>();   ///< 要删除的 fileId列表




    ///////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    @Override
    public String toString() {
        String infoText = "{ mSequenceId=" + mSequenceId
                + ", mDeviceId=" + mDeviceId
                + ", mCmdId=" + mCmdId
                + ", mSendTimestamp=" + mSendTimestamp
                + ", mFileIdList=" + mFileIdList
                + ", mIsRespCmd=" + mIsRespCmd
                + ", mErrCode=" + mErrCode + " }";
        return infoText;
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
            JSONArray idArrayObj = new JSONArray();
            for (int i = 0; i < mFileIdList.size(); i++) {
                String fileId = mFileIdList.get(i);
                idArrayObj.put(i, fileId);
            }
            paramObj.put("fileIdList", idArrayObj);
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
