package com.example.ptcare_cmu;

/*
 * Revised by xi-jun on 2019/5/18 at YZU.
 */

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class BluetoothMonitor extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int MENU_SCAN = 10;
    private static final int MENU_STOP = 20;
    private static final long SCAN_PERIOD = 10*1000;

    private BluetoothAdapter bluetoothAdapter;
    private MyAdapter deviceAdapter;
    Handler handler = new Handler();

    //private Device bleDevice = new Device();
    private boolean Scanning;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth_monitor);
        //
        RecyclerView rcyView = findViewById(R.id.rcylerView);
        RecyclerView.LayoutManager rcyViewLM = new LinearLayoutManager(this);
        rcyView.setLayoutManager(rcyViewLM);


        // 確認是否有開啟藍芽功能
        BluetoothManager bluetoothManager = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        // Checks if Bluetooth is supported on the device.
        if (bluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(),"Device doesn't Support Bluetooth",Toast.LENGTH_SHORT).show();
            finish();
        }
        else {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            }
        }
//        // Android M Permission check
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            AlertDialog.Builder builder = new AlertDialog.Builder(this);
//            builder.setTitle("This app needs location access")
//                   .setMessage("Please grant location access so this app can detect beacons.")
//                   .setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                       @Override
//                       public void onClick(DialogInterface dialog, int which) {
//                           // ACCESS_FINE_LOCATION是GPS權限；ACCESS_COARSE_LOCATION是網路定位
//                           requestPermissions(
//                                   new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
//                                   PERMISSION_REQUEST_COARSE_LOCATION);
//                       }
//                   })
//                   .create()
//                   .show();
//        }

        // 確認是否有開啟定位功能
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (!gps_enabled && !network_enabled) {
            new AlertDialog.Builder(this)
                    .setTitle("提示訊息")
                    .setMessage("需要開啟定位功能")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .create()
                    .show();
        }


        deviceAdapter = new MyAdapter(AdapterLTR);
        rcyView.setAdapter(deviceAdapter);
        //deviceAdapter.notifyDataSetChanged();
    }

    MyAdapter.OnItemClickListener AdapterLTR = new MyAdapter.OnItemClickListener() {
        @Override
        public void onItemClick(BluetoothDevice bluetoothDevice) {
//            Scanning = false;
//            scanLeDevice(false);
            String address = bluetoothDevice.getAddress();
            Toast.makeText(getApplicationContext(),address,Toast.LENGTH_SHORT).show();
            //
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //menu.clear();
        if (! Scanning)
            menu.add(0, MENU_SCAN,0,"Scan").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        else
            menu.add(0, MENU_STOP,0,"Stop Scanning").setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SCAN:
                if (! bluetoothAdapter.isEnabled()) {
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                }
                else
                    Scanning = true;
                break;
            case MENU_STOP:
                Scanning = false;
                break;
        }
        scanLeDevice(Scanning);
        return super.onOptionsItemSelected(item);
    }

    private void scanLeDevice(boolean isScanning) {
        if (isScanning) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Scanning = false;
                    bluetoothAdapter.stopLeScan(scanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);
            deviceAdapter.refresh();
            deviceAdapter.notifyDataSetChanged();
            bluetoothAdapter.startLeScan(scanCallback);
        }
        else {
            handler.removeCallbacksAndMessages(null); //終止postDelayed
            bluetoothAdapter.stopLeScan(scanCallback);
        }
        invalidateOptionsMenu();
    }

    // Bluetooth Device scan callback
    BluetoothAdapter.LeScanCallback scanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    deviceAdapter.addDevice(device, rssi);
                    deviceAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //
        switch (resultCode) {
            case Activity.RESULT_OK:
                Toast.makeText(this,"已開啟藍芽功能",Toast.LENGTH_SHORT).show();
                break;
            case Activity.RESULT_CANCELED:
                Toast.makeText(this,"並未開啟藍芽功能",Toast.LENGTH_SHORT).show();
                break;
        }
    }

//    ////
//    public class Device {
//        private List<BluetoothDevice> itemDevices;
//        private List<Integer> itemRSSIs;
//
//        public Device() {
//            itemDevices = new ArrayList<>();
//            itemRSSIs = new ArrayList<>();
//        }
//
//        public void addDevice(BluetoothDevice device, int rssi) {
//            if (! itemDevices.contains(device)) {
//                itemDevices.add(device);
//                itemRSSIs.add(rssi);
//            }
//        }
//
//        public void refresh() {
//            itemDevices.clear();
//            itemRSSIs.clear();
//        }
//
//        public List<BluetoothDevice> getItemDevices() {
//            return itemDevices;
//        }
//
//        public List<Integer> getItemRSSIs() {
//            return itemRSSIs;
//        }
//    }


    @Override
    protected void onResume() {
        super.onResume();
        //
        if (! bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
        Scanning = true;
        scanLeDevice(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //
        Scanning = false;
        scanLeDevice(false);
    }
}
