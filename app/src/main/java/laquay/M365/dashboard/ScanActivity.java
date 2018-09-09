package laquay.M365.dashboard;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

import laquay.M365.dashboard.device.ConnectionCallback;
import laquay.M365.dashboard.device.Device;
import laquay.M365.dashboard.device.DeviceAdapter;
import laquay.M365.dashboard.device.DeviceManager;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class ScanActivity extends Activity implements BluetoothAdapter.LeScanCallback, ConnectionCallback {
    private BluetoothAdapter mBTAdapter;
    private DeviceAdapter mDeviceAdapter;
    private boolean mIsScanning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        initBT();

        initVisualElements();

        ScanActivityPermissionsDispatcher.startScanWithPermissionCheck(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopScan();
    }

    @Override
    public void onLeScan(final BluetoothDevice newDevice, final int newRssi,
                         final byte[] newScanRecord) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDeviceAdapter.update(newDevice, newRssi, newScanRecord);
            }
        });
    }

    private void initBT() {
        // BLE check
        if (!DeviceManager.isBLESupported(this)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // BT check
        BluetoothManager manager = DeviceManager.getInstance(this).getBluetoothManager();
        if (manager != null) {
            mBTAdapter = manager.getAdapter();
        }
        if (mBTAdapter == null) {
            Toast.makeText(this, R.string.bt_unavailable, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (!mBTAdapter.isEnabled()) {
            Toast.makeText(this, R.string.bt_disabled, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initVisualElements() {
        // initBT listview
        ListView deviceListView = findViewById(R.id.lv_device);
        mDeviceAdapter = new DeviceAdapter(this, R.layout.listitem_device, new ArrayList<>());
        deviceListView.setAdapter(mDeviceAdapter);
        deviceListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterview, View view, int position, long id) {
                Device device = mDeviceAdapter.getItem(position);
                if (device != null) {
                    // stop before change Activity
                    stopScan();

                    Toast.makeText(getApplicationContext(), "Connecting to " + device.getBtDevice().getName(), Toast.LENGTH_SHORT).show();
                    setUpDevice(device);
                    connectToDevice();

                    BluetoothDevice btDevice = device.getBtDevice();
                    Intent intent = new Intent(view.getContext(), DeviceActivity.class);
                    //intent.putExtra(DeviceActivity.EXTRA_BLUETOOTH_DEVICE, selectedDevice);
                    intent.putExtra(DeviceActivity.EXTRAS_DEVICE_NAME, btDevice.getName());
                    intent.putExtra(DeviceActivity.EXTRAS_DEVICE_ADDRESS, btDevice.getAddress());
                    startActivity(intent);
                }
            }
        });
    }

    @NeedsPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    void startScan() {
        if ((mBTAdapter != null) && (!mIsScanning)) {
            mBTAdapter.startLeScan(this);
            mIsScanning = true;
        }
    }

    private void stopScan() {
        if (mBTAdapter != null) {
            mBTAdapter.stopLeScan(this);
        }
        mIsScanning = false;
    }

    private void setUpDevice(Device selectedDevice) {
        DeviceManager.getInstance(this).setBluetoothDevice(selectedDevice.getBtDevice(), selectedDevice.getRssi());
    }

    private void connectToDevice() {
        if (DeviceManager.getInstance(this).isConnected()) {
            DeviceManager.getInstance(this).triggerDisconnect();
        } else {
            DeviceManager.getInstance(this).triggerConnect(this);
        }
    }

    @Override
    public void onConnectionOK(String data) {
        Toast.makeText(this, "Connection OK", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionFail(String error) {
        Toast.makeText(this, "Connection Error", Toast.LENGTH_LONG).show();
    }
}
