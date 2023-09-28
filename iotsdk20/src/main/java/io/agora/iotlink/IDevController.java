/**
 * @file IRtmMgr.java
 * @brief This file define the interface of RTM management
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2022-08-11
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink;


import android.view.View;

import java.util.List;
import java.util.UUID;

/**
 * @brief 设备控制器，可以给设备发控制命令
 */
public interface IDevController  {

    /**
     * @brief 公共的命令回调监听器
     */
    public static interface OnCommandCmdListener {

        /**
         * @brief 命令执行完成回调
         * @param errCode: 命令执行结果错误码
         * @param respData: 设备端返回的响应数据（可能为null）
         */
        default void onDeviceCmdDone(int errCode, final String respData) {}
    }


    ////////////////////////////////////////////////////////////////////////
    //////////////////////////// Public Methods ///////////////////////////
    ////////////////////////////////////////////////////////////////////////

    /**
     * @brief 发送云台控制命令
     * @param action: 动作命令：0-开始，1-停止
     * @param direction: 方向：0-左、1-右、2-上、3-下、4-镜头拉近、5-镜头拉远
     * @param speed: 速度：0-慢，1-适中（默认），2-快
     * @param cmdListener: 命令完成回调
     * @return 返回错误码
     */
    int sendCmdPtzCtrl(int action, int direction, int speed, final OnCommandCmdListener cmdListener);

    /**
     * @brief 发送云台校准命令
     * @param cmdListener: 命令完成回调
     * @return 返回错误码
     */
    int sendCmdPtzReset(final OnCommandCmdListener cmdListener);


    /**
     * @brief 发送存储卡格式化命令
     * @param cmdListener: 命令完成回调
     * @return 返回错误码
     */
    int sendCmdSdcardFmt(final OnCommandCmdListener cmdListener);

    /**
     * @brief 发送定制化的命令数据
     * @param customizeData: 上层自定义数据
     * @param cmdListener: 命令完成回调
     * @return 返回错误码
     */
    int sendCmdCustomize(final String customizeData, final OnCommandCmdListener cmdListener);

    /**
     * @brief 发送设备重启命令
     * @param cmdListener: 命令完成回调
     * @return 返回错误码
     */
    int sendCmdDevReset(final OnCommandCmdListener cmdListener);

}
