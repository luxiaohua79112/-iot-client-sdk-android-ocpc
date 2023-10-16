package io.agora.iotlink.rtmsdk;


import java.util.UUID;

import io.agora.iotlink.IDevController;

/**
 * @brief RTM传输的数据包
 */
public class RtmPacket {
    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    public static final int PKT_TYPE_COMMAND = 0x0000;     ///< 数据包类型：命令(有固定的协议格式)
    public static final int PKT_TYPE_RAWMSG = 0x0001;      ///< 数据包类型: 原始落数据


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    public UUID mSessionId;             ///< 数据包所属的会话
    public long mSequenceId;            ///< 命令的唯一序列号
    public String mPeerId;              ///< 目标对端Id
    public String mPktData;             ///< 消息数据包内容

    public IDevController.OnDevMsgSendListener mSendListener;
    public int mPktType = PKT_TYPE_COMMAND;

    @Override
    public String toString() {
        String infoText = "{ mSessionId=" + mSessionId
                + ", mSequenceId=" + mSequenceId
                + ", mPeerId=" + mPeerId
                + ", mPktData=" + mPktData + " }";
        return infoText;
    }
}
