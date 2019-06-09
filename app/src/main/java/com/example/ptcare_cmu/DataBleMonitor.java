package com.example.ptcare_cmu;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.util.ArrayList;

public class DataBleMonitor extends AppCompatActivity {

    public static final int REQUEST_START_BLE_SCAN = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_ENABLE_LOCATION = 3;

    //private BluetoothAdapter bluetoothAdapter;
    //private LocationManager locationStatus;
    int count = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blemain);

        // 確認是否有開啟藍芽功能
        BluetoothManager bluetoothManager = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
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
            else
                displayLocationSettingsRequest(this);
        }

        // 開啟定位功能
        //displayLocationSettingsRequest(this);

//        // 確認是否有開啟定位功能
//        locationStatus = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
//        boolean gps_enabled = locationStatus.isProviderEnabled(LocationManager.GPS_PROVIDER);
//        boolean network_enabled = locationStatus.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
//        if (!gps_enabled && !network_enabled) {
//            new AlertDialog.Builder(this)
//                    .setTitle("提示訊息")
//                    .setMessage("需要開啟定位功能")
//                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),REQUEST_ENABLE_LOCATION);
//                        }
//                    })
//                    .setNegativeButton("Cancel", (dialog, which) -> {
//                        finish();
//                    })
//                    .setCancelable(false)
//                    .create()
//                    .show();
//        }


        // fab 是新增藍芽的按鈕
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> startActivityForResult(new Intent(this, ScannerActivity.class), REQUEST_START_BLE_SCAN));
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_START_BLE_SCAN:
                if (resultCode == Activity.RESULT_OK) {
                    // 獲取搜尋到的藍芽設備
                    ArrayList<BluetoothDevice> selectedDevice = data.getParcelableArrayListExtra(ScannerActivity.EXTRA_DEVICE);
                    if (selectedDevice.size() != 0) {
                        count++;
                        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.main_activity_content);
                        for (int i=0; i<selectedDevice.size(); i++){
                            ((MainActivityFragment) fragment).addNewDevice(selectedDevice.get(i));
                        }
                    }
                    if(count == 0) {
                        finish();
                    }
                }
                break;
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this,"已開啟藍芽功能",Toast.LENGTH_SHORT).show();
                    // 開啟定位功能
                    displayLocationSettingsRequest(this);
                }
                if (resultCode == Activity.RESULT_CANCELED) {
                    Toast.makeText(this,"尚未開啟藍芽功能",Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            case REQUEST_ENABLE_LOCATION:
                if (resultCode==RESULT_OK) {
                    Toast.makeText(this,"已開啟定位功能",Toast.LENGTH_SHORT).show();
                    displayLocationSettingsRequest(this);
                }
                else {
                    Toast.makeText(this,"尚未開啟定位功能",Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
    }

//    public void checkPermissionState(){
//        if((bluetoothAdapter != null || bluetoothAdapter.isEnabled())
//                &&(locationStatus.isProviderEnabled(LocationManager.GPS_PROVIDER) && locationStatus.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
//        ){
//            startActivityForResult(new Intent(DataBleMonitor.this, ScannerActivity.class), REQUEST_START_BLE_SCAN);
//        }
//    }

    //
    private void displayLocationSettingsRequest(Context context) {
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context).addApi(LocationServices.API).build();
        googleApiClient.connect();

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(10000 / 2);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // 成功時就開起搜尋裝置頁面
                        startActivityForResult(new Intent(DataBleMonitor.this, ScannerActivity.class), REQUEST_START_BLE_SCAN);

                        Log.i("Tag", "All location settings are satisfied.");
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        Log.i("Tag", "Location settings are not satisfied. Show the user a dialog to upgrade location settings ");

                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the result in onActivityResult().
                            status.startResolutionForResult(DataBleMonitor.this, REQUEST_ENABLE_LOCATION);
                        } catch (IntentSender.SendIntentException e) {
                            Log.i("Tag", "PendingIntent unable to execute request.");
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.i("Tag", "Location settings are inadequate, and cannot be fixed here. Dialog not created.");
                        break;
                }
            }
        });
    }



}
