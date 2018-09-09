package laquay.M365.dashboard;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v13.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingDeque;

import laquay.M365.dashboard.component.SpecialTextView;
import laquay.M365.dashboard.device.DeviceManager;
import laquay.M365.dashboard.device.ResponseCallback;
import laquay.M365.dashboard.lib.Statistics;
import laquay.M365.dashboard.lib.requests.AmpereRequest;
import laquay.M365.dashboard.lib.requests.BatteryLifeRequest;
import laquay.M365.dashboard.lib.requests.DistanceRequest;
import laquay.M365.dashboard.lib.requests.IRequest;
import laquay.M365.dashboard.lib.requests.RequestType;
import laquay.M365.dashboard.lib.requests.SpeedRequest;
import laquay.M365.dashboard.lib.requests.SuperBatteryRequest;
import laquay.M365.dashboard.lib.requests.SuperMasterRequest;
import laquay.M365.dashboard.lib.requests.SwitchRequests.Cruise.CheckCruise;
import laquay.M365.dashboard.lib.requests.SwitchRequests.Cruise.CruiseOff;
import laquay.M365.dashboard.lib.requests.SwitchRequests.Cruise.CruiseOn;
import laquay.M365.dashboard.lib.requests.SwitchRequests.Light.CheckLight;
import laquay.M365.dashboard.lib.requests.SwitchRequests.Light.LightOff;
import laquay.M365.dashboard.lib.requests.SwitchRequests.Light.LightOn;
import laquay.M365.dashboard.lib.requests.SwitchRequests.Locking.CheckLock;
import laquay.M365.dashboard.lib.requests.SwitchRequests.Locking.LockOff;
import laquay.M365.dashboard.lib.requests.SwitchRequests.Locking.LockOn;
import laquay.M365.dashboard.lib.requests.SwitchRequests.Recovery.CheckRecovery;
import laquay.M365.dashboard.lib.requests.SwitchRequests.Recovery.MediumMode;
import laquay.M365.dashboard.lib.requests.SwitchRequests.Recovery.StrongMode;
import laquay.M365.dashboard.lib.requests.SwitchRequests.Recovery.WeakMode;
import laquay.M365.dashboard.lib.requests.VoltageRequest;
import laquay.M365.dashboard.util.Constants;
import laquay.M365.dashboard.util.HexString;
import laquay.M365.dashboard.util.LogWriter;

public class DeviceActivity extends AppCompatActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback, ResponseCallback {

    private static final String TAG = DeviceActivity.class.getSimpleName();
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private static long lastTimeStamp;
    private static double currDiff = 0L;
    private SpecialTextView voltageMeter;
    private SpecialTextView ampMeter;
    private SpecialTextView batteryLifeTextView;
    private SpecialTextView currentSpeedTextView;
    private TextView powerMeter;
    private TextView minPowerView;
    private TextView maxPowerView;
    private TextView efficiencyMeter;
    private TextView rangeMeter;
    private TextView recoveredPower;
    private TextView spentPower;
    private TextView statusTextView;
    private TextView battTemp;
    private TextView distance;
    private TextView capacity;
    private TextView averageSpeed;
    private TextView averageEfficiency;
    private TextView motorTemp;

    private Button startHandlerButton;
    private Deque<IRequest> requestQueue = new LinkedBlockingDeque<>();
    private Map<RequestType, IRequest> requestTypes = new HashMap<>();
    private List<SpecialTextView> textViews = new ArrayList<>();
    private ConcurrentSkipListSet<RequestType> checkFirst = new ConcurrentSkipListSet<>();
    private String[] lastResponse;
    private HandlerThread handlerThread;
    private HandlerThread handlerThread1;
    private Handler handler;
    private Handler handler1;
    private LogWriter logWriter = new LogWriter();
    private int lastDepth = 0;
    private boolean storagePermission = false;
    private boolean handlerStarted = false;
    private static final int PERMISSION_EXTERNAL_STORAGE = 0;
    private LinearLayout mRootView;

    private ResponseCallback responseCallback;
    private Menu menu;
    private String mDeviceName;
    private String mDeviceAddress;

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private Runnable process = new Runnable() {
        @Override
        public void run() {
            DeviceManager.getInstance(getBaseContext()).setupNotificationAndSend(responseCallback);
            try {
                Log.e(TAG, "Queue size: " + requestQueue.size());
                IRequest toSend = requestQueue.remove();
                String command = toSend.getRequestString();
                Log.d(TAG, "command:" + command);
                if (DeviceManager.getInstance(getBaseContext()).isConnected()) {
                    DeviceManager.getInstance(getBaseContext()).writeCharacteristic(command);
                    //Log.d(TAG, "Req sent: " + command);
                    if (toSend.getRequestType() != RequestType.NOCOUNT) {
                        Statistics.countRequest();
                    }
                }
            } catch (NoSuchElementException ignored) {
            } finally {
                handler1.postDelayed(this, Constants.QUEUE_DELAY);
            }
        }
    };

    private Runnable updateSuperMasterRunnable = new Runnable() {
        @Override
        public void run() {
            requestQueue.add(new SuperMasterRequest());
            handler.postDelayed(this, Constants.getSpeedDelay());
        }
    };

    private Runnable updateSuperBatteryRunnable = new Runnable() {
        @Override
        public void run() {
            requestQueue.add(new SuperBatteryRequest());
            handler.postDelayed(this, Constants.getAmpereDelay());
        }
    };

    private Runnable logsRunnable = new Runnable() {
        @Override
        public void run() {
            logWriter.writeLog(false);
            handler.postDelayed(this, Constants.getLogDelay());
        }
    };

    private Runnable runnableMeta = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Queue Size:" + requestQueue.size() + " QueueDelay:" + Constants.QUEUE_DELAY + " BaseDelay:" + Constants.BASE_DELAY);
            Log.d(TAG, "Sent:" + Statistics.getRequestsSent() + " Received:" + Statistics.getResponseReceived() + " Ratio:" + (double) Statistics.getRequestsSent() / Statistics.getResponseReceived());
            adjustTiming();

            if (DeviceManager.getInstance(getBaseContext()).isConnected()) {
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(updateSuperMasterRunnable, Constants.getSpeedDelay());
                handler.postDelayed(updateSuperBatteryRunnable, Constants.getAmpereDelay());
                if (storagePermission) {
                    handler.postDelayed(logsRunnable, 2000);
                }
                handler.postDelayed(this, 10000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.MyAppTheme);
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_device);

        Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        mRootView = findViewById(R.id.root);

        voltageMeter = findViewById(R.id.tv_voltage_meter);
        voltageMeter.setType(RequestType.VOLTAGE);
        textViews.add(voltageMeter);

        ampMeter = findViewById(R.id.tv_motor_amp);
        ampMeter.setType(RequestType.AMPERE);
        textViews.add(ampMeter);

        currentSpeedTextView = findViewById(R.id.tv_current_speed);
        currentSpeedTextView.setType(RequestType.SPEED);
        textViews.add(currentSpeedTextView);

        powerMeter = findViewById(R.id.tv_current_power);
        minPowerView = findViewById(R.id.minPowerView);
        maxPowerView = findViewById(R.id.maxPowerView);
        efficiencyMeter = findViewById(R.id.efficiencyMeter);
        rangeMeter = findViewById(R.id.rangeMeter);
        recoveredPower = findViewById(R.id.recoveredPower);
        spentPower = findViewById(R.id.spentPower);
        battTemp = findViewById(R.id.tv_battery_temp);
        distance = findViewById(R.id.tv_distance_meter);
        capacity = findViewById(R.id.tv_remaining_amps);
        averageEfficiency = findViewById(R.id.AverageEfficiencyMeter);
        averageSpeed = findViewById(R.id.averageSpeedMeter);
        motorTemp = findViewById(R.id.tv_motor_temperature);
        statusTextView = findViewById(R.id.tv_status);
        batteryLifeTextView = findViewById(R.id.tv_battery_life);
        batteryLifeTextView.setType(RequestType.BATTERYLIFE);
        textViews.add(batteryLifeTextView);

        requestTypes.put(RequestType.VOLTAGE, new VoltageRequest());
        requestTypes.put(RequestType.AMPERE, new AmpereRequest());
        requestTypes.put(RequestType.BATTERYLIFE, new BatteryLifeRequest());
        requestTypes.put(RequestType.SPEED, new SpeedRequest());
        requestTypes.put(RequestType.DISTANCE, new DistanceRequest());
        requestTypes.put(RequestType.SUPERMASTER, new SuperMasterRequest());
        requestTypes.put(RequestType.SUPERBATTERY, new SuperBatteryRequest());

        requestTypes.put(RequestType.LOCK, new CheckLock());
        requestTypes.put(RequestType.CRUISE, new CheckCruise());
        requestTypes.put(RequestType.LIGHT, new CheckLight());
        requestTypes.put(RequestType.RECOVERY, new CheckRecovery());

        lastTimeStamp = System.nanoTime();
        responseCallback = this;

        handlerThread = new HandlerThread("RequestThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        handlerThread1 = new HandlerThread("LoggingThread");
        handlerThread1.start();
        handler1 = new Handler(handlerThread1.getLooper());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestStoragePermission();
        } else {
            storagePermission = true;
        }

        Runnable connectRunnable = new Runnable() {
            @Override
            public void run() {
                if (DeviceManager.getInstance(getBaseContext()).isConnected()) {
                    handler1.post(process);
                    handler.post(runnableMeta);
                    checkFirst();
                } else {
                    handler1.postDelayed(this, Constants.BASE_DELAY);
                }
            }
        };
        connectRunnable.run();
    }

    private void updateUI(byte[] bytes) {
        if (bytes.length == 0) { //super request returns a third empty message
            return;
        }

        String[] hexString = new String[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            byte[] temp = new byte[1];
            temp[0] = bytes[i];
            hexString[i] = HexString.bytesToHex(temp);
        }

        String requestBit = hexString[5];

        if (bytes.length > 10) { //Super handling
            if (requestBit.equals(requestTypes.get(RequestType.SUPERMASTER).getRequestBit())) {
                lastResponse = hexString;
                Statistics.countRespnse();
                return;
            } else if (requestBit.equals(requestTypes.get(RequestType.SUPERBATTERY).getRequestBit())) {
                Statistics.countRespnse();
                Long now = System.nanoTime();
                double diff = now - lastTimeStamp;
                diff /= 1000000;
                currDiff = diff;

                lastTimeStamp = now;
                Log.d(TAG, "statusTextView in ms:" + diff);
                requestTypes.get(RequestType.SUPERBATTERY).handleResponse(hexString);
            } else {
                String[] combinedRespose = new String[lastResponse.length + hexString.length];
                System.arraycopy(lastResponse, 0, combinedRespose, 0, lastResponse.length);
                System.arraycopy(hexString, 0, combinedRespose, lastResponse.length, hexString.length);
                String speed = requestTypes.get(RequestType.SUPERMASTER).handleResponse(combinedRespose);
                for (SpecialTextView f : textViews) {
                    if (f.getType() == RequestType.SPEED) {
                        runOnUiThread(() -> f.setText(speed));
                    }
                }
            }
        } else {
            Statistics.countRespnse();
            for (IRequest e : requestTypes.values()) {
                if (e.getRequestBit().equals(requestBit)) {
                    String temp = e.handleResponse(hexString);
                    if (e.getRequestType() == RequestType.LOCK) {
                        MenuItem lock = menu.findItem(R.id.lock);
                        runOnUiThread(() -> lock.setChecked(Statistics.isScooterLocked()));
                        if (Statistics.isScooterLocked()) {
                            Constants.BASE_DELAY = 10000;
                        } else {
                            Constants.BASE_DELAY = 300;
                        }
                        if (checkFirst.remove(RequestType.LOCK) && !handlerStarted) {
                            requestQueue.clear(); //remove unnecessary requests
                            if (checkFirst.isEmpty() && !handlerStarted) {
                                handler1.removeCallbacksAndMessages(null);
                            }
                        }
                    } else if (e.getRequestType() == RequestType.CRUISE) {
                        MenuItem cruise = menu.findItem(R.id.cruise);
                        runOnUiThread(() -> cruise.setChecked(Statistics.isCruiseActive()));
                        if (checkFirst.remove(RequestType.CRUISE) && !handlerStarted) {
                            requestQueue.clear();
                            if (checkFirst.isEmpty() && !handlerStarted) {
                                handler1.removeCallbacksAndMessages(null);
                            }
                        }
                    } else if (e.getRequestType() == RequestType.LIGHT) {
                        MenuItem light = menu.findItem(R.id.light);
                        runOnUiThread(() -> light.setChecked(Statistics.isLightActive()));
                        if (checkFirst.remove(RequestType.LIGHT) && !handlerStarted) {
                            requestQueue.clear();
                            if (checkFirst.isEmpty() && !handlerStarted) {
                                handler1.removeCallbacksAndMessages(null);
                            }
                        }
                    } else if (e.getRequestType() == RequestType.RECOVERY) {
                        switch (temp) {
                            case "00":
                                //Log.d(TAG,"weak setting");
                                MenuItem weak = menu.findItem(R.id.weak);
                                runOnUiThread(() -> weak.setChecked(true));
                                //runOnUiThread(() -> medium.setChecked(false));
                                //runOnUiThread(() -> strong.setChecked(false));
                                break;
                            case "01":
                                //Log.d(TAG,"medium setting");
                                //runOnUiThread(() -> weak.setChecked(false));
                                MenuItem medium = menu.findItem(R.id.medium);
                                runOnUiThread(() -> medium.setChecked(true));
                                //runOnUiThread(() -> strong.setChecked(false));
                                break;
                            case "02":
                                //Log.d(TAG,"strong setting");
                                //runOnUiThread(() -> weak.setChecked(false));
                                //runOnUiThread(() -> medium.setChecked(false));
                                MenuItem strong = menu.findItem(R.id.strong);
                                runOnUiThread(() -> strong.setChecked(true));
                                break;
                        }
                        if (checkFirst.remove(RequestType.RECOVERY) && !handlerStarted) {
                            requestQueue.clear();
                            if (checkFirst.isEmpty() && !handlerStarted) {
                                handler1.removeCallbacksAndMessages(null);
                            }
                        }
                    }
                    for (SpecialTextView f : textViews) {
                        if (f.getType() == e.getRequestType()) {
                            runOnUiThread(() -> f.setText(temp));
                        }
                    }
                }
            }
        }

        //update on each response
        Thread t = new Thread() {
            public void run() {
                runOnUiThread(() -> {
                    powerMeter.setText((int) Statistics.getPower() + "W");
                    DecimalFormat df = new DecimalFormat("#.####");
                    df.setRoundingMode(RoundingMode.CEILING);
                    DecimalFormat df1 = new DecimalFormat("##.#");
                    df.setRoundingMode(RoundingMode.CEILING);
                    minPowerView.setText("min Power: " + (int) Statistics.getMinPower() + "W");
                    maxPowerView.setText("max Power: " + (int) Statistics.getMaxPower() + "W");
                    //minPowerView.setText("QueueD: " + Constants.QUEUE_DELAY + "ms");
                    //maxPowerView.setText("Req/Res: " + Statistics.getRequestsSent() + " " + Statistics.getResponseReceived());
                    efficiencyMeter.setText(Statistics.getMampHoursPerKilometer() + " mAh/Km");
                    rangeMeter.setText(Statistics.getRemainingRange() + " km ");
                    spentPower.setText("spent: " + df.format(Statistics.getSpent()) + " Ah");
                    recoveredPower.setText("recovered: " + df.format(Statistics.getRecovered()) + " Ah");
                    statusTextView.setText("Response time: " + Statistics.getCurrDiff() + " ms");
                    batteryLifeTextView.setText(Statistics.getBatteryLife() + " %");
                    currentSpeedTextView.setText(Statistics.getCurrentSpeed() + " km/h");
                    ampMeter.setText(Statistics.getCurrentAmpere() + " A");
                    voltageMeter.setText(Statistics.getCurrentVoltage() + " V");
                    battTemp.setText(Statistics.getBatteryTemperature() + " °C");
                    motorTemp.setText(Statistics.getMotorTemperature() + " °C");
                    capacity.setText(Statistics.getRemainingCapacity() + "");
                    distance.setText(df1.format(Statistics.getDistanceTravelled()) + " km");
                    averageEfficiency.setText(df1.format(Statistics.getAverageEfficiency()) + " mAh/km");
                    averageSpeed.setText(df1.format(Statistics.getAverageSpeed()) + " km/h");
                });
            }
        };
        t.start();
    }

    private void checkFirst() {
        checkFirst.clear();
        checkFirst.add(RequestType.CRUISE);
        checkFirst.add(RequestType.LOCK);
        checkFirst.add(RequestType.LIGHT);
        checkFirst.add(RequestType.RECOVERY);

        for (RequestType e : checkFirst) {
            requestQueue.addFirst(requestTypes.get(e));
        }
    }

    //Change request and queue timings
    private void adjustTiming() {
        double requests = Statistics.getRequestsSent();
        double response = Statistics.getResponseReceived();
        if ((requests / response) > 1.3) {
            Constants.QUEUE_DELAY *= 1.1;
        } else if (requests / response == 1) {
            Constants.QUEUE_DELAY *= 0.9;
        }
        int size = requestQueue.size();
        if ((requestQueue.size() > 50) && (lastDepth <= size)) {
            Constants.BASE_DELAY *= 1.1;
        } else if ((requestQueue.size() < 50) && (lastDepth >= size)) {
            Constants.BASE_DELAY *= 0.9;
        }
        if (requestQueue.size() > 100) {
            requestQueue.clear();
        }
        lastDepth = size;
        Statistics.resetRequestStats();
    }

    private void requestStoragePermission() {
        // Permission has not been granted and must be requested.
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            // Provide an additional rationale to the user if the permission was not granted
            // and the user would benefit from additional context for the use of the permission.
            // Display a SnackBar with cda button to request the missing permission.
            Snackbar.make(mRootView, R.string.permission_request,
                    Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Request the permission
                    ActivityCompat.requestPermissions(DeviceActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            PERMISSION_EXTERNAL_STORAGE);
                }
            }).show();

        } else {
            Snackbar.make(mRootView, R.string.permission_unavailable, Snackbar.LENGTH_SHORT).show();
            // Request the permission. The result will be received in onRequestPermissionResult().
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_EXTERNAL_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_EXTERNAL_STORAGE) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(mRootView, R.string.permission_granted,
                        Snackbar.LENGTH_SHORT)
                        .show();
                storagePermission = true;
            } else {
                // Permission request was denied.
                Snackbar.make(mRootView, R.string.permission_denied,
                        Snackbar.LENGTH_SHORT)
                        .show();
            }
        }
    }

    //------MENU------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // launch settings activity
            Log.d(TAG, "settings clicked");
            startActivity(new Intent(DeviceActivity.this, SettingsActivity.class));
            return true;
        } else if (id == R.id.resetStat) {
            Statistics.resetPowerStats();
            return true;
        } else if (id == R.id.connect) {
            //doConnect();
            return true;
        } else if (id == R.id.lock) {
            if (Statistics.isScooterLocked()) {
                lockOff();
                item.setChecked(false);
            } else {
                lockOn();
                item.setChecked(true);
            }
            return true;
        } else if (id == R.id.cruise) {
            if (Statistics.isCruiseActive()) {
                cruiseOff();
                item.setChecked(false);
            } else {
                cruiseOn();
                item.setChecked(true);
            }
            return true;
        } else if (id == R.id.light) {
            if (Statistics.isLightActive()) {
                lightOff();
                item.setChecked(false);
            } else {
                lightOn();
                item.setChecked(true);
            }
            return true;
        } else if (id == R.id.weak) {
            if (Statistics.getRecoveryMode() != 0) {
                setWeakMode();
            }
            return true;
        } else if (id == R.id.medium) {
            if (Statistics.getRecoveryMode() != 1) {
                setMediumMode();
            }
            return true;
        } else if (id == R.id.strong) {
            if (Statistics.getRecoveryMode() != 2) {
                setStrongMode();
            }
            return true;
        }
        //fillCheckFirstList();
        return super.onOptionsItemSelected(item);
    }

    private void checkRecovery() {
        requestQueue.add(new CheckRecovery());
    }

    private void setStrongMode() {
        requestQueue.addFirst(new StrongMode());
    }

    private void setMediumMode() {
        requestQueue.addFirst(new MediumMode());
    }

    private void setWeakMode() {
        requestQueue.addFirst(new WeakMode());
    }

    private void lightOn() {
        requestQueue.addFirst(new LightOn());
    }

    private void lightOff() {
        requestQueue.addFirst(new LightOff());
    }

    private void cruiseOn() {
        requestQueue.addFirst(new CruiseOn());
    }

    private void cruiseOff() {
        requestQueue.addFirst(new CruiseOff());
    }

    private void lockOn() {
        requestQueue.addFirst(new LockOn());
    }

    private void lockOff() {
        requestQueue.addFirst(new LockOff());
    }

    @Override
    public void onResponse(byte[] bytes) {
        updateUI(bytes);
    }
}
