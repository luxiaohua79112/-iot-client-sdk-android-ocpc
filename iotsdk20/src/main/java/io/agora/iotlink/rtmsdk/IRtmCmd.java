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

    /**
     * @brief 命令响应回调监听器
     */
    public static interface OnRtmCmdRespListener {

        /**
         * @brief 命令执行完成回调
         * @param commandId: 命令Id
         * @param errCode：命令响应结果
         * @param reqCmd: 请求命令
         * @param rspCmd: 对应的响应命令（超时错误时，为null)
         */
        default void onRtmCmdResponsed(int commandId, int errCode,
                                       final IRtmCmd reqCmd, final IRtmCmd rspCmd) { }
    }

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
    public static final int CMDID_MEDIA_DELETE = 2003;     ///< 删除媒体文件
    public static final int CMDID_MEDIA_COVER = 2004;      ///< 查询视频文件封面图片
    public static final int CMDID_MEDIA_PLAY_TIMELINE = 2005; ///< 根据开始时间播放SD卡视频
    public static final int CMDID_MEDIA_PLAY_ID = 2006;    ///< 根据FileId播放单个SDK视频
    public static final int CMDID_MEDIA_STOP = 2007;       ///< 停止当前回看
    public static final int CMDID_MEDIA_RATE = 2008;       ///< 设置回看播放的倍速
    public static final int CMDID_MEDIA_PAUSE = 2009;      ///< 暂停当前回看
    public static final int CMDID_MEDIA_RESUME = 2010;     ///< 恢复当前回看
    public static final int CMDID_FILE_DOWNLOAD = 2011;    ///< 文件下载
    public static final int CMDID_EVENTTIMELINE_QUERY = 2012;    ///< 查询事件分布
    public static final int CMDID_CUSTOMIZE_SEND = 3001;   ///< 定制化命令
    public static final int CMDID_DEVICE_RESET = 3002;     ///< 设备重启命令



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
     * @brief 获取命令发送时间戳，仅针对 请求命令
     */
    long getSendTimestamp();


    /**
     * @brief 获取命令响应监听器，仅针对 请求命令
     */
    OnRtmCmdRespListener getRespListener();

    /**
     * @brief 将请求命令组成JSON字符串
     * @return 返回字命令数据字符串
     */
    String getReqCmdData();

    /**
     * @brief 当前命令是否是回应命令
     */
    boolean isResponseCmd();

    /**
     * @brief 返回响应命令的错误码（仅针对响应命令有效）
     */
    int getRespErrCode();

    /**
     * @brief 返回用户数据
     */
    Object getUserData();

    /**
     * @brief 设置用户数据
     */
     void setUserData(final Object userData);

}
