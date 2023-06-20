package io.agora.iotlinkdemo.models.home;


import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.agora.baselibrary.utils.NetUtils;

import io.agora.iotlink.AIotAppSdkFactory;
import io.agora.iotlink.AlarmVideoDownloader;
import io.agora.iotlinkdemo.base.BaseViewBindingFragment;
import io.agora.iotlinkdemo.databinding.FragmentHomeMineBinding;
import io.agora.iotlinkdemo.models.login.AccountLoginActivity;
import io.agora.iotlinkdemo.models.login.AccountRegisterActivity;
import io.agora.iotlinkdemo.models.settings.AboutActivity;
import io.agora.iotlinkdemo.models.settings.AccountSecurityActivity;
import io.agora.iotlinkdemo.thirdpartyaccount.ThirdAccountMgr;


public class MineFragment extends BaseViewBindingFragment<FragmentHomeMineBinding>
        implements AlarmVideoDownloader.ICallback {
    private static final String TAG = "IOTLINK/MineFragment";



    ///////////////////////////////////////////////////////////////////////////
    //////////////////// Methods of Override BaseFragment /////////////////////
    ///////////////////////////////////////////////////////////////////////////
    @NonNull
    @Override
    protected FragmentHomeMineBinding getViewBinding(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        return FragmentHomeMineBinding.inflate(inflater);
    }

    @Override
    public void initView() {
        getBinding().ivToEdit.setVisibility(View.INVISIBLE);
        getBinding().vToEdit.setVisibility(View.INVISIBLE);

//        String userId = AIotAppSdkFactory.getInstance().getLocalUserId();
//        String nodeId = AIotAppSdkFactory.getInstance().getLocalNodeId();
//        String txtName = userId + "\n (" + nodeId + ")";
//        getBinding().tvUserMobile.setText(txtName);
    }

    @Override
    public void initListener() {
        getBinding().tvAccountSecrutiy.setOnClickListener(view -> {
            if (NetUtils.INSTANCE.isNetworkConnected()) {
                gotoAccountSecurityActivity();
            }
        });

        getBinding().tvAbout.setOnClickListener(view -> {
            gotoAboutActivity();
        });
     }


    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    ///////////////////////////////////////////////////////////////////////////
    //////////////////////////// Internal Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////////

    void gotoAccountSecurityActivity() {
        Intent intent = new Intent(getActivity(), AccountSecurityActivity.class);
        startActivity(intent);
    }

    void gotoAboutActivity() {
        Intent intent = new Intent(getActivity(), AboutActivity.class);
        startActivity(intent);
    }

}
