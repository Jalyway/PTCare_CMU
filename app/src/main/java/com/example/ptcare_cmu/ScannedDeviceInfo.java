package com.example.ptcare_cmu;

import android.bluetooth.BluetoothDevice;

public class ScannedDeviceInfo {
    public final BluetoothDevice btDevice;
    public int rssi;

    public ScannedDeviceInfo(BluetoothDevice btDevice, int rssi) {
        this.btDevice= btDevice;
        this.rssi= rssi;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj == this) ||
                ((obj instanceof ScannedDeviceInfo) && btDevice.equals(((ScannedDeviceInfo) obj).btDevice));
    }
}
