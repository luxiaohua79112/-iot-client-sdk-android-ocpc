package io.agora.iotlink.rtmsdk;


import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import io.agora.iotlink.ErrCode;
import io.agora.iotlink.logger.ALog;
import io.agora.iotlink.utils.JsonUtils;

/**
 * @brief 设备端云台控制参数
 *
 */
public class DevPlzCtrlParam  {

    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Constant Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    private static final String TAG = "IOTSDK/DevPlzCtrlParam";


    ////////////////////////////////////////////////////////////////////////
    //////////////////////// Variable Definition ///////////////////////////
    ////////////////////////////////////////////////////////////////////////
    public int mAction;         ///< 0----开始控制；  1----停止控制
    public int mDirection;      ///< 0-上、1-下、2-左、3-右、4-镜头拉近、5-镜头拉远
    public int mSpeed = 1;      ///< 0-慢，1-适中（默认），2-快


    ///////////////////////////////////////////////////////////////////////
    ////////////////////////// Public Methods  ////////////////////////////
    ///////////////////////////////////////////////////////////////////////
    @Override
    public String toString() {
        String infoText = "{ mAction=" + mAction
                + ", mDirection=" + mDirection
                + ", mSpeed=" + mSpeed + " }";
        return infoText;
    }

}
