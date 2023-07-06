package io.agora.iotlinkdemo.dialog;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.Gravity;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;

import com.agora.baselibrary.base.BaseDialog;
import com.agora.baselibrary.utils.ScreenUtils;

import io.agora.iotlinkdemo.databinding.DialogImageDisplayBinding;
import io.agora.iotlinkdemo.databinding.DialogNeedPowerBinding;

/**
 * 图片显示对话框
 */
public class DialogImageDisplay extends BaseDialog<DialogImageDisplayBinding> {


    public DialogImageDisplay(@NonNull Context context) {
        super(context);
    }

    @NonNull
    @Override
    protected DialogImageDisplayBinding getViewBinding(@NonNull LayoutInflater inflater) {
        return DialogImageDisplayBinding.inflate(inflater);
    }

    @Override
    protected void initView() {

    }

    @Override
    protected void setGravity() {
        getWindow().setLayout(
                ScreenUtils.dp2px(300),
                ScreenUtils.dp2px(176)
        );
        getWindow().getAttributes().gravity = Gravity.CENTER;
    }

    public void setDisplayBmp(final Bitmap bmp) {
        getBinding().ivDisplayImg.setImageBitmap(bmp);
    }
}
