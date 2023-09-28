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
 * @brief 设备端文件查询参数
 *
 */
public class DevFileQueryParam  {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/DevFileQueryParam";


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    public String mFileId;     ///< 媒体文件查询 文件Id，null标识 不传则返回根目录文件夹目录
    public long mBeginTime;    ///< 媒体文件查询 开始时间
    public long mEndTime;      ///< 媒体文件查询 结束时间


    ///////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    @Override
    public String toString() {
        String infoText = "{ mFileId=" + mFileId
                + ", mBeginTime=" + mBeginTime
                + ", mEndTime=" + mEndTime + " }";
        return infoText;
    }

}
