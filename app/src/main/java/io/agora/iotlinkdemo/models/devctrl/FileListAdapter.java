package io.agora.iotlinkdemo.models.devctrl;

import android.text.TextUtils;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

import com.agora.baselibrary.base.BaseAdapter;
import com.agora.baselibrary.utils.StringUtils;


import io.agora.iotlinkdemo.R;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FileListAdapter extends BaseAdapter<FileInfo> {

    private DevCtrlActivity mOwner;
    private RecyclerView mRecycleView;
    private boolean mSelectMode = false;            ///< 是否选择模式下

    /////////////////////////////////////////////////////////////////////////////
    /////////////////////////////// Public Methods /////////////////////////////
    /////////////////////////////////////////////////////////////////////////////
    public FileListAdapter(List<FileInfo> deviceList) {
        super(deviceList);
        mSelectMode = false;
    }

    void setOwner(DevCtrlActivity ownerActivity) {
        mOwner = ownerActivity;
    }

    void setRecycleView(final RecyclerView recycleView) {
        mRecycleView = recycleView;
    }

    /**
     * @brief 切换选择模式
     */
    void switchSelectMode(boolean selectMode) {
        mSelectMode = selectMode;

        // 所有文件项都设置为：还未选择
        List<FileInfo> FileInfoList = getDatas();
        int devCount = FileInfoList.size();
        int i;
        for (i = 0; i < devCount; i++) {
            FileInfo FileInfo = FileInfoList.get(i);
            FileInfo.mSelected = false;
            FileInfoList.set(i, FileInfo);

            // 控制选择按钮是否显示
            if (FileInfo.mViewHolder != null) {
                CheckBox cbSelect = FileInfo.mViewHolder.getView(R.id.cb_file_select);
                cbSelect.setVisibility( selectMode ? View.VISIBLE : View.INVISIBLE);
            }
        }
        setDatas(FileInfoList);  // 更新到内部数据
    }

    boolean isInSelectMode() {
        return mSelectMode;
    }

    /**
     * @brief 设置所有文件项 全选/非全选 状态
     */
    void setAllItemsSelectStatus(boolean selected) {
        if (!mSelectMode) {  // 非选择模式下不做处理
            return;
        }

        List<FileInfo> FileInfoList = getDatas();
        for (int i = 0; i < FileInfoList.size(); i++) {
            FileInfo FileInfo = FileInfoList.get(i);
            FileInfo.mSelected = selected;
            FileInfoList.set(i, FileInfo);

            // 控制选择按钮是否显示
            if (FileInfo.mViewHolder != null) {
                CheckBox cbSelect = FileInfo.mViewHolder.getView(R.id.cb_file_select);
                cbSelect.setChecked(selected);
            }
        }
        setDatas(FileInfoList);
    }

    /**
     * @brief 设置某个文件项 全选/非全选 状态
     */
    void setItemSelectStatus(int position, final FileInfo FileInfo) {
        if (!mSelectMode) {  // 非选择模式下不做处理
            return;
        }

        // 控制选择按钮是否显示
        if (FileInfo.mViewHolder != null) {
            CheckBox cbSelect = FileInfo.mViewHolder.getView(R.id.cb_file_select);
            cbSelect.setChecked(FileInfo.mSelected);
        }

        getDatas().set(position, FileInfo);
    }


    /**
     * @brief 判断是否所有文件项都被选中了
     */
    boolean isAllItemsSelected() {
        List<FileInfo> FileInfoList = getDatas();
        for (int i = 0; i < FileInfoList.size(); i++) {
            FileInfo FileInfo = FileInfoList.get(i);
            if (!FileInfo.mSelected) {
                return false;
            }
        }

        return true;
    }

    /**
     * @brief 获取当前所有选择文件项
     */
    List<FileInfo> getSelectedItems() {
        List<FileInfo> selectedList = new ArrayList<>();

        List<FileInfo> FileInfoList = getDatas();
        for (int i = 0; i < FileInfoList.size(); i++) {
            FileInfo FileInfo = FileInfoList.get(i);
            if (FileInfo.mSelected) {
                selectedList.add(FileInfo);
            }
        }

        return selectedList;
    }

    /**
     * @brief 删除所有选中的文件项
     * @retrun 返回删除的数量
     */
    int deleteSelectedItems() {
        int oldCount = getDatas().size();
        List<FileInfo> unselectedList = new ArrayList<>();

        List<FileInfo> FileInfoList = getDatas();
        for (int i = 0; i < FileInfoList.size(); i++) {
            FileInfo FileInfo = FileInfoList.get(i);
            if (!FileInfo.mSelected) {
                unselectedList.add(FileInfo);
            }
        }

        setDatas(unselectedList);
        this.notifyDataSetChanged();

        return (oldCount - unselectedList.size());
    }

    /**
     * @brief 查询结果
     */
    public static class FindResult {
        public int mPosition = -1;      ///< 查询到的文件在列表中索引值, -1 表示没有查询到
        public FileInfo mFileInfo;     ///< 查询到的文件信息
    }

    /**
     * @brief 根据 非空的文件 FileId 找到文件项
     */
    public FindResult findItemByFileId(final String fileId) {
        FindResult findResult = new FindResult();
        findResult.mPosition = -1;
        if (fileId == null) {
            return findResult;
        }

        List<FileInfo> deviceList = getDatas();
        for (int i = 0; i < deviceList.size(); i++) {
            FileInfo fileInfo = deviceList.get(i);
            if (fileInfo.mFileId == null) {
                continue;
            }
            if (fileId.compareToIgnoreCase(fileInfo.mFileId) == 0) {
                findResult.mPosition = i;
                findResult.mFileInfo = fileInfo;
                return findResult;
            }
        }

        return findResult;
    }


    /**
     * @brief 获取文件列表项
     */
    public FileInfo getItem(int position) {
        FileInfo FileInfo = getDatas().get(position);
        return FileInfo;
    }

    /**
     * @brief 设置文件列表项，同时更新相应控件显示
     */
    public void setItem(int position, final FileInfo FileInfo) {
        getDatas().set(position, FileInfo);
        updateUiWgt(FileInfo);
    }

    /**
     * @brief 根据文件信息，更新 DeviceItem 控件显示
     */
    private void updateUiWgt(final FileInfo fileInfo) {
        if (fileInfo.mViewHolder == null) {
            return;
        }

        // fileId
        if (!TextUtils.isEmpty(fileInfo.mFileId)) {
            fileInfo.mViewHolder.setText(R.id.tvFileId, fileInfo.mFileId);
        } else {
            fileInfo.mViewHolder.setText(R.id.tvFileId, "");
        }

        // event
        fileInfo.mViewHolder.setText(R.id.tvEvent, String.valueOf(fileInfo.mEvent));


        // VideoUrl
        if (!TextUtils.isEmpty(fileInfo.mVideoUrl)) {
            fileInfo.mViewHolder.setText(R.id.tvVideoUrl, fileInfo.mVideoUrl);
        } else {
            fileInfo.mViewHolder.setText(R.id.tvVideoUrl, "");
        }

        // VideoUrl
        if (!TextUtils.isEmpty(fileInfo.mImgUrl)) {
            fileInfo.mViewHolder.setText(R.id.tvImageUrl, fileInfo.mImgUrl);
        } else {
            fileInfo.mViewHolder.setText(R.id.tvImageUrl, "");
        }
    }

    /**
     * @brief 更新所有文件列表
     */
    public void updateItemList(final List<FileInfo> fileList) {
        getDatas().clear();
        getDatas().addAll(fileList);
        this.notifyDataSetChanged();
    }

    /**
     * @brief 增加文件列表项
     */
    public void addNewItem(final FileInfo FileInfo) {
        getDatas().add(FileInfo);
        this.notifyDataSetChanged();
    }


    @Override
    public int getLayoutId(int viewType) {
        return R.layout.item_device_info;
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    @Override
    public void onBindViewHolder(@NonNull CommonViewHolder holder, int position) {
        FileInfo fileInfo = getDatas().get(position);
        if (fileInfo == null) {
            return;
        }
        fileInfo.mViewHolder = holder;
        getDatas().set(position, fileInfo);   // 更新控件信息


        // fileId
        if (!TextUtils.isEmpty(fileInfo.mFileId)) {
            fileInfo.mViewHolder.setText(R.id.tvFileId, fileInfo.mFileId);
        } else {
            fileInfo.mViewHolder.setText(R.id.tvFileId, "");
        }

        // event
        fileInfo.mViewHolder.setText(R.id.tvEvent, String.valueOf(fileInfo.mEvent));


        // VideoUrl
        if (!TextUtils.isEmpty(fileInfo.mVideoUrl)) {
            fileInfo.mViewHolder.setText(R.id.tvVideoUrl, fileInfo.mVideoUrl);
        } else {
            fileInfo.mViewHolder.setText(R.id.tvVideoUrl, "");
        }

        // VideoUrl
        if (!TextUtils.isEmpty(fileInfo.mImgUrl)) {
            fileInfo.mViewHolder.setText(R.id.tvImageUrl, fileInfo.mImgUrl);
        } else {
            fileInfo.mViewHolder.setText(R.id.tvImageUrl, "");
        }

    }


}
