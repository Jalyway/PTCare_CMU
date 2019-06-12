package com.example.ptcare_cmu;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;


import java.util.Locale;
public class ScannedDeviceInfoAdapter extends ArrayAdapter<ScannedDeviceInfo> {
    private final static int RSSI_BAR_LEVELS= 5;
    private final static int RSSI_BAR_SCALE= 100 / RSSI_BAR_LEVELS;

    public ScannedDeviceInfoAdapter(Context context, int resource) {
        super(context, resource);
    }
    //
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView= LayoutInflater.from(getContext()).inflate(R.layout.blescan_entry_lay, parent, false);

            viewHolder= new ViewHolder();
            viewHolder.deviceAddress= (TextView) convertView.findViewById(R.id.ble_mac_address);
            viewHolder.deviceName= (TextView) convertView.findViewById(R.id.ble_device);
            viewHolder.deviceRSSI= (TextView) convertView.findViewById(R.id.ble_rssi_value);
            viewHolder.rssiChart= (ImageView) convertView.findViewById(R.id.ble_rssi_png);
            viewHolder.device_ck=convertView.findViewById(R.id.ck_select);

            convertView.setTag(viewHolder);
        } else {
            viewHolder= (ViewHolder) convertView.getTag();
        }

        ScannedDeviceInfo deviceInfo= getItem(position);
        final String deviceName= deviceInfo.btDevice.getName();

        if (deviceName != null && deviceName.length() > 0)
            viewHolder.deviceName.setText(deviceName);
        else
            viewHolder.deviceName.setText(R.string.label_unknown_device);
        viewHolder.deviceAddress.setText(deviceInfo.btDevice.getAddress());
        viewHolder.deviceRSSI.setText(String.format(Locale.US, "%d dBm", deviceInfo.rssi));
        viewHolder.rssiChart.setImageLevel(Math.min(RSSI_BAR_LEVELS - 1, (127 + deviceInfo.rssi + 5) / RSSI_BAR_SCALE));

        return convertView;
    }
    //--------------------------------------------
    private class ViewHolder {
        public TextView deviceAddress;
        public TextView deviceName;
        public TextView deviceRSSI;
        public ImageView rssiChart;
        public CheckBox device_ck;
    }
    //------------------------------------------
    public void update(ScannedDeviceInfo newInfo) {
        int pos= getPosition(newInfo);
        if (pos == -1) {
            add(newInfo);
        } else {
            getItem(pos).rssi= newInfo.rssi;
            notifyDataSetChanged();
        }
    }
}
