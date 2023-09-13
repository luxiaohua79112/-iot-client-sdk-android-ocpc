package io.agora.iotlinkdemo.models.home;


import android.annotation.SuppressLint;
import android.media.MediaDrm;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.agora.baselibrary.utils.StringUtils;

import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IDeviceSessionMgr;
import io.agora.iotlink.IVodPlayer;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.base.BaseViewBindingFragment;
import io.agora.iotlinkdemo.base.PermissionHandler;
import io.agora.iotlinkdemo.base.PermissionItem;
import io.agora.iotlinkdemo.databinding.FragmentHomeCloudrcdBinding;
import io.agora.iotlinkdemo.presistentconnect.PresistentLinkComp;


public class CloudRcdFragment extends BaseViewBindingFragment<FragmentHomeCloudrcdBinding>
        implements PermissionHandler.ICallback, IDeviceSessionMgr.ISessionCallback,
                    IVodPlayer.ICallback    {

    private static final String TAG = "IOTLINK/CloudRcdFrag";
    private static final long TIMER_INTERVAL = 500;       ///< 定时器500ms


    //
    // message Id
    //
    private static final int MSGID_PLAYING_TIMER = 0x2001;       ///< 播放定时器



    ///////////////////////////////////////////////////////////////////////////
    ///////////////////// Variable Definition /////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    private PermissionHandler mPermHandler;             ///< 权限申请处理
    private Handler mMsgHandler = null;                 ///< 主线程中的消息处理

    private MainActivity mMainActivity;
    private CloudRcdFragment mFragment;
    private IVodPlayer mVodPlayer;
    private IVodPlayer.VodMediaInfo mVodMediaInfo;


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
        mVodPlayer = AIotAppSdkFactory.getVodPlayer();
        mVodPlayer.setDisplayView(getBinding().svDisplayView);
        getBinding().sbPlaying.setEnabled(false);
        getBinding().sbPlaying.setProgress(0);
        getBinding().btnPlayPause.setEnabled(false);

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
        mMsgHandler.removeMessages(MSGID_PLAYING_TIMER);
    }

    @Override
    public void onResume() {
        super.onResume();
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
    //////////////////////////// Event & Widget Methods  ///////////////////////
    ///////////////////////////////////////////////////////////////////////////
    /**
     * @brief 打开或者关闭媒体文件 按钮
     */
    void onBtnOpenClose(View view) {
        //String mediaFilePath = "/sdcard/test.mp4";
        //String mediaFilePath = "http://cloud-store-test.s3.cn-east-1.jdcloud-oss.com/ts-muxer.m3u8";
        String mediaFilePath = "https://stream-media.s3.cn-north-1.jdcloud-oss.com/iot-three/726181688096107589_1694508560758_711350438.m3u8";

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
}
