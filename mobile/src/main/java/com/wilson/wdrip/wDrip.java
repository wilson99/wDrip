package com.wilson.wdrip;

import android.app.Application;
import android.content.Context;
import android.preference.PreferenceManager;

import com.inuker.bluetooth.library.BluetoothContext;
import com.wilson.wdrip.Models.JoH;

/**
 * Created by wzhan025 on 3/13/2018.
 */

public class wDrip extends Application {
    private static final String TAG = "wDrip.java";
    private static Context context;
    private static boolean fabricInited = false;
    private static wDrip instance;

    public static Application getInstance() {
        return instance;
    }
    @Override
    public void onCreate() {
        wDrip.context = getApplicationContext();
        super.onCreate();
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, true);
        JoH.forceBatteryWhitelisting();
        instance = this;
        BluetoothContext.set(this);
    }

    public static Context getAppContext() {
        return wDrip.context;
    }

    public static boolean checkAppContext(Context context) {
        if (getAppContext() == null) {
            wDrip.context = context;
            return false;
        } else {
            return true;
        }
    }
}
