package com.xzy.ble.simplebledemo;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.xzy.ble.simplebledemo.bluetooth.callback.OnDeviceConnectChangedListener;
import com.xzy.ble.simplebledemo.bluetooth.callback.OnScanCallback;
import com.xzy.ble.simplebledemo.bluetooth.callback.OnWriteCallback;
import com.xzy.ble.simplebledemo.bluetooth.device.BleDevice;
import com.xzy.ble.simplebledemo.bluetooth.utils.Permission;

import java.util.ArrayList;

import static com.xzy.ble.simplebledemo.bluetooth.device.BleDevice.MAC;

/**
 * @author xzy
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private TextView mConnectionState;
    private TextView scanResult;
    private TextView mReceiveData;

    private EditText sendCommand;
    private BleDevice bluetoothLeDeviceA;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bluetoothLeDeviceA = new BleDevice(this, new BleDevice.Update() {
            @Override
            public void update(String hexStr) {
                mReceiveData.setText(hexStr);
            }
        });
        bluetoothLeDeviceA.setConnectChangedListener(new OnDeviceConnectChangedListener() {
            @Override
            public void onConnected() {
                runOnUiThread(new Runnable() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void run() {
                        mConnectionState.setText("onConnected");
                    }
                });
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(new Runnable() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void run() {
                        mConnectionState.setText("onDisconnected");
                    }
                });
            }
        });

        BluetoothAdapter mBluetoothAdapter = bluetoothLeDeviceA.isDeviceSupport();
        if (mBluetoothAdapter == null) {
            finish();
            return;
        }

        findViewById(R.id.scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanLeDevice(true);
            }
        });
        mConnectionState = findViewById(R.id.connection_state1);
        mReceiveData = findViewById(R.id.data_value);
        scanResult = findViewById(R.id.scan_result);
        sendCommand = findViewById(R.id.send_command);
        TextView send = findViewById(R.id.send);
        // 发送数据
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mReceiveData.setText("");
                String sendContent = sendCommand.getText().toString();
                if (!TextUtils.isEmpty(sendContent)) {
                    bluetoothLeDeviceA.writeBuffer(sendContent, new OnWriteCallback() {
                        @Override
                        public void onSuccess() {
                            Log.e(TAG, "write data success");
                        }

                        @Override
                        public void onFailed(int state) {
                            Log.e(TAG, "write data failed-----" + state);
                        }
                    });
                } else {
                    Toast.makeText(MainActivity.this, "no content", Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SecondActivity.class);
                startActivity(intent);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        scanLeDevice(true);
    }


    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //bluetoothLeDeviceA.close();
    }

    ArrayList<BluetoothDevice> devices = new ArrayList<>();

    private void scanLeDevice(final boolean enable) {
        if (Permission.isGrantLocationPermission(this)) {
            startScan(enable);
        }
    }

    @SuppressLint("SetTextI18n")
    private void startScan(boolean enable) {
        // 扫描前先断开连接
        bluetoothLeDeviceA.disconnect();
        mConnectionState.setText("onDisconnected");

        bluetoothLeDeviceA.scanBleDevice(enable, new OnScanCallback() {
            @Override
            public void onFinish() {
                //这里可以用ListView将扫描到的设备展示出来，然后选择某一个进行连接
                // bluetoothLeDeviceA.connect(devices.get(0).getAddress())

                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("扫描成功：\n");
                for (BluetoothDevice device : devices) {
                    stringBuilder.append(device.getName());
                    stringBuilder.append(",");
                    stringBuilder.append(device.getAddress());
                    stringBuilder.append("\n");
                }
                scanResult.setText(stringBuilder.toString());
                bluetoothLeDeviceA.connect(MAC);
            }

            @Override
            public void onScanning(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                if (!devices.contains(device)) {
                    devices.add(device);
                    scanResult.setText("扫描中........");
                    Log.e(TAG, "onScanning: " + device.getAddress());
                    if (device.getAddress().equals(MAC)) {
                        Log.e(TAG, "找到目标设备--" + device.getAddress());
                    }
                }
            }
        });
    }
}
