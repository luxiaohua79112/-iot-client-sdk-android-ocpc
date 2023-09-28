package io.agora.iotlink.rtmsdk;


import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IDevMediaMgr;
import io.agora.iotlink.logger.ALog;
import io.agora.iotlink.utils.JsonUtils;

/**
 * @brief 设备端媒体文件下载响应命令，返回下载失败的文件列表
 *
 */
public class RtmDownloadRspCmd extends RtmBaseCmd  {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/RtmDownloadRspCmd";



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    public ArrayList<IDevMediaMgr.DevFileDownloadResult> mDownloadList = new ArrayList<>();




    ///////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    @Override
    public String toString() {
        String infoText = "{ mSequenceId=" + mSequenceId
                + ", mDeviceId=" + mDeviceId
                + ", mCmdId=" + mCmdId
                + ", mDownloadList=" + mDownloadList
                + ", mIsRespCmd=" + mIsRespCmd
                + ", mErrCode=" + mErrCode + " }";
        return infoText;
    }



    ///////////////////////////////////////////////////////////////////////
    //////////////////// Override Methods of IRtmCmd //////////////////////
    ///////////////////////////////////////////////////////////////////////






}
