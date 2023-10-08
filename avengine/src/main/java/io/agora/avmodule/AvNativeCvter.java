package io.agora.avmodule;


import android.content.Context;
import android.media.AudioFormat;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Message;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


/**
 * @brief FFMPEG层转换器封装接口
 *
 */
public class AvNativeCvter  {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Data Structure Definition /////////////////////
    ////////////////////////////////////////////////////////////////////////


    ////////////////////////////////////////////////////////////////////////
    /////////////////////////// Constant Definition ////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "AVMODULE/AvNativeCvter";


    static {
        System.loadLibrary("SoftDecoder");
    }

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private long mCvterHandler = 0;


    ////////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /**
     * @brief 打开转换器，内部打开原始流文件和目标文件
     * @param srcFileUrl : 原始流文件地址
     * @param dstFilePath: 目标文件路径
     * @return 返回错误代码，0：表示成功打开；其他值：表示打开文件失败
     */
    public int open(final String srcFileUrl, final String dstFilePath) {
        int ret = ErrCode.XOK;

        mCvterHandler = native_cvterOpen(srcFileUrl, dstFilePath);
        if (mCvterHandler == 0) {
            ret = ErrCode.XERR_FILE_OPEN;
        }

        Log.d(TAG, "<open> done, ret=" + ret
                + ", srcFilePath=" + srcFileUrl + ", dstFilePath=" + dstFilePath);
        return ret;
    }


    /**
     * @brief 关闭转换器，内部关闭原始流文件和目标文件，释放所有资源
     * @return 返回错误代码
     */
    public int close() {
        int ret = ErrCode.XOK;
        if (mCvterHandler != 0) {
            long t1 = System.currentTimeMillis();
            ret = native_cvterClose(mCvterHandler);
            mCvterHandler = 0;
            long t2 = System.currentTimeMillis();
            Log.d(TAG, "<close> done, costTime=" + (t2-t1));
        }
        return ret;
    }

    /**
     * @brief 获取媒体文件信息，必须要在 open()成功之后调用
     * @return 返回获取到的媒体信息，如果失败则返回null
     */
    public AvMediaInfo getMediaInfo() {
        if (mCvterHandler == 0) {
            Log.e(TAG, "<getMediaInfo> bad state");
            return null;
        }

        AvMediaInfo mediaInfo = new AvMediaInfo();
        int ret = native_cvterGetMediaInfo(mCvterHandler, mediaInfo);
        if (ret != ErrCode.XOK) {
            Log.e(TAG, "<getMediaInfo> fail to get media info, ret=" + ret);
            return null;
        }

        return mediaInfo;
    }

    /**
     * @brief 进行单步转换操作
     * @return 返回错误码
     *          XERR_FILE_EOF 表示转换完成；
     *          XERR_FILE_READ: 表示源数据流读取失败
     *          XERR_FILE_WRITE: 表示写入文件失败
     */
    public int doConvertStep() {
        if (mCvterHandler == 0) {
            Log.e(TAG, "<doConvertStep> bad state");
            return ErrCode.XERR_BAD_STATE;
        }

        int ret = native_cvterDoStep(mCvterHandler);
        return ret;
    }


    /**************************************************************/
    /********************  Native JNI Define *********************/
    /*************************************************************/
    public native long native_cvterOpen(String srcFilePath, String dstFilePath);
    public native int native_cvterClose(long hCvter);
    public native int native_cvterGetMediaInfo(long hCvter, AvMediaInfo outMediaInfo);
    public native int native_cvterDoStep(long hCvter);


}