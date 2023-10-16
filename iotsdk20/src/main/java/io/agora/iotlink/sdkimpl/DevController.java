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
import io.agora.iotlink.logger.ALog;
import io.agora.iotlink.rtmsdk.IRtmCmd;
import io.agora.iotlink.rtmsdk.RtmBaseCmd;
import io.agora.iotlink.rtmsdk.RtmCmdSeqId;
import io.agora.iotlink.rtmsdk.RtmCustomizeReqCmd;
import io.agora.iotlink.rtmsdk.RtmCustomizeRspCmd;
import io.agora.iotlink.rtmsdk.RtmPlzCtrlReqCmd;


/*
 * @brief 设备信令控制器
 */
public class DevController  implements IDevController {


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/DevController";


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
        plzCtrlCmd.mRespListener = new IRtmCmd.OnRtmCmdRespListener() {
            @Override
            public void onRtmCmdResponsed(int commandId, int errCode, IRtmCmd reqCmd, IRtmCmd rspCmd) {
                ALog.getInstance().d(TAG, "<sendCmdPtzCtrl.onRtmCmdResponsed> errCode=" + errCode);
                cmdListener.onDeviceCmdDone(errCode, null);
            }
        };

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
        plzResetCmd.mRespListener = new IRtmCmd.OnRtmCmdRespListener() {
            @Override
            public void onRtmCmdResponsed(int commandId, int errCode, IRtmCmd reqCmd, IRtmCmd rspCmd) {
                ALog.getInstance().d(TAG, "<sendCmdPtzReset.onRtmCmdResponsed> errCode=" + errCode);
                cmdListener.onDeviceCmdDone(errCode, null);
            }
        };

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
        sdCardFmtCmd.mRespListener = new IRtmCmd.OnRtmCmdRespListener() {
            @Override
            public void onRtmCmdResponsed(int commandId, int errCode, IRtmCmd reqCmd, IRtmCmd rspCmd) {
                ALog.getInstance().d(TAG, "<sendCmdSdcardFmt.onRtmCmdResponsed> errCode=" + errCode);
                cmdListener.onDeviceCmdDone(errCode, null);
            }
        };

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
        customizeCmd.mCmdId = IRtmCmd.CMDID_CUSTOMIZE_SEND;
        customizeCmd.mDeviceId = mDeviceId;
        customizeCmd.mSendTimestamp = System.currentTimeMillis();
        customizeCmd.mSendData = customizeData;
        customizeCmd.mRespListener = new IRtmCmd.OnRtmCmdRespListener() {
            @Override
            public void onRtmCmdResponsed(int commandId, int errCode, IRtmCmd reqCmd, IRtmCmd rspCmd) {
                ALog.getInstance().d(TAG, "<sendCmdCustomize.onRtmCmdResponsed> errCode=" + errCode);
                RtmCustomizeRspCmd respCmd = (RtmCustomizeRspCmd)rspCmd;
                String recvData = null;
                if (respCmd != null) {
                    recvData = respCmd.mRecvData;
                }
                cmdListener.onDeviceCmdDone(errCode, recvData);
            }
        };

        int ret = mSessionMgr.getRtmMgrComp().sendCommandToDev(customizeCmd);

        ALog.getInstance().d(TAG, "<sendCmdCustomize> done, ret=" + ret
                + ", customizeCmd=" + customizeCmd);
        return ret;
    }


    @Override
    public int sendCmdDevReset(final OnCommandCmdListener cmdListener) {
        RtmBaseCmd resetCmd = new RtmBaseCmd();
        resetCmd.mSequenceId = RtmCmdSeqId.getSeuenceId();
        resetCmd.mCmdId = IRtmCmd.CMDID_DEVICE_RESET;
        resetCmd.mDeviceId = mDeviceId;
        resetCmd.mSendTimestamp = System.currentTimeMillis();
        resetCmd.mRespListener = new IRtmCmd.OnRtmCmdRespListener() {
            @Override
            public void onRtmCmdResponsed(int commandId, int errCode, IRtmCmd reqCmd, IRtmCmd rspCmd) {
                ALog.getInstance().d(TAG, "<sendCmdDevReset.onRtmCmdResponsed> errCode=" + errCode);
                cmdListener.onDeviceCmdDone(errCode, null);
            }
        };

        int ret = mSessionMgr.getRtmMgrComp().sendCommandToDev(resetCmd);

        ALog.getInstance().d(TAG, "<sendCmdDevReset> done, ret=" + ret
                + ", resetCmd=" + resetCmd);
        return ret;
    }



    @Override
    public int devRawMsgSend(final String sendingMsg, final OnDevMsgSendListener sendListener) {
        int ret = mSessionMgr.getRtmMgrComp().sendRawMsgToDev(mDeviceId, sendingMsg, sendListener);
        ALog.getInstance().d(TAG, "<devRawMsgSend> done, ret=" + ret
                + ", sendingMsg=" + sendingMsg);
        return ret;
    }


    @Override
    public int devRawMsgSetRecvListener(final OnDevMsgRecvListener recvListener) {
        mSessionMgr.getRtmMgrComp().setRawMsgRecvListener(recvListener);
        return ErrCode.XOK;
    }


}
