package com.xzy.ble.simplebledemo.bluetooth.callback;

/**
 * 写操作回调接口
 *
 * @author xzy
 */
@SuppressWarnings("unused")
public interface OnWriteCallback {

    /**
     * 蓝牙未开启
     */
    int FAILED_BLUETOOTH_DISABLE = 1;

    /**
     * 特征无效
     */
    int FAILED_INVALID_CHARACTER = 2;

    /**
     * 写入成功
     */
    void onSuccess();

    /**
     * 写入失败
     *
     * @param state 状态码
     */
    void onFailed(int state);
}
