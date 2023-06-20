
package io.agora.iotlink.callkit;


import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.agora.iotlink.IDeviceSessionMgr;


/**
 * @brief 会话管理器
 */
public class SessionMgr {


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/SessionMgr";



    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private HashMap<UUID, SessionCtx> mSessionMap = new HashMap<>();  ///< 会话映射表


    ////////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////
    /**
     * @brief 增加节点映射
     * @param sessionCtx : 要映射的会话
     * @return None
     */
    public void addSession(final SessionCtx sessionCtx) {
        synchronized (mSessionMap) {
            mSessionMap.put(sessionCtx.mSessionId, sessionCtx);
        }
    }

    /**
     * @brief 更新已经存在的节点信息
     * @param sessionCtx : 要更新的会话
     * @return None
     */
    public void updateSession(final SessionCtx sessionCtx) {
        synchronized (mSessionMap) {
            SessionCtx tmpSession = mSessionMap.get(sessionCtx.mSessionId);
            if (tmpSession == null) {
                return;
            }
            mSessionMap.put(sessionCtx.mSessionId, sessionCtx);
        }
    }

    /**
     * @brief 根据 sessionId 获取会话信息
     * @return 返回提取到的session，如果未提取到则返回null
     */
    public SessionCtx getSession(final UUID sessionId) {
        synchronized (mSessionMap) {
            SessionCtx sessionCtx = mSessionMap.get(sessionId);
            return sessionCtx;
        }
    }

    /**
     * @brief 根据 设备DeviceId 找到第一个会话
     * @return 返回提取到的session，如果未提取到则返回null
     */
    public SessionCtx findSessionByDeviceId(final String deviceId) {
        synchronized (mSessionMap) {
            for (Map.Entry<UUID, SessionCtx> entry : mSessionMap.entrySet()) {
                SessionCtx sessionCtx = entry.getValue();
                if (deviceId.compareToIgnoreCase(sessionCtx.mDeviceId) == 0) {
                    return sessionCtx;
                }
            }
        }

        return null;
    }


    /**
     * @brief 根据 频道名 找到第一个会话
     * @return 返回提取到的session，如果未提取到则返回null
     */
    public SessionCtx findSessionByChannelName(final String chnName) {
        synchronized (mSessionMap) {
            for (Map.Entry<UUID, SessionCtx> entry : mSessionMap.entrySet()) {
                SessionCtx sessionCtx = entry.getValue();
                if (sessionCtx.mChnlName == null) {
                    continue;
                }
                if (chnName.compareToIgnoreCase(sessionCtx.mChnlName) == 0) {
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
    public SessionCtx removeSession(final UUID sessionId) {
        synchronized (mSessionMap) {
            SessionCtx sessionCtx = mSessionMap.remove(sessionId);
            return sessionCtx;
        }
    }


    /**
     * @brief 查询所有连接超时的会话
     * @return 返回超时的会话列表
     */
    public List<SessionCtx> queryTimeoutSessionList(long connectTimeout) {
        ArrayList<SessionCtx> timeoutList = new ArrayList<>();
        long currTimestamp = System.currentTimeMillis();

        synchronized (mSessionMap) {
            for (Map.Entry<UUID, SessionCtx> entry : mSessionMap.entrySet()) {
                SessionCtx sessionCtx = entry.getValue();

                if (sessionCtx.mState == IDeviceSessionMgr.SESSION_STATE_CONNECTING) {
                    long timeDiff = currTimestamp - sessionCtx.mConnectTimestamp;
                    if (timeDiff > connectTimeout) {  // 呼叫超时
                        timeoutList.add(sessionCtx);
                    }
                }
            }
        }

        return timeoutList;
    }


    /**
     * @brief 获取当前所有会话列表
     * @return 返回所有会话列表
     */
    public List<SessionCtx> getAllSessionList() {
        ArrayList<SessionCtx> sessionList = new ArrayList<>();
        synchronized (mSessionMap) {
            for (Map.Entry<UUID, SessionCtx> entry : mSessionMap.entrySet()) {
                SessionCtx sessionCtx = entry.getValue();
                sessionList.add(sessionCtx);
            }
        }

        return sessionList;
    }


    /**
     * @brief  获取映射表数量
     * @return
     */
    public int size() {
        synchronized (mSessionMap) {
            int count = mSessionMap.size();
            return count;
        }
    }

    /**
     * @brief 清空映射表
     * @return None
     */
    public void clear() {
        synchronized (mSessionMap) {
            mSessionMap.clear();
        }
    }




}