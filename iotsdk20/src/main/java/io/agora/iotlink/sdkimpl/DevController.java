/**
 * @file AccountMgr.java
 * @brief This file implement the call kit and RTC management
 *
 * @author xiaohua.lu
 * @email luxiaohua@agora.io
 * @version 1.0.0.1
 * @date 2023-05-19
 * @license Copyright (C) 2021 AgoraIO Inc. All rights reserved.
 */
package io.agora.iotlink.sdkimpl;



import android.view.View;

import java.util.UUID;

import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IDevController;
import io.agora.iotlink.IVodPlayer;


/*
 * @brief 设备信令控制器
 */
public class DevController  implements IDevController {


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/DevController";


    //
    // The mesage Id
    //


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////


    private UUID mSessionId;
    private DeviceSessionMgr mSessionMgr;


    ///////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    public DevController(final UUID sessionId, final DeviceSessionMgr sessionMgr) {
        mSessionId = sessionId;
        mSessionMgr = sessionMgr;
    }

    ///////////////////////////////////////////////////////////////////////
    //////////////// Methods of Override IDevController  //////////////////
    ///////////////////////////////////////////////////////////////////////
    @Override
    public int sendCmdPtzCtrl(int action, int direction, int speed,
                              final OnDevCmdListener cmdListener) {
        return ErrCode.XOK;
    }

    @Override
    public int sendCmdPtzReset(final OnDevCmdListener cmdListener) {
        return ErrCode.XOK;
    }

    @Override
    public int storageCardFormat(final OnDevCmdListener cmdListener) {
        return ErrCode.XOK;
    }

}
