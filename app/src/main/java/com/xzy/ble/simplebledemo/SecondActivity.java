package com.xzy.ble.simplebledemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.xzy.ble.simplebledemo.bluetooth.callback.OnDeviceConnectChangedListener;
import com.xzy.ble.simplebledemo.bluetooth.device.BleDevice;

/**
 * 其他页面
 *
 * @author xzy
 */
public class SecondActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
    }
}
