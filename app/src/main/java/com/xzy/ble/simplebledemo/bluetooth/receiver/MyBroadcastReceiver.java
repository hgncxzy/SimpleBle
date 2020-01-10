package com.xzy.ble.simplebledemo.bluetooth.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import static com.xzy.ble.simplebledemo.bluetooth.BaseBleDevice.notifyAction;
import static com.xzy.ble.simplebledemo.bluetooth.BaseBleDevice.writeFailedAction;
import static com.xzy.ble.simplebledemo.bluetooth.BaseBleDevice.writeSuccessAction;

/**
 * @author xzy
 */
public class MyBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "MyBroadcastReceiver";
    private Update update;
    private Context mContext;

    public MyBroadcastReceiver(Context context,Update u) {
        update = u;
        mContext = context;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String data = intent.getStringExtra("data");
        if (update != null) {
            update.update(data);
        }
        Log.d(TAG, "action:" + action + ",data:" + data);
    }

    public interface Update {
        void update(String hexStr);
    }

    public void registerReceiver(MyBroadcastReceiver myBroadcastReceiver) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(writeSuccessAction);
        intentFilter.addAction(writeFailedAction);
        intentFilter.addAction(notifyAction);
        mContext.registerReceiver(myBroadcastReceiver, intentFilter);
    }
}
