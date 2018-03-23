package com.wilson.wdrip.utils;

import com.inuker.bluetooth.library.BluetoothClient;
import com.wilson.wdrip.wDrip;

/**
 * Created by wzhan025 on 3/23/2018.
 */


public class ClientManager {

    private static BluetoothClient mClient;

    public static BluetoothClient getClient() {
        if (mClient == null) {
            synchronized (ClientManager.class) {
                if (mClient == null) {
                    mClient = new BluetoothClient(wDrip.getInstance());
                }
            }
        }
        return mClient;
    }
}
