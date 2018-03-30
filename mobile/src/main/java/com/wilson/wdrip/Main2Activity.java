package com.wilson.wdrip;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.inuker.bluetooth.library.connect.listener.BleConnectStatusListener;
import com.inuker.bluetooth.library.connect.listener.BluetoothStateListener;
import com.inuker.bluetooth.library.connect.options.BleConnectOptions;
import com.inuker.bluetooth.library.connect.response.BleConnectResponse;
import com.inuker.bluetooth.library.connect.response.BleNotifyResponse;
import com.inuker.bluetooth.library.connect.response.BleWriteResponse;
import com.inuker.bluetooth.library.model.BleGattProfile;
import com.inuker.bluetooth.library.search.SearchRequest;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;
import com.inuker.bluetooth.library.utils.BluetoothLog;
import com.inuker.bluetooth.library.utils.BluetoothUtils;
import com.wilson.wdrip.data.BGData;
import com.wilson.wdrip.utils.ClientManager;
import com.wilson.wdrip.utils.CommonUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.listener.ViewportChangeListener;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.util.ChartUtils;
import lecho.lib.hellocharts.view.LineChartView;
import lecho.lib.hellocharts.view.PreviewLineChartView;

import static com.inuker.bluetooth.library.Code.REQUEST_SUCCESS;
import static com.inuker.bluetooth.library.Constants.STATUS_CONNECTED;
import static com.inuker.bluetooth.library.Constants.STATUS_DISCONNECTED;


public class Main2Activity extends Activity {
    String ACTION_DATABASE = "info.nightscout.client.DBACCESS";

    private static final String MAC = "FF:FF:FF:FF:FF:FF";

    static UUID UUID_NOTIF_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    static UUID UUID_WRITE_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    static UUID UUID_NOTIF_CHARACTER = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    static UUID UUID_WRITE_CHARACTER = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");


    private BluetoothDevice blueDevice;
    private boolean mConnected;
    private int iConnectRetry = 0;
    private String sConnectStatus = "未连接";

    private LineChartView chartView;
    private PreviewLineChartView previewView;
    private LineChartData chartData;

    private TextView txtView;
    private TextView txtCurrentBG;
    private TextView txtDirection;

    /**
     * Deep copy of data.
     */
    private LineChartData previewData;
    List<AxisValue> axisXValues = new ArrayList<AxisValue>();  // X轴 Labels & Values
    List<AxisValue> axisYValues = new ArrayList<AxisValue>();
    List<Line> lines = new ArrayList<Line>();
    List<PointValue> pValues = new ArrayList<PointValue>();
    private int lastSensorTime = 0;
    private long lastBGTime = 0;
    private double lastBG = 0;
    private List<SearchResult> mDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDevices = new ArrayList<SearchResult>();

        ignoreBatteryOptimization(this);

        txtView = (TextView) this.findViewById(R.id.textView);
        txtCurrentBG = (TextView) this.findViewById(R.id.txtCurrentBG);
        txtDirection = (TextView) this.findViewById(R.id.txtDirection);

        chartView = (LineChartView) this.findViewById(R.id.chart);
        previewView = (PreviewLineChartView) this.findViewById(R.id.chart_preview);

        generateDefaultAxis();
        generateDefaultData();

        BluetoothLog.v("fsdaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        if (!BluetoothUtils.isBluetoothEnabled()) {
            BluetoothUtils.openBluetooth();
        }

        blueDevice = BluetoothUtils.getRemoteDevice(MAC);
        ClientManager.getClient().registerConnectStatusListener(MAC, mConnectStatusListener);
        connectDeviceIfNeeded();

        ClientManager.getClient().registerBluetoothStateListener(new BluetoothStateListener() {
            @Override
            public void onBluetoothStateChanged(boolean openOrClosed) {
                BluetoothLog.v(String.format("onBluetoothStateChanged %b", openOrClosed));
            }
        });
        BluetoothLog.v("bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");

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
        double dBG = CommonUtil.formatValue(s2);

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
        previewData = new LineChartData(chartData);

        Axis axisX = new Axis();
        Axis axisY = new Axis();
        axisX.setValues(axisXValues);
        axisY.setValues(axisYValues).setInside(true);
        chartData.setAxisXBottom(axisX.setHasLines(true));
        chartData.setAxisYLeft(axisY.setHasLines(true));
        previewData.setAxisXBottom(axisX.setHasLines(true));
        previewData.setAxisYLeft(axisY.setHasLines(true));
//        if (previewData.getLines().size()>3)
//            previewData.getLines().get(3).setColor(ChartUtils.COLOR_GREEN);

        chartView.setZoomEnabled(false);
        chartView.setScrollEnabled(true);

        chartView.setLineChartData(chartData);
        previewView.setLineChartData(previewData);

        previewView.setViewportChangeListener(new ViewportListener());

        previewX(true);
    }

    private void previewX(boolean move) {
        Viewport tempViewport = new Viewport(chartView.getMaximumViewport());
        long lTime = System.currentTimeMillis();

        tempViewport.left = CommonUtil.fuzz(lTime - 60*60*1000);
        tempViewport.right = CommonUtil.fuzz(lTime + 30*60*1000);
        if (move) {
            previewView.setCurrentViewport(tempViewport);
        }
        previewView.setZoomType(ZoomType.HORIZONTAL);
    }

    /**
     * Viewport listener for preview chart(lower one). in {@link #onViewportChanged(Viewport)} method change
     * viewport of upper chart.
     */
    private class ViewportListener implements ViewportChangeListener {
        @Override
        public void onViewportChanged(Viewport newViewport) {
            // don't use animation, it is unnecessary when using preview chart.
            chartView.setCurrentViewport(newViewport);
        }
    }


    private final BleConnectStatusListener mConnectStatusListener = new BleConnectStatusListener() {
        @Override
        public void onConnectStatusChanged(String mac, int status) {
            BluetoothLog.v(String.format("DeviceDetailActivity onConnectStatusChanged %d in %s",
                    status, Thread.currentThread().getName()));
            BluetoothLog.v("-----------------------" + status );
            if (status == STATUS_CONNECTED) {
                sConnectStatus = "已连接";
                iConnectRetry = 0;
            }
            else if (status == STATUS_DISCONNECTED) {
                sConnectStatus = "未连接";
                iConnectRetry ++;
            }
            txtView.setText(sConnectStatus);
            mConnected = (status == STATUS_CONNECTED);
            connectDeviceIfNeeded();
        }
    };

    private void connectDevice() {
        txtView.setText("重新连接..."+iConnectRetry);
        BleConnectOptions options = new BleConnectOptions.Builder()
                .setConnectRetry(0)
                .setConnectTimeout(20000)
                .setServiceDiscoverRetry(0)
                .setServiceDiscoverTimeout(10000)
                .build();

        ClientManager.getClient().connect(blueDevice.getAddress(), options, new BleConnectResponse() {
            @Override
            public void onResponse(int code, BleGattProfile profile) {
                BluetoothLog.v(String.format("profile:\n%s", profile));
                if (code == REQUEST_SUCCESS) {
                    ClientManager.getClient().notify(MAC, UUID_NOTIF_SERVICE, UUID_NOTIF_CHARACTER, mNotifyRsp);
                }
            }
        });
    }

    private void connectDeviceIfNeeded() {
        if (!mConnected) {
            connectDevice();
        }
    }

    BleWriteResponse mWriteRsp = new BleWriteResponse()
    {
        public void onResponse(int paramAnonymousInt)
        {
            if (paramAnonymousInt == 0) {}
        }
    };

    private final BleNotifyResponse mNotifyRsp = new BleNotifyResponse() {
        ArrayList<Byte> bytes = new ArrayList();
        @Override
        public void onNotify(UUID service, UUID character, byte[] value) {
            BluetoothLog.v(String.format("Notify---------------------------------- : %s", Arrays.toString(value)));
            if (service.equals(UUID_NOTIF_SERVICE) && character.equals(UUID_NOTIF_CHARACTER)) {
                if (value.length > 1) {
                    if ((int)value[0] == 40)
                        this.bytes = new ArrayList();
                    for (byte b : value)
                        bytes.add(Byte.valueOf(b));
                    if ((int)value[value.length-1] == 41)
                        parseAll(bytes);
                }
            }
        }

        @Override
        public void onResponse(int code) {
            if (code == REQUEST_SUCCESS) {
                //CommonUtils.toast("success");
                BluetoothLog.v(String.format("----------------------------------Notify Response code : %s", code));
                ClientManager.getClient().write(MAC, UUID_WRITE_SERVICE, UUID_WRITE_CHARACTER, new byte[] { 48 }, mWriteRsp);
            } else {
                //CommonUtils.toast("failed");
            }
        }
    };

    @Override
    protected void onDestroy() {
        ClientManager.getClient().disconnect(blueDevice.getAddress());
        ClientManager.getClient().unregisterConnectStatusListener(blueDevice.getAddress(), mConnectStatusListener);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void Reconnect(boolean bRestart) {
        if (bRestart) {
            //ClientManager.getClient().closeBluetooth();
            //ClientManager.getClient().openBluetooth();
            ClientManager.getClient().disconnect(MAC);
            ClientManager.getClient().unregisterConnectStatusListener(blueDevice.getAddress(), mConnectStatusListener);
        } else {
            ClientManager.getClient().disconnect(MAC);
            ClientManager.getClient().unregisterConnectStatusListener(blueDevice.getAddress(), mConnectStatusListener);
        }

        blueDevice = BluetoothUtils.getRemoteDevice(MAC);
        ClientManager.getClient().registerConnectStatusListener(MAC, mConnectStatusListener);

        iConnectRetry = 0;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_reconnect) {
            //Intent intent = new Intent(this, SearchActivity.class);
            //startActivity(intent);
            txtView.setText("重新连接...");
            Reconnect(false);
            return true;
        } else if (id == R.id.action_nstest) {
            searchDevice();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
            BluetoothLog.v(String.format("%s---%s---%s---%2.1f",(i-18)/2+1,s1,s2,dBG));

            i++;
        }

        BGData tmpBGData1 = (BGData)localArrayList1.get(0);
        txtDirection.setText(String.format("%s\n%.2f\n%s", tmpBGData1.cDirection, tmpBGData1.dDirection, CommonUtil.getStringTime(tmpBGData1.lTime).substring(8,12)));
        if (tmpBGData1.iBG > 1638 || tmpBGData1.iBG < 900 || tmpBGData1.sDirection == "DoubleUp" || tmpBGData1.sDirection == "DoubleDown")
            playRingtoneDefault();

        lastSensorTime = k;
        BluetoothLog.v(sText);
        txtView.setText(sText);
        CommonUtil.saveLog(sText + "\r\n");
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
            Context context = wDrip.getInstance().getApplicationContext();
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

    private void createTestBroadast() {
        Thread testAddTreatment =  new Thread(){
            @Override
            public void run() {
                Log.d("NS","----- TEST ACTION -----");
                // Create new record with 1U of insulin and 24g of carbs
                //
                // there is a little hack because I don't receive treatments broadcasts here
                // I have to receive _id of created record via MainApp().bus()
                // So onUpdate looks for "NSCLIENTTESTRECORD" key
                try {
                    //Context context = MainApp.instance().getApplicationContext();
                    Context context = wDrip.getInstance().getApplicationContext();
                    JSONObject data = new JSONObject();
                    data.put("eventType", "Meal Bolus");
                    data.put("insulin", 1);
                    data.put("carbs", 24);
                    data.put("created_at", CommonUtil.toISOString(new Date()));
                    data.put("NSCLIENTTESTRECORD", "NSCLIENTTESTRECORD");
                    Bundle bundle = new Bundle();
                    bundle.putString("action", "dbAdd");
                    bundle.putString("collection", "treatments"); // "treatments" || "entries" || "devicestatus" || "profile" || "food"
                    bundle.putString("data", data.toString());
                    Intent intent = new Intent(ACTION_DATABASE);
                    intent.putExtras(bundle);
                    intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                    context.sendBroadcast(intent);
                    List<ResolveInfo> q = context.getPackageManager().queryBroadcastReceivers(intent, 0);
                    if (q.size() < 1) {
                        Log.e("NS","TEST DBADD No receivers");
                    } else Log.d("NS","TEST DBADD dbAdd " + q.size() + " receivers");
                } catch (JSONException e) {
                }
            }
        };
        testAddTreatment.start();
    }

    private void playRingtoneDefault(){
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ;
        Ringtone mRingtone = RingtoneManager.getRingtone(this,uri);
        mRingtone.play();
    }

    private void ShufflePlayback(){
        RingtoneManager manager = new RingtoneManager(this) ;
        Cursor cursor = manager.getCursor();
        int count = cursor.getCount() ;
        int position = (int)(Math.random()*count) ;
        Ringtone mRingtone = manager.getRingtone(position) ;
        mRingtone.play();
    }


    private void searchDevice() {
        SearchRequest request = new SearchRequest.Builder()
                .searchBluetoothLeDevice(5000, 2).build();

        ClientManager.getClient().search(request, mSearchResponse);
    }

    private final SearchResponse mSearchResponse = new SearchResponse() {
        @Override
        public void onSearchStarted() {
            BluetoothLog.w("MainActivity.onSearchStarted");

            txtView.setText("蓝牙扫描中。。。");
            mDevices.clear();
        }

        @Override
        public void onDeviceFounded(SearchResult device) {
//            BluetoothLog.w("MainActivity.onDeviceFounded " + device.device.getAddress());
            txtView.setText("发现设备： " + device.getName() + " : " + device.getAddress());
            if (device.getAddress() == MAC) {
                ClientManager.getClient().stopSearch();
            }
        }

        @Override
        public void onSearchStopped() {
            BluetoothLog.w("MainActivity.onSearchStopped");
            txtView.setText("Search Stopped");
        }

        @Override
        public void onSearchCanceled() {
            BluetoothLog.w("MainActivity.onSearchCanceled");

            txtView.setText("Search Canceled");
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        ClientManager.getClient().stopSearch();
    }
}
