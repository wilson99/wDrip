package com.wilson.wdrip;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.wilson.wdrip.Services.HCollectionService;
import com.wilson.wdrip.data.BGData;
import com.wilson.wdrip.data.BgWatchData;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.util.ChartUtils;
import lecho.lib.hellocharts.view.LineChartView;

public class MainActivity extends Activity {
    private final static String TAG = MainActivity.class.getSimpleName();
    public View layoutView;

    private static final String MAC = "F2:11:7F:A6:B8:FD";
    static UUID UUID_NOTIF_SERVICE = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb");
    static UUID UUID_NOTIF_CHARACTER = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb");

    public int highColor = Color.YELLOW;
    public int lowColor = Color.RED;
    public int midColor = Color.WHITE;
    public int pointSize = 2;
    public BgGraphBuilder bgGraphBuilder;
    public LineChartView chart;
    public ArrayList<BgWatchData> bgDataList = new ArrayList<>();

    private HCollectionService mBTService;

    String ACTION_DATABASE = "info.nightscout.client.DBACCESS";

    private BluetoothDevice blueDevice;
    private boolean mConnected;
    private int iConnectRetry = 0;
    private String sConnectStatus = "未连接";

    private LineChartView chartView;
    private LineChartData chartData;

    private TextView txtView;
    private TextView txtCurrentBG;
    private TextView txtDirection;

    /**
     * Deep copy of data.
     */
    List<AxisValue> axisXValues = new ArrayList<AxisValue>();  // X轴 Labels & Values
    List<AxisValue> axisYValues = new ArrayList<AxisValue>();
    List<Line> lines = new ArrayList<Line>();
    List<PointValue> pValues = new ArrayList<PointValue>();
    private int lastSensorTime = 0;
    private long lastBGTime = 0;
    private double lastBG = 0;

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mConnectionState;
    private TextView mDataField;
    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.v(TAG, "Service Connected");
            mBTService = ((HCollectionService.LocalBinder) service).getService();
            if (!mBTService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
            }
            // Automatically connects to the device upon successful start-up initialization.
            //mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBTService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (HCollectionService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                txtView.setText("已连接");
            } else if (HCollectionService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                txtView.setText("未连接");
                //clearUI();
            } else if (HCollectionService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                //displayGattServices(mBluetoothLeService.getSupportedGattServices());
                txtView.setText("已发现服务");
            } else if (HCollectionService.ACTION_DATA_AVAILABLE.equals(action)) {
                //displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                int heartRate = intent.getIntExtra(HCollectionService.EXTRA_DATA, 0);
                txtView.setText(String.format("已获取数据:%s", heartRate));

                setBGLine(heartRate, "", System.currentTimeMillis());
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        layoutView = inflater.inflate(R.layout.activity_hchart, null);
        setContentView(R.layout.activity_main);

        ignoreBatteryOptimization(this);

        txtView = (TextView) findViewById(R.id.textView);
        txtCurrentBG = (TextView) findViewById(R.id.txtCurrentBG);
        txtDirection = (TextView) findViewById(R.id.txtDirection);

        chartView = (LineChartView) findViewById(R.id.chart);

        generateDefaultAxis();
        generateDefaultData();

        mDeviceAddress = MAC;

        Log.v(TAG, "Start bind service.....");
        Intent gattServiceIntent = new Intent(this, HCollectionService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mGattUpdateReceiver);
        unbindService(mServiceConnection);
        mBTService = null;
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(HCollectionService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(HCollectionService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(HCollectionService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(HCollectionService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    /**
     * 忽略电池优化
     */
    public void ignoreBatteryOptimization(Activity activity) {

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);

        boolean hasIgnored = powerManager.isIgnoringBatteryOptimizations(activity.getPackageName());
        //  判断当前APP是否有加入电池优化的白名单，如果没有，弹出加入电池优化的白名单的设置对话框。
        if(!hasIgnored) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse("package:"+activity.getPackageName()));
            startActivity(intent);
        }
    }

    private void generateDefaultData() {
        long lTime = CommonUtil.getLongHtime();
        long tTime = lTime;
        List<PointValue> pointValues = new ArrayList<PointValue>();
        for (int i = 0; i < 1; i++) {
            //values2.add(new PointValue((tTime%100000000) / 1000, (float) Math.random() * 100f));
            pointValues.add(new PointValue(CommonUtil.fuzz(tTime - 3*30*60*1000), 10));
            pointValues.add(new PointValue(CommonUtil.fuzz(tTime), 3));
        }
        Line line2 = new Line(pointValues);
        line2.setHasLines(false).setHasPoints(true).setPointRadius(0).setColor(ChartUtils.COLOR_GREEN);// too many values so don't draw points.
        lines.add(line2);

        refreshChart();
    }

    private void setBGLine(int s2, String sDirection, long...aTime) {
        double dBG = (double)s2;

        long lTime = System.currentTimeMillis();
        if (aTime != null)
            lTime = aTime[0];

        long tTime = lTime;
        pValues.add(new PointValue(CommonUtil.fuzz(tTime), (float) dBG));

        Line line2 = new Line(pValues);
        line2.setHasLines(false).setHasPoints(true).setPointRadius(2).setHasLabelsOnlyForSelected(true).setColor(ChartUtils.COLOR_BLUE);// too many values so don't draw points.
        if (lines.size() > 3)
            lines.remove(3);
        lines.add(line2);

        refreshChart();

        lastBGTime = lTime;
        lastBG = dBG;
    }

    private void generateDefaultAxis() {

        long lTime = CommonUtil.getLongHtime();
        long tTime = lTime - 3*30*60*1000;
        List<PointValue> pointValues = new ArrayList<PointValue>();
        for (int i = 0; i < 24; i++) {
            AxisValue aValue = new AxisValue(CommonUtil.fuzz(tTime));
            aValue.setLabel(CommonUtil.getLabelTime(tTime));
            axisXValues.add(aValue);
            pointValues.add(new PointValue(CommonUtil.fuzz(tTime), (float)9.1));

            tTime = tTime + 30*60*1000;
        }
        Line line1 = new Line(pointValues);
        line1.setHasLines(true).setHasPoints(false).setStrokeWidth(1).setColor(ChartUtils.COLOR_ORANGE);// too many values so don't draw points.
        lines.add(line1);

        tTime = lTime - 3*30*60*1000;
        pointValues = new ArrayList<PointValue>();
        for (int i = 0; i < 24; i++) {
            pointValues.add(new PointValue(CommonUtil.fuzz(tTime), (float)5));

            tTime = tTime + 30*60*1000;
        }
        Line line2 = new Line(pointValues);
        line2.setHasLines(true).setHasPoints(false).setStrokeWidth(1).setColor(ChartUtils.COLOR_RED);// too many values so don't draw points.
        lines.add(line2);

        for (int i = 3; i <= 20; i++) {
            AxisValue aValue = new AxisValue(i);
            aValue.setLabel(String.format("%s", i));
            axisYValues.add(aValue);
        }

        refreshChart();
    }

    private void refreshChart() {
        chartData = new LineChartData(lines);

        Axis axisX = new Axis();
        Axis axisY = new Axis();
        axisX.setValues(axisXValues).setHasLines(false).setInside(true);
        axisY.setValues(axisYValues).setHasLines(false).setInside(true);
        chartData.setAxisXBottom(axisX.setHasLines(false));
        chartData.setAxisYLeft(axisY.setHasLines(false));

        chartView.setZoomEnabled(false);
        chartView.setZoomType(ZoomType.HORIZONTAL);
        chartView.setScrollEnabled(false);

        chartView.setLineChartData(chartData);

        previewX(true);
    }

    private void previewX(boolean move) {
        Viewport tempViewport = new Viewport(chartView.getMaximumViewport());
        long lTime = System.currentTimeMillis();

        tempViewport.left = CommonUtil.fuzz(lTime - 20*60*1000);
        tempViewport.right = CommonUtil.fuzz(lTime + 10*60*1000);
        if (move) {
            chartView.setCurrentViewport(tempViewport);
        }
    }


    private void parseAll(ArrayList<Byte>  bytes) {
        long lTime = System.currentTimeMillis();
        int k = (((Byte)bytes.get(3)).byteValue() & 0xFF) * 256 + (((Byte)bytes.get(4)).byteValue() & 0xFF);
        int n = (((Byte)bytes.get(13).byteValue()));
        Object localObject1 = new Byte[4];
        localObject1 = CommonUtil.bytesToHexString((Byte[])bytes.subList(5, 13).toArray((Object[])localObject1));

        String currentTime = CommonUtil.getStringTime(lTime).substring(8,12);
        String sText = String.format("%s, %2.1f, %s, %s, %s", k, (20880-k)/(24.0f*60.0f), n, currentTime, localObject1);
        double currentBG = 0.00;
        double dDirection = 0.00;
        String sDirection = "NONE";
        String cDirection = "";
        ArrayList localArrayList1 = new ArrayList();
        ArrayList localArrayList2 = new ArrayList();
        long lTime15 = 0;
        int i15 = 0;

        for (int i=18; i<bytes.size()-1; i++){
            Byte[] bb = new Byte[] { ((Byte)bytes.get(i + 1)).byteValue(), ((Byte)bytes.get(i)).byteValue() };
            String s1 = CommonUtil.bytesToHexString(bb);
            int s2 = CommonUtil.getGlucoseRaw2(s1);
            Double dBG = CommonUtil.formatValue(s2);

            if (i<bytes.size()-4) {
                currentBG = dBG;

                Byte[] bbb = new Byte[]{((Byte) bytes.get(i + 3)).byteValue(), ((Byte) bytes.get(i + 2)).byteValue()};
                String ss1 = CommonUtil.bytesToHexString(bbb);
                int ss2 = CommonUtil.getGlucoseRaw2(ss1);
                double lastBG = CommonUtil.formatValue(ss2);

                dDirection = currentBG - lastBG;
            }
            else {
                dDirection = 0.00;  //暂定，需要改为减去上次的读数。
            }

            if (dDirection >= 0.2) {
                sDirection = "DoubleUp";  // add direction
                cDirection = "⇈";
                txtDirection.setText(String.format("⇈\n+%2.2f\n%s", dDirection, currentTime));
            } else if (dDirection >= 0.13) {
                sDirection = "SingleUp";  // add direction
                cDirection = "↑";
                txtDirection.setText(String.format("↑\n+%2.2f\n%s", dDirection, currentTime));
            } else if (dDirection >= 0.05) {
                sDirection = "FortyFiveUp";  // add direction
                cDirection = "↗";
                txtDirection.setText(String.format("↗\n+%2.2f\n%s", dDirection, currentTime));
            } else if (dDirection >= 0) {
                sDirection = "Flat";  // add direction
                cDirection = "→";
                txtDirection.setText(String.format("→\n+%2.2f\n%s", dDirection, currentTime));
            } else if (dDirection > -0.05) {
                sDirection = "Flat";  // add direction
                cDirection = "→";
                txtDirection.setText(String.format("→\n%2.2f\n%s", dDirection, currentTime));
            } else if (dDirection > -0.13) {
                sDirection = "FortyFiveDown";  // add direction
                cDirection = "↘";
                txtDirection.setText(String.format("↘\n%2.2f\n%s", dDirection, currentTime));
            } else if (dDirection > -0.2) {
                sDirection = "SingleDown";  // add direction
                cDirection = "↓";
                txtDirection.setText(String.format("↓\n%2.2f\n%s", dDirection, currentTime));
            } else {
                sDirection = "DoubleDown";  // add direction
                cDirection = "⇊";
                txtDirection.setText(String.format("⇊\n%2.2f\n%s", dDirection, currentTime));
            }

            if (i >=18 && i <= 36 && i <= 18 + (k - lastSensorTime)*2) {
                setBGLine(s2, sDirection, lTime - (i - 18) / 2 * 60 * 1000);
                localArrayList1.add(new BGData(s2, dDirection, sDirection, cDirection, lTime - (i - 18) / 2 * 60 * 1000));
                if (i <= 26 && (k-(i-18)/2)%5 == 0)
                    uploadNS(s2, sDirection, lTime - (i - 18) / 2 * 60 * 1000);
            }

            if (i>=18 && i <=36) {
                if (i >= 20 && (k-(i-18)/2)%15 == 0) {
                    lTime15 = lTime - (i - 20) / 2 * 60 * 1000;
                    i15 = i;
                }
            } else if (i >= 38 && lTime15 > 0) {
                //setBGLine(s2, sDirection, lTime15 - (i - 38) / 2  * 15 * 60 * 1000);
                localArrayList2.add(new BGData(s2, dDirection, sDirection, cDirection, lTime15 - (i - 38) / 2  * 15 * 60 * 1000));
            }

            sText = String.format("%s, %.2f", sText, dBG);
            Log.v("parseAll", String.format("%s---%s---%s---%2.1f",(i-18)/2+1,s1,s2,dBG));

            i++;
        }

        BGData tmpBGData1 = (BGData)localArrayList1.get(0);
        txtDirection.setText(String.format("%s\n%.2f\n%s", tmpBGData1.cDirection, tmpBGData1.dDirection, CommonUtil.getStringTime(tmpBGData1.lTime).substring(8,12)));
        if (tmpBGData1.iBG > 1638 || tmpBGData1.iBG < 900 || tmpBGData1.sDirection == "DoubleUp" || tmpBGData1.sDirection == "DoubleDown")
            playRingtoneDefault();

        lastSensorTime = k;
        Log.v("parseAll", sText);
        txtView.setText(sText);
//        CommonUtil.saveLog(sText + "\r\n");
        txtCurrentBG.setText(String.format("%.1f", CommonUtil.formatValue(tmpBGData1.iBG)));

    }


    private void uploadNS(int s2, String sDirection, long lTime) {
        Log.d("NS","----- Add BG -----");
        // Create new record with 1U of insulin and 24g of carbs
        //
        // there is a little hack because I don't receive treatments broadcasts here
        // I have to receive _id of created record via MainApp().bus()
        // So onUpdate looks for "NSCLIENTTESTRECORD" key
        try {
            //Context context = MainApp.instance().getApplicationContext();
            Context context = wDrip.getAppContext();
            JSONObject data = new JSONObject();
            data.put("date", lTime); // add long date
            data.put("dateString", CommonUtil.toISOString(new Date(lTime)));
            data.put("rssi", 100);
            data.put("device", "BGReader://libre/"); // add sensor id
            data.put("direction", sDirection);  // add direction
            data.put("rawbg", String.format("%3.1f", s2*0.1f));  // add rawbg
            data.put("sgv", String.format("%3.1f", s2*0.1f));  // add sgv
            data.put("type", "sgv");
            Bundle bundle = new Bundle();
            bundle.putString("action", "dbAdd");
            bundle.putString("collection", "entries"); // "treatments" || "entries" || "devicestatus" || "profile" || "food"
            bundle.putString("data", data.toString());
            Intent intent = new Intent(ACTION_DATABASE);
            intent.putExtras(bundle);
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            context.sendBroadcast(intent);
//                    List<ResolveInfo> q = context.getPackageManager().queryBroadcastReceivers(intent, 0);
//                    if (q.size() < 1) {
//                        Log.e("NS","TEST DBADD No receivers");
//                    } else Log.d("NS","TEST DBADD dbAdd " + q.size() + " receivers");
        } catch (JSONException e) {
        }
    }

    private void playRingtoneDefault(){
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ;
        Ringtone mRingtone = RingtoneManager.getRingtone(this,uri);
        mRingtone.play();
    }


}
