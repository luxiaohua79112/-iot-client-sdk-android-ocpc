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



import android.view.SurfaceView;
import android.view.View;

import java.io.IOException;

import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IVodPlayer;
import io.agora.iotlink.base.AtomicInteger;
import io.agora.iotlink.logger.ALog;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

//import com.shuyu.gsyvideoplayer.listener.GSYMediaPlayerListener;
//import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;

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
    private SurfaceView mDisplayView;
    private ICallback mCallback;

    private IjkMediaPlayer mIjkPlayer;
    private VodMediaInfo mMediaInfo;
    private AtomicInteger mState = new AtomicInteger();


    ///////////////////////////////////////////////////////////////////////
    /////////////////// Methods of Override IVodPlayer  ///////////////////
    ///////////////////////////////////////////////////////////////////////

    @Override
    public int setDisplayView(final SurfaceView displayView) {
        mDisplayView = displayView;
        return ErrCode.XOK;
    }

    @Override
    public int open(final String mediaUrl, final ICallback callback) {
        mState.setValue(IVodPlayer.VODPLAYER_STATE_OPENING);
        mIjkPlayer = new IjkMediaPlayer();

        try {
            mIjkPlayer.setDataSource(mediaUrl);
            mIjkPlayer.prepareAsync();
            mCallback = callback;

            // 开启硬解码
            mIjkPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);

            // 自动旋转
            mIjkPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1);

            // 精准seek
            mIjkPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "enable-accurate-seek", 1);

            // 打开后立即播放
            mIjkPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 1);

            mIjkPlayer.setOnVideoSizeChangedListener(new IMediaPlayer.OnVideoSizeChangedListener() {
                @Override
                public void onVideoSizeChanged(IMediaPlayer iMediaPlayer, int i, int i1, int i2, int i3) {

                }
            });

            mIjkPlayer.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
                 @Override
                 public void onPrepared(IMediaPlayer iMediaPlayer) {
                     mState.setValue(IVodPlayer.VODPLAYER_STATE_PLAYING);  // 准备就绪后自动播放
                     mMediaInfo = new VodMediaInfo();
                     mMediaInfo.mMediaUrl = mediaUrl;
                     mMediaInfo.mDuration = mIjkPlayer.getDuration();
                     mMediaInfo.mVideoWidth = mIjkPlayer.getVideoWidth();
                     mMediaInfo.mVideoHeight = mIjkPlayer.getVideoHeight();

                     ALog.getInstance().d(TAG, "<open.onPrepared> mMediaInfo=" + mMediaInfo);
                     if (mCallback != null) {    // 直接回调给上层
                         mCallback.onVodOpenDone(mMediaInfo.mMediaUrl);
                     }
                 }
             });

            mIjkPlayer.setOnCompletionListener(new IMediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(IMediaPlayer iMediaPlayer) {
                    ALog.getInstance().d(TAG, "<open.onCompletion> ");
                    mState.setValue(IVodPlayer.VODPLAYER_STATE_PAUSED);
                    if (mCallback != null) {    // 直接回调给上层
                        mCallback.onVodPlayingDone(mMediaInfo.mMediaUrl, mMediaInfo.mDuration);
                    }
                }
            });

            mIjkPlayer.setOnErrorListener(new IMediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(IMediaPlayer iMediaPlayer, int what, int extra) {
                    ALog.getInstance().e(TAG, "<open.onError> what=" + what + ", extra=" + extra);
                    mState.setValue(IVodPlayer.VODPLAYER_STATE_PAUSED);
                    if (mCallback != null) {    // 直接回调给上层
                        mCallback.onVodPlayingError(mMediaInfo.mMediaUrl, what);
                    }
                    return false;
                }
            });

            // 设置显示控件
            mIjkPlayer.setDisplay(mDisplayView.getHolder());

        } catch (IOException ioExp) {
            ioExp.printStackTrace();
            ALog.getInstance().e(TAG, "<open> [IO_EXP] mediaUrl=" + mediaUrl + ", ioExp=" + ioExp);
            mIjkPlayer.release();
            mIjkPlayer = null;
            mState.setValue(IVodPlayer.VODPLAYER_STATE_CLOSED);
            return ErrCode.XERR_FILE_OPEN;

        } catch (IllegalArgumentException illegalExp) {
            illegalExp.printStackTrace();
            ALog.getInstance().e(TAG, "<open> [ILLEGAL_EXP] mediaUrl=" + mediaUrl + ", illegalExp=" + illegalExp);
            mIjkPlayer.release();
            mIjkPlayer = null;
            mState.setValue(IVodPlayer.VODPLAYER_STATE_CLOSED);
            return ErrCode.XERR_FILE_OPEN;

        } catch (SecurityException securityExp) {
            securityExp.printStackTrace();
            ALog.getInstance().e(TAG, "<open> [SECURITY_EXP] mediaUrl=" + mediaUrl + ", securityExp=" + securityExp);
            mIjkPlayer.release();
            mIjkPlayer = null;
            mState.setValue(IVodPlayer.VODPLAYER_STATE_CLOSED);
            return ErrCode.XERR_FILE_OPEN;

        } catch (Exception exp) {
            exp.printStackTrace();
            ALog.getInstance().e(TAG, "<open> [EXP] mediaUrl=" + mediaUrl + ", exp=" + exp);
            mIjkPlayer.release();
            mIjkPlayer = null;
            mState.setValue(IVodPlayer.VODPLAYER_STATE_CLOSED);
            return ErrCode.XERR_FILE_OPEN;
        }


        ALog.getInstance().e(TAG, "<open> done, mediaUrl=" + mediaUrl);
        return ErrCode.XOK;
    }

    @Override
    public void close() {
        if (mIjkPlayer != null) {
            mIjkPlayer.release();
            mIjkPlayer = null;
            mMediaInfo = null;
            mState.setValue(IVodPlayer.VODPLAYER_STATE_CLOSED);
            ALog.getInstance().d(TAG, "<close> done");
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
            mState.setValue(IVodPlayer.VODPLAYER_STATE_PAUSED);

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
            mIjkPlayer.seekTo(seekPos);

        } catch (IllegalStateException illegalExp) {
            illegalExp.printStackTrace();
            ALog.getInstance().e(TAG, "<seek> [ILLEGAL_EXP] illegalExp=" + illegalExp);
            return ErrCode.XERR_PLAYER_PLAY;
        }

        ALog.getInstance().d(TAG, "<seek> done, seekPos=" + seekPos);
        return ErrCode.XOK;
    }

}
