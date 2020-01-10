package com.xzy.ble.simplebledemo.bluetooth.device;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.util.Log;

import com.xzy.ble.simplebledemo.bluetooth.BaseBleDevice;
import com.xzy.ble.simplebledemo.bluetooth.utils.HexUtil;

import java.util.UUID;


/**
 * 具体的硬件设备,如果有多台设备，可以有多个该类的定义。
 *
 * @author 002034
 */
public class BleDevice extends BaseBleDevice {
    private static final String TAG = "BleDevice";

    /**
     * 根据具体硬件进行设置
     **/
    public static final String MAC = "A4:34:F1:4A:08:05";
    private static final UUID DEVICE_SERVICE_UUID = UUID.fromString("00001000-0000-1000-8000-00805f9b34fb");
    private static final UUID DEVICE_WRITE_UUID = UUID.fromString("00001001-0000-1000-8000-00805f9b34fb");
    private static final UUID DEVICE_NOTIFY_UUID = UUID.fromString("00001002-0000-1000-8000-00805f9b34fb");


    @SuppressLint("StaticFieldLeak")
    private static BleDevice mBleDevice;

    private BleDevice(Context context) {
        super(context);
        serviceUuid = DEVICE_SERVICE_UUID;
        writeUuid = DEVICE_WRITE_UUID;
        notifyUuid = DEVICE_NOTIFY_UUID;
    }

    public static BleDevice getInstance(Context context){
        if(mBleDevice == null){
            mBleDevice = new BleDevice(context);
        }
        return mBleDevice;
    }
}
