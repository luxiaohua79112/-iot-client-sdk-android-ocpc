/**
 * @file IAgoraIotAppSdk.java
 * @brief This file define the SDK interface for Agora Iot AppSdk 2.0
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2022-01-26
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink;



import io.agora.iotlink.sdkimpl.DeviceSessionMgr;
import io.agora.iotlink.sdkimpl.VodPlayer;


/*
 * @brief SDK引擎接口
 */

public class AIotAppSdkFactory  {

    private static IDeviceSessionMgr  mDevSessionMgrInstance = null;
    private static IVodPlayer mVodPlayerInstance = null;

    public static IDeviceSessionMgr getDevSessionMgr() {
        if(mDevSessionMgrInstance == null) {
            synchronized (DeviceSessionMgr.class) {
                if(mDevSessionMgrInstance == null) {
                    mDevSessionMgrInstance = new DeviceSessionMgr();
                }
            }
        }

        return mDevSessionMgrInstance;
    }

    public static IVodPlayer getVodPlayer() {
        if(mVodPlayerInstance == null) {
            synchronized (VodPlayer.class) {
                if(mVodPlayerInstance == null) {
                    mVodPlayerInstance = new VodPlayer();
                }
            }
        }

        return mVodPlayerInstance;
    }


}
