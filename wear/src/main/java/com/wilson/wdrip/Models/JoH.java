package com.wilson.wdrip.Models;

import android.annotation.SuppressLint;
import android.os.IBinder;
import android.util.Log;

import com.wilson.wdrip.wDrip;

import java.lang.reflect.Method;

/**
 * Created by wzhan025 on 3/13/2018.
 */

public class JoH {
    private final static String TAG = "jamorham JoH";

    // Use system privileges to force ourselves in to the whitelisted battery optimization list
    // by reflecting in to the hidden system interface for this. I don't know a better way
    // to achieve this on android wear because it doesn't offer a user interface for it.
    @SuppressLint("PrivateApi")
    public static boolean forceBatteryWhitelisting() {
        final String myself = wDrip.getAppContext().getPackageName();
        IDeviceIdleController iDeviceIdleController;
        Method method;
        try {
            method = Class.forName("android.os.ServiceManager").getMethod("getService", String.class);
            IBinder binder = (IBinder) method.invoke(null, "deviceidle");
            if (binder != null) {
                iDeviceIdleController = IDeviceIdleController.Stub.asInterface(binder);
                Log.d(TAG, "Forcing battery optimization whitelisting for: " + myself);
                iDeviceIdleController.addPowerSaveWhitelistApp(myself);
                Log.d(TAG, "Forced battery optimization whitelisting for: " + myself);
            } else {
                Log.d(TAG, "Could not gain binder when trying to force whitelisting");
            }
            return true;
        } catch (Exception e) {
            Log.d(TAG, "Got exception trying to force whitelisting: " + e);
            return false;
        }
    }

    // emulate the system interface ourselves
    interface IDeviceIdleController extends android.os.IInterface {
        void addPowerSaveWhitelistApp(String name) throws android.os.RemoteException;

        abstract class Stub extends android.os.Binder implements IDeviceIdleController {
            private static final java.lang.String DESCRIPTOR = "android.os.IDeviceIdleController";

            public Stub() {
                this.attachInterface(this, DESCRIPTOR);
            }

            static IDeviceIdleController asInterface(android.os.IBinder obj) {
                if ((obj == null)) {
                    return null;
                }
                android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
                if (((iin != null) && (iin instanceof IDeviceIdleController))) {
                    return ((IDeviceIdleController) iin);
                }
                return new IDeviceIdleController.Stub.Proxy(obj);
            }

            @Override
            public android.os.IBinder asBinder() {
                return this;
            }

            static final int TRANSACTION_addPowerSaveWhitelistApp = android.os.IBinder.FIRST_CALL_TRANSACTION;

            @Override
            public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException {
                switch (code) {
                    case INTERFACE_TRANSACTION: {
                        reply.writeString(DESCRIPTOR);
                        return true;
                    }
                    case TRANSACTION_addPowerSaveWhitelistApp: {
                        data.enforceInterface(DESCRIPTOR);
                        java.lang.String _arg0;
                        _arg0 = data.readString();
                        this.addPowerSaveWhitelistApp(_arg0);
                        reply.writeNoException();
                        return true;
                    }
                }
                return true;
            }

            private static class Proxy implements IDeviceIdleController {
                private android.os.IBinder mRemote;

                Proxy(android.os.IBinder remote) {
                    mRemote = remote;
                }

                @Override
                public android.os.IBinder asBinder() {
                    return mRemote;
                }

                @Override
                public void addPowerSaveWhitelistApp(java.lang.String name) throws android.os.RemoteException {
                    android.os.Parcel _data = android.os.Parcel.obtain();
                    android.os.Parcel _reply = android.os.Parcel.obtain();
                    try {
                        _data.writeInterfaceToken(DESCRIPTOR);
                        _data.writeString(name);
                        mRemote.transact(Stub.TRANSACTION_addPowerSaveWhitelistApp, _data, _reply, 0);
                        _reply.readException();
                    } finally {
                        _reply.recycle();
                        _data.recycle();
                    }
                }
            }
        }
    }

}
