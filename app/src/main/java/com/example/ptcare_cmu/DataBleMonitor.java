package com.example.ptcare_cmu;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import com.mbientlab.metawear.MetaWearBoard;

import java.util.UUID;

public class DataBleMonitor extends AppCompatActivity {
    private BluetoothAdapter bluetoothAdapter;

    //
    public static final int REQUEST_START_BLE_SCAN = 1;
    private static final int REQUEST_ENABLE_BT = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blemain);

        // 確認是否有開啟藍芽功能
        BluetoothManager bluetoothManager = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        // Checks if Bluetooth is supported on the device.
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Device doesn't Support Bluetooth", Toast.LENGTH_SHORT).show();
            finish();
        }
        else {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            }
        }

        // 確認是否有開啟定位功能
        LocationManager locationStatus = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = locationStatus.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean network_enabled = locationStatus.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
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
                    .setNegativeButton("Cancel", (dialog, which) -> finish())
                    .create()
                    .show();
        }

        // fab 是新增藍芽的按鈕
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> startActivityForResult(new Intent(this, ScannerActivity.class), REQUEST_START_BLE_SCAN));

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_START_BLE_SCAN:
                if (resultCode == Activity.RESULT_OK) {
                    // 获取查找到的蓝牙设备
                    BluetoothDevice selectedDevice = data.getParcelableExtra(ScannerActivity.EXTRA_DEVICE);
                    if (selectedDevice != null) {
                        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main_activity_content);
                        ((MainActivityFragment) fragment).addNewDevice(selectedDevice);
                    }
                }
                break;
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK)
                    Toast.makeText(this,"已開啟藍芽功能",Toast.LENGTH_SHORT).show();
                if (resultCode == Activity.RESULT_CANCELED)
                    Toast.makeText(this,"並未開啟藍芽功能",Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
