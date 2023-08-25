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
        if (mIsRunning) {
            return;
        }
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
            mDuration += (System.currentTimeMillis() - mBeginTicks) * mRunSpeed;
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


    /**
     * @brief 单元测试用例
     */
    public static void UnitTest() {
        MediaPlayingClock clock = new MediaPlayingClock();

        Log.d(TAG, "<UnitTest> ================ BEGIN ================");


        // 从第 5000ms 开始播放4秒
        clock.startWithProgress(5000);
        ThreadSleep(4000);
        Log.d(TAG, "<UnitTest> stage 1, progress=" + clock.getProgress());  // 这里应该是9000

        // 暂停 3000ms
        clock.stop();
        ThreadSleep(3000);
        Log.d(TAG, "<UnitTest> stage 2, progress=" + clock.getProgress());  // 这里应该是9000

        // 继续播放 6000ms
        clock.start();
        ThreadSleep(6000);
        Log.d(TAG, "<UnitTest> stage 3, progress=" + clock.getProgress());  // 这里应该是15000

        // 直接停止到 3000ms
        clock.stopWithProgress(3000);
        Log.d(TAG, "<UnitTest> stage 4, progress=" + clock.getProgress());  // 这里应该是3000

        // 设置3倍的播放倍速
        clock.setRunSpeed(3);

        // 播放 4000ms
        clock.start();
        ThreadSleep(4000);
        Log.d(TAG, "<UnitTest> stage 5, progress=" + clock.getProgress());  // 这里应该是15000

        // 直接设置时长到 4000ms
        clock.setProgress(4000);
        Log.d(TAG, "<UnitTest> stage 6, progress=" + clock.getProgress());  // 这里应该是4000

        // 设置2倍的播放倍速
        clock.setRunSpeed(2);

        // 已经在播放状态下，再次调用播放3000ms
        clock.start();
        ThreadSleep(3000);
        Log.d(TAG, "<UnitTest> stage 7, progress=" + clock.getProgress());  // 这里应该是10000

        // 设置1倍速播放倍速
        clock.setRunSpeed(1);

        // 已经在播放状态下，从0开始 再次调用播放 4000ms
        clock.startWithProgress(0);
        ThreadSleep(4000);
        Log.d(TAG, "<UnitTest> stage 8, progress=" + clock.getProgress());  // 这里应该是4000


        Log.d(TAG, "<UnitTest> ================ END ================");
    }

    public static void ThreadSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interruptExp) {
            interruptExp.printStackTrace();
        }
    }

}