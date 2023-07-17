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
import io.agora.iotlinkdemo.databinding.FragmentHomePageBinding;
import io.agora.iotlinkdemo.dialog.DialogNewDevice;
import io.agora.iotlinkdemo.models.devctrl.DevCtrlActivity;
import io.agora.iotlinkdemo.presistentconnect.PresistentLinkComp;
import io.agora.iotlinkdemo.utils.AppStorageUtil;
import io.agora.iotlinkdemo.utils.FileUtils;


public class HomePageFragment extends BaseViewBindingFragment<FragmentHomePageBinding>
        implements PermissionHandler.ICallback, IDeviceSessionMgr.ISessionCallback  {
    private static final String TAG = "IOTLINK/HomePageFrag";


    private PermissionHandler mPermHandler;             ///< 权限申请处理

    private MainActivity mMainActivity;
    private HomePageFragment mFragment;
    private DeviceListAdapter mDevListAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private AlertDialog mAnswerRjectDlg = null;


    ///////////////////////////////////////////////////////////////////////////
    //////////////////// Methods of Override BaseFragment /////////////////////
    ///////////////////////////////////////////////////////////////////////////
    @NonNull
    @Override
    protected FragmentHomePageBinding getViewBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentHomePageBinding.inflate(inflater);
    }

    @Override
    public void initView() {
        mMainActivity = (MainActivity)getActivity();
        mFragment = this;

        //
        // 初始化设备列表
        //
        List<DeviceInfo> deviceList = deviceListLoad();

        if (mDevListAdapter == null) {
            mDevListAdapter = new DeviceListAdapter(deviceList);
            mDevListAdapter.setOwner(this);
            mDevListAdapter.setRecycleView(getBinding().rvDeviceList);
            getBinding().rvDeviceList.setLayoutManager(new LinearLayoutManager(getActivity()));
            getBinding().rvDeviceList.setAdapter(mDevListAdapter);
            mDevListAdapter.setMRVItemClickListener((view, position, data) -> {
            });
        }
        mSwipeRefreshLayout = getBinding().srlDevList;

        getBinding().titleView.hideLeftImage();

        //
        //
        // Microphone权限判断处理
        //
        int[] permIdArray = new int[2];
        permIdArray[0] = PermissionHandler.PERM_ID_RECORD_AUDIO;
        permIdArray[1] = PermissionHandler.PERM_ID_READ_STORAGE;
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
        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            mSwipeRefreshLayout.post(() -> mSwipeRefreshLayout.setRefreshing(false));
        });

        getBinding().titleView.setRightIconClick(view -> {
            onBtnDeviceMgr(view);
        });

        getBinding().cbAllSelect.setOnClickListener(view -> {
            onBtnSelectAll(view);
        });

        getBinding().btnDoDelete.setOnClickListener(view -> {
            onBtnDelete(view);
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

        // 当从全屏播放界面返回来时，要重新设置各个设备的视频播放控件
        UUID sessionId = PushApplication.getInstance().getFullscrnSessionId();
        if (sessionId != null) {
            resetDeviceDisplayView(sessionId);
            PushApplication.getInstance().setFullscrnSessionId(null);
        }
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
        if (mDevListAdapter.isInSelectMode()) {
            // 切换回非选择模式
            switchSelectMode(false);
            return true;
        }

        return false;
    }

    /**
     * @brief 选择模式 / 非选择模式 相互切换
     */
    void switchSelectMode(boolean selectMode) {

        if (selectMode) {
            // 切换到选择模式
            mDevListAdapter.switchSelectMode(true);
            getBinding().cbAllSelect.setChecked(false);
            mMainActivity.setNavigatebarVisibility(View.GONE);
            getBinding().clBottomDel.setVisibility(View.VISIBLE);

        } else {
            // 切换到非选择模式
            mDevListAdapter.switchSelectMode(false);
            getBinding().cbAllSelect.setChecked(false);
            mMainActivity.setNavigatebarVisibility(View.VISIBLE);
            getBinding().clBottomDel.setVisibility(View.GONE);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    //////////////////////////// Event & Widget Methods  ///////////////////////
    ///////////////////////////////////////////////////////////////////////////
    /**
     * @brief 设备管理
     */
    void onBtnDeviceMgr(View view) {

        PopupMenu deviceMenu = new PopupMenu(getActivity(), view);
        getActivity().getMenuInflater().inflate(R.menu.menu_device, deviceMenu.getMenu());

        deviceMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()){
                    case R.id.m_device_add:
                        onMenuAddDevice();
                        break;

                    case R.id.m_device_remove:
                        onMenuRemoveDevice();
                        break;

                }
                return true;
            }
        });
        deviceMenu.show();
    }

    /**
     * @brief 全选按钮点击事件
     */
    void onBtnSelectAll(View view) {
        boolean allChecked = getBinding().cbAllSelect.isChecked();
        if (allChecked) {
            mDevListAdapter.setAllItemsSelectStatus(true);
        } else {
            mDevListAdapter.setAllItemsSelectStatus(false);
        }
    }


    /**
     * @brief 删除按钮点击事件
     */
    void onBtnDelete(View view) {
        List<DeviceInfo> selectedList = mDevListAdapter.getSelectedItems();
        int selectedCount = selectedList.size();
        if (selectedCount <= 0) {
            popupMessage("Please select one device at least!");
            return;
        }

        showLoadingView();

        // 挂断所有要删除的通话
        //ICallkitMgr callkitMgr = AIotAppSdkFactory.getDevSessionMgr().getDevPreviewMgr();
        for (int i = 0; i < selectedCount; i++) {
            DeviceInfo deviceInfo = selectedList.get(i);
            if (deviceInfo.mSessionId != null) {    // 要删除的设备进行挂断操作
        //        callkitMgr.callHangup(deviceInfo.mSessionId);
            }
        }

        // 获取所有剩余的设备列表
        int deleteCount = mDevListAdapter.deleteSelectedItems();

        // 切换回 非选择模式
        switchSelectMode(false);

        // 保存新的设备列表
        List<DeviceInfo> deviceInfoList = mDevListAdapter.getDatas();
        deviceListStore(deviceInfoList);

        hideLoadingView();

        popupMessage("Total " + selectedCount + " devices already deleted!");
    }


    /**
     * @brief 新增一个设备
     */
    void onMenuAddDevice() {
        DialogNewDevice newDevDlg = new DialogNewDevice(this.getActivity());
        newDevDlg.setOnButtonClickListener(new BaseDialog.OnButtonClickListener() {
            @Override
            public void onLeftButtonClick() {
            }

            @Override
            public void onRightButtonClick() {
            }
        });

        newDevDlg.mSingleCallback = (integer, obj) -> {
            if (integer == 0) {
                String nodeId = (String)obj;
                DeviceListAdapter.FindResult findResult = mDevListAdapter.findItemByDevNodeId(nodeId);
                if (findResult.mDevInfo != null) {
                    popupMessage("Device :" + nodeId + " already exist!");
                    return;
                }

                DeviceInfo newDevice = new DeviceInfo();
                newDevice.mNodeId = nodeId;
                mDevListAdapter.addNewItem(newDevice);

                List<DeviceInfo> deviceInfoList = mDevListAdapter.getDatas();
                deviceListStore(deviceInfoList);
            }
        };
        newDevDlg.setCanceledOnTouchOutside(false);
        newDevDlg.show();
    }

    /**
     * @brief 进入编辑模式删除设备
     */
    void onMenuRemoveDevice() {
        List<DeviceInfo> deviceInfoList = mDevListAdapter.getDatas();
        int devCount = deviceInfoList.size();
        int i;
        for (i = 0; i < devCount; i++) {
            DeviceInfo deviceInfo = deviceInfoList.get(i);
            if (deviceInfo.mSessionId != null) {
                popupMessage("There are some devices in talking, should hangup all devices!");
                return;
            }
        }

        // 切换到选择模式
        switchSelectMode(true);

    }

    ///////////////////////////////////////////////////////////////////////////
    //////////////////////////// Session Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////////

    /**
     * @brief 连接/断开 设备 按钮点击事件
     */
    void onDevItemDialHangupClick(View view, int position, DeviceInfo deviceInfo) {
        if (mDevListAdapter.isInSelectMode()) {
            return;
        }

        if (deviceInfo.mConnectId == null) {
            // 长连接 请求设备连接
            String attachMsg = "Call_" + deviceInfo.mNodeId + "_at_" + getTimestamp();

            PresistentLinkComp.ReqConnectResult connectRslt;
            connectRslt = PresistentLinkComp.getInstance().devReqConnect(deviceInfo.mNodeId, attachMsg,
                new PresistentLinkComp.OnDevReqConnectListener() {
                    @Override
                    public void onDevReqConnectDone(int errCode, UUID connectId, String deviceId,
                                                    int localRtcUid, String chnlName,
                                                    String rtcToken, String rtmToken) {
                        Log.d(TAG, "<onDevReqConnectDone> errCode=" + errCode + ", connectId=" + connectId);

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (errCode != ErrCode.XOK) {  // 连接设备请求失败
                                    hideLoadingView();
                                    PresistentLinkComp.getInstance().devReqDisconnect(connectId); // 删除连接请求
                                    // 更新 设备信息
                                    deviceInfo.clear();
                                    mDevListAdapter.setItem(position, deviceInfo);
                                    popupMessage("Fail to requect connect device, errCode=" + errCode);
                                    return;
                                }

                                //
                                // SDK中 连接设备会话 操作
                                //
                                IDeviceSessionMgr sessionMgr = AIotAppSdkFactory.getDevSessionMgr();
                                IDeviceSessionMgr.ConnectParam connectParam = new IDeviceSessionMgr.ConnectParam();
                                connectParam.mUserId = PresistentLinkComp.getInstance().getLocalNodeId();
                                connectParam.mPeerDevId = deviceId;
                                connectParam.mLocalRtcUid = localRtcUid;
                                connectParam.mChannelName = chnlName;
                                connectParam.mRtcToken = rtcToken;
                                connectParam.mRtmToken = rtmToken;

                                IDeviceSessionMgr.ConnectResult sdkConnectRslt = sessionMgr.connect(connectParam, mFragment);
                                if (sdkConnectRslt.mErrCode != ErrCode.XOK) {
                                    PresistentLinkComp.getInstance().devReqDisconnect(connectId); // 删除连接请求
                                    // 更新 设备信息
                                    deviceInfo.clear();
                                    mDevListAdapter.setItem(position, deviceInfo);

                                    popupMessage("Fail to SDK connect device, errCode=" + errCode);
                                    return;
                                }

                                // 更新 sessionId 和 提示信息
                                DeviceInfo newDeviceInfo = mDevListAdapter.getItem(position);
                                newDeviceInfo.mSessionId = sdkConnectRslt.mSessionId;
                                newDeviceInfo.mTips = "Device is connecting...";
                                mDevListAdapter.setItem(position, newDeviceInfo);
                            }
                        });

                    }
                });

            if (connectRslt.mErrCode != ErrCode.XOK) {
                popupMessage("Connect device: " + deviceInfo.mNodeId + " failure, errCode=" + connectRslt.mErrCode);
                return;
            }

            // 更新 connectId 和 提示信息
            deviceInfo.mConnectId = connectRslt.mConnectId;
            deviceInfo.mTips = "Device connect requesting...";
            deviceInfo.mUserCount = 1;
            mDevListAdapter.setItem(position, deviceInfo);

            // 设置设备显示控件
            if (deviceInfo.mVideoView != null) {
                deviceInfo.mVideoView.setVisibility(View.VISIBLE);
            }
            // callkitMgr.setPeerVideoView(deviceInfo.mSessionId, deviceInfo.mVideoView);

        } else {
            // SDK中断开连接
            if (deviceInfo.mSessionId != null) { // 有可能此时SDK还未连接设备
                IDeviceSessionMgr sessionMgr = AIotAppSdkFactory.getDevSessionMgr();
                int ret = sessionMgr.disconnect(deviceInfo.mSessionId);
                if (ret != ErrCode.XOK) {
                    popupMessage("SDK Disconnect device: " + deviceInfo.mNodeId + " failure, ret=" + ret);
                    return;
                }
            }

            // 长联接 断开操作
            int errCode = PresistentLinkComp.getInstance().devReqDisconnect(deviceInfo.mConnectId);
            if (errCode != ErrCode.XOK) {
                popupMessage("Disconnect device: " + deviceInfo.mNodeId + " failure, errCode=" + errCode);
                return;
            }

            // 更新设备状态信息
            deviceInfo.clear();
            mDevListAdapter.setItem(position, deviceInfo);

            popupMessage("Disconnect device: " + deviceInfo.mNodeId + " successful!");
        }
    }

    /**
     * @brief 静音 按钮点击事件
     */
    void onDevItemMuteAudioClick(View view, int position, DeviceInfo deviceInfo) {
        if (mDevListAdapter.isInSelectMode()) {
            return;
        }
        if (deviceInfo.mSessionId == null) {
            return;
        }
        boolean devMute = (!deviceInfo.mDevMute);
        IDeviceSessionMgr sessionMgr = AIotAppSdkFactory.getDevSessionMgr();
        IDevPreviewMgr previewMgr = sessionMgr.getDevPreviewMgr(deviceInfo.mSessionId);
        if (previewMgr == null) {
            return;
        }

        IDeviceSessionMgr.SessionInfo sessionInfo = sessionMgr.getSessionInfo(deviceInfo.mSessionId);
        if (sessionInfo.mState != IDeviceSessionMgr.SESSION_STATE_CONNECTED) {
            Log.d(TAG, "<onDevItemMuteAudioClick> device not connected, state=" + sessionInfo.mState);
            return;
        }

        int errCode = previewMgr.muteDeviceAudio(devMute);
        if (errCode != ErrCode.XOK) {
            popupMessage("Mute or unmute device: " + deviceInfo.mNodeId + " failure, errCode=" + errCode);
            return;
        }

        deviceInfo.mDevMute = devMute;
        mDevListAdapter.setItem(position, deviceInfo);
    }

    /**
     * @brief 录像 按钮点击事件
     */
    void onDevItemRecordClick(View view, int position, DeviceInfo deviceInfo) {
        if (mDevListAdapter.isInSelectMode()) {
            return;
        }
        if (deviceInfo.mSessionId == null) {
            return;
        }
        IDeviceSessionMgr sessionMgr = AIotAppSdkFactory.getDevSessionMgr();
        IDevPreviewMgr previewMgr = sessionMgr.getDevPreviewMgr(deviceInfo.mSessionId);
        if (previewMgr == null) {
            return;
        }

        IDeviceSessionMgr.SessionInfo sessionInfo = sessionMgr.getSessionInfo(deviceInfo.mSessionId);
        if (sessionInfo.mState != IDeviceSessionMgr.SESSION_STATE_CONNECTED) {
            Log.d(TAG, "<onDevItemRecordClick> device not connected, state=" + sessionInfo.mState);
            return;
        }

        boolean recording = previewMgr.isRecording();
        if (recording) {
            // 停止录像
            int errCode = previewMgr.recordingStop();
            if (errCode != ErrCode.XOK) {
                popupMessage("Device: " + deviceInfo.mNodeId + " stop recording failure, errCode=" + errCode);
                return;
            }

            popupMessage("Device: " + deviceInfo.mNodeId + " recording stopped!");
            deviceInfo.mRecording = false;
            mDevListAdapter.setItem(position, deviceInfo);

        } else {
            // 启动录像
            String strSavePath = FileUtils.getFileSavePath(deviceInfo.mNodeId, false);
            int errCode = previewMgr.recordingStart(strSavePath);
            if (errCode != ErrCode.XOK) {
                popupMessage("Device: " + deviceInfo.mNodeId + " start recording failure, errCode=" + errCode);
                return;
            }

            popupMessage("Device: " + deviceInfo.mNodeId + " start recording......");
            deviceInfo.mRecording = true;
            mDevListAdapter.setItem(position, deviceInfo);
        }
    }

    /**
     * @brief 通话 按钮点击事件
     */
    void onDevItemMicClick(View view, int position, DeviceInfo deviceInfo) {
        if (mDevListAdapter.isInSelectMode()) {
            return;
        }
        if (deviceInfo.mSessionId == null) {
            return;
        }
        boolean micPush = (!deviceInfo.mMicPush);

        IDeviceSessionMgr sessionMgr = AIotAppSdkFactory.getDevSessionMgr();
        IDevPreviewMgr previewMgr = sessionMgr.getDevPreviewMgr(deviceInfo.mSessionId);
        if (previewMgr == null) {
            return;
        }

        IDeviceSessionMgr.SessionInfo sessionInfo = sessionMgr.getSessionInfo(deviceInfo.mSessionId);
        if (sessionInfo.mState != IDeviceSessionMgr.SESSION_STATE_CONNECTED) {
            Log.d(TAG, "<onDevItemMicClick> device not connected, state=" + sessionInfo.mState);
            return;
        }

        int errCode = previewMgr.muteLocalAudio(!micPush);
        if (errCode != ErrCode.XOK) {
            popupMessage("Voice or unvoice device: " + deviceInfo.mNodeId + " failure, errCode=" + errCode);
            return;
        }

        deviceInfo.mMicPush = micPush;
        mDevListAdapter.setItem(position, deviceInfo);
    }

    /**
     * @brief 全屏 按钮点击事件
     */
    void onDevItemFullscrnClick(View view, int position, DeviceInfo deviceInfo) {
        if (mDevListAdapter.isInSelectMode()) {
            return;
        }
        if (deviceInfo.mSessionId == null) {
            return;
        }
        IDeviceSessionMgr sessionMgr = AIotAppSdkFactory.getDevSessionMgr();
        IDevPreviewMgr previewMgr = sessionMgr.getDevPreviewMgr(deviceInfo.mSessionId);
        if (previewMgr == null) {
            return;
        }

        IDeviceSessionMgr.SessionInfo sessionInfo = sessionMgr.getSessionInfo(deviceInfo.mSessionId);
        if (sessionInfo.mState != IDeviceSessionMgr.SESSION_STATE_CONNECTED) {
            Log.d(TAG, "<onDevItemFullscrnClick> device not connected, state=" + sessionInfo.mState);
            return;
        }


        PushApplication.getInstance().setFullscrnSessionId(deviceInfo.mSessionId);
        gotoDevPreviewActivity();

    }


    /**
     * @brief 截屏 按钮点击事件
     */
    void onDevItemShotCaptureClick(View view, int position, DeviceInfo deviceInfo) {
        if (mDevListAdapter.isInSelectMode()) {
            return;
        }
        if (deviceInfo.mSessionId == null) {
            return;
        }
        IDeviceSessionMgr sessionMgr = AIotAppSdkFactory.getDevSessionMgr();
        IDevPreviewMgr previewMgr = sessionMgr.getDevPreviewMgr(deviceInfo.mSessionId);
        if (previewMgr == null) {
            return;
        }

        IDeviceSessionMgr.SessionInfo sessionInfo = sessionMgr.getSessionInfo(deviceInfo.mSessionId);
        if (sessionInfo.mState != IDeviceSessionMgr.SESSION_STATE_CONNECTED) {
            Log.d(TAG, "<onDevItemShotCaptureClick> device not connected, state=" + sessionInfo.mState);
            return;
        }

        String strSavePath = FileUtils.getFileSavePath(deviceInfo.mNodeId, true);
        int errCode = previewMgr.captureVideoFrame(strSavePath);
        if (errCode != ErrCode.XOK) {
            popupMessage("Device: " + deviceInfo.mNodeId + " shot capture failure, errCode=" + errCode);
            return;
        }

        popupMessage("Device: " + deviceInfo.mNodeId + " capture successful, save to file=" + strSavePath);
    }

    /**
     * @brief 选择 按钮点击事件
     */
    void onDevItemCheckBox(CompoundButton compoundButton, int position, final DeviceInfo deviceInfo,
                           boolean selected) {
        // 处理全选按钮
        boolean selectAll = mDevListAdapter.isAllItemsSelected();
        getBinding().cbAllSelect.setChecked(selectAll);
    }

    void onDevItemCheckBoxClick(View view, int position, DeviceInfo deviceInfo) {
        boolean selected = (!deviceInfo.mSelected);
        deviceInfo.mSelected = selected;
        mDevListAdapter.setItemSelectStatus(position, deviceInfo);

        // 处理全选按钮
        boolean selectAll = mDevListAdapter.isAllItemsSelected();
        getBinding().cbAllSelect.setChecked(selectAll);

    }

    String getTimestamp() {
        String time_txt = "";
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR) ;
        int month = calendar.get(Calendar.MONTH);
        int date = calendar.get(Calendar.DATE);
        int hour = calendar.get(Calendar.HOUR);
        int minute = calendar.get(Calendar.MINUTE);
        int second = calendar.get(Calendar.SECOND);
        int ms = calendar.get(Calendar.MILLISECOND);

        time_txt = String.format(Locale.getDefault(), "%d-%02d-%02d %02d:%02d:%02d.%d",
                year, month, date, hour,minute, second, ms);
        return time_txt;
    }

    void gotoDevPreviewActivity() {
        Intent intent = new Intent(getActivity(), DevCtrlActivity.class);
        startActivity(intent);
    }

    /**
     * @brief 重新设置所有设备的视频显示控件
     */
    void resetDeviceDisplayView(final UUID sessionId) {

        List<DeviceInfo> deviceList = mDevListAdapter.getDatas();
        if (deviceList == null) {
            return;
        }
        int deviceCount = deviceList.size();
        for (int i = 0; i < deviceCount; i++) {
            DeviceInfo deviceInfo = deviceList.get(i);
            if (deviceInfo.mSessionId == null) {
                continue;
            }
            if (sessionId.compareTo(deviceInfo.mSessionId) == 0) {
                Log.d(TAG, "<resetDeviceDisplayView> sessionId=" + sessionId
                        + ", mNodeId=" + deviceInfo.mNodeId);
                return;
            }
        }


    }


    ////////////////////////////////////////////////////////////////////////////////
    //////////////////////// Override Methods of ISessionCallback //////////////////
    ////////////////////////////////////////////////////////////////////////////////
    @Override
    public void onSessionConnectDone(final UUID sessionId,
                                     final IDeviceSessionMgr.ConnectParam connectParam,
                                     int errCode) {
        Log.d(TAG, "<onSessionConnectDone> sessionId=" + sessionId + ", errCode=" + errCode
                + ", connectParam=" + connectParam);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DeviceListAdapter.FindResult findResult = mDevListAdapter.findItemBySessionId(sessionId);
                if (findResult.mDevInfo == null) {
                    Log.e(TAG, "<onSessionConnectDone> NOT found session, sessionId=" + sessionId);
                    return;
                }

                if (errCode != ErrCode.XOK) {  // 连接设备会话失败
                    // 从长联接中删除连接
                    PresistentLinkComp.getInstance().devReqDisconnect(findResult.mDevInfo.mConnectId);

                    // 更新设备状态信息
                    findResult.mDevInfo.clear();
                    mDevListAdapter.setItem(findResult.mPosition, findResult.mDevInfo);

                    // 弹出错误提示
                    popupMessage("Fail to SDK connect device, errCode=" + errCode);

                } else {

                    // 更新设备状态信息
                    findResult.mDevInfo.mTips = "Talking...";
                    mDevListAdapter.setItem(findResult.mPosition, findResult.mDevInfo);
                    popupMessage("SDK connect device: " + connectParam.mPeerDevId + " successful");

                    // 直接开始设备预览
                    IDeviceSessionMgr deviceSessionMgr = AIotAppSdkFactory.getDevSessionMgr();
                    IDevPreviewMgr previewMgr = deviceSessionMgr.getDevPreviewMgr(sessionId);
                    if (previewMgr != null) {
                        // 设置显示控件
                        previewMgr.setDisplayView(findResult.mDevInfo.mVideoView);

                        // 开始预览操作
                        previewMgr.previewStart(true, new IDevPreviewMgr.OnPreviewListener() {
                            @Override
                            public void onDeviceFirstVideo(UUID sessionId, int videoWidth, int videoHeight) {
                                Log.d(TAG, "<onDeviceFirstVideo> sessionId=" + sessionId
                                        + ", videoWidth=" + videoWidth + ", videoHeight=" + videoHeight);
                            }
                        });
                    }
                }
            }
        });

    }

    @Override
    public void onSessionDisconnected(final UUID sessionId) {
        Log.d(TAG, "<onSessionDisconnected> sessionId=" + sessionId);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DeviceListAdapter.FindResult findResult = mDevListAdapter.findItemBySessionId(sessionId);
                if (findResult.mDevInfo == null) {
                    Log.e(TAG, "<onSessionDisconnected> NOT found session, sessionId=" + sessionId);
                    return;
                }

                // 长联接断开
                if (findResult.mDevInfo.mConnectId != null) {
                    PresistentLinkComp.getInstance().devReqDisconnect(findResult.mDevInfo.mConnectId);
                }

                // 更新设备状态信息
                findResult.mDevInfo.clear();
                mDevListAdapter.setItem(findResult.mPosition, findResult.mDevInfo);

                // 如果有设备控制界面Activity,则直接停止
                if (DevCtrlActivity.mActivity != null) {
                    DevCtrlActivity.mActivity.finish();
                }

                popupMessage("Peer device: " + findResult.mDevInfo.mNodeId + " disconnected!");

                if (mAnswerRjectDlg != null) {
                    mAnswerRjectDlg.cancel();
                    mAnswerRjectDlg = null;
                }

            }
        });

    }

    @Override
    public void onSessionOtherUserOnline(final UUID sessionId, int onlineUserCount) {
        Log.d(TAG, "<onSessionOtherUserOnline> sessionId=" + sessionId
                + ", onlineUserCount" + onlineUserCount);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DeviceListAdapter.FindResult findResult = mDevListAdapter.findItemBySessionId(sessionId);
                if (findResult.mDevInfo == null) {
                    Log.e(TAG, "<onOtherUserOnline> NOT found session, sessionId=" + sessionId);
                    return;
                }

                // 更新设备状态信息
                findResult.mDevInfo.mUserCount = onlineUserCount;
                mDevListAdapter.setItem(findResult.mPosition, findResult.mDevInfo);
            }
        });
    }

    @Override
    public void onSessionOtherUserOffline(final UUID sessionId, int onlineUserCount) {
        Log.d(TAG, "<onSessionOtherUserOffline> sessionId=" + sessionId
                + ", onlineUserCount" + onlineUserCount);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DeviceListAdapter.FindResult findResult = mDevListAdapter.findItemBySessionId(sessionId);
                if (findResult.mDevInfo == null) {
                    Log.e(TAG, "<onOtherUserOffline> NOT found session, sessionId=" + sessionId);
                    return;
                }

                // 更新设备状态信息
                findResult.mDevInfo.mUserCount = onlineUserCount;
                mDevListAdapter.setItem(findResult.mPosition, findResult.mDevInfo);
            }
        });
    }

    @Override
    public void onSessionError(final UUID sessionId, int errCode) {
        Log.d(TAG, "<onSessionError> sessionId=" + sessionId + ", errCode" + errCode);
    }


    ///////////////////////////////////////////////////////////////////////////
    //////////////////// Methods of DeviceList Storage  ///////////////////////
    ///////////////////////////////////////////////////////////////////////////
    /**
     * @brief 从本地存储读取设备列表信息
     */
    private List<DeviceInfo> deviceListLoad() {
        List<DeviceInfo> deviceInfoList = new ArrayList<>();

        String localUserId = PresistentLinkComp.getInstance().getLocalUserId();
        String keyDevCount = localUserId + "_device_count";

        int devCount = AppStorageUtil.queryIntValue(keyDevCount, 0);
        for (int i = 0; i < devCount; i++) {
            DeviceInfo deviceInfo = new DeviceInfo();
            String keyNodeId = localUserId + "_device_index_" + i;
            deviceInfo.mNodeId = AppStorageUtil.queryValue(keyNodeId, null);
            if (deviceInfo.mNodeId == null) {
                Log.e(TAG, "<deviceListLoad> fail to read key: " + keyNodeId);
                continue;
            }
            deviceInfoList.add(deviceInfo);
        }

        Log.d(TAG, "<deviceListLoad> stored device list, devCount=" + devCount);


        return deviceInfoList;
    }


    /**
     * @brief 将设备列表存储到本地
     */
    private void deviceListStore(final List<DeviceInfo> deviceInfoList) {
        int devCount = deviceInfoList.size();

        String localUserId = PresistentLinkComp.getInstance().getLocalUserId();
        String keyDevCount = localUserId + "_device_count";

        AppStorageUtil.keepShared(keyDevCount, devCount);
        for (int i = 0; i < devCount; i++) {
            DeviceInfo deviceInfo = deviceInfoList.get(i);
            String keyNodeId = localUserId + "_device_index_" + i;
            AppStorageUtil.keepShared(keyNodeId, deviceInfo.mNodeId);
        }

        Log.d(TAG, "<deviceListStore> stored device list, devCount=" + devCount);
    }


}
