package io.agora.iotlink.rtmsdk;


import java.util.UUID;

/**
 * @brief RTM传输的数据包
 */
public class RtmPacket {
    public UUID mSessionId;             ///< 数据包所属的会话
    public long mSequenceId;            ///< 命令的唯一序列号
    public String mPeerId;              ///< 目标对端Id
    public String mPktData;             ///< 消息数据包内容


    @Override
    public String toString() {
        String infoText = "{ mSessionId=" + mSessionId
                + ", mSequenceId=" + mSequenceId
                + ", mPeerId=" + mPeerId
                + ", mPktData=" + mPktData + " }";
        return infoText;
    }
}
