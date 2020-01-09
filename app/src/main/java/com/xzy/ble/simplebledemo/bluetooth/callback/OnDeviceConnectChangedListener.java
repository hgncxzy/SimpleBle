package com.xzy.ble.simplebledemo.bluetooth.callback;

/**
 * 蓝牙设备连接状态发生变化回调
 *
 * @author xzy
 */
public interface OnDeviceConnectChangedListener {
    void onConnected();

    void onDisconnected();
}
