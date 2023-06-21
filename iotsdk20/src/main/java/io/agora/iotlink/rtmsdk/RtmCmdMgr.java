
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
    private HashMap<Long, RtmCmdCtx> mCmdMap = new HashMap<>();  ///< 命令映射表


    ////////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /**
     * @brief 增加节点映射
     * @param RtmCmdCtx : 要映射的会话
     * @return None
     */
    public void addCommand(final RtmCmdCtx RtmCmdCtx) {
        synchronized (mCmdMap) {
            mCmdMap.put(RtmCmdCtx.mSequenceId, RtmCmdCtx);
        }
    }

    /**
     * @brief 更新已经存在的节点信息
     * @param RtmCmdCtx : 要更新的会话
     * @return None
     */
    public void updateCommand(final RtmCmdCtx RtmCmdCtx) {
        synchronized (mCmdMap) {
            RtmCmdCtx tmpSession = mCmdMap.get(RtmCmdCtx.mSequenceId);
            if (tmpSession == null) {
                return;
            }
            mCmdMap.put(RtmCmdCtx.mSequenceId, RtmCmdCtx);
        }
    }

    /**
     * @brief 根据 sequenceId 获取命令信息
     * @return 返回提取到的session，如果未提取到则返回null
     */
    public RtmCmdCtx getCommand(long sequenceId) {
        synchronized (mCmdMap) {
            RtmCmdCtx RtmCmdCtx = mCmdMap.get(sequenceId);
            return RtmCmdCtx;
        }
    }


    /**
     * @brief 根据 sequenceId 删除命令信息
     * @return 返回删除的会话，如果未找到则返回null
     */
    public RtmCmdCtx removeCommand(final UUID sequenceId) {
        synchronized (mCmdMap) {
            RtmCmdCtx RtmCmdCtx = mCmdMap.remove(sequenceId);
            return RtmCmdCtx;
        }
    }


    /**
     * @brief 查询所有响应超时的命令
     * @return 返回超时的命令列表
     */
    public List<RtmCmdCtx> queryTimeoutCommandList(long timeout) {
        ArrayList<RtmCmdCtx> timeoutList = new ArrayList<>();
        long currTimestamp = System.currentTimeMillis();

        synchronized (mCmdMap) {
            for (Map.Entry<Long, RtmCmdCtx> entry : mCmdMap.entrySet()) {
                RtmCmdCtx rtmCmdCtx = entry.getValue();

                if (!rtmCmdCtx.mIsRespCmd) {
                    long timeDiff = currTimestamp - rtmCmdCtx.mSendTimestamp;
                    if (timeDiff > timeout) {  // 呼叫超时
                        timeoutList.add(rtmCmdCtx);
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
    public List<RtmCmdCtx> getAllCommandList() {
        ArrayList<RtmCmdCtx> commandList = new ArrayList<>();
        synchronized (mCmdMap) {
            for (Map.Entry<Long, RtmCmdCtx> entry : mCmdMap.entrySet()) {
                RtmCmdCtx rtmCmdCtx = entry.getValue();
                commandList.add(rtmCmdCtx);
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