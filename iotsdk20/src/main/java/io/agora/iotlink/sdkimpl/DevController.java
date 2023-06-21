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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.agora.iotlink.ErrCode;
import io.agora.iotlink.IDevController;
import io.agora.iotlink.IVodPlayer;
import io.agora.iotlink.logger.ALog;
import io.agora.iotlink.rtmsdk.RtmCmdCtx;


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
                              final OnCommandCmdListener cmdListener) {

        RtmCmdCtx command = new RtmCmdCtx();



        JSONObject body = new JSONObject();
        ALog.getInstance().d(TAG, "<sendCmdPtzCtrl> [BEGIN] action=" + action
                + ", direction=" + direction + ", speed=" + speed);




        // body内容
        try {
            body.put("sequenceId", 0);
            body.put("cmd", "PTZ_CTRL");

            JSONObject paramObj = new JSONObject();
            paramObj.put("action", action);
            paramObj.put("direction", direction);
            paramObj.put("speed", speed);
            body.put("param", paramObj);

        } catch (JSONException jsonExp) {
            jsonExp.printStackTrace();
            ALog.getInstance().e(TAG, "<sendCmdPtzCtrl> [EXP] jsonExp=" + jsonExp);
            return ErrCode.XERR_JSON_WRITE;
        }

        String realBody = String.valueOf(body);



        ALog.getInstance().d(TAG, "<sendCmdPtzCtrl> [BEGIN] action=" + action
                + ", direction=" + direction + ", speed=" + speed);
        return ErrCode.XOK;
    }

    @Override
    public int sendCmdPtzReset(final OnCommandCmdListener cmdListener) {
        return ErrCode.XOK;
    }

    @Override
    public int storageCardFormat(final OnCommandCmdListener cmdListener) {
        return ErrCode.XOK;
    }

}
