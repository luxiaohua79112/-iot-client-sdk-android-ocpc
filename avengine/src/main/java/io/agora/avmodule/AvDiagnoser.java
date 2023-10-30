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
 * @brief 码流诊断器
 *
 */
public class AvDiagnoser extends AvCompBase {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Data Structure Definition /////////////////////
    ////////////////////////////////////////////////////////////////////////
    /**
     * @brief 诊断器回调事件接口
     */
    public static interface IAvDiagnoserCallback {

        /**
         * @brief 文件打开完成事件
         * @param diagnoseParam : 诊断器参数
         * @param errCode: 错误码，0表示打开成功
         */
        void onDiagnoserOpenDone(DiagnoseParam diagnoseParam, int errCode);

        /**
         * @brief 诊断完成事件
         * @param diagnoseParam : 诊断器参数
         * @param totalDuration: 总时长
         */
        void onDiagnoserParsingDone(DiagnoseParam diagnoseParam, long totalDuration);


        /**
         * @brief 诊断过程中出现了不可恢复的错误事件
         * @param diagnoseParam : 诊断器参数
         * @param errCode: 错误码
         */
        void onDiagnoserParseError(DiagnoseParam diagnoseParam, int errCode);

    }

    /*
     * @brief 转换组件初始化参数
     */
    public static class DiagnoseParam {
        public IAvDiagnoserCallback mCallback;
        public Context mContext;
        public String mSrcFileUrl;
    }


    ////////////////////////////////////////////////////////////////////////
    /////////////////////////// Constant Definition ////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "AVMODULE/AvDiagnoser";
    private static final String COMP_NAME = "AvDiagnoser";

    /**
     * @brief 定义状态机
     */
    public static final int DIAGNOSE_STATE_CLOSED = 0x0000;      ///< 当前下载还未初始化
    public static final int DIAGNOSE_STATE_OPENING = 0x0001;     ///< 初始化成功，正在请求云媒体文件信息
    public static final int DIAGNOSE_STATE_PAUSED = 0x0002;      ///< 正常下载转换过程中
    public static final int DIAGNOSE_STATE_ONGOING = 0x0003;     ///< 正在转码
    public static final int DIAGNOSE_STATE_ERROR = 0x0005;       ///< 错误状态,不能再继续


    //
    // The mesage Id
    //
    protected static final int MSG_ID_OPEN = 0x1001;             ///< 打开处理
    protected static final int MSG_ID_PARSE = 0x1002;            ///< 分析处理






    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private final Object mDataLock = new Object();    ///< 同步访问锁,类中相应变量需要进行加锁处理
    private DiagnoseParam mDiagnoseParam;

    private AvNativeDiagnose mNativeDiagnoser;
    private AvMediaInfo mMediaInfo;            ///< 原始媒体文件信息
    private int mParseProgress = 0;           ///< 当前分析进度
    private volatile int mState = DIAGNOSE_STATE_CLOSED;


    ////////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /**
     * @brief 初始化诊断组件
     *        初始化成功后，状态机切换为 DIAGNOSE_STATE_OPENING
     * @param diagoseParam : 初始化参数
     * @return 返回错误代码，0：表示成功打开；其他值：表示打开文件失败
     */
    public int initialize(final DiagnoseParam diagoseParam) {
        int ret;

        mDiagnoseParam = diagoseParam;
        mParseProgress = 0;

        // 启动组件线程
        ret = runStart(COMP_NAME);
        if (ret != ErrCode.XERR_NONE) {
            Log.e(TAG, "<initialize> fail to start component");
            release();
            return ret;
        }

        // 设置状态机为 打开中
        setState(DIAGNOSE_STATE_OPENING);

        // 发送处理消息
        sendSingleMessage(MSG_ID_OPEN, 0, 0, null, 0);

        Log.d(TAG, "<initialize> done");
        return ErrCode.XOK;
    }


    /**
     * @brief 关闭录像，释放所有的编解码器，关闭输入输出文件
     *        释放完成后，状态机切换为 DIAGNOSE_STATE_CLOSED
     * @return 返回错误代码
     */
    public int release() {
        Log.d(TAG, "<release> [BEGIN] mState=" + mState);

        // 停止组件线程
        runStop();

        if (mNativeDiagnoser != null) {
            mNativeDiagnoser.close();
            mNativeDiagnoser = null;
        }


        // 设置状态机为 关闭
        setState(DIAGNOSE_STATE_CLOSED);

        synchronized (mDataLock) {
            mMediaInfo = null;
        }
        Log.d(TAG, "<release> [END] mState=" + mState);
        return ErrCode.XOK;
    }


    /**
     * @brief 获取当前录像的状态
     * @return 返回状态机
     */
    public int getState() {
        synchronized (mDataLock) {
            return mState;
        }
    }

    /*
     * @brief 设置新的状态机，仅供内部调用
     * @param newState ：新状态机
     */
    private void setState(int newState) {
        synchronized (mDataLock) {
            mState = newState;
        }
    }

    /*
     * @brief 开始转换处理，仅当成功打开后才能调用
     * @return 错误码
     */
    public int start() {
        int state = getState();
        if (state != DIAGNOSE_STATE_PAUSED) {
            Log.e(TAG, "<start> [ERROR] bad state, state=" + state);
            return ErrCode.XERR_BAD_STATE;
        }

        setState(DIAGNOSE_STATE_ONGOING);
        sendSingleMessage(MSG_ID_PARSE, 0, 0, null, 0);
        return ErrCode.XOK;
    }

    /*
     * @brief 暂停转换处理，仅当成功打开后才能调用
     * @return 错误码
     */
    public int stop() {
        int state = getState();
        if (state != DIAGNOSE_STATE_ONGOING) {
            Log.e(TAG, "<start> [ERROR] bad state, state=" + state);
            return ErrCode.XERR_BAD_STATE;
        }

        setState(DIAGNOSE_STATE_PAUSED);
        return ErrCode.XOK;
    }

    /*
     * @brief 获取当前转换进度
     * @return 进度百分比，值范围 [0, 100]
     */
    public int getProgress() {
        synchronized (mDataLock) {
            return mParseProgress;
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////
    ///////////////////////// Override AvCompBase Methods /////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////
    @Override
    protected void removeAllMessages() {
        if (mWorkHandler != null) {
            mWorkHandler.removeMessages(MSG_ID_OPEN);
            mWorkHandler.removeMessages(MSG_ID_PARSE);
            Log.d(TAG, "<removeAllMessages> done");
        }
    }

    @Override
    protected void processWorkMessage(Message msg) {
        switch (msg.what) {
            case MSG_ID_OPEN:
                onMessageOpen(msg);
                break;

            case MSG_ID_PARSE:
                onMessageParsing(msg);
                break;
        }
    }

    /**
     * @brief 工作线程中运行，打开源和目标流文件
     */
    void onMessageOpen(Message msg) {
        mNativeDiagnoser = new AvNativeDiagnose();
        int ret = mNativeDiagnoser.open(mDiagnoseParam.mSrcFileUrl);
        if (ret != ErrCode.XOK) {
            Log.e(TAG, "<onMessageOpen> fail to open(), ret=" + ret);
            mNativeDiagnoser = null;

        } else {
            AvMediaInfo mediaInfo = mNativeDiagnoser.getMediaInfo();
            synchronized (mDataLock) {
                mMediaInfo = mediaInfo;
            }
        }

        setState(DIAGNOSE_STATE_PAUSED); // 切换到暂停状态

        Log.d(TAG, "<onMessageOpen> done, ret=" + ret);
        if (mDiagnoseParam.mCallback != null) {  // 回调给上层 文件打开完成
            mDiagnoseParam.mCallback.onDiagnoserOpenDone(mDiagnoseParam, ret);
        }
    }


    /**
     * @brief 工作线程中运行，下载转码处理
     */
    void onMessageParsing(Message msg) {
        if (mNativeDiagnoser == null) {
            Log.e(TAG, "<onMessageParsing> [ERROR] bad state, mNativeDiagnoser is NULL");
            return;
        }

        int state = getState();
        if ((state == DIAGNOSE_STATE_OPENING) || (state == DIAGNOSE_STATE_CLOSED)) {   // 状态错误
            Log.e(TAG, "<onMessageParsing> [ERROR] bad state, state=" + state);
            return;
        }
        if (state == DIAGNOSE_STATE_PAUSED) { // 暂停状态，延时100ms后再判断
            sendSingleMessage(MSG_ID_PARSE, 0, 0, null, 100);
            return;
        }


        int ret = mNativeDiagnoser.doConvertStep();
        int cvtProgress = mNativeDiagnoser.getConvertProgress();
        synchronized (mDataLock) {
            mParseProgress = cvtProgress;
        }
        if (ret == ErrCode.XERR_FILE_EOF) {
            Log.d(TAG, "<onMessageParsing> convering is done!");
            if (mDiagnoseParam.mCallback != null) {  // 回调给上层 转换完成
                mDiagnoseParam.mCallback.onDiagnoserParsingDone(mDiagnoseParam, mMediaInfo.mFileDuration);
            }

        } else if (ret != ErrCode.XOK) {
            Log.e(TAG, "<onMessageParsing> convering error, ret=" + ret);
            if (mDiagnoseParam.mCallback != null) {  // 回调给上层 转换错误
                mDiagnoseParam.mCallback.onDiagnoserParseError(mDiagnoseParam, ret);
            }

        } else {  // 继续转换处理
            sendSingleMessage(MSG_ID_PARSE, 0, 0, null, 0);
        }
    }


}