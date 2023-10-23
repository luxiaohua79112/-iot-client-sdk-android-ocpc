package io.agora.iotlinkdemo.models.home;


import android.annotation.SuppressLint;
import android.media.MediaDrm;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.agora.baselibrary.utils.StringUtils;

import java.io.File;

import io.agora.avmodule.AvMediaConverter;
import io.agora.avmodule.AvMediaInfo;
import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.iotlink.AlarmVideoDownloader;
import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IDeviceSessionMgr;
import io.agora.iotlink.IVodPlayer;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.base.BaseViewBindingFragment;
import io.agora.iotlinkdemo.base.PermissionHandler;
import io.agora.iotlinkdemo.base.PermissionItem;
import io.agora.iotlinkdemo.databinding.FragmentHomeCloudrcdBinding;
import io.agora.iotlinkdemo.presistentconnect.PresistentLinkComp;
import io.agora.iotlinkdemo.utils.FileUtils;


public class CloudRcdFragment extends BaseViewBindingFragment<FragmentHomeCloudrcdBinding>
        implements PermissionHandler.ICallback, IDeviceSessionMgr.ISessionCallback,
                    IVodPlayer.ICallback, AlarmVideoDownloader.ICallback,
        AvMediaConverter.IAvMediaCvtCallback    {

    private static final String TAG = "IOTLINK/CloudRcdFrag";
    private static final long TIMER_INTERVAL = 500;       ///< 定时器500ms


    //
    // message Id
    //
    private static final int MSGID_PLAYING_TIMER = 0x2001;       ///< 播放定时器
    private static final int MSGID_DNLOADING_TIMER = 0x2002;     ///< 下载定时器



    ///////////////////////////////////////////////////////////////////////////
    ///////////////////// Variable Definition /////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    private PermissionHandler mPermHandler;             ///< 权限申请处理
    private Handler mMsgHandler = null;                 ///< 主线程中的消息处理

    private MainActivity mMainActivity;
    private CloudRcdFragment mFragment;
    private IVodPlayer mVodPlayer;
    private IVodPlayer.VodMediaInfo mVodMediaInfo;

    private AlarmVideoDownloader mDownloader;
    private AvMediaInfo mDnloadMediaInfo;

    private AvMediaConverter mMediaCvter;
    private AvMediaInfo mSrcMediaInfo;


    ///////////////////////////////////////////////////////////////////////////
    //////////////////// Methods of Override BaseFragment /////////////////////
    ///////////////////////////////////////////////////////////////////////////
    @NonNull
    @Override
    protected FragmentHomeCloudrcdBinding getViewBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentHomeCloudrcdBinding.inflate(inflater);
    }

    @Override
    public void initView() {
        mMainActivity = (MainActivity)getActivity();
        mFragment = this;

        getBinding().titleView.hideLeftImage();

        // 播放控件初始化
        mVodPlayer = AIotAppSdkFactory.getVodPlayer();
        mVodPlayer.setDisplayView(getBinding().svDisplayView);
        getBinding().sbPlaying.setEnabled(false);
        getBinding().sbPlaying.setProgress(0);
        getBinding().btnPlayPause.setEnabled(false);

        // 下载控件初始化
        getBinding().btnDownloadOpenclose.setText("打开下载");
        getBinding().btnDownloadStartstop.setText("暂停下载");
        getBinding().btnDownloadStartstop.setEnabled(false);
        getBinding().sbDownloading.setProgress(0);

        // 创建主线程消息处理
        mMsgHandler = new Handler(getActivity().getMainLooper())
        {
            @SuppressLint("HandlerLeak")
            @RequiresApi(api = Build.VERSION_CODES.M)
            public void handleMessage(Message msg)
            {
                switch (msg.what)
                {
                    case MSGID_PLAYING_TIMER:
                        onMessagePlayingTimer();
                        break;

                    case MSGID_DNLOADING_TIMER:
                        onMessageDnloadingTimer();
                        break;
                }
            }
        };


        //
        //
        // Microphone权限判断处理
        //
        int[] permIdArray = new int[1];
        permIdArray[0] = PermissionHandler.PERM_ID_RECORD_AUDIO;
        mPermHandler = new PermissionHandler(getActivity(), this, permIdArray);
        if (!mPermHandler.isAllPermissionGranted()) {
            Log.d(TAG, "<initView> requesting permission...");
            mPermHandler.requestNextPermission();
        } else {
            Log.d(TAG, "<initView> permission ready");

        }

        Log.d(TAG, "<initView> done");
    }

    @Override
    public void initListener() {

        getBinding().titleView.setRightIconClick(view -> {

        });

        getBinding().btnOpenClose.setOnClickListener(view -> {
            onBtnOpenClose(view);
        });

        getBinding().btnPlayPause.setOnClickListener(view -> {
            onBtnPlayPause(view);
        });

        getBinding().btn1x.setOnClickListener(view -> {
            onBtnPlaySpeed(view,1.0f);
        });
        getBinding().btn2x.setOnClickListener(view -> {
            onBtnPlaySpeed(view,2.0f);
        });
        getBinding().btn3x.setOnClickListener(view -> {
            onBtnPlaySpeed(view,3.0f);
        });
        getBinding().btn4x.setOnClickListener(view -> {
            onBtnPlaySpeed(view,4.0f);
        });

        getBinding().sbPlaying.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mVodMediaInfo == null) {
                    return;
                }
                int playState = mVodPlayer.getPlayingState();
                if (playState == IVodPlayer.VODPLAYER_STATE_OPENING ||
                    playState == IVodPlayer.VODPLAYER_STATE_CLOSED) {
                    return;
                }

                long seekTime = seekBar.getProgress() * mVodMediaInfo.mDuration / 1000;
                int ret = mVodPlayer.seek(seekTime);

            }
        });

        getBinding().sbVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mVodMediaInfo == null) {
                    return;
                }
                float volumeLevel = (seekBar.getProgress() * 1.0f) / 100.0f;
                mVodPlayer.setVolume(volumeLevel);
            }
        });

        getBinding().btnDownloadOpenclose.setOnClickListener(view -> {
            //onBtnDnloadOpenClose(view);
            onBtnCvterOpenClose(view);
        });

        getBinding().btnDownloadStartstop.setOnClickListener(view -> {
            //onBtnDnloadStartStop(view);
            onBtnCvterStartStop(view);
        });

        getBinding().sbDownloading.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
//                if (mVodMediaInfo == null) {
//                    return;
//                }
//                float volumeLevel = (seekBar.getProgress() * 1.0f) / 100.0f;
//                mVodPlayer.setVolume(volumeLevel);
            }
        });

        // 注册回调函数
        SurfaceHolder surfaceHolder = getBinding().svDisplayView.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(TAG, "<initListener.surfaceCreated> ");
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.d(TAG, "<initListener.surfaceChanged> format=" + format
                    + ", width=" + width + ", height=" + height);
                mVodPlayer.setDisplayView(getBinding().svDisplayView);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d(TAG, "<initListener.surfaceDestroyed>");
            }
        });

        Log.d(TAG, "<initListener> done");
    }

    public void onFragRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        Log.d(TAG, "<onFragRequestPermissionsResult> requestCode=" + requestCode);
        if (mPermHandler != null) {
            mPermHandler.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


    @Override
    public void onAllPermisonReqDone(boolean allGranted, final PermissionItem[] permItems) {
        Log.d(TAG, "<onAllPermisonReqDone> allGranted = " + allGranted);

        if (permItems[0].requestId == PermissionHandler.PERM_ID_CAMERA) {  // Camera权限结果
            if (allGranted) {
                //PagePilotManager.pageDeviceAddScanning();
            } else {
                popupMessage(getString(R.string.no_permission));
            }

        } else if (permItems[0].requestId == PermissionHandler.PERM_ID_RECORD_AUDIO) { // 麦克风权限结果
            if (allGranted) {
            //    doCallDial(mSelectedDev);
            } else {
                popupMessage(getString(R.string.no_permission));
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "<onStart> ");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "<onStop> ");
    }

    @Override
    public void onResume() {
        super.onResume();

//        // 延迟调用查询当前MCU固件版本版本
//        new android.os.Handler(Looper.getMainLooper()).postDelayed(
//                new Runnable() {
//                    public void run() {
//                        mVodPlayer.setDisplayView(getBinding().svDisplayView);
//                    }
//                },
//                100);

        Log.d(TAG, "<onResume> ");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "<onPause> ");
    }

    /**
     * @brief 响应 Back按键处理
     * @return 如果 Fragment已经处理返回 true; 否则返回false
     */
    boolean onBackKeyEvent() {
        return false;
    }


    ///////////////////////////////////////////////////////////////////////////
    //////////////////////////////// 云录视频播放处理  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    /**
     * @brief 打开或者关闭媒体文件 按钮
     */
    void onBtnOpenClose(View view) {
        //String mediaFilePath = "/sdcard/test.mp4";
        //String mediaFilePath = "http://cloud-store-test.s3.cn-east-1.jdcloud-oss.com/ts-muxer.m3u8";
        //String mediaFilePath = "https://stream-media.s3.cn-north-1.jdcloud-oss.com/iot-three/726181688096107589_1694508560758_711350438.m3u8";
        //String mediaFilePath = "https://stream-media.s3.cn-north-1.jdcloud-oss.com/iot-seven/766781697442934487_1697453301090_2080565081.m3u8?agora-key=NzgxNTU1MDY4MTJhMTYxMQ==";
        String mediaFilePath = "https://s3.cn-north-1.jdcloud-oss.com/stream-media/testVideo/output.m3u8";

        int playState = mVodPlayer.getPlayingState();
        if (playState == IVodPlayer.VODPLAYER_STATE_CLOSED) {
            // 打开媒体文件
            int ret = mVodPlayer.open(mediaFilePath, this);
            if (ret != ErrCode.XOK) {
                popupMessage("Fail to open file: " + mediaFilePath);
                return;
            }
            getBinding().sbPlaying.setEnabled(false);
            getBinding().btnPlayPause.setEnabled(false);

        } else {
            // 关闭媒体文件
            mVodPlayer.close();
            popupMessage("Closed media file!");
            mMsgHandler.removeMessages(MSGID_PLAYING_TIMER);
            setUiStateToClosed();
        }

    }

    /**
     * @brief 播放/暂停 按钮
     */
    void onBtnPlayPause(View view) {
        int playState = mVodPlayer.getPlayingState();
        if (playState == IVodPlayer.VODPLAYER_STATE_PAUSED) {
            // resume
            mVodPlayer.play();
            getBinding().btnPlayPause.setText("暂停");

        } else if (playState == IVodPlayer.VODPLAYER_STATE_PLAYING) {
            // Pause
            mVodPlayer.pause();
            getBinding().btnPlayPause.setText("播放");
        }
    }

    /**
     * @brief 设置播放倍速
     */
    void onBtnPlaySpeed(View view, float speed) {
        mVodPlayer.setSpeed(speed);
    }

    /**
     * @brief 播放定时器处理
     */
    void onMessagePlayingTimer() {
        int playState = mVodPlayer.getPlayingState();
        if (playState == IVodPlayer.VODPLAYER_STATE_CLOSED) {
            return;
        }

        if (playState == IVodPlayer.VODPLAYER_STATE_PLAYING ||
            playState == IVodPlayer.VODPLAYER_STATE_PAUSED ) {

            long position = mVodPlayer.getPlayingProgress();
            int progress = (int)(position * 1000L / mVodMediaInfo.mDuration);
            getBinding().sbPlaying.setProgress(progress);

            String textPosition = StringUtils.INSTANCE.getDurationTimeSS(position / 1000);
            String textDuration = StringUtils.INSTANCE.getDurationTimeSS(mVodMediaInfo.mDuration / 1000);
            String textTime = textPosition + " / " + textDuration;
            getBinding().tvTime.setText(textTime);
        }

        mMsgHandler.sendEmptyMessageDelayed(MSGID_PLAYING_TIMER, TIMER_INTERVAL);
    }

    void setUiStateToOpened() {
        getBinding().btnOpenClose.setText("关闭");
        getBinding().btnPlayPause.setText("暂停");
        getBinding().tvTime.setText("00:00:00 / 00:00:00");
        getBinding().sbPlaying.setProgress(0);
        getBinding().sbPlaying.setEnabled(true);
        getBinding().btnPlayPause.setEnabled(true);

    }

    void setUiStateToClosed() {
        getBinding().btnOpenClose.setText("打开");
        getBinding().btnPlayPause.setText("播放");
        getBinding().tvTime.setText("00:00:00 / 00:00:00");
        getBinding().sbPlaying.setProgress(0);
        getBinding().sbPlaying.setEnabled(false);
        getBinding().btnPlayPause.setEnabled(false);
    }


    ///////////////////////////////////////////////////////////////////////////
    //////////////// Override Methods of IVodPlayer.ICallback  ////////////////
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public void onVodPlayingStateChanged(final String mediaUrl, int newState) {
        Log.d(TAG, "<onVodPlayingStateChanged> mediaUrl=" + mediaUrl + ", newState=" + newState);
    }

    @Override
    public void onVodOpenDone(final String mediaUrl) {
        Log.d(TAG, "<onVodOpenDone> mediaUrl=" + mediaUrl);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                hideLoadingView();
                setUiStateToOpened();
                mVodMediaInfo = mVodPlayer.getMediaInfo();
                mMsgHandler.sendEmptyMessage(MSGID_PLAYING_TIMER);  // 开始刷新进度

                popupMessage("Opened media file: " + mediaUrl);
            }
        });
    }

    @Override
    public void onVodPlayingDone(final String mediaUrl, long duration) {
        Log.d(TAG, "<onVodPlayingDone> mediaUrl=" + mediaUrl + ", duration=" + duration);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVodPlayer.close();
                setUiStateToClosed();
                mMsgHandler.removeMessages(MSGID_PLAYING_TIMER);
                popupMessage("Media playing completed!");
            }
        });
    }

    @Override
    public void onVodPlayingError(final String mediaUrl, int errCode) {
        Log.d(TAG, "<onVodPlayingError> mediaUrl=" + mediaUrl + ", errCode=" + errCode);
    }

    @Override
    public void onVodSeekingDone(final String mediaUrl, long seekPos) {
        Log.d(TAG, "<onVodSeekingDone> mediaUrl=" + mediaUrl + ", seekPos=" + seekPos);
    }



    ///////////////////////////////////////////////////////////////////////////
    //////////////////////////////// 云录视频下载处理  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    /**
     * @brief 打开或者关闭下载 按钮
     */
    void onBtnDnloadOpenClose(View view) {
        //String mediaFilePath = "/sdcard/test.mp4";
        //String mediaFilePath = "http://cloud-store-test.s3.cn-east-1.jdcloud-oss.com/ts-muxer.m3u8";
        //String cloudFilePath = "https://stream-media.s3.cn-north-1.jdcloud-oss.com/0000000/output.m3u8";
        String cloudFilePath = "https://s3.cn-north-1.jdcloud-oss.com/stream-media/testVideo/output.m3u8";

        File file = getActivity().getExternalFilesDir(null);
        String cachePath = file.getAbsolutePath();
        String timeText = StringUtils.INSTANCE.getDetailTime("yyyy-MM-dd_HH_mm_ss", System.currentTimeMillis() / 1000);
        String localFilePath = cachePath + "/download01.mp4";

        if (mDownloader == null) {
            // 打开下载器
            mDownloader = new AlarmVideoDownloader();
            int ret = mDownloader.open(cloudFilePath, localFilePath, this);
            if (ret != ErrCode.XOK) {
                popupMessage("Fail to open downloader, couldFile=" + cloudFilePath);
                return;
            }
            getBinding().sbDownloading.setProgress(0);
            getBinding().btnDownloadStartstop.setEnabled(false);

        } else {
            // 关闭下载器
            mDownloader.close();
            mDownloader = null;
            popupMessage("Closed downloader!");
            mMsgHandler.removeMessages(MSGID_DNLOADING_TIMER);
            setDnloadUiStateToClosed();
        }
    }

    /**
     * @brief 开始/暂停 下载 按钮
     */
    void onBtnDnloadStartStop(View view) {
        if (mDownloader == null) {
            return;
        }

        int dnloadState = mDownloader.getState();
        if (dnloadState == AlarmVideoDownloader.DOWNLOAD_STATE_PAUSED) {
            // resume
            mDownloader.resume();
            getBinding().btnDownloadStartstop.setText("暂停下载");

        } else if (dnloadState == AlarmVideoDownloader.DOWNLOAD_STATE_ONGOING) {
            // Pause
            mDownloader.pause();
            getBinding().btnDownloadStartstop.setText("恢复下载");
        }
    }


    /**
     * @brief 下载定时器处理
     */
    void onMessageDnloadingTimer() {
        if (mDownloader == null) {
            return;
        }

        int dnloadState = mDownloader.getState();
        if (dnloadState == AlarmVideoDownloader.DOWNLOAD_STATE_ONGOING ||
                dnloadState == AlarmVideoDownloader.DOWNLOAD_STATE_PAUSED ) {

            long videoTimestamp = mDownloader.getVideoTimestamp();
            int progress = (int)(videoTimestamp * 1000L / mDnloadMediaInfo.mVideoDuration);
            getBinding().sbDownloading.setProgress(progress);

            long videoSec = videoTimestamp / 1000000L;
            long durationSec = mDnloadMediaInfo.mVideoDuration / 1000000L;
            String textPosition = StringUtils.INSTANCE.getDurationTimeSS(videoSec);
            String textDuration = StringUtils.INSTANCE.getDurationTimeSS(durationSec);
            String textTime = textPosition + " / " + textDuration;
            getBinding().tvTimeDownload.setText(textTime);
        }

        mMsgHandler.sendEmptyMessageDelayed(MSGID_DNLOADING_TIMER, TIMER_INTERVAL);
    }

    void setDnloadUiStateToOpened() {
        getBinding().btnDownloadOpenclose.setText("关闭下载");
        getBinding().btnDownloadStartstop.setText("暂停下载");
        getBinding().tvTimeDownload.setText("00:00:00 / 00:00:00");
        getBinding().sbDownloading.setProgress(0);
        getBinding().sbDownloading.setEnabled(true);
        getBinding().btnDownloadStartstop.setEnabled(true);

    }

    void setDnloadUiStateToClosed() {
        getBinding().btnDownloadOpenclose.setText("打开下载");
        getBinding().btnDownloadStartstop.setText("暂停下载");
        getBinding().tvTimeDownload.setText("00:00:00 / 00:00:00");
        getBinding().sbDownloading.setProgress(0);
        getBinding().sbDownloading.setEnabled(false);
        getBinding().btnDownloadStartstop.setEnabled(false);
    }


    ///////////////////////////////////////////////////////////////////////////
    /////////// Override Methods of AlarmVideoDownloader.ICallback /////////////
    ///////////////////////////////////////////////////////////////////////////
    @Override
    public void onDownloadPrepared(final String videoUrl, final AvMediaInfo mediaInfo) {
        Log.d(TAG, "<onDownloadPrepared> videoUrl=" + videoUrl + ", mediaInfo=" + mediaInfo);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                hideLoadingView();
                setDnloadUiStateToOpened();
                mDnloadMediaInfo = mediaInfo;
                mMsgHandler.sendEmptyMessage(MSGID_DNLOADING_TIMER);  // 开始刷新进度

                popupMessage("Opened downloader: videoUrl=" + videoUrl);
            }
        });
    }

    @Override
    public void onDownloadDone(final String videoUrl) {
        Log.d(TAG, "<onDownloadDone> videoUrl=" + videoUrl);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDownloader.close();
                mDownloader = null;
                setDnloadUiStateToClosed();
                mMsgHandler.removeMessages(MSGID_PLAYING_TIMER);

                popupMessage("Media donloading completed!");
            }
        });
    }

    @Override
    public void onDownloadError(final String videoUrl, int errCode) {
        Log.d(TAG, "<onDownloadError> videoUrl=" + videoUrl + ", errCode=" + errCode);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDownloader.close();
                mDownloader = null;
                setDnloadUiStateToClosed();
                mMsgHandler.removeMessages(MSGID_PLAYING_TIMER);

                popupMessage("Media downloading error, errCode=" + errCode);
            }
        });
    }


    ///////////////////////////////////////////////////////////////////////////
    //////////////////////////////// 云录视频转换处理  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    void onBtnCvterOpenClose(View view) {
        int ret;

        if (mMediaCvter == null) {
            //String srcFileUrl = "https://stream-media.s3.cn-north-1.jdcloud-oss.com/0000000/output.m3u8";
            //String srcFileUrl = "/sdcard/test.mp4";
            //String srcFileUrl = "https://stream-media.s3.cn-north-1.jdcloud-oss.com/iot-seven/766781697442934487_1697453301090_2080565081.m3u8?agora-key=NzgxNTU1MDY4MTJhMTYxMQ==";
            String srcFileUrl = "https://s3.cn-north-1.jdcloud-oss.com/stream-media/testVideo/output.m3u8";

            File file = getActivity().getExternalFilesDir(null);
            String cachePath = file.getAbsolutePath();
            String dstFilePath = cachePath + "/converted.mp4";
            FileUtils.deleteFile(dstFilePath);

            AvMediaConverter.MediaCvtParam cvtParam = new AvMediaConverter.MediaCvtParam();
            cvtParam.mCallback = this;
            cvtParam.mContext = this.getContext();
            cvtParam.mSrcFileUrl = srcFileUrl;
            cvtParam.mDstFilePath = dstFilePath;

            mMediaCvter = new AvMediaConverter();
            ret = mMediaCvter.initialize(cvtParam);
            if (ret != ErrCode.XOK) {
                popupMessage("Fail to open converter, errCode=" + ret);
                mMediaCvter = null;
                return;
            }
            popupMessage("Opening media converter ......");

        } else {
            mMediaCvter.release();
            mMediaCvter = null;
            popupMessage("Media converter closed!");

            getBinding().btnDownloadOpenclose.setText("打开下载");
            getBinding().btnDownloadStartstop.setEnabled(false);
            getBinding().btnDownloadStartstop.setText("开始下载");
        }
    }

    void onBtnCvterStartStop(View view) {
        if (mMediaCvter == null) {
            return;
        }

        int state = mMediaCvter.getState();
        if (state == AvMediaConverter.CONVERT_STATE_PAUSED) {
            mMediaCvter.start();
            getBinding().btnDownloadStartstop.setText("暂停下载");

        } else if (state == AvMediaConverter.CONVERT_STATE_ONGOING) {
            mMediaCvter.stop();
            getBinding().btnDownloadStartstop.setText("开始下载");
        }
    }

    //////////////////////////////////////////////////////////////////////////////////
    /////////// Override Methods of AvMediaConverter.IAvMediaCvtCallback /////////////
    /////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onMediaCvtOpenDone(AvMediaConverter.MediaCvtParam cvtParam, int errCode) {
        Log.d(TAG, "<onMediaCvtOpenDone> errCode=" + errCode);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                hideLoadingView();
                if (errCode == ErrCode.XOK) {
                    popupMessage("Open source stream successful!");
                    getBinding().btnDownloadOpenclose.setText("关闭下载");
                    getBinding().btnDownloadStartstop.setEnabled(true);
                    getBinding().btnDownloadStartstop.setText("开始下载");

                } else {
                    popupMessage("Open source stream failure, errCode=" + errCode);
                }
            }
        });
    }

    @Override
    public void onMediaConvertingDone(AvMediaConverter.MediaCvtParam cvtParam, long totalDuration) {
        Log.d(TAG, "<onMediaConvertingDone> totalDuration=" + totalDuration);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                popupMessage("Media converting finished!");
            }
        });
    }

    @Override
    public void onMediaConvertingError(AvMediaConverter.MediaCvtParam cvtParam, int errCode) {
        Log.d(TAG, "<onMediaConvertingError> errCode=" + errCode);
    }
}
