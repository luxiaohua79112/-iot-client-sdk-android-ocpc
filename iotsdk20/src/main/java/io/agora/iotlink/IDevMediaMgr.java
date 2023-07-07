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
 * @brief 设备端媒体文件管理器
 */
public interface IDevMediaMgr  {


    ////////////////////////////////////////////////////////////////////////
    ////////////////// Methods for Device Media Management /////////////////
    ////////////////////////////////////////////////////////////////////////




    /**
     * @brief 分页查询文件参数，可以进行查询组合
     */
    public static class QueryParam {
        public String mFileId;          ///< 文件Id, 0表示则返回根目录文件夹目录
        public long mBeginTimestamp;    ///< 查询时间段的开始时间戳，单位秒
        public long mEndTimestamp;      ///< 查询时间段的结束时间戳，单位秒
        public int mPageIndex;          ///< 查询开始的页索引，从1开始
        public int mPageSize;           ///< 一页文件数量


        @Override
        public String toString() {
            String infoText = "{ mFileId=" + mFileId
                    + ", mBeginTimestamp=" + mBeginTimestamp
                    + ", mEndTimestamp=" + mEndTimestamp
                    + ", mPageIndex=" + mPageIndex
                    + ", mPageSize=" + mPageSize + " }";
            return infoText;
        }
    }

    /**
     * @brief 查询到的 设备媒体项
     */
    public static class DevMediaItem {
        public String mFileId;            ///< 媒体文件Id，是文件唯一标识
        public long mStartTimestamp;    ///< 录制开始时间，单位秒
        public long mStopTimestamp;     ///< 录制结束时间，单位秒
        public int mType;               ///< 文件类型：0--媒体文件；1--目录
        public int mEvent;              ///< 事件类型：0-全部事件、1-页面变动、2-有人移动
        public String mImgUrl;          ///< 录像封面图片URL地址
        public String mVideoUrl;        ///< 录像下载的URL地址

        @Override
        public String toString() {
            String infoText = "{ mFileId=" + mFileId
                    + ", mStartTimestamp=" + mStartTimestamp
                    + ", mStopTimestamp=" + mStopTimestamp
                    + ", mType=" + mType
                    + ", mEvent=" + mEvent
                    + ", mImgUrl=" + mImgUrl
                    + ", mVideoUrl=" + mVideoUrl  + " }";
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
     * @brief 根据媒体文件的Url来删除设备端多个媒体文件，该方法是异步调用，通过回调返回删除结果
     * @param deletingList: 要删除的 媒体文件Url的列表
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





    ////////////////////////////////////////////////////////////////////////
    ////////////////// Methods for Device Media Playing ////////////////////
    ////////////////////////////////////////////////////////////////////////
    public static final String FILE_ID_GLOBALT_IMELINE = "GlobalTimeline";     ///< 全局时间轴播放的FileId

    //
    // 设备媒体文件播放状态机
    //
    public static final int DEVPLAYER_STATE_STOPPED = 0x0000;     ///< 当前播放器关闭
    public static final int DEVPLAYER_STATE_PLAYING = 0x0001;    ///< 当前正在播放
    public static final int DEVPLAYER_STATE_PAUSED = 0x0002;     ///< 当前暂停播放
    public static final int DEVPLAYER_STATE_SEEKING = 0x0003;    ///< 当前正在SEEK操作

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
         * @brief 当前播放状态变化事件
         * @param fileId : 媒体文件Id
         * @param newState : 切换后的新状态
         */
        default void onDevPlayingStateChanged(final String fileId, int newState) {}

        /**
         * @brief 设备媒体文件打开完成事件
         * @param fileId : 媒体文件Id
         * @param errCode : 错误码，0表示打开成功
         */
        default void onDevMediaOpenDone(final String fileId, int errCode) { }

        /**
         * @brief 设备媒体文件Seek完成事件
         * @param fileId : 媒体文件Id
         * @param errCode : 错误码，0表示Seek成功
         * @param targetPos : 要seek到的时间戳
         * @param seekedPos : 实际跳转到的时间戳
         */
        default void onDevMediaSeekDone(final String fileId, int errCode,
                                        long targetPos, long seekedPos) { }

        /**
         * @brief 当前媒体文件播放完成事件，通常此时可以调用 stop()回到开始重新play()，或者close()关闭播放器
         * @param fileId : 媒体文件Id
         */
        default void onDevMediaPlayingDone(final String fileId) {}

        /**
         * @brief 播放过程中遇到错误，并且不能恢复，此时上层只能调用 close()关闭播放器
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
     * @brief 开始播放，成功后切换到 DEVPLAYER_STATE_PLAYING 状态
     * @param globalStartTime: 全局开始时间
     * @param playSpeed: 播放倍速
     * @param playingCallback : 播放回调接口
     * @return 返回错误码
     */
    int play(long globalStartTime, int playSpeed, final IPlayingCallback playingCallback);

    /**
     * @brief 开始播放，成功后切换到 DEVPLAYER_STATE_PLAYING 状态
     * @param fileId: 要播放的媒体文件Id
     * @param startPos: 开始播放的开始时间点
     * @param playSpeed: 播放倍速
     * @param playingCallback : 播放回调接口
     * @return 返回错误码
     */
    int play(final String fileId, long startPos, int playSpeed, final IPlayingCallback playingCallback);

    /**
     * @brief 停止当前播放，成功后切换到 DEVPLAYER_STATE_STOPPED 状态
     * @return 错误码
     */
    int stop();


    /**
     * @brief 暂停播放，切换到 DEVPLAYER_STATE_PAUSED 状态
     * @return 错误码
     */
    int pause();

    /**
     * @brief 恢复暂停的播放，切换到 DEVPLAYER_STATE_PLAYING 状态
     * @return 错误码
     */
    int resume();

    /**
     * @brief 直接跳转播放进度，先切换成 DEVPLAYER_STATE_SEEKING状态，之后切换回原先 播放/暂停状态
     * @param seekPos: 需要跳转到的目标时间戳，单位ms
     * @return 返回错误码
     */
    int seek(long seekPos);

    /**
     * @brief 设置快进、快退播放倍速，默认是正常播放速度（speed=1）
     * @param speed: 固定几个倍速：1; 2; 3;
     * @return 返回错误码
     */
    int setPlayingSpeed(int speed);


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
