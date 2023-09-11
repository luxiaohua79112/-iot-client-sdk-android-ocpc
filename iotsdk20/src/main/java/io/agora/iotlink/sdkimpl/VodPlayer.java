/**
 * @file AccountMgr.java
 * @brief This file implement the call kit and RTC management
 *
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2023-05-19
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink.sdkimpl;



import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import java.io.IOException;

import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IVodPlayer;
import io.agora.iotlink.base.AtomicInteger;
import io.agora.iotlink.logger.ALog;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;


/*
 * @brief 云录视频播放器
 */
public class VodPlayer implements IVodPlayer {


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/VodPlayer";


    //
    // The mesage Id
    //


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private FrameLayout mDisplayLayout;

    private ICallback mCallback;

    private IjkMediaPlayer mIjkPlayer;
    private VodMediaInfo mMediaInfo = new VodMediaInfo();
    private AtomicInteger mState = new AtomicInteger();
    private SurfaceView mDisplayView;




    ///////////////////////////////////////////////////////////////////////
    /////////////////// Methods of Override IVodPlayer  ///////////////////
    ///////////////////////////////////////////////////////////////////////
    @Override
    public int setDisplayLayout(final FrameLayout displayLayout) {
        mDisplayLayout = displayLayout;
        return ErrCode.XOK;
    }

    @Override
    public int open(final String mediaUrl, final ICallback callback) {
        mState.setValue(IVodPlayer.VODPLAYER_STATE_OPENING);
        mIjkPlayer = new IjkMediaPlayer();
        mMediaInfo.clear();
        mCallback = callback;
        mMediaInfo.mMediaUrl = mediaUrl;
        callbackPlayingState(IVodPlayer.VODPLAYER_STATE_OPENING);  // 回调正在打开状态

        try {
            // 设置显示控件
            renderViewSetDisplay();

            mIjkPlayer.setDataSource(mediaUrl);
            mIjkPlayer.prepareAsync();

            // 开启硬解码
            mIjkPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);

            // 自动旋转
            mIjkPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1);

            mIjkPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 1);

            // 精准seek
            mIjkPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "enable-accurate-seek", 1);

            // 打开后立即播放
            mIjkPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 1);

            mIjkPlayer.setOnVideoSizeChangedListener(new IMediaPlayer.OnVideoSizeChangedListener() {
                @Override
                public void onVideoSizeChanged(IMediaPlayer iMediaPlayer,  int width, int height, int sarNum, int sarDen) {
                    ALog.getInstance().d(TAG, "<open.onVideoSizeChanged> width=" + width
                        + ", height=" + height + ", sarNum=" + sarNum + ", sarDen=" + sarDen);

                    // 设置显示控件
                    //renderViewSetDisplay();
                }
            });

            mIjkPlayer.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
                 @Override
                 public void onPrepared(IMediaPlayer iMediaPlayer) {
                     ALog.getInstance().d(TAG, "<open.onPrepared> ");
                     mMediaInfo.mDuration = mIjkPlayer.getDuration();
                     mMediaInfo.mVideoWidth = mIjkPlayer.getVideoWidth();
                     mMediaInfo.mVideoHeight = mIjkPlayer.getVideoHeight();

                     // 创建显示控件
                     renderViewCreate(mMediaInfo.mVideoWidth, mMediaInfo.mVideoHeight);

                     // 设置显示控件
                     //renderViewSetDisplay();

                     ALog.getInstance().d(TAG, "<open.onPrepared> mMediaInfo=" + mMediaInfo);
                     if (mCallback != null) {    // 直接回调给上层
                         mCallback.onVodOpenDone(mediaUrl);
                     }

                     mState.setValue(IVodPlayer.VODPLAYER_STATE_PLAYING);  // 准备就绪后自动播放
                     callbackPlayingState(IVodPlayer.VODPLAYER_STATE_PLAYING);  // 回调正在播放状态
                 }
             });

            mIjkPlayer.setOnCompletionListener(new IMediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(IMediaPlayer iMediaPlayer) {
                    ALog.getInstance().d(TAG, "<open.onCompletion> ");
                    mState.setValue(IVodPlayer.VODPLAYER_STATE_STOPPED);
                    callbackPlayingState(IVodPlayer.VODPLAYER_STATE_STOPPED);  // 回调停止播放状态
                    if (mCallback != null) {    // 直接回调给上层
                        mCallback.onVodPlayingDone(mediaUrl, mMediaInfo.mDuration);
                    }
                }
            });

            mIjkPlayer.setOnErrorListener(new IMediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(IMediaPlayer iMediaPlayer, int what, int extra) {
                    ALog.getInstance().e(TAG, "<open.onError> what=" + what + ", extra=" + extra);
                    mState.setValue(IVodPlayer.VODPLAYER_STATE_STOPPED);
                    callbackPlayingState(IVodPlayer.VODPLAYER_STATE_STOPPED);  // 回调停止播放状态
                    if (mCallback != null) {    // 直接回调给上层
                        mCallback.onVodPlayingError(mediaUrl, what);
                    }
                    return false;
                }
            });

            // 设置显示控件
            //mIjkPlayer.setDisplay(mDisplayView.getHolder());

        } catch (IOException ioExp) {
            ioExp.printStackTrace();
            ALog.getInstance().e(TAG, "<open> [IO_EXP] mediaUrl=" + mediaUrl + ", ioExp=" + ioExp);
            mIjkPlayer.release();
            mIjkPlayer = null;
            mState.setValue(IVodPlayer.VODPLAYER_STATE_CLOSED);
            callbackPlayingState(IVodPlayer.VODPLAYER_STATE_CLOSED);  // 回调关闭播放状态
            return ErrCode.XERR_FILE_OPEN;

        } catch (IllegalArgumentException illegalExp) {
            illegalExp.printStackTrace();
            ALog.getInstance().e(TAG, "<open> [ILLEGAL_EXP] mediaUrl=" + mediaUrl + ", illegalExp=" + illegalExp);
            mIjkPlayer.release();
            mIjkPlayer = null;
            mState.setValue(IVodPlayer.VODPLAYER_STATE_CLOSED);
            callbackPlayingState(IVodPlayer.VODPLAYER_STATE_CLOSED);  // 回调关闭播放状态
            return ErrCode.XERR_FILE_OPEN;

        } catch (SecurityException securityExp) {
            securityExp.printStackTrace();
            ALog.getInstance().e(TAG, "<open> [SECURITY_EXP] mediaUrl=" + mediaUrl + ", securityExp=" + securityExp);
            mIjkPlayer.release();
            mIjkPlayer = null;
            mState.setValue(IVodPlayer.VODPLAYER_STATE_CLOSED);
            callbackPlayingState(IVodPlayer.VODPLAYER_STATE_CLOSED);  // 回调关闭播放状态
            return ErrCode.XERR_FILE_OPEN;

        } catch (Exception exp) {
            exp.printStackTrace();
            ALog.getInstance().e(TAG, "<open> [EXP] mediaUrl=" + mediaUrl + ", exp=" + exp);
            mIjkPlayer.release();
            mIjkPlayer = null;
            mState.setValue(IVodPlayer.VODPLAYER_STATE_CLOSED);
            callbackPlayingState(IVodPlayer.VODPLAYER_STATE_CLOSED);  // 回调关闭播放状态
            return ErrCode.XERR_FILE_OPEN;
        }


        ALog.getInstance().d(TAG, "<open> done, mediaUrl=" + mediaUrl);
        return ErrCode.XOK;
    }

    @Override
    public void close() {
        if (mIjkPlayer != null) {
            mIjkPlayer.release();
            mIjkPlayer = null;
            mState.setValue(IVodPlayer.VODPLAYER_STATE_CLOSED);
            callbackPlayingState(IVodPlayer.VODPLAYER_STATE_CLOSED);  // 回调关闭播放状态
            ALog.getInstance().d(TAG, "<close> done");
        }

        if (mDisplayLayout != null) {
            mDisplayLayout.removeAllViews();
        }
    }

    @Override
    public VodMediaInfo getMediaInfo() {
        return mMediaInfo;
    }

    @Override
    public long getPlayingProgress() {
        if (mIjkPlayer == null) {
            ALog.getInstance().d(TAG, "<getPlayingProgress> bad state");
            return 0;
        }

        long postion = mIjkPlayer.getCurrentPosition();
        ALog.getInstance().d(TAG, "<getPlayingProgress> postion=" + postion);
        return postion;
    }

    @Override
    public int getPlayingState() {
        return mState.getValue();
    }

    @Override
    public int play() {
        if (mIjkPlayer == null) {
            ALog.getInstance().d(TAG, "<play> bad state");
            return ErrCode.XERR_BAD_STATE;
        }

        try {
            mIjkPlayer.start();
            mState.setValue(IVodPlayer.VODPLAYER_STATE_PLAYING);
            callbackPlayingState(IVodPlayer.VODPLAYER_STATE_PLAYING);  // 回调正在播放状态

        } catch (IllegalStateException illegalExp) {
            illegalExp.printStackTrace();
            ALog.getInstance().e(TAG, "<play> [ILLEGAL_EXP] illegalExp=" + illegalExp);
            return ErrCode.XERR_PLAYER_PLAY;
        }

        ALog.getInstance().d(TAG, "<play> done");
        return ErrCode.XOK;
    }

    @Override
    public int pause() {
        if (mIjkPlayer == null) {
            ALog.getInstance().d(TAG, "<pause> bad state");
            return ErrCode.XERR_BAD_STATE;
        }

        try {
            mIjkPlayer.pause();
            mState.setValue(IVodPlayer.VODPLAYER_STATE_PAUSED);
            callbackPlayingState(IVodPlayer.VODPLAYER_STATE_PAUSED);  // 回调停止播放状态

        } catch (IllegalStateException illegalExp) {
            illegalExp.printStackTrace();
            ALog.getInstance().e(TAG, "<pause> [ILLEGAL_EXP] illegalExp=" + illegalExp);
            return ErrCode.XERR_PLAYER_PLAY;
        }

        ALog.getInstance().d(TAG, "<pause> done");
        return ErrCode.XOK;
    }

    @Override
    public int stop() {
        if (mIjkPlayer == null) {
            ALog.getInstance().d(TAG, "<stop> bad state");
            return ErrCode.XERR_BAD_STATE;
        }

        try {
            mIjkPlayer.stop();
            mState.setValue(IVodPlayer.VODPLAYER_STATE_STOPPED);
            callbackPlayingState(IVodPlayer.VODPLAYER_STATE_STOPPED);  // 回调停止播放状态

        } catch (IllegalStateException illegalExp) {
            illegalExp.printStackTrace();
            ALog.getInstance().e(TAG, "<stop> [ILLEGAL_EXP] illegalExp=" + illegalExp);
            return ErrCode.XERR_PLAYER_PLAY;
        }

        ALog.getInstance().d(TAG, "<stop> done");
        return ErrCode.XOK;
    }

    @Override
    public int seek(long seekPos) {
        if (mIjkPlayer == null) {
            ALog.getInstance().d(TAG, "<seek> bad state, seekPos=" + seekPos);
            return ErrCode.XERR_BAD_STATE;
        }

        try {

            mIjkPlayer.setOnSeekCompleteListener(new IMediaPlayer.OnSeekCompleteListener() {
                @Override
                public void onSeekComplete(IMediaPlayer iMediaPlayer) {
                    ALog.getInstance().d(TAG, "<seek.onSeekComplete> seekPos=" + seekPos);
                    if (mCallback != null) {    // 直接回调给上层
                        mCallback.onVodSeekingDone(mMediaInfo.mMediaUrl, seekPos);
                    }
                }
            });

            mIjkPlayer.seekTo(seekPos);

        } catch (IllegalStateException illegalExp) {
            illegalExp.printStackTrace();
            ALog.getInstance().e(TAG, "<seek> [ILLEGAL_EXP] illegalExp=" + illegalExp);
            return ErrCode.XERR_PLAYER_PLAY;
        }

        ALog.getInstance().d(TAG, "<seek> done, seekPos=" + seekPos);
        return ErrCode.XOK;
    }


    @Override
    public int setVolume(float volumeLevel) {
        if (mIjkPlayer == null) {
            ALog.getInstance().d(TAG, "<setVolume> bad state, volumeLevel=" + volumeLevel);
            return ErrCode.XERR_BAD_STATE;
        }

        mIjkPlayer.setVolume(volumeLevel, volumeLevel);
        ALog.getInstance().d(TAG, "<setVolume> done, volumeLevel=" + volumeLevel);
        return ErrCode.XOK;
    }

    ///////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////// Internal Methods //////////////////////////
    ///////////////////////////////////////////////////////////////////////////////
    void callbackPlayingState(int newState) {
        if (mCallback != null) {
            mCallback.onVodPlayingStateChanged(mMediaInfo.mMediaUrl, newState);
        }
    }

    /**
     * @brief 根据视频帧的大小，来创新显示控件，并且添加到相应的布局中
     * @param frameWidth: 视频帧宽度
     * @param frameHeight: 视频帧高度
     * @return 错误码
     */
    int renderViewCreate(int frameWidth, int frameHeight) {
        if (mDisplayLayout == null) {
            return ErrCode.XERR_BAD_STATE;
        }


        // 计算显示控件大小
        int layoutWidth = mDisplayLayout.getWidth();
        int layoutHeight = mDisplayLayout.getHeight();
        ZoomSize zoomSize = calculateFitInSize(layoutWidth, layoutHeight, frameWidth, frameHeight);

        mDisplayView = new SurfaceView(mDisplayLayout.getContext());
        FrameLayout.LayoutParams widgetParam = new FrameLayout.LayoutParams(
                zoomSize.width, zoomSize.height, Gravity.CENTER);
        mDisplayView.setLayoutParams(widgetParam);

        // 显示控件添加到布局文件中
        mDisplayLayout.removeAllViews();
        mDisplayLayout.addView(mDisplayView);

        ALog.getInstance().d(TAG, "<renderViewCreate> done, layoutWidth=" + layoutWidth
                + ", layoutHeight=" + layoutHeight
                + ", viewWidth=" + zoomSize.width + ", viewHeight=" + zoomSize.height);
        return ErrCode.XOK;
    }

    /**
     * @brief 销毁显示控件，从布局中删除
     */
    void renderViewDestroy() {
        if (mDisplayLayout != null) {
            mDisplayLayout.removeAllViews();
            ALog.getInstance().d(TAG, "<renderViewDestroy> done");
        }
        mDisplayView = null;
    }


    /**
     * @brief 设置显示控件
     */
    void renderViewSetDisplay() {
        if ((mIjkPlayer != null) && (mDisplayView != null)) {
            SurfaceHolder holder = mDisplayView.getHolder();
            if (holder != null) {
                mIjkPlayer.setDisplay(holder);
                ALog.getInstance().d(TAG, "<renderViewSetDisplay> done, holder=" + holder);
            }
        }
    }


    private static class ZoomSize {
        public double   fZoomLevel;
        public int      width;
        public int      height;
    }

    /**
     * @brief 根据显示区域大小 和 实际图像大小，计算fitIn 区域大小
     */
    static public ZoomSize calculateFitInSize(int dispWidth, int dispHeight, int imgWidth, int imgHeight)
    {
        int orgWidth = imgWidth;
        int orgHeight= imgHeight;
        ZoomSize  fitSize = new ZoomSize();

        if (orgWidth <= dispWidth && orgHeight <= dispHeight)
        {
            fitSize.fZoomLevel = 1.0f;
            fitSize.width = orgWidth;
            fitSize.height = orgHeight;
            return fitSize;
        }

        double fRadioW = (double)dispWidth*1.0 / orgWidth;
        double fRadiuH = (double)dispHeight*1.0 / orgHeight;

        fitSize.fZoomLevel = (fRadioW < fRadiuH) ? fRadioW : fRadiuH;
        fitSize.width  = (int)(orgWidth * fitSize.fZoomLevel + 0.5);
        fitSize.height = (int)(orgHeight * fitSize.fZoomLevel + 0.5);

        if (fitSize.width <= 0)  {
            fitSize.width = 1;
        }

        if (fitSize.height <= 0)    {
            fitSize.height = 1;
        }

        return fitSize;
    }


}
