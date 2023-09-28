package io.agora.iotlinkdemo.models.devctrl;


import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.agora.baselibrary.base.BaseAdapter;

import java.util.UUID;

import io.agora.iotlink.IDevMediaMgr;

/**
 * @brief 文件信息
 */
public class FileInfo {

    public IDevMediaMgr.DevMediaItem mMediaInfo;

    public BaseAdapter.CommonViewHolder mViewHolder;    ///< 设备显示的 ViewHolder

    public boolean      mSelected;          ///< 选择模式下是否被选中

    @Override
    public String toString() {
        String infoText = "";
        if (mMediaInfo != null) {
            infoText = mMediaInfo.toString();
        }
        return infoText;
    }


}
