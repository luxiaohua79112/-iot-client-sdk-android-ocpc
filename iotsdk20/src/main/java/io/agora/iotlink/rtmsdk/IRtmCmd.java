package io.agora.iotlink.rtmsdk;


import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IDevController;
import io.agora.iotlink.logger.ALog;
import io.agora.iotlink.utils.JsonUtils;

/**
 * @brief RTM命令上下文信息，每个 RTM命令都是由  request--response 构成
 *        有命令请求，就有对应的响应，如果超时没有接收到响应数据，则返回超时
 */
public interface IRtmCmd  {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////


    /**
     * @brief 定义所有的RTM请求命令
     */
    public static final int CMDID_PTZ_CTRL = 1001;         ///< 云台控制命令
    public static final int CMDID_PTZ_RESET = 1002;        ///< 云台校准命令
    public static final int CMDID_SDCARD_FMT = 2001;       ///< SD卡格式化
    public static final int CMDID_MEDIA_QUERY = 2002;      ///< 查询存储卡视频文件列表
    public static final int CMDID_MEDIA_COVER = 2003;      ///< 查询视频文件封面图片
    public static final int CMDID_MEDIA_DELETE = 2004;     ///< 删除媒体文件
    public static final int CMDID_MEDIA_PLAY_TIMELINE = 2005; ///< 根据开始时间播放SD卡视频
    public static final int CMDID_MEDIA_PLAY_ID = 2006;    ///< 根据FileId播放单个SDK视频
    public static final int CMDID_MEDIA_STOP = 2007;       ///< 停止当前回看
    public static final int CMDID_MEDIA_RATE = 2008;       ///< 设置回看播放的倍速
    public static final int CMDID_CUSTOMIZE_SEND = 3001;   ///< 定制化命令




    ///////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    /**
     * @brief 获取 sequenceId
     */
    long getSequenceId();

    /**
     * @brief 获取 commandId
     */
    int getCommandId();

    /**
     * @brief 获取命令目标设备
     */
    String getDeviceId();


    /**
     * @brief 获取命令发送时间戳
     */
    long getSendTimestamp();

    /**
     * @brief 当前命令是否是回应命令
     */
    boolean isResponseCmd();

    /**
     * @brief 将请求命令组成JSON字符串，转换成字节流返回
     * @return 返回字节流数据
     */
    byte[] getReqCmdDataBytes();

}
