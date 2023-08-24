package io.agora.iotlink.sdkimpl;


import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import io.agora.iotlink.ErrCode;
import io.agora.iotlink.logger.ALog;


/*
 * @brief 媒体播放使用的时钟
 */
public class MediaPlayingClock {




    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/MediaPlayClock";

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private boolean mIsRunning = false;         ///< 当前时钟是否在运行
    private long mBeginTicks = 0;               ///< 时钟开始运行的时刻点
    private long mDuration = 0;                 ///< 时钟前面总时长
    private int mRunSpeed = 1;                  ///< 时钟倍速，通常是 1,2,4


    ////////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /**
     * @brief 播放器时钟从 当前进度 开始运行
     */
    public synchronized void start() {
        mIsRunning = true;
        mBeginTicks = System.currentTimeMillis();
    }

    /**
     * @brief 播放器时钟从 指定的进度开始运行
     * @param startProgress : 指定开始运行的进度
     */
    public synchronized void startWithProgress(long startProgress) {
        mIsRunning = true;
        mBeginTicks = System.currentTimeMillis();
        mDuration = startProgress;
    }

    /**
     * @brief 播放器时钟立即停止运行，保留当前的运行进度
     */
    public synchronized void stop() {
        mIsRunning = false;
        mDuration += (System.currentTimeMillis() - mBeginTicks) * mRunSpeed;
        mBeginTicks = System.currentTimeMillis();
    }

    /**
     * @brief 播放器时钟立即停止运行，并且设置指定进度
     * @param setProgress : 指定停止后的运行进度
     */
    public synchronized void stopWithProgress(long setProgress) {
        mIsRunning = false;
        mBeginTicks = System.currentTimeMillis();
        mDuration = setProgress;
    }

    /**
     * @brief 设置运行进度的倍速，通常是 1倍速，2倍速
     * @param setSpeed : 指定当前运行进度
     */
    public synchronized void setRunSpeed(int setSpeed) {
        // 先更新一下已经运行的时长
        if (mIsRunning) {
            mDuration =  + (System.currentTimeMillis() - mBeginTicks) * mRunSpeed;
        }
        mBeginTicks = System.currentTimeMillis();

        mRunSpeed = setSpeed;
    }


    /**
     * @brief 直接设置播放器时钟当前进度，通常在 seek时调用
     * @param setProgress : 指定当前运行进度
     */
    public synchronized void setProgress(long setProgress) {
        mDuration = setProgress;
        mBeginTicks = System.currentTimeMillis();
    }

    /**
     * @brief 获取当前播放器时钟运行进度
     * @return 返回当前时钟进度
     */
    public synchronized long getProgress() {
        long time;
        if (mIsRunning) {
            time = mDuration + (System.currentTimeMillis() - mBeginTicks) * mRunSpeed;
        } else {
            time = mDuration;
        }
        return time;
    }


}