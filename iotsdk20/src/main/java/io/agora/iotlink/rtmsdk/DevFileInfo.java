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
 * @brief 设备端文件信息
 *
 */
public class DevFileInfo  {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/DevFileInfo";


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    public int mFileType;           ///< 文件类型: 0--文件;  1--目录
    public String mFileId;          ///< 文件Id，是文件的唯一标识
    public String mImgUrl;          ///< 封面图片文件路径
    public String mVideoUrl;        ///< 视频文件URL
    public long mStartTime;         ///< 录像开始时间戳，单位：秒
    public long mStopTime;          ///< 录像结束时间戳，单位：秒
    public int mEvent;


    ///////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    @Override
    public String toString() {
        String infoText = "{ mFileType=" + mFileType
                + ", mFileId=" + mFileId
                + ", mImgUrl=" + mImgUrl
                + ", mVideoUrl=" + mVideoUrl
                + ", mStartTime=" + mStartTime
                + ", mStopTime=" + mStopTime
                + ", mEvent=" + mEvent + " }";
        return infoText;
    }

}
