package com.example.ptcare_cmu;

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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import com.mbientlab.metawear.MetaWearBoard;

import java.util.UUID;

public class DataBleMonitor extends AppCompatActivity {
    private BluetoothAdapter mBluetoothAdapter;
    private LocationManager status;
    //
    public static final int REQUEST_START_BLE_SCAN = 1;
    private static final int REQUEST_ENABLE_BT = 1;
    //
    private final static UUID[] mwServiceUuids = new UUID[] {
            MetaWearBoard.METAWEAR_GATT_SERVICE,
            MetaWearBoard.METABOOT_SERVICE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blemain);

        //fab 是新增藍芽的按鈕
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(view -> startActivityForResult(new Intent(DataBleMonitor.this, ScannerActivity.class), REQUEST_START_BLE_SCAN));

        //
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        BluetoothLeScanner test = mBluetoothAdapter.getBluetoothLeScanner();
        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
//            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        status = (LocationManager) (this.getSystemService(Context.LOCATION_SERVICE));
        if(!status.isProviderEnabled(LocationManager.GPS_PROVIDER)&& !status.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            Toast.makeText(DataBleMonitor.this, "請開啟定位", Toast.LENGTH_LONG).show();
            new AlertDialog.Builder(DataBleMonitor.this)
                    .setTitle("Error")
                    .setMessage("需要開啟定位")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)); //開啟設定頁面
                        }
                    })
                    .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .show();
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_START_BLE_SCAN:
                // 获取查找到的蓝牙设备
                BluetoothDevice selectedDevice= data.getParcelableExtra(ScannerActivity.EXTRA_DEVICE);
                if (selectedDevice != null) {
                    ((MainActivityFragment) getSupportFragmentManager().findFragmentById(R.id.main_activity_content)).addNewDevice(selectedDevice);
                }
                break;
        }
    }
}
