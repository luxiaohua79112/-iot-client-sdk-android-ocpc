/**
 * @file IAccountMgr.java
 * @brief This file define the interface of call kit and RTC management
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2022-01-26
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink;



import android.view.View;
import java.util.UUID;


/*
 * @brief 设备预览接口
 */
public interface IDevPreviewMgr {


    /**
     * @brief 音效属性
     */
    public enum AudioEffectId {
        NORMAL, OLDMAN, BABYBOY, BABYGIRL, ZHUBAJIE, ETHEREAL, HULK
    }



    /**
     * @brief RTC状态信息
     */
    public static class RtcNetworkStatus {
        public int totalDuration;
        public int txBytes;
        public int rxBytes;
        public int txKBitRate;
        public int txAudioBytes;
        public int rxAudioBytes;
        public int txVideoBytes;
        public int rxVideoBytes;
        public int rxKBitRate;
        public int txAudioKBitRate;
        public int rxAudioKBitRate;
        public int txVideoKBitRate;
        public int rxVideoKBitRate;
        public int lastmileDelay;
        public double cpuTotalUsage;
        public double cpuAppUsage;
        public int users;
        public int connectTimeMs;
        public int txPacketLossRate;
        public int rxPacketLossRate;
        public double memoryAppUsageRatio;
        public double memoryTotalUsageRatio;
        public int memoryAppUsageInKbytes;
    }

    /**
     * @brief 预览监听器
     */
    public static interface OnPreviewListener {

        /**
         * @brief 设备端首帧出图
         * @param sessionId : 会话唯一标识
         * @param videoWidth : 首帧视频宽度
         * @param videoHeight : 首帧视频高度
         */
        default void onDeviceFirstVideo(final UUID sessionId, int videoWidth, int videoHeight) {}

    }


    /**
     * @brief 截图监听器
     */
    public static interface OnCaptureFrameListener {

        /**
         * @brief 设备端截图完成事件
         * @param sessionId : 会话唯一标识
         * @param errCode : 错误码：0表示截图成功
         * @param filePath : 截图保存的路径
         * @param width : 截图宽度
         * @param height : 截图高度
         */
        default void onSnapshotDone(final UUID sessionId, int errCode,
                                    final String filePath, int width, int height) {}
    }


    ////////////////////////////////////////////////////////////////////////
    //////////////////////////// Public Methods ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /**
     * @brief 设置设备端视频帧显示控件，如果不设置则不显示对端视频
     * @param displayView: 设备端视频显示控件
     * @return 错误码
     */
    int setDisplayView(final View displayView);

    /**
     * @brief 开始预览设备音视频流
     * @param bSubAudio : 预览开始后是否订阅设备端音频流
     * @param previewListener : 预览监听事件
     * @return 返回错误码
     */
    int previewStart(boolean bSubAudio, final OnPreviewListener previewListener);

    /**
     * @brief 停止设备音视频流预览
     * @return 返回错误码
     */
    int previewStop();

    /**
     * @brief 返回当前设备是否正在预览
     */
    boolean isPreviewing();

    /**
     * @brief 开始录制当前预览（包括音视频流），仅在预览状态下才能调用
     *         同一时刻只能启动一路录像功能
     * @param outFilePath : 输出保存的视频文件路径（应用层确保文件有可写权限）
     * @return 错误码
     */
    int recordingStart(final String outFilePath);

    /**
     * @brief 停止录制当前预览，仅在预览状态下才能调用
     * @return 错误码
     */
    int recordingStop();

    /**
     * @brief 判断当前是否正在本地录制
     * @return true 表示正在本地录制频道； false: 不在录制
     */
    boolean isRecording();

    /**
     * @brief 截屏设备端视频帧图像
     * @param saveFilePath : 保存的文件（应用层确保文件有可写权限）
     * @return 错误码
     */
    int captureVideoFrame(final String saveFilePath, final OnCaptureFrameListener captureListener);


    /**
     * @brief 禁止/启用 本地音频推流到对端
     * @param mute: 是否禁止
     * @return 错误码
     */
    int muteLocalAudio(boolean mute);

    /**
     * @brief 设置本地推流的语音音量
     * @param volume: 设置的语音音量，范围 [0, 400], 默认100(原始音量)，自带增益保护
     * @return 错误码
     */
    int setLocalAudioVolume(int volume);

    /**
     * @brief 禁止/启用 拉流设备端视频
     * @param mute: 是否禁止
     * @return 错误码
     */
    int muteDeviceVideo(boolean mute);

    /**
     * @brief 禁止/启用 拉流设备端音频
     * @param mute: 是否禁止
     * @return 错误码
     */
    int muteDeviceAudio(boolean mute);

    /**
     * @brief 获取当前网络状态
     * @return 返回RTC网络状态信息
     */
    RtcNetworkStatus getNetworkStatus();

    /**
     * @brief 设置本地播放所有混音后音频的音量
     * @param volumeLevel: 音量级别
     * @return 错误码
     */
    int setPlaybackVolume(int volumeLevel);

    /**
     * @brief 设置音效效果（通常是变声等音效），如果本地推音频流，会影响推送音频效果
     * @param effectId: 音效Id
     * @return 错误码
     */
    int setAudioEffect(final AudioEffectId effectId);

    /**
     * @brief 获取指定会话的当前音效
     * @return 返回音效Id
     */
    AudioEffectId getAudioEffect();

    /**
     * @brief 设置RTC私有参数
     * @param privateParam : 要设置的私参
     * @return 错误码
     */
    int setRtcPrivateParam(String privateParam);


}
