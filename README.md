# SimpleBle
纯手写实现极简方式 BLE 开发，并适度封装。实现扫描、连接、发送、接收等功能

下面带大家从 0 实现基本的 BLE 开发

### 权限

进行蓝牙相关操作，需要使用到蓝牙权限，在AndroidManifest.xml清单文件中添加相应权限

```xml
<uses-feature
    android:name="android.hardware.bluetooth_le"
    android:required="true" />
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
```

在 android6.0 以后，蓝牙BLE还需要需要获得位置权限

```xml
  <uses-permission  android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

这个位置权限，需要动态申请

```java
/**
     * 请求位置权限
     *
     * @param activity 上下文
     * @return boolean
     */
    public static boolean isGrantLocationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && activity.checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
            }, GROUP_LOCATION);

            return false;
        }
        return true;
    }
```



### 初始化蓝牙 Adapter

```java
final BluetoothManager bluetoothManager =
 (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
    if (bluetoothManager != null){
   	 mBluetoothAdapter = bluetoothManager.getAdapter();
    }
```

判断蓝牙是否可用或者是否开启，如果蓝牙关闭，那么开启蓝牙

```java
    if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
        Intent enableBtIntent = new   Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, 1);
    }
```

### 扫描

扫描过程中，需要对扫描结果进行回调，在 onLeScan(）方法中对扫描的结果进行相关处理

```java
private BluetoothDevice mDevice;
final BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
    @Override
    public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
        Log.d("haha", "onLeScan:  " + device.getName() + " : " + rssi);
        String name = device.getName();
        if (name != null) {
            deviceName.setText(name);
            if (name.equals("test_ble")) {// 或者比较 mac 地址
                mDevice = device;
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }
    }
};
```

由于蓝牙扫描耗时耗电，所以在进行扫描的时候，注意自定义一个合适的扫描时间，在实际的开发和项目应用过程中，自己选择合适的时间。定义好蓝牙扫描回调，开始扫描蓝牙，扫描到想要的蓝牙，就可以停止扫描

```java
    private void scanLeDevice(final boolean enable) {
    if (enable) {
        // Stops scanning after a pre-defined scan period.
        // 预先定义停止蓝牙扫描的时间（因为蓝牙扫描需要消耗较多的电量）

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mScanning = false;
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }, 30000);
        mScanning = true;
        // 定义一个回调接口供扫描结束处理
        mBluetoothAdapter.startLeScan(mLeScanCallback);
    } else {
        mScanning = false;
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
    }
}
```

通过获取的 mBluetoothAdapter 调用 startLeScan() 传入 mLeScanCallback 参数，即可进行蓝牙扫描。

### BluetoothGattCallback 实现

GATT 是用于发送和接收的通用规范， BLE 之间的文件数据传输基于 GATT，因此在进行连接之前，需要进行Gatt接口回调。

```java
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
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED 
                       || status != BluetoothGatt.GATT_SUCCESS) {
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
                    BluetoothGattCharacteristic characteristic = gattService
                        .getCharacteristic(notifyUuid);
                    boolean b = mBluetoothGatt.setCharacteristicNotification(characteristic, true);
                    if (b) {
                        List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
                        for (BluetoothGattDescriptor descriptor : descriptors) {
                            boolean b1 = descriptor
                                .setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
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
        public void onCharacteristicChanged(BluetoothGatt gatt, 
                                            BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicChanged: 接收数据成功,value =" + HexUtil
                  .bytesToHexString(characteristic.getValue()));
            // parseData(characteristic);
            sendBroadcast(notifyAction, HexUtil.bytesToHexString(characteristic.getValue()));
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, 
                                      BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.d(TAG, "onDescriptorWrite: " + "设置成功");
        }
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, 
                                          BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d(TAG, "onCharacteristicWrite: " + "发送数据成功，value = " + HexUtil
                  .bytesToHexString(characteristic.getValue()));
            sendBroadcast(writeSuccessAction, HexUtil.bytesToHexString(characteristic.getValue()));
        }
    };
```

通过status对当前连接进行判断，当status != BluetoothGatt.GATT_SUCCESS时，可以进行Gatt的重置操作，尝试重连。当newState == BluetoothProfile.STATE_CONNECTED时，此时连接成功。

### 连接

```java
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
```

### 发现服务

在连接成功后，可以通过 Gatt 进行 discoverServices()

```java
if (newState == BluetoothProfile.STATE_CONNECTED) {
     if (connectChangedListener != null) {
         connectChangedListener.onConnected();
      }
      mConnectionState = STATE_CONNECTED;
      sendBroadcast(connectSuccess, "连接成功");
      mBluetoothGatt.discoverServices();
}
```

在 mGattCallback 回调添加 Servicest 的相关回调

```java
  //发现服务回调。
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        Log.d("haha", "onServicesDiscovered: " + "发现服务 : " + status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
                 //成功
            }
        }
```

### 读写开关

当返回的 status  == BluetoothGatt.GATT_SUCCESS 时，进行读写以及通知相关的操作， 调用 writeDescriptor()，注意设置 setValue 为 ENABLE_NOTIFICATION_VALUE，否则可能后续读取不到数据。

```java

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
```

设置成功，会在 onDescriptorWrite 方法进行回调，注意 服务 UUID， 特征值UUID，通知 UUID，可以询问公司固件端的开发人员，和开发人员配合修改。

```java
     @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status{
        super.onDescriptorWrite(gatt, descriptor, status);
        Log.d(TAG, "onDescriptorWrite: " + "设置成功");
    }
```

### 发送数据

```java

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
```

### 读取数据

读取数据在 onCharacteristicChanged 方法中，注意进制间的转换。

```java

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, 
                                            BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicChanged: 接收数据成功,value =" + HexUtil
                  .bytesToHexString(characteristic.getValue()));
            // parseData(characteristic);
            sendBroadcast(notifyAction, HexUtil.bytesToHexString(characteristic.getValue()));
        }
```

### 断开操作

```java
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
```



### 注意事项

1. BLE 实现android 4.3 以后 google 才提供了支持，所以Ble项目只可运行在 API 18 以上的手机；Android 6.0 以上，Ble 服务还需要定位权限，并且需要动态申请，否则使用不了。不使用时，还应注意对蓝牙进行关闭或断开操作，由于 Ble 连接属于独占操作，有设备连接上了，其它设备是无法进行任何操作的。
2. 一定要进行读写开关操作，注意 descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)，否则可能读取不到数据。

### 参考 & 感谢

1. [一步一步实现Android低功耗蓝牙（BLE）基本开发](https://www.jianshu.com/p/a63dd17bd314)
2. [Android BLE低功耗蓝牙开发极简系列（一）之扫描与连接](https://www.jianshu.com/p/87ed84431ec1)
3. [Android BLE低功耗蓝牙开发极简系列（二）之读写操作](https://www.jianshu.com/p/046c1f5a7163)