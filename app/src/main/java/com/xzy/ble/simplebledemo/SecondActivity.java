package com.xzy.ble.simplebledemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.xzy.ble.simplebledemo.bluetooth.callback.OnWriteCallback;
import com.xzy.ble.simplebledemo.bluetooth.device.BleDevice;
import com.xzy.ble.simplebledemo.bluetooth.receiver.MyBroadcastReceiver;

/**
 * 其他页面
 *
 * @author xzy
 */
public class SecondActivity extends AppCompatActivity implements MyBroadcastReceiver.Update {
    private static final String TAG = "SecondActivity";
    private MyBroadcastReceiver myBroadcastReceiver = new MyBroadcastReceiver(this, this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        myBroadcastReceiver.registerReceiver(myBroadcastReceiver);
        findViewById(R.id.click).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BleDevice.getInstance(getApplicationContext()).writeBuffer("48429002000199", new OnWriteCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "发送成功");
                    }

                    @Override
                    public void onFailed(int state) {
                        Log.d(TAG, "发送失败:" + state);
                    }
                });
            }
        });
    }

    @Override
    public void update(String hexStr) {
        Toast.makeText(this, hexStr, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(myBroadcastReceiver);
    }
}
