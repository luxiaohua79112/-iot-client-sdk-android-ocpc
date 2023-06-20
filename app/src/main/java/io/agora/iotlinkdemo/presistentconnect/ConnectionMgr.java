package io.agora.iotlinkdemo.presistentconnect;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;



/**
 * @brief 会话管理器
 */
public class ConnectionMgr {


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/ConnectMgr";



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private HashMap<UUID, ConnectionCtx> mConnectMap = new HashMap<>();  ///< 会话映射表


    ////////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /**
     * @brief 增加节点映射
     * @param ConnectionCtx : 要映射的会话
     * @return None
     */
    public void addConnection(final ConnectionCtx ConnectionCtx) {
        synchronized (mConnectMap) {
            mConnectMap.put(ConnectionCtx.mConnectId, ConnectionCtx);
        }
    }

    /**
     * @brief 更新已经存在的节点信息
     * @param ConnectionCtx : 要更新的会话
     * @return None
     */
    public void updateConnection(final ConnectionCtx ConnectionCtx) {
        synchronized (mConnectMap) {
            ConnectionCtx tmpSession = mConnectMap.get(ConnectionCtx.mConnectId);
            if (tmpSession == null) {
                return;
            }
            mConnectMap.put(ConnectionCtx.mConnectId, ConnectionCtx);
        }
    }

    /**
     * @brief 根据 sessionId 获取会话信息
     * @return 返回提取到的session，如果未提取到则返回null
     */
    public ConnectionCtx getConnection(final UUID sessionId) {
        synchronized (mConnectMap) {
            ConnectionCtx ConnectionCtx = mConnectMap.get(sessionId);
            return ConnectionCtx;
        }
    }

    /**
     * @brief 根据 设备DeviceId 找到第一个会话
     * @return 返回提取到的session，如果未提取到则返回null
     */
    public ConnectionCtx findConnectionByDeviceId(final String deviceId) {
        synchronized (mConnectMap) {
            for (Map.Entry<UUID, ConnectionCtx> entry : mConnectMap.entrySet()) {
                ConnectionCtx ConnectionCtx = entry.getValue();
                if (deviceId.compareToIgnoreCase(ConnectionCtx.mDeviceId) == 0) {
                    return ConnectionCtx;
                }
            }
        }

        return null;
    }


    /**
     * @brief 根据 频道名 找到第一个会话
     * @return 返回提取到的session，如果未提取到则返回null
     */
    public ConnectionCtx findConnectionByChannelName(final String chnName) {
        synchronized (mConnectMap) {
            for (Map.Entry<UUID, ConnectionCtx> entry : mConnectMap.entrySet()) {
                ConnectionCtx ConnectionCtx = entry.getValue();
                if (ConnectionCtx.mChnlName == null) {
                    continue;
                }
                if (chnName.compareToIgnoreCase(ConnectionCtx.mChnlName) == 0) {
                    return ConnectionCtx;
                }
            }
        }

        return null;
    }

    /**
     * @brief 根据 traceId 找到第一个会话
     * @return 返回提取到的session，如果未提取到则返回null
     */
    public ConnectionCtx findConnectionByTraceId(final long traceId) {
        synchronized (mConnectMap) {
            for (Map.Entry<UUID, ConnectionCtx> entry : mConnectMap.entrySet()) {
                ConnectionCtx sessionCtx = entry.getValue();
                if (traceId == sessionCtx.mTraceId) {
                    return sessionCtx;
                }
            }
        }

        return null;
    }

    /**
     * @brief 根据 sessionId 删除会话信息
     * @return 返回删除的会话，如果未找到则返回null
     */
    public ConnectionCtx removeConnection(final UUID connectId) {
        synchronized (mConnectMap) {
            ConnectionCtx ConnectionCtx = mConnectMap.remove(connectId);
            return ConnectionCtx;
        }
    }


    /**
     * @brief 获取当前所有会话列表
     * @return 返回所有会话列表
     */
    public List<ConnectionCtx> getAllConnectionList() {
        ArrayList<ConnectionCtx> sessionList = new ArrayList<>();
        synchronized (mConnectMap) {
            for (Map.Entry<UUID, ConnectionCtx> entry : mConnectMap.entrySet()) {
                ConnectionCtx ConnectionCtx = entry.getValue();
                sessionList.add(ConnectionCtx);
            }
        }

        return sessionList;
    }


    /**
     * @brief  获取映射表数量
     * @return
     */
    public int size() {
        synchronized (mConnectMap) {
            int count = mConnectMap.size();
            return count;
        }
    }

    /**
     * @brief 清空映射表
     * @return None
     */
    public void clear() {
        synchronized (mConnectMap) {
            mConnectMap.clear();
        }
    }




}