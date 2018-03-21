package com.wilson.wdrip.Services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ListenerService extends WearableListenerService implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String WEARABLE_DATA_PATH = "/nightscout_watch_data";
    private static final String WEARABLE_RESEND_PATH = "/nightscout_watch_data_resend";
    private static final String OPEN_SETTINGS = "/openwearsettings";
    private static final String NEW_STATUS_PATH = "/sendstatustowear";
    private static final String SYNC_DB_PATH = "/xdrip_plus_syncweardb";//KS
    private static final String RESET_DB_PATH = "/xdrip_plus_resetweardb";//KS
    private static final String SYNC_BGS_PATH = "/xdrip_plus_syncwearbgs";//KS
    private static final String SYNC_LOGS_PATH = "/xdrip_plus_syncwearlogs";
    private static final String SYNC_TREATMENTS_PATH = "/xdrip_plus_syncweartreatments";
    private static final String SYNC_LOGS_REQUESTED_PATH = "/xdrip_plus_syncwearlogsrequested";
    private static final String SYNC_STEP_SENSOR_PATH = "/xdrip_plus_syncwearstepsensor";
    private static final String SYNC_HEART_SENSOR_PATH = "/xdrip_plus_syncwearheartsensor";
    public static final String SYNC_ALL_DATA = "/xdrip_plus_syncalldata";//KS
    private static final String CLEAR_LOGS_PATH = "/xdrip_plus_clearwearlogs";
    private static final String CLEAR_TREATMENTS_PATH = "/xdrip_plus_clearweartreatments";
    private static final String STATUS_COLLECTOR_PATH = "/xdrip_plus_statuscollector";
    private static final String START_COLLECTOR_PATH = "/xdrip_plus_startcollector";
    private static final String WEARABLE_REPLYMSG_PATH = "/xdrip_plus_watch_data_replymsg";
    public static final String WEARABLE_INITDB_PATH = "/xdrip_plus_watch_data_initdb";
    public static final String WEARABLE_INITTREATMENTS_PATH = "/xdrip_plus_watch_data_inittreatments";
    private static final String WEARABLE_TREATMENTS_DATA_PATH = "/xdrip_plus_watch_treatments_data";//KS
    private static final String WEARABLE_BLOODTEST_DATA_PATH = "/xdrip_plus_watch_bloodtest_data";//KS
    private static final String WEARABLE_INITPREFS_PATH = "/xdrip_plus_watch_data_initprefs";
    private static final String WEARABLE_LOCALE_CHANGED_PATH = "/xdrip_plus_locale_changed_data";//KS
    private static final String WEARABLE_BG_DATA_PATH = "/xdrip_plus_watch_bg_data";//KS
    private static final String WEARABLE_CALIBRATION_DATA_PATH = "/xdrip_plus_watch_cal_data";//KS
    private static final String WEARABLE_SENSOR_DATA_PATH = "/xdrip_plus_watch_sensor_data";//KS
    private static final String WEARABLE_PREF_DATA_PATH = "/xdrip_plus_watch_pref_data";//KS
    private static final String WEARABLE_ACTIVEBTDEVICE_DATA_PATH = "/xdrip_plus_watch_activebtdevice_data";//KS
    private static final String WEARABLE_ALERTTYPE_DATA_PATH = "/xdrip_plus_watch_alerttype_data";//KS
    private static final String WEARABLE_SNOOZE_ALERT = "/xdrip_plus_snooze_payload";
    private static final String DATA_ITEM_RECEIVED_PATH = "/xdrip_plus_data-item-received";//KS
    private static final String ACTION_RESEND = "com.dexdrip.stephenblack.nightwatch.RESEND_DATA";
    private static final String ACTION_SENDDATA = "com.dexdrip.stephenblack.nightwatch.SEND_DATA";
    public static final String WEARABLE_FIELD_SENDPATH = "field_xdrip_plus_sendpath";
    public static final String WEARABLE_FIELD_PAYLOAD = "field_xdrip_plus_payload";
    public static final String WEARABLE_TREATMENT_PAYLOAD = "/xdrip_plus_treatment_payload";
    public static final String WEARABLE_TOAST_NOTIFICATON = "/xdrip_plus_toast";
    private static final String WEARABLE_TOAST_LOCAL_NOTIFICATON = "/xdrip_plus_local_toast";
    public static final String WEARABLE_G5BATTERY_PAYLOAD = "/xdrip_plus_battery_payload";
    public final static String ACTION_BLUETOOTH_COLLECTION_SERVICE_UPDATE
            = "com.eveningoutpost.dexdrip.BLUETOOTH_COLLECTION_SERVICE_UPDATE";

    // Phone
    private static final String CAPABILITY_PHONE_APP = "phone_app_sync_bgs";
    private static String localnode = "";

    private static final String TAG = "jamorham listener";
    private static SharedPreferences mPrefs;//KS
    private static boolean mLocationPermissionApproved;//KS
    private static long last_send_previous = 0;//KS
    final private static String pref_last_send_previous = "last_send_previous";
    private static long last_send_previous_log = 0;
    final private static String pref_last_send_previous_log = "last_send_previous_log";
    private static long last_send_previous_step_sensor = 0;
    private static long last_send_previous_heart_sensor = 0;
    final private static String pref_last_send_previous_step_sensor = "last_send_step_sensor";
    final private static String pref_last_send_previous_heart_sensor = "last_send_heart_sensor";
    private static long last_send_previous_treatments = 0;
    final private static String pref_last_send_previous_treatments = "last_send_previous_treatments";
    final private static int send_bg_count = 300;//288 equals full day of transmitter readings
    final private static int send_log_count = 600;//1000 records equals @160K non-compressed, 23K compressed, max transfer 2.7 seconds
    final private static int send_step_count = 600;
    final private static int send_heart_count = 600;
    final private static int send_treatments_count = 100;
    final private static int three_days_ms = 3*24*60*60*1000;
    //private static boolean doDeleteDB = false;//TODO remove once confirm not needed
    private boolean is_using_bt = false;
    private static int aggressive_backoff_timer = 120;

    private GoogleApiClient googleApiClient;
    private static long lastRequest = 0;
//    private DataRequester mDataRequester = null;
    private static final int GET_CAPABILITIES_TIMEOUT_MS = 5000;
//    private AsyncBenchmarkTester mAsyncBenchmarkTester = null;
    private static boolean bBenchmarkBgs = false;
    private static boolean bBenchmarkLogs = false;
    private static boolean bBenchmarkRandom = false;
    private static boolean bBenchmarkDup = false;
    private static boolean bInitPrefs = true;
    //Restart collector for change in the following received from phone in syncPrefData():
    private static final Set<String> restartCollectorPrefs = new HashSet<String>(Arrays.asList(
            new String[]{
                    "dex_collection_method", "share_key", "dex_txid", "use_transmiter_pl_bluetooth",
                    "use_rfduino_bluetooth", "automatically_turn_bluetooth_on", "bluetooth_excessive_wakelocks", "close_gatt_on_ble_disconnect", "bluetooth_frequent_reset", "bluetooth_watchdog"}
    ));

    //Sensor Step Counter variables
    private final static int SENS_STEP_COUNTER = android.hardware.Sensor.TYPE_STEP_COUNTER;
    //max batch latency is specified in microseconds
    private static final int BATCH_LATENCY_1s = 1000000;
    private static final int BATCH_LATENCY_400s = 400000000;
    //Steps counted in current session
    private int mSteps = 0;
    //Value of the step counter sensor when the listener was registered.
    //(Total steps are calculated from this value.)
    private int mCounterSteps = 0;
    //Steps counted by the step counter previously. Used to keep counter consistent across rotation
    //changes
    private int mPreviousCounterSteps = 0;
    private SensorManager mSensorManager;
    private static long last_movement_timestamp = 0;
    final private static String pref_last_movement_timestamp = "last_movement_timestamp";
    final private static String pref_msteps = "msteps";


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    public static void requestData(Context context) {
        Log.d(TAG, "requestData (Context context) ENTER");
        Intent intent = new Intent(context, ListenerService.class);
        intent.setAction(ACTION_RESEND);
        context.startService(intent);
    }

    // generic send data
    public static void SendData(Context context, String path, byte[] payload) {
        Intent intent = new Intent(context, ListenerService.class);
        intent.setAction(ACTION_SENDDATA);
        intent.putExtra(WEARABLE_FIELD_SENDPATH, path);
        intent.putExtra(WEARABLE_FIELD_PAYLOAD, payload);
        context.startService(intent);
    }
}
