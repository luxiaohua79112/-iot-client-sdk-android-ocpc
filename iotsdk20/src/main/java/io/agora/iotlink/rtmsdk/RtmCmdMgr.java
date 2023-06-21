
package io.agora.iotlink.rtmsdk;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.agora.iotlink.IDeviceSessionMgr;


/**
 * @brief RMT命令管理器
 */
public class RtmCmdMgr {


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/RtmCmdMgr";



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private HashMap<Long, IRtmCmd> mCmdMap = new HashMap<>();  ///< 命令映射表


    ////////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /**
     * @brief 增加节点映射
     * @param rtmCmd : 要映射的会话
     * @return None
     */
    public void addCommand(final IRtmCmd rtmCmd) {
        synchronized (mCmdMap) {
            mCmdMap.put(rtmCmd.getSequenceId(), rtmCmd);
        }
    }

    /**
     * @brief 更新已经存在的节点信息
     * @param rtmCmd : 要更新的会话
     * @return None
     */
    public void updateCommand(final IRtmCmd rtmCmd) {
        synchronized (mCmdMap) {
            IRtmCmd tmpSession = mCmdMap.get(rtmCmd.getSequenceId());
            if (tmpSession == null) {
                return;
            }
            mCmdMap.put(rtmCmd.getSequenceId(), rtmCmd);
        }
    }

    /**
     * @brief 根据 sequenceId 获取命令信息
     * @return 返回提取到的session，如果未提取到则返回null
     */
    public IRtmCmd getCommand(long sequenceId) {
        synchronized (mCmdMap) {
            IRtmCmd IRtmCmd = mCmdMap.get(sequenceId);
            return IRtmCmd;
        }
    }


    /**
     * @brief 根据 sequenceId 删除命令信息
     * @return 返回删除的会话，如果未找到则返回null
     */
    public IRtmCmd removeCommand(final UUID sequenceId) {
        synchronized (mCmdMap) {
            IRtmCmd IRtmCmd = mCmdMap.remove(sequenceId);
            return IRtmCmd;
        }
    }


    /**
     * @brief 查询所有响应超时的命令
     * @return 返回超时的命令列表
     */
    public List<IRtmCmd> queryTimeoutCommandList(long timeout) {
        ArrayList<IRtmCmd> timeoutList = new ArrayList<>();
        long currTimestamp = System.currentTimeMillis();

        synchronized (mCmdMap) {
            for (Map.Entry<Long, IRtmCmd> entry : mCmdMap.entrySet()) {
                IRtmCmd rtmCmd = entry.getValue();

                if (!rtmCmd.isResponseCmd()) {
                    long timeDiff = currTimestamp - rtmCmd.getSendTimestamp();
                    if (timeDiff > timeout) {  // 呼叫超时
                        timeoutList.add(rtmCmd);
                    }
                }
            }
        }

        return timeoutList;
    }


    /**
     * @brief 获取当前所有命令列表
     * @return 返回所有命令列表
     */
    public List<IRtmCmd> getAllCommandList() {
        ArrayList<IRtmCmd> commandList = new ArrayList<>();
        synchronized (mCmdMap) {
            for (Map.Entry<Long, IRtmCmd> entry : mCmdMap.entrySet()) {
                IRtmCmd rtmCmd = entry.getValue();
                commandList.add(rtmCmd);
            }
        }

        return commandList;
    }


    /**
     * @brief  获取映射表数量
     * @return
     */
    public int size() {
        synchronized (mCmdMap) {
            int count = mCmdMap.size();
            return count;
        }
    }

    /**
     * @brief 清空映射表
     * @return None
     */
    public void clear() {
        synchronized (mCmdMap) {
            mCmdMap.clear();
        }
    }

}