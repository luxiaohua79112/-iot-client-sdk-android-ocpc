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


import android.view.SurfaceView;
import android.view.View;


/**
 * @brief 云录视频播放器 （VOD播放器）
 */
public interface IVodPlayer  {


    //
    // VOD播放状态机
    //
    public static final int VODPLAYER_STATE_CLOSED = 0x0000;     ///< 没有媒体文件打开
    public static final int VODPLAYER_STATE_OPENING = 0x0001;    ///< 媒体文件正在打开
    public static final int VODPLAYER_STATE_PAUSED = 0x0002;     ///< 当前暂停播放
    public static final int VODPLAYER_STATE_PLAYING = 0x0003;    ///< 当前正在播放


    /**
     * @brief Vod媒体文件信息
     */
    public static class VodMediaInfo {
        public String mMediaUrl;        ///< 媒体文件Url
        public long mDuration;          ///< 播放时长，单位ms
        public int mVideoWidth;         ///< 视频帧宽度
        public int mVideoHeight;        ///< 视频帧高度

        @Override
        public String toString() {
            String infoText = "{ mMediaUrl=" + mMediaUrl
                    + ", mDuration=" + mDuration
                    + ", mVideoWidth=" + mVideoWidth
                    + ", mVideoHeight=" + mVideoHeight + " }";
            return infoText;
        }
    }


    /**
     * @brief VOD播放回调接口
     */
    public static interface ICallback {

        /**
         * @brief 当前播放状态变化事件
         * @param mediaUrl : 媒体文件Url
         * @param newState : 切换后的新状态
         */
        default void onVodPlayingStateChanged(final String mediaUrl, int newState) { }

        /**
         * @brief 当前媒体文件打开完成事
         * @param mediaUrl : 媒体文件Url
         */
        default void onVodOpenDone(final String mediaUrl) { }

        /**
         * @brief 当前媒体文件播放完成事件，通常此时可以调用 stop()回到开始重新play()，或者close()关闭播放器
         * @param mediaUrl : 媒体文件Url
         * @param duration : 整个播放时长
         */
        default void onVodPlayingDone(final String mediaUrl, long duration) { }

        /**
         * @brief 播放过程中遇到错误，并且不能恢复，此时上层只能调用 close()关闭播放器
         * @param mediaUrl : 媒体文件Url
         * @param errCode : 错误码
         */
        default void onVodPlayingError(final String mediaUrl, int errCode) { }
    }


    /**
     * @brief 设置播放器视频帧显示控件
     * @param displayView: 视频帧显示控件
     * @return 返回错误码
     */
    int setDisplayView(final SurfaceView displayView);

    /**
     * @brief 打开媒体文件准备播放，打开成功后播放进度位于开始0处，状态切换到 VODPLAYER_STATE_PAUSED
     * @param mediaUrl: 要播放的媒体文件URL，包含密码信息
     * @param callback : 播放回调接口
     * @return 返回错误码
     */
    int open(final String mediaUrl, final ICallback callback);

    /**
     * @brief 关闭当前播放器，释放所有的播放资源，状态切换到 VODPLAYER_STATE_CLOSED
     * @return 错误码
     */
    void close();

    /**
     * @brief 获取当前VOD媒体文件信息，只有在正常打开后才能获取到
     * @return 错误码
     */
    VodMediaInfo getMediaInfo();

    /**
     * @brief 获取当前播放的时间戳，单位ms
     * @return 播放进度时间戳
     */
    long getPlayingProgress();

    /**
     * @brief 获取当前播放状态机
     * @return 返回当前播放状态
     */
    int getPlayingState();

    /**
     * @brief 从当前进度开始播放，状态切换到 VODPLAYER_STATE_PLAYING
     * @return 错误码
     */
    int play();

    /**
     * @brief 暂停播放，状态切换到 VODPLAYER_STATE_PAUSED
     * @return 错误码
     */
    int pause();

    /**
     * @brief 暂停当前播放，并且将播放进度回归到开始0处，状态切换到 VODPLAYER_STATE_PAUSED
     * @return 错误码
     */
    int stop();

    /**
     * @brief 直接跳转播放进度
     * @param seekPos: 需要跳转到的目标时间戳，单位ms
     * @return 返回错误码
     */
    int seek(long seekPos);

}
