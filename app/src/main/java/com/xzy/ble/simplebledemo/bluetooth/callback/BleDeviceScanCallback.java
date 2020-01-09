package com.xzy.ble.simplebledemo.bluetooth.callback;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;


/**
 * 蓝牙设备扫描回调接口
 * @author xzy
 */
public class BleDeviceScanCallback implements BluetoothAdapter.LeScanCallback {
    private OnScanCallback mScanCallback;

    public BleDeviceScanCallback(OnScanCallback scanCallback) {
        this.mScanCallback = scanCallback;
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (null != mScanCallback) {
            //每次扫描到设备会回调此方法,这里一般做些过滤在添加进list列表
            mScanCallback.onScanning(device, rssi, scanRecord);
        }
    }
}
