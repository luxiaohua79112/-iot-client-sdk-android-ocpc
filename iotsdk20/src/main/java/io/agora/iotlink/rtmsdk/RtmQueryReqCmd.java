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
 * @brief 设备端媒体文件查询请求命令
 *
 */
public class RtmQueryReqCmd extends RtmBaseCmd  {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/RtmQueryReqCmd";



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    public DevFileQueryParam mQueryParam = new DevFileQueryParam();   ///< 设备端媒体文件查询参数



    ///////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    @Override
    public String toString() {
        String infoText = "{ mSequenceId=" + mSequenceId
                + ", mDeviceId=" + mDeviceId
                + ", mCmdId=" + mCmdId
                + ", mSendTimestamp=" + mSendTimestamp
                + ", mFileId=" + mQueryParam.mFileId
                + ", mBeginTime=" + mQueryParam.mBeginTime
                + ", mEndTime=" + mQueryParam.mEndTime
                + ", mPageIndex=" + mQueryParam.mPageIndex
                + ", mPageSize=" + mQueryParam.mPageSize + " }";
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

            if (!TextUtils.isEmpty(mQueryParam.mFileId)) {
                paramObj.put("fileId", mQueryParam.mFileId);
            }
            if (mQueryParam.mBeginTime >= 0) {
                paramObj.put("beginTime", mQueryParam.mBeginTime);
            }
            paramObj.put("endTime", mQueryParam.mEndTime);
            paramObj.put("pageIndex", mQueryParam.mPageIndex);
            paramObj.put("pageSize", mQueryParam.mPageSize);
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
