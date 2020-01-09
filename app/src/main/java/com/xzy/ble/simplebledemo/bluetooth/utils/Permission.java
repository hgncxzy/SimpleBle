package com.xzy.ble.simplebledemo.bluetooth.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

/**
 * xzy
 * 请求权限组
 */
@SuppressWarnings("all")
public class Permission {
    public static final int GROUP_LOCATION = 0x03;

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

}

