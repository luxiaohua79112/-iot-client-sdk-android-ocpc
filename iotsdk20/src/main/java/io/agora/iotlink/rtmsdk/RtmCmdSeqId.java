package io.agora.iotlink.rtmsdk;


import java.util.UUID;

/**
 * @brief RTM命令序列Id控制类
 */
public class RtmCmdSeqId {

    private static long mSequenceId = 1;


    /**
     * @brief 获取一个新的命令序列
     */

    public static synchronized long getSeuenceId() {
        long seqId = mSequenceId;
        mSequenceId++;
        return seqId;
    }
}
