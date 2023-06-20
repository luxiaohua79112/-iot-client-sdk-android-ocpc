package io.agora.iotlinkdemo.models.login;

import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.agora.iotlinkdemo.R;
import io.agora.iotlinkdemo.base.BaseViewBindingActivity;
import io.agora.iotlinkdemo.common.Constant;
import io.agora.iotlinkdemo.databinding.ActivityWebviewBinding;


public class WebViewActivity extends BaseViewBindingActivity<ActivityWebviewBinding> {

    String url = "https://agoralink.sd-rtn.com/terms/termsofuse";


    @Override
    protected ActivityWebviewBinding getViewBinding(@NonNull LayoutInflater layoutInflater) {
        return ActivityWebviewBinding.inflate(layoutInflater);
    }

    @Override
    public void initView(@Nullable Bundle savedInstanceState) {
        if (url.contains("termsofuse")) {
            getBinding().titleView.setTitle(getString(R.string.user_agreement));
        } else {
            getBinding().titleView.setTitle(getString(R.string.privacy_policy));
        }
        getBinding().webView.loadUrl(url);
    }
}
