package io.agora.iotlinkdemo.models.home;

import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.agora.baselibrary.base.BaseDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;


import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IDevPreviewMgr;
import io.agora.iotlink.IDeviceSessionMgr;
import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.base.BaseViewBindingFragment;
import io.agora.iotlinkdemo.base.PermissionHandler;
import io.agora.iotlinkdemo.base.PermissionItem;
import io.agora.iotlinkdemo.base.PushApplication;
import io.agora.iotlinkdemo.databinding.FragmentHomeCloudrcdBinding;
import io.agora.iotlinkdemo.databinding.FragmentHomePageBinding;
import io.agora.iotlinkdemo.dialog.DialogNewDevice;
import io.agora.iotlinkdemo.models.player.DevPreviewActivity;
import io.agora.iotlinkdemo.presistentconnect.PresistentLinkComp;
import io.agora.iotlinkdemo.utils.AppStorageUtil;
import io.agora.iotlinkdemo.utils.FileUtils;


public class CloudRcdFragment extends BaseViewBindingFragment<FragmentHomeCloudrcdBinding>
        implements PermissionHandler.ICallback, IDeviceSessionMgr.ISessionCallback  {

    private static final String TAG = "IOTLINK/CloudRcdFrag";


    private PermissionHandler mPermHandler;             ///< 权限申请处理

    private MainActivity mMainActivity;
    private CloudRcdFragment mFragment;



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

    }

    /**
     * @brief 播放/暂停 按钮
     */
    void onBtnPlayPause(View view) {

    }



}
