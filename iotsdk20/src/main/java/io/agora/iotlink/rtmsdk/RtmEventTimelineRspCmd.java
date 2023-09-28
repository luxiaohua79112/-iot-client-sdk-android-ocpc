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
 * @brief 设备端事件分布查询响应命令，返回查询到的事件视频时刻点列表
 *
 */
public class RtmEventTimelineRspCmd extends RtmBaseCmd  {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/RtmEventRspCmd";



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    public ArrayList<Long> mVideoTimeList = new ArrayList<>();



    ///////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    @Override
    public String toString() {
        String infoText = "{ mSequenceId=" + mSequenceId
                + ", mDeviceId=" + mDeviceId
                + ", mCmdId=" + mCmdId
                + ", mVideoTimeList=" + mVideoTimeList
                + ", mIsRespCmd=" + mIsRespCmd
                + ", mErrCode=" + mErrCode + " }";
        return infoText;
    }



    ///////////////////////////////////////////////////////////////////////
    //////////////////// Override Methods of IRtmCmd //////////////////////
    ///////////////////////////////////////////////////////////////////////





}
