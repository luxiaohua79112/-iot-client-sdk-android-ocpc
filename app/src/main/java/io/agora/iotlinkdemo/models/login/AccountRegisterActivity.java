package io.agora.iotlinkdemo.models.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import io.agora.iotlink.ErrCode;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;

import io.agora.iotlinkdemo.base.PushApplication;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.databinding.ActivityRegisterBinding;
import io.agora.iotlinkdemo.thirdpartyaccount.ThirdAccountMgr;
import io.agora.iotlinkdemo.utils.AppStorageUtil;


/**
 * 注册
 */
public class AccountRegisterActivity extends BaseViewBindingActivity<ActivityRegisterBinding> {
    private final String TAG = "IOTLINK/RegisterAct";


    private AccountRegisterActivity mActivity;


    ///////////////////////////////////////////////////////////////////////////
    //////////////////// Methods of Override BaseActivity /////////////////////
    ///////////////////////////////////////////////////////////////////////////
    @Override
    protected ActivityRegisterBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return ActivityRegisterBinding.inflate(inflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
    }

    @Override
    public void initListener() {
        mActivity = this;

        getBinding().btnRegister.setOnClickListener(view -> {
            onBtnRegister();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    ///////////////////////////////////////////////////////////////////////////
    //////////////////////////// Internal Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    void onBtnRegister() {

        String userId = getBinding().etUserId.getText().toString();
        if (TextUtils.isEmpty(userId)) {
            popupMessage("Please input valid userId!");
            return;
        }

        Bundle metaData = ((PushApplication)getApplication()).getMetaData();
        String appId = metaData.getString("AGORA_APPID", "");
        if (TextUtils.isEmpty(appId)) {
            popupMessage("Fail to get appId!");
            return;
        }

        showLoadingView();

        ThirdAccountMgr.RegisterParam registerParam = new ThirdAccountMgr.RegisterParam();
        registerParam.mMasterAppId = appId;
        registerParam.mUserId = userId;
        registerParam.mClientType = 2;
        ThirdAccountMgr.getInstance().register(registerParam, new ThirdAccountMgr.IRegisterCallback() {
            @Override
            public void onThirdAccountRegisterDone(int errCode, String errMsg, ThirdAccountMgr.RegisterParam registerParam, String retrievedNodeId, String region) {
                Log.d(TAG, "<onBtnRegister.onThirdAccountRegisterDone> errCode=" + errCode);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideLoadingView();
                        if (errCode != ErrCode.XOK) {
                            popupMessage("Fail to register account, errCode=" + errCode
                                    + ", errMessage=" + errMsg);
                            return;
                        }

                        popupMessageLongTime("Register account successful, nodeId=" + retrievedNodeId
                                + ", region=" + region);

                        AppStorageUtil.safePutString(mActivity, Constant.ACCOUNT, retrievedNodeId);
                        finish();
                    }
                });
            }
        });
    }



}
