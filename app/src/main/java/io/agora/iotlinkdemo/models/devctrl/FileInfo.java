package io.agora.iotlinkdemo.models.devctrl;


import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.agora.baselibrary.base.BaseAdapter;

import java.util.UUID;

/**
 * @brief 文件信息
 */
public class FileInfo {

    public String mFileId;
    public long mBeginTime;
    public long mEndTime;
    public int mEvent;
    public String mVideoUrl;
    public String mImgUrl;

    public BaseAdapter.CommonViewHolder mViewHolder;    ///< 设备显示的 ViewHolder

    public boolean      mSelected;          ///< 选择模式下是否被选中

    @Override
    public String toString() {
        String infoText = "{ mFileId=" + mFileId
                + ", mBeginTime=" + mBeginTime
                + ", mEndTime=" + mEndTime
                + ", mEvent=" + mEvent
                + ", mVideoUrl=" + mVideoUrl
                + ", mImgUrl=" + mImgUrl + " }";
        return infoText;
    }


}
