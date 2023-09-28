package io.agora.iotlinkdemo.models.devctrl;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.ActivityKt;
import androidx.navigation.NavController;
import androidx.navigation.ui.BottomNavigationViewKt;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.listener.ISingleCallback;
import com.agora.baselibrary.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.iotlink.ErrCode;
//import io.agora.iotlink.ICallkitMgr;
import io.agora.iotlink.IDevController;
import io.agora.iotlink.IDevMediaMgr;
import io.agora.iotlink.IDeviceSessionMgr;
import io.agora.iotlink.IVodPlayer;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.base.AgoraApplication;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.base.PushApplication;
import io.agora.iotlinkdemo.databinding.ActivityDevCtrlBinding;
import io.agora.iotlinkdemo.databinding.ActivityMainBinding;
import io.agora.iotlinkdemo.dialog.CommonDialog;
import io.agora.iotlinkdemo.dialog.DialogImageDisplay;
import io.agora.iotlinkdemo.dialog.NoPowerDialog;
import io.agora.iotlinkdemo.models.home.DeviceInfo;
import io.agora.iotlinkdemo.models.home.DeviceListAdapter;
import io.agora.iotlinkdemo.presistentconnect.PresistentLinkComp;


public class DevCtrlActivity extends BaseViewBindingActivity<ActivityDevCtrlBinding>
    implements IDevMediaMgr.IPlayingCallback    {

    private static final String TAG = "IOTLINK/DevCtrlAct";
    private static final long TIMER_INTERVAL = 500;       ///< 定时器500ms
    private static final long MEDIA_DURATION = 60000;      ///< 固定时长 60s

    //
    // message Id
    //
    private static final int MSGID_PLAYING_TIMER = 0x3001;       ///< 播放定时器




    public static DevCtrlActivity mActivity;
    private UUID mSessionId = null;
    private FileListAdapter mFileListAdapter;

    private DialogImageDisplay mImgDisplayDlg;

    private Handler mMsgHandler = null;                 ///< 主线程中的消息处理

    ///////////////////////////////////////////////////////////////////////////
    //////////////////// Methods of Override BaseActivity /////////////////////
    ///////////////////////////////////////////////////////////////////////////
    @Override
    protected ActivityDevCtrlBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityDevCtrlBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        mActivity = this;
        IDeviceSessionMgr sessionMgr = AIotAppSdkFactory.getDevSessionMgr();
        mSessionId = PushApplication.getInstance().getFullscrnSessionId();
        IDeviceSessionMgr.SessionInfo sessionInfo = sessionMgr.getSessionInfo(mSessionId);
        getBinding().tvNodeId.setText(sessionInfo.mPeerDevId);

        //
        // 初始化文件列表
        //
        List<FileInfo> fileList = new ArrayList<>();

        if (mFileListAdapter == null) {
            mFileListAdapter = new FileListAdapter(fileList);
            mFileListAdapter.setOwner(this);
            mFileListAdapter.setRecycleView(getBinding().rvFileList);
            getBinding().rvFileList.setLayoutManager(new LinearLayoutManager(this));
            getBinding().rvFileList.setAdapter(mFileListAdapter);
            mFileListAdapter.setMRVItemClickListener((view, position, data) -> {
            });
        }

        // 设置SD卡播放的视频控件
        IDevMediaMgr mediaMgr = sessionMgr.getDevMediaMgr(mSessionId);
        if (mediaMgr == null) {
            popupMessage("Not found device media mgr with sessionId=" + mSessionId);
            return;
        }
        mediaMgr.setDisplayView(getBinding().svDeviceView);


        // 创建主线程消息处理
        mMsgHandler = new Handler(getMainLooper())
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

        getBinding().sbProgress.setProgress(0);

        Log.d(TAG, "<initView> ");
    }

    @Override
    protected boolean isCanExit() {
        return false;
    }

    @Override
    public void initListener() {
        getBinding().btnPlzCtrl.setOnClickListener(view -> {
            onBtnPlzCtrl(view);
        });

        getBinding().btnPlzStop.setOnClickListener(view -> {
            onBtnPlzStop(view);
        });

        getBinding().btnPlzReset.setOnClickListener(view -> {
            onBtnPlzReset(view);
        });

        getBinding().btnSdcardFmt.setOnClickListener(view -> {
            onBtnSdcardFormat(view);
        });

        getBinding().btnMediaQuery.setOnClickListener(view -> {
            onBtnMediaQuery(view);
        });

        getBinding().btnMediaDelete.setOnClickListener(view -> {
            onBtnMediaDelete(view);
        });

        getBinding().btnMediaCover.setOnClickListener(view -> {
            onBtnMediaCover(view);
        });

        getBinding().btnCustomizeCmd.setOnClickListener(view -> {
            onBtnCustomizeCmd(view);
        });

        getBinding().btnPlayGlobaltime.setOnClickListener(view -> {
            onBtnPlayGlobalTime(view);
        });

        getBinding().btnPlayStop.setOnClickListener(view -> {
            onBtnPlayStop(view);
        });

        getBinding().btnPauseResume.setOnClickListener(view -> {
            onBtnPauseResume(view);
        });

        getBinding().btnSpeed.setOnClickListener(view -> {
            onBtnPlaySpeed(view);
        });

        getBinding().btnFileDownload.setOnClickListener(view -> {
            onBtnDownloadFile(view);
        });

        getBinding().btnEventQuery.setOnClickListener(view -> {
            onBtnEventQuery(view);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "<onStart> mSessionId=" + mSessionId);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "<onStop> ");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "<onResume> ");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "<onPause> ");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "<onDestroy> ");
        mSessionId = null;
        mActivity = null;
        mMsgHandler.removeMessages(MSGID_PLAYING_TIMER);
    }


    ///////////////////////////////////////////////////////////////////////////
    //////////////////////////// Event & Widget Methods  ///////////////////////
    ///////////////////////////////////////////////////////////////////////////
    /**
    * @brief 云台旋转控制
    */
    void onBtnPlzCtrl(View view) {
        IDeviceSessionMgr sessionMgr = AIotAppSdkFactory.getDevSessionMgr();
        IDevController devController = sessionMgr.getDevController(mSessionId);
        if (devController == null) {
            popupMessage("Not found device controller with sessionId=" + mSessionId);
            return;
        }

        int action = 0;
        int direction = 1;
        int speed = 1;
        showLoadingView();
        int ret = devController.sendCmdPtzCtrl(action, direction, speed, new IDevController.OnCommandCmdListener() {
            @Override
            public void onDeviceCmdDone(int errCode, String respData) {
                Log.d(TAG, "<onBtnPlzCtrl.onDeviceCmdDone> errCode=" + errCode);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (errCode != ErrCode.XOK) {  // 控制命令失败
                            hideLoadingView();
                            popupMessage("Fail to plz control, errCode=" + errCode);

                        } else {
                            hideLoadingView();
                            popupMessage("Successful to plz control!");
                        }
                    }
                });
            }
        });
        if (ret != ErrCode.XOK) {
            hideLoadingView();
            popupMessage("Fail to plz control, errCode=" + ret);
        }

    }

    /**
     * @brief 云台控制停止
     */
    void onBtnPlzStop(View view) {
        IDeviceSessionMgr sessionMgr = AIotAppSdkFactory.getDevSessionMgr();
        IDevController devController = sessionMgr.getDevController(mSessionId);
        if (devController == null) {
            popupMessage("Not found device controller with sessionId=" + mSessionId);
            return;
        }

        showLoadingView();
        int ret = devController.sendCmdPtzCtrl(1, 1, 1, new IDevController.OnCommandCmdListener() {
            @Override
            public void onDeviceCmdDone(int errCode, String respData) {
                Log.d(TAG, "<onBtnPlzStop.onDeviceCmdDone> errCode=" + errCode);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (errCode != ErrCode.XOK) {  // 控制命令失败
                            hideLoadingView();
                            popupMessage("Fail to plz stop, errCode=" + errCode);

                        } else {
                            hideLoadingView();
                            popupMessage("Successful to plz stop!");
                        }
                    }
                });
            }
        });
        if (ret != ErrCode.XOK) {
            hideLoadingView();
            popupMessage("Fail to plz stop, errCode=" + ret);
        }
    }

   /**
    * @brief 云台校准
    */
    void onBtnPlzReset(View view) {
        IDeviceSessionMgr sessionMgr = AIotAppSdkFactory.getDevSessionMgr();
        IDevController devController = sessionMgr.getDevController(mSessionId);
        if (devController == null) {
            popupMessage("Not found device controller with sessionId=" + mSessionId);
            return;
        }

        showLoadingView();
        int ret = devController.sendCmdPtzReset( new IDevController.OnCommandCmdListener() {
            @Override
            public void onDeviceCmdDone(int errCode, String respData) {
                Log.d(TAG, "<onBtnPlzReset.onDeviceCmdDone> errCode=" + errCode);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (errCode != ErrCode.XOK) {  // 校准命令失败
                            hideLoadingView();
                            popupMessage("Fail to plz reset, errCode=" + errCode);

                        } else {
                            hideLoadingView();
                            popupMessage("Successful to plz reset!");
                        }
                    }
                });
            }
        });
        if (ret != ErrCode.XOK) {
            hideLoadingView();
            popupMessage("Fail to plz reset, errCode=" + ret);
        }
    }

    /**
     * @brief SD卡格式化
     */
    void onBtnSdcardFormat(View view) {
        IDeviceSessionMgr sessionMgr = AIotAppSdkFactory.getDevSessionMgr();
        IDevController devController = sessionMgr.getDevController(mSessionId);
        if (devController == null) {
            popupMessage("Not found device controller with sessionId=" + mSessionId);
            return;
        }

        showLoadingView();
        int ret = devController.sendCmdSdcardFmt( new IDevController.OnCommandCmdListener() {
            @Override
            public void onDeviceCmdDone(int errCode, String respData) {
                Log.d(TAG, "<onBtnSdcardFormat.onDeviceCmdDone> errCode=" + errCode);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (errCode != ErrCode.XOK) {  // 格式化命令失败
                            hideLoadingView();
                            popupMessage("Fail to Sdcard format, errCode=" + errCode);

                        } else {
                            hideLoadingView();
                            popupMessage("Successful to Sdcard format!");
                        }
                    }
                });
            }
        });
        if (ret != ErrCode.XOK) {
            hideLoadingView();
            popupMessage("Fail to sdcard format, errCode=" + ret);
        }
    }

    /**
    * @brief SD卡媒体文件查询
    */
    void onBtnMediaQuery(View view) {
        IDeviceSessionMgr sessionMgr = AIotAppSdkFactory.getDevSessionMgr();
        IDevMediaMgr mediaMgr = sessionMgr.getDevMediaMgr(mSessionId);
        if (mediaMgr == null) {
          popupMessage("Not found device media mgr with sessionId=" + mSessionId);
          return;
        }

        long currTimeSec = (System.currentTimeMillis() / 1000);
        IDevMediaMgr.QueryParam queryParam = new IDevMediaMgr.QueryParam();
        queryParam.mFileId = null;
        queryParam.mBeginTimestamp = currTimeSec - (24*3600);
        queryParam.mEndTimestamp = currTimeSec;

        showLoadingView();
        int ret = mediaMgr.queryMediaList(queryParam, new IDevMediaMgr.OnQueryListener() {
          @Override
          public void onDevMediaQueryDone(int errCode, final List<IDevMediaMgr.DevMediaItem> mediaList) {
              Log.d(TAG, "<onBtnMediaQuery.onDevMediaQueryDone> errCode=" + errCode
                            + ", mediaList=" + mediaList);
              runOnUiThread(new Runnable() {
                  @Override
                  public void run() {
                      hideLoadingView();
                      if (errCode != ErrCode.XOK) {  // 查询媒体文件失败
                          popupMessage("Fail to query file list, errCode=" + errCode);
                          return;
                      }

                      List<FileInfo> fileList = new ArrayList<>();
                      int fileCount = mediaList.size();
                      for (int i = 0; i < fileCount; i++) {
                          IDevMediaMgr.DevMediaItem mediaItem = mediaList.get(i);

                          FileInfo newFileInfo = new FileInfo();
                          newFileInfo.mMediaInfo = mediaItem;
                          fileList.add(newFileInfo);
                      }
                      mFileListAdapter.updateItemList(fileList);

                      popupMessage("Successful to query file list, fileCount=" + mediaList.size());
                  }
              });
          }
        });
        if (ret != ErrCode.XOK) {
            hideLoadingView();
          popupMessage("Fail to query media file list, errCode=" + ret);
        }
    }
    /**
    * @brief SD卡媒体文件删除
    */
    void onBtnMediaDelete(View view) {
        IDeviceSessionMgr sessionMgr = AIotAppSdkFactory.getDevSessionMgr();
        IDevMediaMgr mediaMgr = sessionMgr.getDevMediaMgr(mSessionId);
        if (mediaMgr == null) {
            popupMessage("Not found device media mgr with sessionId=" + mSessionId);
            return;
        }

        List<FileInfo> selectedList = mFileListAdapter.getSelectedItems();
        int selectedCount = selectedList.size();
        if (selectedCount <= 0) {
            popupMessage("Please select one file at least!");
            return;
        }

        ArrayList<String> deletingIdList = new ArrayList<>();
        for (int i = 0; i < selectedCount; i++) {
            FileInfo fileInfo = selectedList.get(i);
            deletingIdList.add(fileInfo.mMediaInfo.mFileId);
        }


        showLoadingView();
        int ret = mediaMgr.deleteMediaList(deletingIdList, new IDevMediaMgr.OnDeleteListener() {
            @Override
            public void onDevMediaDeleteDone(int errCode,
                                             final List<IDevMediaMgr.DevMediaDelResult> undeletedList) {
                Log.d(TAG, "<onBtnMediaDelete.onDevMediaDeleteDone> errCode=" + errCode
                        + ", undeletedList=" + undeletedList);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideLoadingView();
                        if (errCode == ErrCode.XOK) {
                            popupMessage("Media files delete successful!");

                        } else if (errCode == ErrCode.XERR_MEDIAMGR_DEL_PARTIAL) {
                            popupMessage("Media files delete only partial  undelCount=" + undeletedList.size());

                        } else  if (errCode == ErrCode.XERR_MEDIAMGR_DEL_EXCEPT) {
                            popupMessage("Media files delete failure: sdcard exception!");

                        } else  if (errCode == ErrCode.XERR_MEDIAMGR_DEL_SDCARD) {
                            popupMessage("Media files delete failure: no sdcard!");

                        } else {
                            popupMessage("Media files delete failure: errCode=" + errCode);
                        }
                    }
                });
            }
        });
        if (ret != ErrCode.XOK) {
            hideLoadingView();
            popupMessage("Fail to delete media file list, errCode=" + ret);
        }
    }

    /**
     * @brief 获取媒体文件封面
     */
    void onBtnMediaCover(View view) {
        IDeviceSessionMgr sessionMgr = AIotAppSdkFactory.getDevSessionMgr();
        IDevMediaMgr mediaMgr = sessionMgr.getDevMediaMgr(mSessionId);
        if (mediaMgr == null) {
            popupMessage("Not found device media mgr with sessionId=" + mSessionId);
            return;
        }

        String imgUrl = "media_01_cover.jpg";

        showLoadingView();
        int ret = mediaMgr.getMediaCoverData(imgUrl, new IDevMediaMgr.OnCoverDataListener() {
            @Override
            public void onDevMediaCoverDataDone(int errCode, final String imgUrl, final byte[] data) {
                 Log.d(TAG, "<onBtnMediaCover.onDevMediaCoverDataDone> errCode=" + errCode
                        + ", imgUrl=" + imgUrl + ", data=" + data);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideLoadingView();
                        if (errCode == ErrCode.XOK) {
                            popupMessage("Media cover get successful!");

                            try {
                                Bitmap recvBmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                                if (recvBmp != null) {
                                    if (mImgDisplayDlg == null) {
                                        mImgDisplayDlg = new DialogImageDisplay(mActivity);
                                    }
                                    mImgDisplayDlg.setDisplayBmp(recvBmp);
                                    mImgDisplayDlg.show();

                                    int width = recvBmp.getWidth();
                                    int height = recvBmp.getHeight();
                                    Log.d(TAG, "<onDevMediaCoverDataDone> width=" + width
                                            + ", height=" + height);
                                }

                            } catch (Exception exp) {
                                exp.printStackTrace();
                            }


                        } else {
                            popupMessage("Media cover get failure: errCode=" + errCode);
                        }
                    }
                });
            }
        });
        if (ret != ErrCode.XOK) {
            hideLoadingView();
            popupMessage("Fail to get media cover, errCode=" + ret);
        }
    }

    /**
     * @brief 定制命令
     */
    void onBtnCustomizeCmd(View view) {
        IDeviceSessionMgr sessionMgr = AIotAppSdkFactory.getDevSessionMgr();
        IDevController devController = sessionMgr.getDevController(mSessionId);
        if (devController == null) {
            popupMessage("Not found device dev controller with sessionId=" + mSessionId);
            return;
        }

        // 自定义命令
        int ret = devController.sendCmdCustomize("This is customize command", new IDevController.OnCommandCmdListener() {
            @Override
            public void onDeviceCmdDone(int errCode, String respData) {
                popupMessage("Customize response: errCode=" + errCode + ", respData=" + respData);
            }
        });
        if (ret != ErrCode.XOK) {
            popupMessage("Fail to send customize command, errCode=" + ret);
            return;
        }
    }


    /**
     * @brief 全局时间轴播放命令
     */
    void onBtnPlayGlobalTime(View view) {
        IDeviceSessionMgr sessionMgr = AIotAppSdkFactory.getDevSessionMgr();
        IDevMediaMgr mediaMgr = sessionMgr.getDevMediaMgr(mSessionId);
        if (mediaMgr == null) {
            popupMessage("Not found device media mgr with sessionId=" + mSessionId);
            return;
        }

        int playingState = mediaMgr.getPlayingState();
        int ret;
        if (playingState != IDevMediaMgr.DEVPLAYER_STATE_STOPPED) {
            popupMessage("Meida is playing!");
            return;
        }

        // 播放媒体文件
        long currTimeSec = (System.currentTimeMillis() / 1000);
        long globalTime = currTimeSec - (12*3600);
        ret = mediaMgr.play(globalTime, 1, this);
        if (ret != ErrCode.XOK) {
            popupMessage("Fail to start Media global timeline playing, errCode=" + ret);
            return;
        }
        getBinding().btnPlayStop.setText("停止");

    }


    /**
    * @brief 媒体文件 播放/停止
    */
    void onBtnPlayStop(View view) {
      IDeviceSessionMgr sessionMgr = AIotAppSdkFactory.getDevSessionMgr();
      IDevMediaMgr mediaMgr = sessionMgr.getDevMediaMgr(mSessionId);
      if (mediaMgr == null) {
          popupMessage("Not found device media mgr with sessionId=" + mSessionId);
          return;
      }

      int playingState = mediaMgr.getPlayingState();
      int ret;
      if (playingState == IDevMediaMgr.DEVPLAYER_STATE_STOPPED) {

          // 播放媒体文件，从 0ms以 1倍速 开始播放
          String fileId = "record01";
          long startTime = 0;
          ret = mediaMgr.play(fileId, startTime, 1, this);
          if (ret != ErrCode.XOK) {
              popupMessage("Fail to start Media playing, errCode=" + ret);
              return;
          }
          getBinding().btnPlayStop.setText("停止");
          progressTimerStart();

      } else {
        // 停止播放
        ret = mediaMgr.stop();
        popupMessage("Media playing stopped!");
        getBinding().btnPlayStop.setText("播放");
        progressTimerStop();
        getBinding().sbProgress.setProgress(0);
        getBinding().tvTime.setText("00:00:00 / 00:00:00");
      }
    }


    /**
    * @brief 媒体文件 暂停/恢复
    */
    void onBtnPauseResume(View view) {
      IDeviceSessionMgr sessionMgr = AIotAppSdkFactory.getDevSessionMgr();
      IDevMediaMgr mediaMgr = sessionMgr.getDevMediaMgr(mSessionId);
      if (mediaMgr == null) {
          popupMessage("Not found device media mgr with sessionId=" + mSessionId);
          return;
      }

      int playingState = mediaMgr.getPlayingState();
      int ret;
      if (playingState == IDevMediaMgr.DEVPLAYER_STATE_PLAYING) {
          // 进行暂停操作
          ret = mediaMgr.pause();
          if (ret != ErrCode.XOK) {
              popupMessage("Paused failed, ret=" + ret);
          }


      } else if (playingState == IDevMediaMgr.DEVPLAYER_STATE_PAUSED) {
          // 进行恢复操作
          ret = mediaMgr.resume();
          if (ret != ErrCode.XOK) {
              popupMessage("Resume failed, ret=" + ret);
          }
      }
    }

    /**
     * @brief 设置播放倍速
     */
    int mPlayingSpeed = 1;
    void onBtnPlaySpeed(View view) {
        IDeviceSessionMgr sessionMgr = AIotAppSdkFactory.getDevSessionMgr();
        IDevMediaMgr mediaMgr = sessionMgr.getDevMediaMgr(mSessionId);
        if (mediaMgr == null) {
            popupMessage("Not found device media mgr with sessionId=" + mSessionId);
            return;
        }

        mPlayingSpeed++;
        if (mPlayingSpeed > 3) {
            mPlayingSpeed = 1;
        }

        int ret = mediaMgr.setPlayingSpeed(mPlayingSpeed);
        if (ret != ErrCode.XOK) {
            popupMessage("Fail to set playing speed, errCode=" + ret);
        } else {
            popupMessage("Successful to set playing speed, mPlayingSpeed=" + mPlayingSpeed);
        }
    }

    /**
     * @brief 启动进度条显示定时器
     */
    boolean progressTimerStart() {

        mMsgHandler.removeMessages(MSGID_PLAYING_TIMER);
        mMsgHandler.sendEmptyMessage(MSGID_PLAYING_TIMER);
        return true;
    }

    /**
     * @brief 结束进度条显示定时器
     */
    void progressTimerStop() {
        mMsgHandler.removeMessages(MSGID_PLAYING_TIMER);
    }


    /**
     * @brief 播放定时器处理
     */
    void onMessagePlayingTimer() {
        IDeviceSessionMgr sessionMgr = AIotAppSdkFactory.getDevSessionMgr();
        IDevMediaMgr mediaMgr = sessionMgr.getDevMediaMgr(mSessionId);
        if (mediaMgr == null) {
            Log.d(TAG, "<onMessagePlayingTimer> Not found device media mgr with sessionId=" + mSessionId);
            return;
        }

        int playState = mediaMgr.getPlayingState();
        if (playState == IDevMediaMgr.DEVPLAYER_STATE_STOPPED) {  // 播放器关闭了
            return;
        }


        long progress = mediaMgr.getPlayingProgress();
        int seekPos = (int)(progress * getBinding().sbProgress.getMax() / MEDIA_DURATION);
        getBinding().sbProgress.setProgress(seekPos);

        String textPosition = StringUtils.INSTANCE.getDurationTimeSS(progress / 1000);
        String textDuration = StringUtils.INSTANCE.getDurationTimeSS(MEDIA_DURATION / 1000);
        String textTime = textPosition + " / " + textDuration;
        getBinding().tvTime.setText(textTime);

        mMsgHandler.sendEmptyMessageDelayed(MSGID_PLAYING_TIMER, TIMER_INTERVAL);
    }


    /**
     * @brief 文件下载处理
     */
    void onBtnDownloadFile(View view) {
        IDeviceSessionMgr sessionMgr = AIotAppSdkFactory.getDevSessionMgr();
        IDevMediaMgr mediaMgr = sessionMgr.getDevMediaMgr(mSessionId);
        if (mediaMgr == null) {
            popupMessage("Not found device media mgr with sessionId=" + mSessionId);
            return;
        }

//        List<FileInfo> selectedList = mFileListAdapter.getSelectedItems();
//        int selectedCount = selectedList.size();
//        if (selectedCount <= 0) {
//            popupMessage("Please select one file at least!");
//            return;
//        }

        ArrayList<String> dnloadingIdList = new ArrayList<>();
//        for (int i = 0; i < selectedCount; i++) {
//            FileInfo fileInfo = selectedList.get(i);
//            dnloadingIdList.add(fileInfo.mMediaInfo.mFileId);
//        }
        dnloadingIdList.add("av_xxx_01");


        showLoadingView();
        int ret = mediaMgr.downloadFileList(dnloadingIdList, new IDevMediaMgr.OnDownloadListener() {
            @Override
            public void onDevFileDownloadDone(int errCode,
                                              final List<IDevMediaMgr.DevFileDownloadResult> downloadList) {
                Log.d(TAG, "<onBtnMediaDelete.onDevFileDownloadDone> errCode=" + errCode
                        + ", downloadList=" + downloadList);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideLoadingView();
                        if (errCode == ErrCode.XOK) {
                            popupMessage("Media files download successful!");

                        } else if (errCode == ErrCode.XERR_MEDIAMGR_DOWNLOAD_PARTIAL) {
                            popupMessage("Media files download only partial downloadCount=" + downloadList.size());

                        } else  if (errCode == ErrCode.XERR_MEDIAMGR_DOWNLOAD_EXCEPT) {
                            popupMessage("Media files download failure: sdcard exception!");

                        } else  if (errCode == ErrCode.XERR_MEDIAMGR_DOWNLOAD_SDCARD) {
                            popupMessage("Media files download failure: no sdcard!");

                        } else {
                            popupMessage("Media files download failure: errCode=" + errCode);
                        }
                    }
                });
            }
        });
        if (ret != ErrCode.XOK) {
            hideLoadingView();
            popupMessage("Fail to download media file list, errCode=" + ret);
        }
    }


    /**
     * @brief 事件分布查询处理
     */
    void onBtnEventQuery(View view) {
        IDeviceSessionMgr sessionMgr = AIotAppSdkFactory.getDevSessionMgr();
        IDevMediaMgr mediaMgr = sessionMgr.getDevMediaMgr(mSessionId);
        if (mediaMgr == null) {
            popupMessage("Not found device media mgr with sessionId=" + mSessionId);
            return;
        }

        showLoadingView();
        int ret = mediaMgr.queryEventTimeline(new IDevMediaMgr.OnQueryEventListener() {
            @Override
            public void onDevQueryEventDone(int errCode, List<Long> videoTimeList) {
                Log.d(TAG, "<onBtnEventQuery.onDevQueryEventDone> errCode=" + errCode
                        + ", videoTimeList=" + videoTimeList);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideLoadingView();
                        popupMessage("Query event timeline done, errCode=" + errCode
                            + ", videoTimeListCount=" + videoTimeList.size());
                    }
                });
            }
        });

        if (ret != ErrCode.XOK) {
            hideLoadingView();
            popupMessage("Fail to query event timeline, errCode=" + ret);
        }
    }


    //////////////////////////////////////////////////////////////////////////////////
    //////////////////////// Override Methods of IPlayingCallback ///////////////////
    //////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onDevMediaOpenDone(final String fileId, int errCode) {
        Log.d(TAG, "<onDevMediaOpenDone> fileId=" + fileId + ", errCode=" + errCode);
        if (errCode != ErrCode.XOK) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    popupMessage("Media playing fileId=" + fileId + " open failed, errCode=" + errCode);
                    getBinding().btnPlayStop.setText("播放");
                }
            });
        }
    }

    @Override
    public void onDevMediaPlayingDone(final String fileId) {
        Log.d(TAG, "<onDevMediaOpenDone> fileId=" + fileId);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                popupMessage("Media playing fileId=" + fileId + " finished!");
                getBinding().btnPlayStop.setText("播放");
            }
        });
    }

    @Override
    public void onDevMediaPauseDone(final String fileId, int errCode) {
        Log.d(TAG, "<onDevMediaPauseDone> fileId=" + fileId + ", errCode=" + errCode);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (errCode == ErrCode.XOK) {
                    popupMessage("Paused successful!");
                    getBinding().btnPauseResume.setText("恢复");

                } else {
                    popupMessage("Paused failure, errCode=" + errCode);
                    getBinding().btnPauseResume.setText("暂停");
                }
            }
        });

    }

    @Override
    public void onDevMediaResumeDone(final String fileId, int errCode) {
        Log.d(TAG, "<onDevMediaResumeDone> fileId=" + fileId + ", errCode=" + errCode);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (errCode == ErrCode.XOK) {
                    popupMessage("Resume successful!");
                    getBinding().btnPauseResume.setText("暂停");

                } else {
                    popupMessage("Resume failure, errCode=" + errCode);
                    getBinding().btnPauseResume.setText("恢复");
                }
            }
        });
    }

    @Override
    public void onDevMediaSetSpeedDone(final String fileId, int errCode, int speed) {
        Log.d(TAG, "<onDevPlayingError> fileId=" + fileId
                + ", errCode=" + errCode + ", speed=" + speed);
    }

    @Override
    public void onDevPlayingError(final String fileId, int errCode) {
        Log.d(TAG, "<onDevPlayingError> fileId=" + fileId + ", errCode=" + errCode);

    }
}
