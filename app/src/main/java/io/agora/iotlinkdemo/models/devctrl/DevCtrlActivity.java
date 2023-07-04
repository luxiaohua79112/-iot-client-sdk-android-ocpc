package io.agora.iotlinkdemo.models.devctrl;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
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

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.listener.ISingleCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.iotlink.ErrCode;
//import io.agora.iotlink.ICallkitMgr;
import io.agora.iotlink.IDevController;
import io.agora.iotlink.IDevMediaMgr;
import io.agora.iotlink.IDeviceSessionMgr;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.base.AgoraApplication;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.base.PushApplication;
import io.agora.iotlinkdemo.databinding.ActivityDevCtrlBinding;
import io.agora.iotlinkdemo.databinding.ActivityMainBinding;
import io.agora.iotlinkdemo.dialog.CommonDialog;
import io.agora.iotlinkdemo.models.home.DeviceInfo;
import io.agora.iotlinkdemo.models.home.DeviceListAdapter;
import io.agora.iotlinkdemo.presistentconnect.PresistentLinkComp;


public class DevCtrlActivity extends BaseViewBindingActivity<ActivityDevCtrlBinding>
      {
    private static final String TAG = "IOTLINK/DevCtrlAct";


    private UUID mSessionId = null;



    ///////////////////////////////////////////////////////////////////////////
    //////////////////// Methods of Override BaseActivity /////////////////////
    ///////////////////////////////////////////////////////////////////////////
    @Override
    protected ActivityDevCtrlBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityDevCtrlBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        IDeviceSessionMgr sessionMgr = AIotAppSdkFactory.getDevSessionMgr();
        mSessionId = PushApplication.getInstance().getFullscrnSessionId();
        IDeviceSessionMgr.SessionInfo sessionInfo = sessionMgr.getSessionInfo(mSessionId);
        getBinding().tvNodeId.setText(sessionInfo.mPeerDevId);

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

        getBinding().btnSdcardFmt.setOnClickListener(view -> {
            onBtnMediaDelete(view);
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

        IDevMediaMgr.QueryParam queryParam = new IDevMediaMgr.QueryParam();
        queryParam.mFileId = null;
        queryParam.mBeginTimestamp = 0;
        queryParam.mEndTimestamp = (System.currentTimeMillis() / 1000);
        queryParam.mPageIndex = 1;
        queryParam.mPageSize = 20;

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

        ArrayList<String> deletingIdList = new ArrayList<>();
        deletingIdList.add("record01");
        deletingIdList.add("record02");

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
                        if (errCode != ErrCode.XOK) {  // 查询媒体文件失败
                            popupMessage("Fail to delete file list, errCode=" + errCode);
                            return;
                        }

                        popupMessage("Successful to delete file list, undeletedCount=" + undeletedList.size());
                    }
                });
            }
        });
        if (ret != ErrCode.XOK) {
            hideLoadingView();
            popupMessage("Fail to delete media file list, errCode=" + ret);
        }
    }


}
