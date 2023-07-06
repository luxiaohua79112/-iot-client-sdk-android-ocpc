package io.agora.iotlinkdemo.utils;

import com.agora.baselibrary.utils.ToastUtils;
import io.agora.iotlink.ErrCode;

public class ErrorToastUtils {


    private static void showErrorText(String text) {
        ToastUtils.INSTANCE.showToast(text);
    }
}
