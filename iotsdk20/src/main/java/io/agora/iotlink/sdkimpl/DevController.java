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
import io.agora.iotlink.IDeviceSessionMgr;
import io.agora.iotlink.IVodPlayer;
import io.agora.iotlink.logger.ALog;
import io.agora.iotlink.rtmsdk.IRtmCmd;
import io.agora.iotlink.rtmsdk.RtmBaseCmd;
import io.agora.iotlink.rtmsdk.RtmCmdCtx;
import io.agora.iotlink.rtmsdk.RtmCmdSeqId;
import io.agora.iotlink.rtmsdk.RtmCustomizeReqCmd;
import io.agora.iotlink.rtmsdk.RtmPlzCtrlReqCmd;


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
    private String mDeviceId;


    ///////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    public DevController(final UUID sessionId, final DeviceSessionMgr sessionMgr) {
        mSessionId = sessionId;
        mSessionMgr = sessionMgr;
        IDeviceSessionMgr.SessionInfo sessionInfo = mSessionMgr.getSessionInfo(sessionId);
        mDeviceId = sessionInfo.mPeerDevId;
    }

    ///////////////////////////////////////////////////////////////////////
    //////////////// Methods of Override IDevController  //////////////////
    ///////////////////////////////////////////////////////////////////////
    @Override
    public int sendCmdPtzCtrl(int action, int direction, int speed,
                              final OnCommandCmdListener cmdListener) {

        RtmPlzCtrlReqCmd plzCtrlCmd = new RtmPlzCtrlReqCmd();
        plzCtrlCmd.mSequenceId = RtmCmdSeqId.getSeuenceId();
        plzCtrlCmd.mCmdId = IRtmCmd.CMDID_PTZ_CTRL;
        plzCtrlCmd.mDeviceId = mDeviceId;
        plzCtrlCmd.mSendTimestamp = System.currentTimeMillis();
        plzCtrlCmd.mAction = action;
        plzCtrlCmd.mDirection = direction;
        plzCtrlCmd.mSpeed = speed;

        int ret = mSessionMgr.getRtmMgrComp().sendCommandToDev(plzCtrlCmd);

        ALog.getInstance().d(TAG, "<sendCmdPtzCtrl> done, ret=" + ret
                + ", plzCtrlCmd=" + plzCtrlCmd);
        return ret;
    }

    @Override
    public int sendCmdPtzReset(final OnCommandCmdListener cmdListener) {
        RtmBaseCmd plzResetCmd = new RtmBaseCmd();
        plzResetCmd.mSequenceId = RtmCmdSeqId.getSeuenceId();
        plzResetCmd.mCmdId = IRtmCmd.CMDID_PTZ_RESET;
        plzResetCmd.mDeviceId = mDeviceId;
        plzResetCmd.mSendTimestamp = System.currentTimeMillis();

        int ret = mSessionMgr.getRtmMgrComp().sendCommandToDev(plzResetCmd);

        ALog.getInstance().d(TAG, "<sendCmdPtzReset> done, ret=" + ret
                + ", plzResetCmd=" + plzResetCmd);
        return ret;
    }

    @Override
    public int sendCmdSdcardFmt(final OnCommandCmdListener cmdListener) {
        RtmBaseCmd sdCardFmtCmd = new RtmBaseCmd();
        sdCardFmtCmd.mSequenceId = RtmCmdSeqId.getSeuenceId();
        sdCardFmtCmd.mCmdId = IRtmCmd.CMDID_SDCARD_FMT;
        sdCardFmtCmd.mDeviceId = mDeviceId;
        sdCardFmtCmd.mSendTimestamp = System.currentTimeMillis();

        int ret = mSessionMgr.getRtmMgrComp().sendCommandToDev(sdCardFmtCmd);

        ALog.getInstance().d(TAG, "<sendCmdSdcardFmt> done, ret=" + ret
                + ", sdCardFmtCmd=" + sdCardFmtCmd);
        return ret;
    }

    @Override
    public int sendCmdCustomize(final String customizeData,
                                final OnCommandCmdListener cmdListener) {
        RtmCustomizeReqCmd customizeCmd = new RtmCustomizeReqCmd();
        customizeCmd.mSequenceId = RtmCmdSeqId.getSeuenceId();
        customizeCmd.mCmdId = IRtmCmd.CMDID_SDCARD_FMT;
        customizeCmd.mDeviceId = mDeviceId;
        customizeCmd.mSendTimestamp = System.currentTimeMillis();
        customizeCmd.mSendData = customizeData;

        int ret = mSessionMgr.getRtmMgrComp().sendCommandToDev(customizeCmd);

        ALog.getInstance().d(TAG, "<sendCmdCustomize> done, ret=" + ret
                + ", customizeCmd=" + customizeCmd);
        return ret;
    }

}
