//老師有修改過 把顯示資料 原本官方範例的方向改為"加速度"
/*
 * Copyright 2015 MbientLab Inc. All rights reserved.
 *
 * IMPORTANT: Your use of this Software is limited to those specific rights
 * granted under the terms of a software license agreement between the user who
 * downloaded the software, his/her employer (which must be your employer) and
 * MbientLab Inc, (the "License").  You may not use this Software unless you
 * agree to abide by the terms of the License which can be found at
 * www.mbientlab.com/terms . The License limits your use, and you acknowledge,
 * that the  Software may not be modified, copied or distributed and can be used
 * solely and exclusively in conjunction with a MbientLab Inc, product.  Other
 * than for the foregoing purpose, you may not use, reproduce, copy, prepare
 * derivative works of, modify, distribute, perform, display or sell this
 * Software and/or its documentation for any purpose.
 *
 * YOU FURTHER ACKNOWLEDGE AND AGREE THAT THE SOFTWARE AND DOCUMENTATION ARE
 * PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED,
 * INCLUDING WITHOUT LIMITATION, ANY WARRANTY OF MERCHANTABILITY, TITLE,
 * NON-INFRINGEMENT AND FITNESS FOR A PARTICULAR PURPOSE. IN NO EVENT SHALL
 * MBIENTLAB OR ITS LICENSORS BE LIABLE OR OBLIGATED UNDER CONTRACT, NEGLIGENCE,
 * STRICT LIABILITY, CONTRIBUTION, BREACH OF WARRANTY, OR OTHER LEGAL EQUITABLE
 * THEORY ANY DIRECT OR INDIRECT DAMAGES OR EXPENSES INCLUDING BUT NOT LIMITED
 * TO ANY INCIDENTAL, SPECIAL, INDIRECT, PUNITIVE OR CONSEQUENTIAL DAMAGES, LOST
 * PROFITS OR LOST DATA, COST OF PROCUREMENT OF SUBSTITUTE GOODS, TECHNOLOGY,
 * SERVICES, OR ANY CLAIMS BY THIRD PARTIES (INCLUDING BUT NOT LIMITED TO ANY
 * DEFENSE THEREOF), OR OTHER SIMILAR COSTS.
 *
 * Should you have any questions regarding your right to use this Software,
 * contact MbientLab Inc, at www.mbientlab.com.
 */

package com.example.ptcare_cmu;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.example.ptcare_cmu.db.DBHelper;
import com.mbientlab.metawear.AsyncDataProducer;
import com.mbientlab.metawear.CodeBlock;
import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.DeviceInformation;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.data.AngularVelocity;
import com.mbientlab.metawear.data.MagneticField;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.AccelerometerBosch;
import com.mbientlab.metawear.module.AccelerometerMma8452q;
import com.mbientlab.metawear.module.AmbientLightLtr329;
import com.mbientlab.metawear.module.BarometerBosch;
import com.mbientlab.metawear.module.ColorTcs34725;
import com.mbientlab.metawear.module.Debug;
import com.mbientlab.metawear.module.GyroBmi160;
import com.mbientlab.metawear.module.HumidityBme280;
import com.mbientlab.metawear.module.MagnetometerBmm150;
import com.mbientlab.metawear.module.ProximityTsl2671;
import com.mbientlab.metawear.module.Switch;
import com.mbientlab.metawear.module.Temperature;
import com.mbientlab.metawear.module.Timer;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimerTask;

import bolts.Capture;
import bolts.Continuation;
import bolts.Task;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment implements ServiceConnection {
    private final HashMap<DeviceState, MetaWearBoard> stateToBoards;
    private BtleService.LocalBinder binder;
    private GyroBmi160 gyroBmi160;
    private Accelerometer accelerometer;
    public Handler mHandler;
    private DeviceState newDeviceState;
    public int tsec=0;
    private String na=null;
    private Boolean isSDPresent = false;
    private DBHelper dbhelper = null;
    private SQLiteDatabase sdb;
    private String mwMacAddress;
    private boolean isSampling=false;

    private final static int HANDLER_DEVICE_CONNECTED = 3;
    private final static int HANDLER_SHOW_ERROR = 4;
    private final static int HANDLER_UPDATE_ACCELEROMETER_RESULT = 100;
    private final static int HANDLER_APP_CONFIGURED = 2;
    private final static int HANDLER_UPDATE_GYRO_RESULT = 105;
    private ConnectedDevicesAdapter connectedDevices= null;

    public MainActivityFragment() {
        stateToBoards = new HashMap<>();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler(new handlerUpdate());

        dbhelper = new DBHelper(getContext());
        dbhelper.openDatabase();

        Activity owner= getActivity();
        owner.getApplicationContext().bindService(new Intent(owner, BtleService.class), this, Context.BIND_AUTO_CREATE);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        getActivity().getApplicationContext().unbindService(this);
    }


    public void addNewDevice(BluetoothDevice btDevice) {
        newDeviceState= new DeviceState(btDevice);
        final MetaWearBoard newBoard= binder.getMetaWearBoard(btDevice);

        mwMacAddress = btDevice.getAddress();
        newDeviceState.connecting= true;
        connectedDevices.add(newDeviceState);
        stateToBoards.put(newDeviceState, newBoard);

        final Capture<AsyncDataProducer> orientCapture = new Capture<>();
        final Capture<Accelerometer> accelCapture = new Capture<>();

        newBoard.onUnexpectedDisconnect(status -> getActivity().runOnUiThread(() -> connectedDevices.remove(newDeviceState)));
        newBoard.connectAsync().onSuccessTask(task -> {
            MainActivityFragment.this.getActivity().runOnUiThread(() -> {
                newDeviceState.connecting = false;
                connectedDevices.notifyDataSetChanged();
            });
            accelerometer = newBoard.getModule(Accelerometer.class);
            accelCapture.set(accelerometer);

            final AsyncDataProducer orientation;
            orientation = accelerometer.acceleration();
            orientCapture.set(orientation);


            return orientation.addRouteAsync(source -> source.stream((data, env) -> {
                MainActivityFragment.this.getActivity().runOnUiThread(() -> {
                    Log.w("WWW", "xxx");
                    newDeviceState.deviceAcceleration = data.value(Acceleration.class).toString();
                    String[] angularResult=MainActivityFragment.this.CalculateAngles(data.value(Acceleration.class));
                    newDeviceState.deviceAngle = angularResult[0];
                    Log.w("WWW", newDeviceState.deviceAcceleration);
                    connectedDevices.notifyDataSetChanged();
                    //---------------------------------------------------------------
                    String accel_entry = String.format("%.6f", data.value(Acceleration.class).x()) + "acc," +
                            String.format("%.6f", data.value(Acceleration.class).y())  + "acc," +
                            String.format("%.6f", data.value(Acceleration.class).z()) +"acc";
//                    if (isSampling) {
////                        OutputStream out;
//                        try {
//                            File fout = new File(na);
//                            FileOutputStream fos = new FileOutputStream(fout,true);
//                            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(fos));
//                            out.write(accel_entry);
//                            out.close();
////
////                            out = new BufferedOutputStream(new FileOutputStream(na, true));
////                            out.write(accel_entry.getBytes());
////                            out.write("\n".getBytes());
////                            out.close();
//                        } catch (Exception e) {
//                            Log.e("r", "CSV creation error", e);
//                        }
//                    }
//                    //---------------------------------------------------------------
//                    if (isSampling) {
////                        OutputStream out;
//                        try {
//                            File fout = new File(na);
//                            FileOutputStream fos = new FileOutputStream(fout,true);
//                            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(fos));
//                            out.write(angularResult[1]);
//                            out.close();
////
////                            out = new BufferedOutputStream(new FileOutputStream(na, true));
////                            out.write(angularResult[1].getBytes());
////                            out.write("\n".getBytes());
////                            out.close();
//                        } catch (Exception e) {
//                            Log.e("r", "CSV creation error", e);
//                        }
//                    }
                });
            }));
        }).onSuccessTask(task -> newBoard.getModule(Switch.class).state().addRouteAsync(source -> source.stream((Subscriber) (data, env) -> {
            getActivity().runOnUiThread(() -> {
                newDeviceState.pressed = data.value(Boolean.class);
                connectedDevices.notifyDataSetChanged();
            });
        }))).continueWith((Continuation<Route, Void>) task -> {
            if (task.isFaulted()) {
                if (!newBoard.isConnected()) {
                    getActivity().runOnUiThread(() -> connectedDevices.remove(newDeviceState));
                } else {
                    Snackbar.make(getActivity().findViewById(R.id.activity_main_layout), task.getError().getLocalizedMessage(), Snackbar.LENGTH_SHORT).show();
                    newBoard.tearDown();
                    newBoard.disconnectAsync().continueWith((Continuation<Void, Void>) task1 -> {
                        connectedDevices.remove(newDeviceState);
                        return null;
                    });
                }
            } else {
                orientCapture.get().stop();
                accelCapture.get().stop();
            }
            return null;

        });
        //--------------------------------------------------------------------------------------------
        newBoard.connectAsync().onSuccessTask(task -> {
            getActivity().runOnUiThread(() -> {
                newDeviceState.connecting= false;
                connectedDevices.notifyDataSetChanged();
            });
            //--------------------------------
            gyroBmi160 = newBoard.getModule(GyroBmi160.class);
            gyroBmi160.angularVelocity().stop();
            gyroBmi160.stop();
            gyroBmi160.configure()
                    .odr(GyroBmi160.OutputDataRate.ODR_25_HZ)
                    .range(GyroBmi160.Range.FSR_2000)
                    .commit();
            return gyroBmi160.angularVelocity().addRouteAsync(source -> source.stream((data, env) -> {
                getActivity().runOnUiThread(() -> {
                    newDeviceState.deviceGYRO = data.value(AngularVelocity.class).toString();
                    connectedDevices.notifyDataSetChanged();
                    //-------------------------------------------------------------------------------------------------------------------
                    AngularVelocity dataAngular = (AngularVelocity) data.value(AngularVelocity.class);
                    final double Deg2Rad = Math.PI / 180.0;

                    String angular_entry =   ","+String.format("%.6f", dataAngular.x() * Deg2Rad) + "agacc," +
                            String.format("%.6f", dataAngular.y() * Deg2Rad) + "agacc," +
                            String.format("%.6f", dataAngular.z() * Deg2Rad)+"agacc";
                    // String csv_accel_entry = accel_entry ;
//                    if (isSampling) {
////                        OutputStream out;
//                        try {
//                            File fout = new File(na);
//                            FileOutputStream fos = new FileOutputStream(fout,true);
//                            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(fos));
//                            out.write(angular_entry);
//                            out.newLine();
//                            out.close();
////                            out = new BufferedOutputStream(new FileOutputStream(na, true));
////                            out.write(angular_entry.getBytes());
////                               out.write("\n".getBytes());
////                            out.close();
//                        } catch (Exception e) {
//                            Log.e("r", "CSV creation error", e);
//                        }
//                    }
                });
            }));
        }).onSuccessTask(task -> newBoard.getModule(Switch.class).state().addRouteAsync(source -> source.stream((Subscriber) (data, env) -> {
            getActivity().runOnUiThread(() -> {
                newDeviceState.pressed = data.value(Boolean.class);
                connectedDevices.notifyDataSetChanged();
            });
        }))).continueWith((Continuation<Route, Void>) task -> {
            if (task.isFaulted()) {
                if (!newBoard.isConnected()) {
                    getActivity().runOnUiThread(() -> connectedDevices.remove(newDeviceState));
                } else {
                    Snackbar.make(getActivity().findViewById(R.id.activity_main_layout), task.getError().getLocalizedMessage(), Snackbar.LENGTH_SHORT).show();
                    newBoard.tearDown();
                    newBoard.disconnectAsync().continueWith((Continuation<Void, Void>) task1 -> {
                        connectedDevices.remove(newDeviceState);
                        return null;
                    });
                }
            } else {

            }
            return null;
        });
        //--------------------------------------------------------------------------------------------------------------------------------
        newBoard.connectAsync().onSuccessTask(new Continuation<Void, Task<Route>>() {
            @Override
            public Task<Route> then(Task<Void> task) throws Exception {
                if (task.isFaulted()) {
                    Log.i("mwHome", "Fail to connect the board");
                    mHandler.obtainMessage(HANDLER_SHOW_ERROR, "Fail to connect the board").sendToTarget();
                } else {
                    Log.i("mwHome", "Connect to " + mwMacAddress);
                    mHandler.obtainMessage(HANDLER_DEVICE_CONNECTED).sendToTarget();
                }

                if (accelerometer != null) {
//                    listSensors.add("Accelerometer");
                    accelerometer.configure()
                            .odr(25f)       // Set sampling frequency to 25Hz, or closest valid ODR
                            .range(4f)      // Set data range to +/-4g, or closet valid range
                            .commit();
                    accelerometer.acceleration().addRouteAsync(new RouteBuilder() {
                        @Override
                        public void configure(RouteComponent source) {
                            source.stream(new Subscriber() {
                                @Override
                                public void apply(Data data, Object... env) {
                                    Log.i("mwHome", "Accelerometer = " + data.value(Acceleration.class).toString());
                                    mHandler.obtainMessage(HANDLER_UPDATE_ACCELEROMETER_RESULT, data.value(Acceleration.class)).sendToTarget();
                                }
                            });
                        }
                    });
                }
                // Setup Gyro
                    gyroBmi160.angularVelocity().addRouteAsync(new RouteBuilder() {
                        @Override
                        public void configure(RouteComponent source) {
                            source.stream(new Subscriber() {
                                @Override
                                public void apply(Data data, Object ... env) {
                                    Log.i("mwHome", data.value(AngularVelocity.class).toString());
                                    mHandler.obtainMessage(HANDLER_UPDATE_GYRO_RESULT, data.value(AngularVelocity.class)).sendToTarget();
                                }
                            });
                        }
                    });
                return null;
            }
        }).continueWith(new Continuation<Route, Void>() {
            @Override
            public Void then(Task<Route> task) throws Exception {
                if (task.isFaulted()) {
                    Log.i("mwHome", "Fail to configure app", task.getError());
                    mHandler.obtainMessage(HANDLER_SHOW_ERROR, "Fail to configure app!").sendToTarget();
                } else {
                    Log.i("mwHome", "App configure");
                    mHandler.obtainMessage(HANDLER_APP_CONFIGURED).sendToTarget();
                }
                return null;
            }
        });
        //--------------------------------------------------------------------------------------------------------------------------------
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        connectedDevices= new ConnectedDevicesAdapter(getActivity(), R.id.metawear_status_layout);
        connectedDevices.setNotifyOnChange(true);
        setRetainInstance(true);
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ListView connectedDevicesView= (ListView) view.findViewById(R.id.connected_devices);
        connectedDevicesView.setAdapter(connectedDevices);
        connectedDevicesView.setOnItemLongClickListener((parent, view1, position, id) -> {
            DeviceState current= connectedDevices.getItem(position);
            final MetaWearBoard selectedBoard= stateToBoards.get(current);

            Accelerometer accelerometer = selectedBoard.getModule(Accelerometer.class);
            accelerometer.stop();
            if (accelerometer instanceof AccelerometerBosch) {
                //((AccelerometerBosch) accelerometer).orientation().stop();
                ((AccelerometerBosch) accelerometer).acceleration().stop();
            } else {
                //((AccelerometerMma8452q) accelerometer).orientation().stop();
                ((AccelerometerMma8452q) accelerometer).acceleration().stop();
            }

            selectedBoard.tearDown();
            selectedBoard.getModule(Debug.class).disconnectAsync();

            connectedDevices.remove(current);
            return false;
        });
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        binder= (BtleService.LocalBinder) service;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }
    private String[] CalculateAngles(Acceleration AccelerationData)
    {
        double Denominator = Math.sqrt(Math.pow(AccelerationData.x(), 2) + Math.pow(AccelerationData.y(), 2) + Math.pow(AccelerationData.z(), 2));

        double AngleX = CalculateAngle(AccelerationData.x(), Denominator);
        double AngleY = CalculateAngle(AccelerationData.y(), Denominator);
        double AngleZ = CalculateAngle(AccelerationData.z(), Denominator);
        String ans="(x:\t"+ String.format("%.3f", AngleX) + "g, " +
                "y:\t" + String.format("%.3f", AngleY) + "g, " +
                "z:\t" + String.format("%.3f", AngleZ) + "g)";
        //----------------------------------------------------------------------------------------------------------
        String accel_entry=","+AngleX+","+AngleY+","+AngleZ;
        return new String[]{ans,accel_entry};
    } // CalculateAngles
    //-------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    private void CalculateAngle(Acceleration AccelerationData)
    {
        double Denominator = Math.sqrt(Math.pow(AccelerationData.x(), 2) + Math.pow(AccelerationData.y(), 2) + Math.pow(AccelerationData.z(), 2));

        double AngleX = CalculateAngle(AccelerationData.x(), Denominator);
        double AngleY = CalculateAngle(AccelerationData.y(), Denominator);
        double AngleZ = CalculateAngle(AccelerationData.z(), Denominator);
        String accel_entry=","+AngleX+","+AngleY+","+AngleZ;

        OutputStream out;
        try {
            out = new BufferedOutputStream(new FileOutputStream(na, true));
            out.write(accel_entry.getBytes());
            out.write("\n".getBytes());
            out.close();
        } catch (Exception e) {
            Log.e("r", "CSV creation error", e);
        }

    }
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////-----------------------CalculateAngle
    private double CalculateAngle(float Value, double Denominator)      //使用弧度計算角度
    {
        double Radian = Math.acos(Value / Denominator);
        return ((Radian * (double)180) / Math.PI);
    } // end CalculateAngleByRadian
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public void handleStartSampling() {

        accelerometer.acceleration().start();
        accelerometer.start();

        gyroBmi160.angularVelocity().start();
        gyroBmi160.start();

        tsec=0;
        mHandler.postDelayed(runnable,1000); // 開始Timer

//        magnetometer.magneticField().start();
//        magnetometer.start();

        isSampling = true;
    }

    public void handleStopSampling() {

        accelerometer.acceleration().stop();
        accelerometer.stop();

        gyroBmi160.angularVelocity().stop();
        gyroBmi160.stop();

        mHandler.removeCallbacks(runnable); //停止Timer
//        magnetometer.magneticField().stop();
//        magnetometer.stop();
        isSampling = false;
    }
    public Runnable runnable = new Runnable() {
        public void run ( ) {
            //	update( );
            tsec++;
            mHandler.postDelayed(this,1000);

            newDeviceState.deviceTime=Integer.toString(tsec);

        }
    };
    //-------------------------------------------------------------------------------------------------------
    class handlerUpdate implements Handler.Callback {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case HANDLER_DEVICE_CONNECTED:

                    return false;
                case HANDLER_SHOW_ERROR:

                    return false;
                // Sensors
                case HANDLER_UPDATE_ACCELEROMETER_RESULT:
                    Log.i("handleMessage", "HANDLER_UPDATE_ACCELEROMETER_RESULT -> (Data) msg.obj = " + (Acceleration) msg.obj);
                    if (isSampling) {
                        Acceleration data = ((Acceleration) msg.obj);
                        CalculateAngle(data);

                        String accel_entry = String.format("%.6f", data.x()) + "," +
                                String.format("%.6f", data.y()) + "," +
                                String.format("%.6f", data.z());

                        OutputStream out;
                        try {
                            out = new BufferedOutputStream(new FileOutputStream(na, true));
                            out.write(accel_entry.getBytes());
                            //   out.write("\n".getBytes());
                            out.close();
                        } catch (Exception e) {
                            Log.e("r", "CSV creation error", e);
                        }

                    }

                    return false;
                case HANDLER_UPDATE_GYRO_RESULT:
                    Log.i("handleMessage", "HANDLER_UPDATE_GYRO_RESULT -> (AngularVelocity) msg.obj = " + (AngularVelocity) msg.obj);
                    if (isSampling) {
                        AngularVelocity data = (AngularVelocity) msg.obj;
                        final double Deg2Rad = Math.PI / 180.0;

                        String accel_entry = "," + String.format("%.6f", data.x() * Deg2Rad) + "," +
                                String.format("%.6f", data.y() * Deg2Rad) + "," +
                                String.format("%.6f", data.z() * Deg2Rad);
                        // String csv_accel_entry = accel_entry ;
                        OutputStream out;
                        try {
                            out = new BufferedOutputStream(new FileOutputStream(na, true));
                            out.write(accel_entry.getBytes());
                            //   out.write("\n".getBytes());
                            out.close();
                        } catch (Exception e) {
                            Log.e("r", "CSV creation error", e);
                        }

                    }
                    return false;
            }
            return false;
        }
    }
    //-----------------------------------------------------------------------------------------------------
    public void createSysDir()
    {
        File[] dirs = ContextCompat.getExternalFilesDirs(getContext(), null);
        Log.e("Kenny", String.valueOf(dirs[0]));
        File removable = null;

        if (dirs.length > 0) {
            removable= dirs[0];
        }

        if (removable.exists() && removable.canRead() && removable.canWrite()) {

            SimpleDateFormat formatter1 = new SimpleDateFormat("YYYYMMddHHmm ");
            Date curDate = new Date(System.currentTimeMillis());//獲取當前時間

            String cur=formatter1.format(curDate);
            Log.e("Kenny", removable.toString());
            File test = new File(removable,cur.trim()+".txt");
            try {
                test.createNewFile(); // Throws the exception mentioned above
                //test.mkdir();
                na=test.getParent()+"/"+cur.trim()+".txt";

                isSDPresent=true;

            }
            catch (Exception e) {
                Log.e(getClass().getSimpleName(), "Exception creating file", e);
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }
    //檔案寫入DB
    public void f2d() {
        //  sdb = dbhelper.getWritableDatabase();

        //   dbhelper.openDatabase();
        sdb = dbhelper.getWritableDatabase();

        sdb.delete("jy61_record",null,null);
        try {

            InputStreamReader isr = new InputStreamReader(new FileInputStream(na));//檔案讀取路徑
            BufferedReader reader = new BufferedReader(isr);
            String line = null;

            String rectime=na.substring(na.lastIndexOf("/"));

            int i = 0;
            if (sdb != null) {
                if (sdb.isOpen()) {
                    while ((line = reader.readLine()) != null) {
                        String item[] = line.split(",");
                        //  Log.d("length: ",String.valueOf(item.length));

                        String sql = "insert into jy61_record (device_name,start_time,accx,accy,accz,angvx,angvy,angvz,angx,angy,angz ) " +
                                "values (?, ?, ?, ?,?, ?, ?, ?,?, ?, ?);";
                        if(item.length==9){

                            sdb.beginTransaction();
                            SQLiteStatement stmt = sdb.compileStatement(sql);

                            //  for (int i = 0; i < insert_db; i++) {
                            stmt.bindString(1, mwMacAddress);
                            stmt.bindString(2, rectime.substring(1));
                            stmt.bindString(3, item[0].trim());
                            stmt.bindString(4, item[1].trim());
                            stmt.bindString(5, item[2].trim());
                            stmt.bindString(6, item[3].trim());
                            stmt.bindString(7, item[4].trim());
                            stmt.bindString(8, item[5].trim());
                            stmt.bindString(9, item[6].trim());
                            stmt.bindString(10, item[7].trim());
                            stmt.bindString(11, item[8].trim());

                            long entryID = stmt.executeInsert();

                            stmt.clearBindings();
                            sdb.setTransactionSuccessful();
                            sdb.endTransaction();
                        }
                    }

                }
            }
            sdb.close();  //可自行變化成存入陣列或arrayList方便之後存取

        } catch(IOException e) {
            e.printStackTrace();
        }
    }

}
