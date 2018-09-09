package laquay.M365.dashboard.device;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import laquay.M365.dashboard.util.Constants;
import laquay.M365.dashboard.util.HexString;

public class DeviceManager {
    private static final String TAG = DeviceManager.class.getSimpleName();
    private static final String UNKNOWN = "Unknown";
    private static DeviceManager instance;
    private BluetoothDevice mDevice;
    private BluetoothManager bluetoothManager;
    private String mDisplayName;
    private RxBleClient rxBleClient;
    private Observable<RxBleConnection> connectionObservable;
    private Disposable connectionDisposable;
    private RxBleConnection connection;
    private RxBleDevice bleDevice;
    private Context context;
    private String mDeviceName;
    private String mDeviceAddress;
    private int mDeviceRssi;

    public DeviceManager(Context context) {
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        rxBleClient = RxBleClient.create(context);

        this.context = context;
    }

    public static DeviceManager getInstance(Context ctx) {
        if (instance == null) {
            createInstance(ctx);
        }
        return instance;
    }

    private synchronized static void createInstance(Context ctx) {
        if (instance == null) {
            instance = new DeviceManager(ctx);
        }
    }

    /**
     * check if BLE Supported device
     */
    public static boolean isBLESupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    public void setBluetoothDevice(BluetoothDevice device, int rssi) {
        if (device == null) {
            throw new IllegalArgumentException("BluetoothDevice is null");
        }
        mDevice = device;
        mDisplayName = device.getName();
        if ((mDisplayName == null) || (mDisplayName.length() == 0)) {
            mDisplayName = UNKNOWN;
        }
        mDeviceAddress = device.getAddress();
        mDeviceRssi = rssi;

        setBTConnection();
    }

    private void setBTConnection() {
        bleDevice = rxBleClient.getBleDevice(mDeviceAddress);
        connectionObservable = prepareConnectionObservable();
    }

    private Observable<RxBleConnection> prepareConnectionObservable() {
        return bleDevice.establishConnection(false);
    }

    public boolean isConnected() {
        return bleDevice.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTED;
    }

    public void triggerConnect(ConnectionCallback connectionCallback) {
        connectionDisposable = bleDevice.establishConnection(false)
                //.compose(bindUntilEvent(PAUSE))
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(this::dispose)
                .doOnError(throwable -> {
                    dispose();
                    Toast.makeText(context, "Could not connect to scooter, please retry", Toast.LENGTH_LONG).show();
                })
                .subscribe(rxBleConnection -> {
                    // All GATT operations are done through the rxBleConnection.
                    connectionCallback.onConnectionOK("");
                    onConnectionReceived(rxBleConnection);
                }, throwable -> {
                    connectionCallback.onConnectionFail("");
                    onConnectionFailure(throwable);
                });
    }

    public void triggerDisconnect() {
        if (connectionDisposable != null) {
            connectionDisposable.dispose();
        }
    }

    private void dispose() {
        connectionDisposable = null;
    }

    private void onConnectionReceived(RxBleConnection connection) {
        this.connection = connection;
    }

    private void onConnectionFailure(Throwable throwable) {
        Log.d(TAG, "connection fail: " + throwable.getMessage());
        Toast.makeText(context, "Could not connect to scooter, please retry", Toast.LENGTH_LONG).show();
    }

    public void writeCharacteristic(String command) {
        connection.writeCharacteristic(UUID.fromString(Constants.CHAR_WRITE), HexString.hexToBytes(command)).subscribe();
    }

    @SuppressLint("CheckResult")
    public void setupNotificationAndSend(ResponseCallback responseCallback) {
        connection.setupNotification(UUID.fromString(Constants.CHAR_READ))
                .doOnNext(notificationObservable -> {
                })
                .flatMap(notificationObservable -> notificationObservable) // <-- Notification has been set up, now observe value changes.
                .timeout(200, TimeUnit.MILLISECONDS)
                .onErrorResumeNext(Observable.empty())
                .subscribe(
                        bytes -> responseCallback.onResponse(bytes)
                );
    }

    public BluetoothManager getBluetoothManager() {
        return bluetoothManager;
    }

    public BluetoothDevice getDevice() {
        return mDevice;
    }
}
