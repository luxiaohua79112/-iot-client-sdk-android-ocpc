package io.agora.iotlinkdemo.models.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


import java.io.File;

import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IDeviceSessionMgr;
import io.agora.iotlink.sdkimpl.MediaPlayingClock;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.base.PushApplication;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.databinding.ActivityLoginBinding;
import io.agora.iotlinkdemo.models.home.MainActivity;
import io.agora.iotlinkdemo.presistentconnect.PresistentLinkComp;
import io.agora.iotlinkdemo.utils.AppStorageUtil;


public class AccountLoginActivity extends BaseViewBindingActivity<ActivityLoginBinding> {
    private final String TAG = "IOTLINK/LoginAct";




    ///////////////////////////////////////////////////////////////////////////
    //////////////////// Methods of Override BaseActivity /////////////////////
    ///////////////////////////////////////////////////////////////////////////
    @Override
    protected ActivityLoginBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityLoginBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {

        String account = AppStorageUtil.safeGetString(this, Constant.ACCOUNT, null);
        if (!TextUtils.isEmpty(account)) {
            getBinding().etAccounts.setText(account);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    @Override
    protected boolean isCanExit() {
        return true;
    }

    @Override
    public void initListener() {

        getBinding().iBtnClearAccount.setOnClickListener(view -> {
            getBinding().etAccounts.setText("");
        });

        getBinding().btnLogin.setOnClickListener(view -> {
            onBtnLogin();
        });

        getBinding().btnRegister.setOnClickListener(view -> {
            gotoRegisterActivity();
        });
    }



    ///////////////////////////////////////////////////////////////////////////
    //////////////////////////// Internal Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    void onBtnLogin() {
        String account = getBinding().etAccounts.getText().toString();
        if (TextUtils.isEmpty(account)) {
            popupMessage("Please input valid account!");
            return;
        }

        Bundle metaData = ((PushApplication)getApplication()).getMetaData();
        String appId = metaData.getString("AGORA_APPID", "");
        if (TextUtils.isEmpty(appId)) {
            popupMessage("Fail to get appId!");
            return;
        }

        showLoadingView();

        PresistentLinkComp.InitParam linkInitParam = PresistentLinkComp.getInstance().getInitParam();



        //
        // 长联接进行登录操作
        //
        PresistentLinkComp.PrepareParam prepareParam = new PresistentLinkComp.PrepareParam();
        prepareParam.mAppId = linkInitParam.mAppId;
        prepareParam.mUserId = account;
        prepareParam.mClientType = 2;
        prepareParam.mPusherId = linkInitParam.mPusherId;


        int ret = PresistentLinkComp.getInstance().prepare(prepareParam, new PresistentLinkComp.OnPrepareListener() {
            @Override
            public void onSdkPrepareDone(PresistentLinkComp.PrepareParam prepareParam1, int errCode) {
                Log.d(TAG, "<onBtnLogin.onSdkPrepareDone> errCode=" + errCode);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideLoadingView();
                        if (errCode != ErrCode.XOK) {
                            popupMessage("Fail to prepare presistent link, errCode=" + errCode);
                            return;
                        }

                        // MediaPlayingClock.UnitTest();

                        //
                        // 初始化 SDK引擎
                        //
                        IDeviceSessionMgr.InitParam initParam = new IDeviceSessionMgr.InitParam();
                        initParam.mContext = linkInitParam.mContext;
                        initParam.mAppId = linkInitParam.mAppId;
                        initParam.mProjectID = linkInitParam.mProjectID;
                        initParam.mUserId = PresistentLinkComp.getInstance().getLocalNodeId();
                        File file = initParam.mContext.getExternalFilesDir(null);
                        String cachePath = file.getAbsolutePath();
                        initParam.mLogFilePath = cachePath + "/callkit.log";
                        int retSdk = AIotAppSdkFactory.getDevSessionMgr().initialize(initParam);
                        if (retSdk != ErrCode.XOK) {
                            popupMessage("Fail to init SDK, errCode=" + errCode);
                            return;
                        }

                        popupMessageLongTime("Account Login successful!");
                        gotoMainActivity();
                    }
                });
            }
        });

        if (ret != ErrCode.XOK) {
            hideLoadingView();
            popupMessage("Fail to Login, errCode=" + ret);
            return;
        }

        AppStorageUtil.safePutString(this, Constant.ACCOUNT, account);
    }


    void gotoRegisterActivity() {
        Intent intent = new Intent(AccountLoginActivity.this, AccountRegisterActivity.class);
        startActivity(intent);
    }

    void gotoMainActivity() {
        Intent intent = new Intent(AccountLoginActivity.this, MainActivity.class);
        startActivity(intent);
    }
}
