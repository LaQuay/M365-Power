package laquay.M365.dashboard.device;

import android.bluetooth.BluetoothDevice;

public class Device {
    private BluetoothDevice btDevice;
    private int rssi;

    public Device(BluetoothDevice newDevice, int rssi) {
        this.btDevice = newDevice;
        this.rssi = rssi;
    }

    public BluetoothDevice getBtDevice() {
        return btDevice;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }
}
