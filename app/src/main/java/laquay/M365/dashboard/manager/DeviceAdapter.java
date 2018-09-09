package laquay.M365.dashboard.manager;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import laquay.M365.dashboard.R;

public class DeviceAdapter extends ArrayAdapter<DeviceManager> {
    private static final String PREFIX_RSSI = "RSSI:";
    private List<DeviceManager> mList;
    private LayoutInflater mInflater;
    private int mResId;

    public DeviceAdapter(Context context, int resId, List<DeviceManager> objects) {
        super(context, resId, objects);
        mResId = resId;
        mList = objects;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        DeviceManager item = getItem(position);

        if (convertView == null) {
            convertView = mInflater.inflate(mResId, null);
        }
        TextView name = convertView.findViewById(R.id.device_name);
        name.setText(item.getDisplayName());
        TextView address = convertView.findViewById(R.id.device_address);
        address.setText(item.getDevice().getAddress());
        TextView rssi = convertView.findViewById(R.id.device_rssi);
        rssi.setText(PREFIX_RSSI + Integer.toString(item.getRssi()));

        return convertView;
    }

    /**
     * add or update BluetoothDevice
     */
    public void update(BluetoothDevice newDevice, int rssi, byte[] scanRecord) {
        if ((newDevice == null) || (newDevice.getAddress() == null)) {
            return;
        }

        boolean contains = false;
        for (DeviceManager device : mList) {
            if (newDevice.getAddress().equals(device.getDevice().getAddress())) {
                contains = true;
                device.setRssi(rssi); // update
                break;
            }
        }
        if (!contains) {
            // add new BluetoothDevice
            mList.add(new DeviceManager(newDevice, rssi));
        }
        notifyDataSetChanged();
    }
}
