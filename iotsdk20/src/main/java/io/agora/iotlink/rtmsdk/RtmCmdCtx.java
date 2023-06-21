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
 * @brief RTM命令上下文信息，每个 RTM命令都是由  request--response 构成
 *        有命令请求，就有对应的响应，如果超时没有接收到响应数据，则返回超时
 */
public class RtmCmdCtx  {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/RtmCmdCtx";

    /**
     * @brief 定义所有的RTM请求命令
     */
    private static final int CMDID_PTZ_CTRL = 1001;         ///< 云台控制命令
    private static final int CMDID_PTZ_RESET = 1002;        ///< 云台校准命令
    private static final int CMDID_SDCARD_FMT = 2001;       ///< SD卡格式化
    private static final int CMDID_MEDIA_QUERY = 2002;      ///< 查询存储卡视频文件列表
    private static final int CMDID_MEDIA_COVER = 2003;      ///< 查询视频文件封面图片
    private static final int CMDID_MEDIA_DELETE = 2004;     ///< 删除媒体文件
    private static final int CMDID_MEDIA_PLAY_TIMELINE = 2005; ///< 根据开始时间播放SD卡视频
    private static final int CMDID_MEDIA_PLAY_ID = 2006;    ///< 根据FileId播放单个SDK视频
    private static final int CMDID_MEDIA_STOP = 2007;       ///< 停止当前回看
    private static final int CMDID_MEDIA_RATE = 2008;       ///< 设置回看播放的倍速
    private static final int CMDID_CUSTOMIZE_SEND = 3001;   ///< 定制化命令



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    public static long mGlobalSeqId = 1;

    public long  mSequenceId;       ///< 序列号，request--response的序列号一一对应
    public String mDeviceId;        ///< 命令到达的设备Id
    public int mCmdId;              ///< 命令字符串
    public long mSendTimestamp;     ///< 命令发送的时间戳，用于超时判断
    public boolean mIsRespCmd;      ///< 是否是响应命令包，true

    ////////////////////////////////////
    /////////// 请求命令中参数 ////////////
    ////////////////////////////////////
    public String mParam;           ///< 请求命令中：参数数据

    public int mPtzCtrlAction;      ///< 云台控制动作
    public int mPtzCtrlDirection;   ///< 云台控制方向
    public int mPtzCtrlSpeed;       ///< 云台控制速度

    public String mQueryFileId;     ///< 媒体文件查询 文件Id
    public long mQueryBeginTime;    ///< 媒体文件查询 开始时间
    public long mQueryEndTime;      ///< 媒体文件查询 结束时间
    public int mQueryPageIndex;     ///< 媒体文件查询 页面索引，从1开始
    public int mQueryPageSize;      ///< 媒体文件查询 页面最多记录数



    ////////////////////////////////////
    /////////// 回应命令中数据 ////////////
    ////////////////////////////////////
    public int mErrCode;            ///< 回应命令中：错误码
    public String mRespData;        ///< 响应数据包





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
                + ", mParam=" + mParam
                + ", mErrCode=" + mErrCode
                + ", mRespData=" + mRespData + " }";
        return infoText;
    }

    public RtmCmdCtx() {
        synchronized (RtmCmdCtx.class) {
            mSequenceId = mGlobalSeqId;
            mGlobalSeqId++;
        }
    }

    /**
     * @brief 将请求命令组成JSON字符串，转换成字节流返回
     */
    public byte[] getReqCmdDataBytes() {
        JSONObject body = new JSONObject();

        // body内容
        try {
            body.put("sequenceId", mSequenceId);
            body.put("commandId", mCmdId);
            body.put("param", mParam);

        } catch (JSONException jsonExp) {
            jsonExp.printStackTrace();
            ALog.getInstance().e(TAG, "<getReqCmdDataBytes> [EXP] jsonExp=" + jsonExp);
            return null;
        }

        String realBody = String.valueOf(body);
        byte[]  dataBytes = realBody.getBytes(StandardCharsets.UTF_8);
        return dataBytes;
    }

    /**
     * @brief 将回应字节流数据转换到JSON字符串，并最终解析成命令对象
     */
    public static RtmCmdCtx parseRspCmdDataBytes(final byte[] data) {
        String jsonText = String.valueOf(data);
        if (TextUtils.isEmpty(jsonText)) {
            ALog.getInstance().e(TAG, "<parseRspCmdDataBytes> fail to convert data bytes!");
            return null;
        }

        JSONObject recvJsonObj = JsonUtils.generateJsonObject(jsonText);
        if (recvJsonObj == null) {
            ALog.getInstance().e(TAG, "<parseRspCmdDataBytes> fail to convert JSON object, jsonText=" + jsonText);
            return null;
        }

        RtmCmdCtx rtmCmdCtx = new RtmCmdCtx();
        rtmCmdCtx.mSequenceId = JsonUtils.parseJsonLongValue(recvJsonObj, "sequenceId", -1);
        rtmCmdCtx.mCmdId = JsonUtils.parseJsonIntValue(recvJsonObj, "commandId", -1);
        rtmCmdCtx.mErrCode = JsonUtils.parseJsonIntValue(recvJsonObj, "code", 0);
        JSONObject dataJsonObj = JsonUtils.parseJsonObject(recvJsonObj, "data", null);
        if (dataJsonObj != null) {
            rtmCmdCtx.mRespData = String.valueOf(dataJsonObj);
        }

        ALog.getInstance().d(TAG, "<parseRspCmdDataBytes> done, rtmCmdCtx=" + rtmCmdCtx);
        return rtmCmdCtx;
    }
}
