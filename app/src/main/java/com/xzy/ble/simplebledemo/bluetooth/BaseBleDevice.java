/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xzy.ble.simplebledemo.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;

import com.xzy.ble.simplebledemo.bluetooth.callback.BleDeviceScanCallback;
import com.xzy.ble.simplebledemo.bluetooth.callback.OnDeviceConnectChangedListener;
import com.xzy.ble.simplebledemo.bluetooth.callback.OnScanCallback;
import com.xzy.ble.simplebledemo.bluetooth.callback.OnWriteCallback;
import com.xzy.ble.simplebledemo.bluetooth.utils.HexUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.xzy.ble.simplebledemo.bluetooth.device.BleDevice.MAC;

/**
 * 抽象的硬件，提取公共部分
 *
 * @author xzy
 */

@SuppressWarnings({"Dreprecated", "unused"})
public abstract class BaseBleDevice {
    private final static String TAG = BaseBleDevice.class.getSimpleName();

    /**
     * 扫描结果广播
     */
    public static String scanResultNotify = "com.xzy.ble.action.scan.result.notify";
    public static String scanFinish = "com.xzy.ble.action.scan.result.finish";

    /**
     * 连接广播
     */
    public static String connectSuccess = "com.xzy.ble.action.connect.success";
    public static String connectFail = "com.xzy.ble.action.connect.fail";
    public static String connectTimeout = "com.xzy.ble.action.connect.timeout";
    public static String disconnect = "com.xzy.ble.action.connect.disconnect";
    public static String retryConnect = "com.xzy.ble.action.connect.retry.connect";

    /**
     * 读写
     */
    public static String writeSuccessAction = "com.xzy.ble.action.write_success";
    public static String writeFailedAction = "com.xzy.ble.action.write_failed";
    public static String notifyAction = "com.xzy.ble.action.notify";

    private Context context;

    /**
     * 默认扫描时间：10s
     */
    private static final int SCAN_TIME = 10000;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;

    private int mConnectionState = STATE_DISCONNECTED;
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private HashMap<String, Map<String, BluetoothGattCharacteristic>> servicesMap = new HashMap<>();

    /**
     * 服务 UUID
     */
    protected UUID serviceUuid = null;
    /**
     * 写数据 UUID
     */
    protected UUID writeUuid = null;
    /**
     * 读数据 UUID
     */
    protected UUID notifyUuid = null;

    private Handler handler = new Handler();
    private boolean mScanning;
    private BleDeviceScanCallback bleDeviceScanCallback;
    private OnDeviceConnectChangedListener connectChangedListener;
    private boolean isServiceConnected = false;

    public BaseBleDevice(Context context) {
        this.context = context;
        initialize();
    }


    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (connectChangedListener != null) {
                    connectChangedListener.onConnected();
                }
                mConnectionState = STATE_CONNECTED;
                sendBroadcast(connectSuccess, "连接成功");
                mBluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED || status != BluetoothGatt.GATT_SUCCESS) {
                if (connectChangedListener != null) {
                    connectChangedListener.onDisconnected();
                    sendBroadcast(connectFail, "连接失败");
                }
                mConnectionState = STATE_DISCONNECTED;
                // 重置
                gatt.close();
                close();
                // 重连操作 todo
                connect(MAC);
            }

            Log.d(TAG, "连接状态:" + mConnectionState);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onServicesDiscovered: " + "发现服务 : status = " + status);
                //成功
                isServiceConnected = true;
                if (mBluetoothGatt != null) {
                    BluetoothGattService gattService = mBluetoothGatt.getService(serviceUuid);
                    BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(notifyUuid);
                    boolean b = mBluetoothGatt.setCharacteristicNotification(characteristic, true);
                    if (b) {

                        List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
                        for (BluetoothGattDescriptor descriptor : descriptors) {

                            boolean b1 = descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            if (b1) {
                                mBluetoothGatt.writeDescriptor(descriptor);
                                Log.d(TAG, "描述 UUID :" + descriptor.getUuid().toString());
                            }
                        }
                        Log.d(TAG, "startRead: " + "监听接收数据开始");
                    }
                }
            } else {
                Log.w(TAG, "onServicesDiscovered : 发现服务异常 status = " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicChanged: 接收数据成功,value =" + HexUtil.bytesToHexString(characteristic.getValue()));
            // parseData(characteristic);
            sendBroadcast(notifyAction, HexUtil.bytesToHexString(characteristic.getValue()));
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.d(TAG, "onDescriptorWrite: " + "设置成功");
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d(TAG, "onCharacteristicWrite: " + "发送数据成功，value = " + HexUtil.bytesToHexString(characteristic.getValue()));
            sendBroadcast(writeSuccessAction, HexUtil.bytesToHexString(characteristic.getValue()));
        }
    };


    /**
     * 回调数据解析
     *
     * @param characteristic BluetoothGattCharacteristic
     */
    //public abstract void parseData(BluetoothGattCharacteristic characteristic);


    /**
     * Initializes a reference to the local Bluetooth adapter.
     */
    private void initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, " --------- Unable to initialize BluetoothManager. --------- ");
                return;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, " --------- Unable to obtain a BluetoothAdapter. --------- ");
        }
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     */
    public void connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, " --------- BluetoothAdapter not initialized or unspecified address. --------- ");
            return;
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, " --------- Device not found.  Unable to connect. --------- ");
            return;
        }
        mBluetoothGatt = device.connectGatt(context, true, mGattCallback);
        Log.d(TAG, " --------- Trying to create a new connection. --------- ");
        mConnectionState = STATE_CONNECTING;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, " --------- BluetoothAdapter not initialized --------- ");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.disconnect();
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * 当前蓝牙是否打开
     */
    private boolean isBleEnable() {
        if (null != mBluetoothAdapter) {
            return mBluetoothAdapter.isEnabled();
        }
        return false;
    }


    /**
     * @param enable       是否扫描设备
     * @param scanCallback 扫描回到
     */
    public void scanBleDevice(final boolean enable, final OnScanCallback scanCallback) {
        scanBleDevice(SCAN_TIME, enable, scanCallback, null);
    }

    /**
     * @param enable       是否扫描设备
     * @param scanCallback 扫描回到
     * @param ids          扫描指定service uuid的设备
     */
    public void scanBleDevice(final boolean enable, final OnScanCallback scanCallback, UUID[] ids) {
        scanBleDevice(SCAN_TIME, enable, scanCallback, ids);
    }

    /**
     * @param time         扫描时长
     * @param enable       是否开启扫描
     * @param scanCallback 扫描回调
     * @param ids          扫描指定的 uuid 设备
     */
    public void scanBleDevice(int time, final boolean enable, final OnScanCallback scanCallback, UUID[] ids) {
        if (!isBleEnable()) {
            mBluetoothAdapter.enable();
            Log.e(TAG, "Bluetooth is not open!");
        }
        if (null != mBluetoothGatt) {
            mBluetoothGatt.close();
        }
        if (bleDeviceScanCallback == null) {
            bleDeviceScanCallback = new BleDeviceScanCallback(scanCallback);
        }
        if (enable) {
            if (mScanning) {
                return;
            }
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    //time后停止扫描
                    mBluetoothAdapter.stopLeScan(bleDeviceScanCallback);
                    scanCallback.onFinish();
                }
            }, time <= 0 ? SCAN_TIME : time);
            mScanning = true;
            if (ids != null) {
                mBluetoothAdapter.startLeScan(ids, bleDeviceScanCallback);
            } else {
                mBluetoothAdapter.startLeScan(bleDeviceScanCallback);
            }
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(bleDeviceScanCallback);
            Log.d(TAG, "停止扫描");
        }
    }


    public void stopScan() {
        mScanning = false;
        mBluetoothAdapter.stopLeScan(bleDeviceScanCallback);
        Log.d(TAG, "停止扫描");
    }

    /**
     * 发送数据
     *
     * @param value         指令
     * @param writeCallback 发送回调
     */
    public void writeBuffer(String value, OnWriteCallback writeCallback) {
        if ((mBluetoothGatt != null) && (isServiceConnected)) {
            BluetoothGattService gattService = mBluetoothGatt.getService(serviceUuid);
            BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(writeUuid);
            characteristic.setValue(HexUtil.hexStringToBytes(value));
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            boolean b = mBluetoothGatt.writeCharacteristic(characteristic);
            if (b) {
                if (writeCallback != null) {
                    writeCallback.onSuccess();
                }
            } else {
                sendBroadcast(writeFailedAction, "发送失败");
            }
        }
    }

    public BluetoothAdapter isDeviceSupport() {
        //需要设备支持ble
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            return null;
        }

        //需要有BluetoothAdapter
        final BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        assert bluetoothManager != null;
        return bluetoothManager.getAdapter();
    }

    public void setConnectChangedListener(OnDeviceConnectChangedListener connectChangedListener) {
        this.connectChangedListener = connectChangedListener;
    }

    private void sendBroadcast(String action, String data) {
        Intent intent = new Intent(action);
        intent.putExtra("data", data);
        context.sendBroadcast(intent);
    }
}
