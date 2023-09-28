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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @brief 设备端媒体文件管理器
 */
public interface IDevMediaMgr  {


    ////////////////////////////////////////////////////////////////////////
    ////////////////// Methods for Device Media Management /////////////////
    ////////////////////////////////////////////////////////////////////////
    public static final String FILE_ID_TIMELINE = "GlobalTimeline";     ///< 特定的fileId，表示全局时间轴

    /**
     * @brief 分页查询文件参数，可以进行查询组合
     */
    public static class QueryParam {
        public String mFileId;          ///< 文件Id, 0表示则返回根目录文件夹目录
        public long mBeginTimestamp;    ///< 查询时间段的开始时间戳，单位秒
        public long mEndTimestamp;      ///< 查询时间段的结束时间戳，单位秒


        @Override
        public String toString() {
            String infoText = "{ mFileId=" + mFileId
                    + ", mBeginTimestamp=" + mBeginTimestamp
                    + ", mEndTimestamp=" + mEndTimestamp + " }";
            return infoText;
        }
    }

    /**
     * @brief 查询到的 设备事件项
     */
    public static class DevEventItem {
        public int mEventType;      ///< 告警类型：0-画面变动、1-异常情况、2-有人移动、3-异常响声、4-宝宝哭声
        public long mStartTime;     ///< 设备录像文件的开始时间（时间戳精确到秒）
        public long mStopTime;      ///< 设备录像文件的结束时间（时间戳精确到秒）
        public String mPicUrl;      ///< 设备录像封面图片地址
        public String mVideoUrl;    ///< 设备录像下载地址

        @Override
        public String toString() {
            String infoText = "{ mEventType=" + mEventType
                    + ", mStartTime=" + mStartTime
                    + ", mStopTime=" + mStopTime
                    + ", mPicUrl=" + mPicUrl
                    + ", mVideoUrl=" + mVideoUrl  + " }";
            return infoText;
        }
    }

    /**
     * @brief 查询到的 设备媒体项
     */
    public static class DevMediaItem {
        public String mFileId;          ///< 设备录像文件夹id
        public long mStartTimestamp;    ///< 录制开始时间，单位秒
        public long mStopTimestamp;     ///< 录制结束时间，单位秒
        public int mType;               ///< 文件类型：0--媒体文件；1--目录
        public List<DevEventItem> mEventList = new ArrayList<>();    ///< 事件列表

        @Override
        public String toString() {
            String infoText = "{ mFileId=" + mFileId
                    + ", mStartTimestamp=" + mStartTimestamp
                    + ", mStopTimestamp=" + mStopTimestamp
                    + ", mType=" + mType
                    + ", mEventList=" + mEventList + " }";
            return infoText;
        }
    }

    /**
     * @brief 设备媒体文件查询回调监听器
     */
    public static interface OnQueryListener {
        /**
         * @brief 媒体项查询完成事件
         * @param errCode : 查询结果错误码，0表示查询成功
         * @param mediaList : 输出查询到的媒体项列表
         */
        default void onDevMediaQueryDone(int errCode, final List<DevMediaItem> mediaList) {}
    }

    /**
     * @brief 根据查询条件来分页查询相应的设备端 媒体文件列表，该方法是异步调用，通过回调返回查询结果
     * @param queryParam: 查询参数
     * @param queryListener : 查询结果回调监听器
     * @return 返回错误码
     */
    int queryMediaList(final QueryParam queryParam, final OnQueryListener queryListener);


    /**
     * @brief 每一个媒体项删除的结果
     */
    public static class DevMediaDelResult {
        public String mFileId;            ///< 媒体文件Id，是文件唯一标识
        public int mErrCode;

        @Override
        public String toString() {
            String infoText = "{ mFileId=" + mFileId + ", mErrCode=" + mErrCode + " }";
            return infoText;
        }
    }

    /**
     * @brief 设备媒体文件删除回调监听器
     */
    public static interface OnDeleteListener {
        /**
         * @brief 媒体项删除完成事件
         * @param errCode : 查询结果错误码，0标识查询成功
         * @param undeletedList : 输出未成功删除的媒体项列表
         */
        default void onDevMediaDeleteDone(int errCode, final List<DevMediaDelResult> undeletedList) {}
    }

    /**
     * @brief 根据 fileId 来删除设备端多个媒体文件，该方法是异步调用，通过回调返回删除结果
     * @param deletingList: 要删除的 fileId的列表
     * @param deleteListener : 删除结果回调监听器
     * @return 返回错误码
     */
    int deleteMediaList(final List<String> deletingList, final OnDeleteListener deleteListener);



    /**
     * @brief 查询封面数据回调监听器
     */
    public static interface OnCoverDataListener {
        /**
         * @brief 媒体项删除完成事件
         * @param errCode : 查询结果错误码，0标识查询成功
         * @param imgUrl : 封面文件路径
         * @param coverData : 封面图像的数据
         */
        default void onDevMediaCoverDataDone(int errCode, final String imgUrl, final byte[] coverData) {}
    }

    /**
     * @brief 查询媒体文件封面，该方法是异步调用，通过回调返回删除结果
     * @param imgUrl: 封面文件路径
     * @param coverDataListener : 查询结果回调监听器
     * @return 返回错误码
     */
    int getMediaCoverData(final String imgUrl, final OnCoverDataListener coverDataListener);



    /**
     * @brief 每一个文件项下载命令的结果
     */
    public static class DevFileDownloadResult {
        public String mFileId;              ///< 媒体文件Id，是文件唯一标识
        public String mFileName;            ///< 单个文件项全录节目


        @Override
        public String toString() {
            String infoText = "{ mFileId=" + mFileId + ", mFileName=" + mFileName + " }";
            return infoText;
        }
    }

    /**
     * @brief 设备媒体文件下载命令回调监听器
     */
    public static interface OnDownloadListener {
        /**
         * @brief 媒体项下载命令完成事件
         * @param errCode : 查询结果错误码，0标识查询成功
         * @param downloadList : 各个媒体项下载结果列表
         */
        default void onDevFileDownloadDone(int errCode, final List<DevFileDownloadResult> downloadList) {}
    }

    /**
     * @brief 根据 fileId 来下载设备端多个媒体文件，该方法是异步调用，通过回调返回下载命令结果
     *         这里的回调仅表示设备端已经初步处理下载命令，但并不表示实际文件下载完成
     * @param downloadList: 要下载的 fileId的列表
     * @param downloadListener : 下载命令结果回调监听器
     * @return 返回错误码
     */
    int downloadFileList(final List<String> downloadList, final OnDownloadListener downloadListener);



    /**
     * @brief 设备事件分布查询回调监听器
     */
    public static interface OnQueryEventListener {
        /**
         * @brief 媒体项查询完成事件
         * @param errCode : 查询结果错误码，0表示查询成功
         * @param videoTimeList : 视频时间戳列表
         */
        default void onDevQueryEventDone(int errCode, final List<Long> videoTimeList) {}
    }

    /**
     * @brief 查询事件分布，该方法是异步调用，通过回调返回查询结果
     * @param queryListener : 查询结果回调监听器
     * @return 返回错误码
     */
    int queryEventTimeline(final OnQueryEventListener queryListener);


    ////////////////////////////////////////////////////////////////////////
    ////////////////// Methods for Device Media Playing ////////////////////
    ////////////////////////////////////////////////////////////////////////
    public static final String FILE_ID_GLOBALT_IMELINE = "GlobalTimeline";     ///< 全局时间轴播放的FileId

    //
    // 设备媒体文件播放状态机
    //
    public static final int DEVPLAYER_STATE_STOPPED = 0x0000;   ///< 当前播放器关闭
    public static final int DEVPLAYER_STATE_OPENING = 0x0001;   ///< 正在打开媒体文件
    public static final int DEVPLAYER_STATE_PLAYING = 0x0002;   ///< 当前正在播放
    public static final int DEVPLAYER_STATE_PAUSING = 0x0003;   ///< 正在暂停当前播放
    public static final int DEVPLAYER_STATE_PAUSED = 0x0004;    ///< 当前播放已经暂停
    public static final int DEVPLAYER_STATE_RESUMING = 0x0005;  ///< 正在恢复当前播放


    /**
     * @brief 设备端单个文件媒体信息
     */
    public static class DevMediaInfo {
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
     * @brief 设备媒体文件 播放回调接口
     */
    public static interface IPlayingCallback {

        /**
         * @brief 设备媒体文件打开完成事件
         * @param fileId : 媒体文件Id
         * @param errCode : 错误码，0表示打开成功直接播放，切换为 DEVPLAYER_STATE_PLAYING 状态
         *                        其他值表示打开失败，状态还是原先的 DEVPLAYER_STATE_STOPPED 状态
         */
        default void onDevMediaOpenDone(final String fileId, int errCode) { }

        /**
         * @brief 媒体文件播放完成事件，此时状态机切换为 DEVPLAYER_STATE_STOPPED 状态
         * @param fileId : 媒体文件Id
         */
        default void onDevMediaPlayingDone(final String fileId) {}

        /**
         * @brief 暂停操作完成事件
         * @param fileId : 媒体文件Id
         * @param errCode : 错误码，0表示暂停成功，状态切换为 DEVPLAYER_STATE_PAUSED；
         *                        其他值表示暂停失败，状态还是原先的 DEVPLAYER_STATE_PLAYING 状态
         */
        default void onDevMediaPauseDone(final String fileId, int errCode) {}

        /**
         * @brief 恢复操作完成事件
         * @param fileId : 媒体文件Id
         * @param errCode : 错误码，0表示恢复成功，状态切换为 DEVPLAYER_STATE_PLAYING；
         *                        其他值表示暂停失败，状态还是原先的 DEVPLAYER_STATE_PAUSED 状态
         */
        default void onDevMediaResumeDone(final String fileId, int errCode) {}


        /**
         * @brief 设备媒体文件倍速完成事件
         * @param fileId : 媒体文件Id
         * @param errCode : 错误码，0表示Seek成功
         * @param speed : 设置的倍速
         */
        default void onDevMediaSetSpeedDone(final String fileId, int errCode, int speed) { }

        /**
         * @brief 播放过程中遇到错误，并且不能恢复，此时上层只能调用 stop()关闭播放器
         * @param fileId : 媒体文件Id
         * @param errCode : 错误码
         */
        default void onDevPlayingError(final String fileId, int errCode) {}
    }


    /**
     * @brief 设置播放器视频帧显示控件
     * @param displayView: 视频帧显示控件
     * @return 返回错误码
     */
    int setDisplayView(final View displayView);

    /**
     * @brief 开始播放，先切换到 DEVPLAYER_STATE_OPENING 状态
     *        操作完成后触发 onDevMediaOpenDone() 回调，并且更新状态
     * @param globalStartTime: 全局开始时间
     * @param playSpeed: 播放倍速
     * @param playingCallback : 播放回调接口
     * @return 返回错误码
     */
    int play(long globalStartTime, int playSpeed, final IPlayingCallback playingCallback);

    /**
     * @brief 开始播放，先切换到 DEVPLAYER_STATE_OPENING 状态
     *        操作完成后触发 onDevMediaOpenDone() 回调，并且更新状态
     * @param fileId: 要播放的媒体文件Id
     * @param startPos: 开始播放的开始时间点
     * @param playSpeed: 播放倍速
     * @param playingCallback : 播放回调接口
     * @return 返回错误码
     */
    int play(final String fileId, long startPos, int playSpeed, final IPlayingCallback playingCallback);

    /**
     * @brief 停止当前播放，成功后切换到 DEVPLAYER_STATE_STOPPED 状态
     *        注意：这个调用不会触发 onDevMediaPlayingDone()回调； 只有回看文件正常播放完成后才会触发
     * @return 错误码
     */
    int stop();


    /**
     * @brief 暂停当前播放，先切换到 DEVPLAYER_STATE_PAUSING 状态
     *        操作完成后触发 onDevMediaPauseDone() 回调，并且更新状态
     * @return 错误码
     */
    int pause();

    /**
     * @brief 恢复当前暂停的播放，先切换到 DEVPLAYER_STATE_RESUMING 状态
     *        操作完成后触发 onDevMediaResumeDone() 回调，并且更新状态
     * @return 错误码
     */
    int resume();

    /**
     * @brief 设置快进、快退播放倍速，默认是正常播放速度（speed=1）
     * @param speed: 固定几个倍速：1; 2; 3;
     * @return 返回错误码
     */
    int setPlayingSpeed(int speed);

    /**
     * @brief 设置播放过程中是否有声音
     * @param mute: true--播放静音；  false--正常播放
     * @return 返回错误码
     */
    int setAudioMute(boolean mute);


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

}
