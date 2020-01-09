package com.xzy.ble.simplebledemo.bluetooth.callback;

import android.bluetooth.BluetoothDevice;


/**
 * 蓝牙设备扫描中间状态回调接口
 *
 * @author xzy
 */
public interface OnScanCallback {
    /**
     * 扫描完成回调
     */
    void onFinish();

    /**
     * 扫描过程中,每扫描到一个设备回调一次
     *
     * @param device     扫描到的设备
     * @param rssi       设备的信息强度
     * @param scanRecord 远程设备提供的广告记录的内容
     */
    void onScanning(final BluetoothDevice device, int rssi, byte[] scanRecord);
}
