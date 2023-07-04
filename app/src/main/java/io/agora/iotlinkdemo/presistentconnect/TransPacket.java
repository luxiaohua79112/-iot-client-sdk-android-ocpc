package io.agora.iotlinkdemo.presistentconnect;


import org.json.JSONObject;

import java.util.UUID;

/**
 * @brief 传输的数据包
 */
public class TransPacket {
    public String mTopic;
    public int mMessageId;
    public String mContent;
    public UUID mConnectId;

    public JSONObject mBodyJsonObj;
    public long mTraceId;

    @Override
    public String toString() {
        String infoText = "{ mTopic=" + mTopic
                + ", mMessageId=" + mMessageId
                + ", mContent=" + mContent
                + ", mConnectId=" + mConnectId + " }";
        return infoText;
    }
}
