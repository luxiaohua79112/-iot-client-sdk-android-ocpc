package io.agora.iotlink.rtmsdk;


import java.util.UUID;

/**
 * @brief RTM传输的数据包
 */
public class RtmPacket {
    public UUID mSessionId;             ///< 数据包所属的会话
    public String mPeerId;              ///< 目标对端Id
    public byte[] mPktData;             ///< 消息数据包内容


    @Override
    public String toString() {
        String infoText = "{ mSessionId=" + mSessionId
                + ", mPeerId=" + mPeerId
                + ", mPktData.length=" + mPktData.length + " }";
        return infoText;
    }
}
