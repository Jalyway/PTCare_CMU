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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.example.ptcare_cmu.db.DBHelper;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.data.AngularVelocity;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.Debug;
import com.mbientlab.metawear.module.GyroBmi160;
import com.mbientlab.metawear.module.Switch;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import bolts.Continuation;
import bolts.Task;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment implements ServiceConnection {
    private final HashMap<DeviceState, MetaWearBoard> stateToBoards;
    private BtleService.LocalBinder binder;
    public Handler mHandler;
    public int tsec=0;
    private int deviceConnectNum=0;
    private boolean isSampling=false;
    private String[] na=null;
    private String[] convert_FilePath=null;
    private Boolean isSDPresent = false;
    private DBHelper dbhelper = null;
    private SQLiteDatabase sdb;
    private String mwMacAddress;
    public List<GyroBmi160> gyroBmi160=new ArrayList<>();
    public List<Accelerometer> accelerometer=new ArrayList<>();
    public List<DeviceState> newDeviceStateList=new ArrayList<>();
    private List<List<String>> metaWearDataBase = new ArrayList<>();

    private final static int HANDLER_UPDATE_ACCELEROMETER_RESULT = 100;
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
        DeviceState newDeviceState= new DeviceState(btDevice);
        MetaWearBoard newBoard= binder.getMetaWearBoard(btDevice);
        mwMacAddress = btDevice.getAddress();
        newDeviceState.connecting= true;
        newDeviceState.deviceNum=deviceConnectNum;
        connectedDevices.add(newDeviceState);
        stateToBoards.put(newDeviceState, newBoard);
        this.deviceConnectNum++;
        newBoard.onUnexpectedDisconnect(status -> getActivity().runOnUiThread(() -> {
            connectedDevices.remove(newDeviceState);
            this.deviceConnectNum--;
        }));
        //--------------------------------------------------------------------------------------------------------------------------------
        newBoard.connectAsync().onSuccessTask((Continuation<Void, Task<Route>>) task -> {
            MainActivityFragment.this.getActivity().runOnUiThread(() -> {
                newDeviceState.connecting = false;
                connectedDevices.notifyDataSetChanged();
            });

            newDeviceStateList.add(newDeviceState);
            accelerometer.add(newBoard.getModule(Accelerometer.class));
            Log.e("Kenny","accelerometer++");
            gyroBmi160.add(newBoard.getModule(GyroBmi160.class));
            Log.e("Kenny","gyroBmi160++");

            int deviceNum=newDeviceStateList.size()-1;
            List<String> metaWearDataflash = new ArrayList<>();
            metaWearDataBase.add(metaWearDataflash);
            Thread metaWear_setupThread=new metaWear_stepUpThread(deviceNum);
            metaWear_setupThread.start();

            return null;
        }).onSuccessTask(task -> newBoard.getModule(Switch.class).state().addRouteAsync(source -> source.stream((Subscriber) (data, env) -> {
            getActivity().runOnUiThread(() -> {
                newDeviceState.pressed = data.value(Boolean.class);
                connectedDevices.notifyDataSetChanged();
            });
        }))).continueWith((Continuation<Route, Void>) task -> {
            if (task.isFaulted()) {
                if (!newBoard.isConnected()) {
                    getActivity().runOnUiThread(() -> connectedDevices.remove(newDeviceState));
                    this.deviceConnectNum--;
                } else {
                    Snackbar.make(getActivity().findViewById(R.id.activity_main_layout), task.getError().getLocalizedMessage(), Snackbar.LENGTH_SHORT).show();
                    newBoard.tearDown();
                    newBoard.disconnectAsync().continueWith((Continuation<Void, Void>) task1 -> {
                        connectedDevices.remove(newDeviceState);
                        this.deviceConnectNum--;
                        return null;
                    });
                }
            }
            return null;
        });
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
            GyroBmi160 gyroBmi160=selectedBoard.getModule(GyroBmi160.class);
            gyroBmi160.stop();

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
    //-------------------------------------------------------------------------------------------------------------------------------------------------------------------------
    private void CalculateAngle(Acceleration AccelerationData,DeviceState newDeviceState)
    {
        double Denominator = Math.sqrt(Math.pow(AccelerationData.x(), 2) + Math.pow(AccelerationData.y(), 2) + Math.pow(AccelerationData.z(), 2));

        double AngleX = CalculateAngle(AccelerationData.x(), Denominator);
        double AngleY = CalculateAngle(AccelerationData.y(), Denominator);
        double AngleZ = CalculateAngle(AccelerationData.z(), Denominator);
        String ans="(x:\t"+ String.format("%.3f", AngleX) + "g, " +
                "y:\t" + String.format("%.3f", AngleY) + "g, " +
                "z:\t" + String.format("%.3f", AngleZ) + "g)";
        newDeviceState.deviceAngle = ans;
        String ang_entry=","+AngleX+","+AngleY+","+AngleZ;
        newDeviceState.deviceAngleRC=ang_entry;
    }
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////-----------------------CalculateAngle
    private double CalculateAngle(float Value, double Denominator)      //使用弧度計算角度
    {
        double Radian = Math.acos(Value / Denominator);
        return ((Radian * (double)180) / Math.PI);
    } // end CalculateAngleByRadian
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public void handleStartSampling() {
        isSampling = true;
        for (int i=0; i< accelerometer.size();i++){
            accelerometer.get(i).packedAcceleration().start();
            accelerometer.get(i).start();

            gyroBmi160.get(i).packedAngularVelocity().start();
            gyroBmi160.get(i).start();
        }

        tsec=0;
        mHandler.postDelayed(runnable,1000); // 開始Timer
    }

    public void handleStopSampling() {
        for (int i=0; i< accelerometer.size();i++){
            accelerometer.get(i).packedAcceleration().stop();
            accelerometer.get(i).stop();

            gyroBmi160.get(i).packedAngularVelocity().stop();
            gyroBmi160.get(i).stop();
        }
        isSampling = false;

        mHandler.removeCallbacks(runnable); //停止Timer
    }
    public Runnable runnable = new Runnable() {
        public void run ( ) {
            //	update( );
            tsec++;
            mHandler.postDelayed(this,1000);
            for (int i=0; i< accelerometer.size();i++){
                newDeviceStateList.get(i).deviceTime=Integer.toString(tsec);
            }


        }
    };
    //-------------------------------------------------------------------------------------------------------
    class handlerUpdate implements Handler.Callback {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                // Sensors
                case HANDLER_UPDATE_ACCELEROMETER_RESULT:
                    connectedDevices.notifyDataSetChanged();
                    return false;
                case HANDLER_UPDATE_GYRO_RESULT:
                    connectedDevices.notifyDataSetChanged();
                    return false;
            }
            return false;
        }
    }
    //-----------------------------------------------------------------------------------------------------
    public void createSysDir()
    {
        File[] dirs = ContextCompat.getExternalFilesDirs(getContext(), null);
        File removable = null;
        na=new String[deviceConnectNum];
        convert_FilePath=new String[deviceConnectNum];

        if (dirs.length > 0) {
            removable= dirs[0];
        }

        if (removable.exists() && removable.canRead() && removable.canWrite()) {

            SimpleDateFormat formatter1 = new SimpleDateFormat("yyyyMMddHHmm ");
            Date curDate = new Date(System.currentTimeMillis());//獲取當前時間

            String cur=formatter1.format(curDate);
            for (int i=0; i<deviceConnectNum;i++){
                File test = new File(removable,cur.trim()+"_"+i+".txt");
                File convertFile = new File(removable,cur.trim()+"_"+i+".csv");
                try {
                    test.createNewFile(); // Throws the exception mentioned above
                    convertFile.createNewFile();
                    //test.mkdir();
                    na[i]=test.getParent()+"/"+cur.trim()+"_"+i+".txt";
                    convert_FilePath[i]=convertFile.getParent()+"/"+cur.trim()+"_"+i+".csv";

                    isSDPresent=true;
                }
                catch (Exception e) {
                    Log.e(getClass().getSimpleName(), "Exception creating file", e);
                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

        }
    }
    //
    public void data2file(){
        createSysDir();
//        Log.e("Kenny",String.valueOf(angvL.size()));
//
        for (int i=0;i<na.length;i++){
            Log.e("r", na[i]);
            if (na[i]!=null){
                OutputStream out;
                Log.e("metaWear "+i, String.valueOf(metaWearDataBase.get(i).size()));
                for (int j=0; j<metaWearDataBase.get(i).size(); j++){
                    try {
                        out = new BufferedOutputStream(new FileOutputStream(na[i], true));
                        if (metaWearDataBase.get(i).get(j)!=null){
                            out.write(metaWearDataBase.get(i).get(j).getBytes());
                            out.write("\n".getBytes());
                        }
                        out.close();
                    } catch (Exception e) {
                        Log.e("r", "CSV creation error", e);
                    }
                }
            }
        }
    }
    //檔案寫入DB
    public void f2d() {
        //--------------------------------------------------------------------------------------------------------------------------------------------------------
        //  sdb = dbhelper.getWritableDatabase();
        //   dbhelper.openDatabase();
        for (int i=0;i<na.length;i++) {
            sdb = dbhelper.getWritableDatabase();

            sdb.delete("jy61_record",null,null);
            try {
                InputStreamReader isr = new InputStreamReader(new FileInputStream(na[i]));//檔案讀取路徑
                BufferedReader reader = new BufferedReader(isr);
                String line = null;
                String rectime=na[i].substring(na[i].lastIndexOf("/"));

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
            db2CSV(i);
        }
    }

//    檔案輸出成csv
    public void db2CSV(int i){
        DataTransfer dataTransfer=new DataTransfer();
        dataTransfer.calMotion(convert_FilePath[i],getContext());
    }

    //
    private void metaWearSensor(Accelerometer accelerometer, GyroBmi160 gyroBmi160,DeviceState newDeviceState){
        // Setup Gyro
        gyroBmi160.packedAngularVelocity().stop();
        gyroBmi160.stop();

        if (gyroBmi160 != null) {
//                    listSensors.add("Gyro");
            // set the data rat to 50Hz and the
            // data range to +/- 2000 degrees/s
            gyroBmi160.configure()
                    .odr(GyroBmi160.OutputDataRate.ODR_100_HZ)
                    .range(GyroBmi160.Range.FSR_2000)
                    .commit();
            gyroBmi160.packedAngularVelocity().addRouteAsync(source -> source.stream((Subscriber) (data, env) -> {
                newDeviceState.deviceGYRO = data.value(AngularVelocity.class).toString();
                final double Deg2Rad = Math.PI / 180.0;
                String angv_entry =   ","+String.format("%.6f", data.value(AngularVelocity.class).x() * Deg2Rad) + "," +
                        String.format("%.6f", data.value(AngularVelocity.class).y() * Deg2Rad) + "," +
                        String.format("%.6f", data.value(AngularVelocity.class).z() * Deg2Rad);
                newDeviceState.deviceGYRORC = angv_entry;
                //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
                mHandler.obtainMessage(HANDLER_UPDATE_GYRO_RESULT, data.value(AngularVelocity.class)).sendToTarget();
            }));
        }

        accelerometer.packedAcceleration().stop();
        accelerometer.stop();

        if (accelerometer != null) {
            accelerometer.configure()
                    .odr(100f)       // Set sampling frequency to 25Hz, or closest valid ODR
                    .range(4f)      // Set data range to +/-4g, or closet valid range
                    .commit();
            accelerometer.packedAcceleration().addRouteAsync(source -> source.stream((Subscriber) (data, env) -> {
                CalculateAngle(data.value(Acceleration.class),newDeviceState);
                newDeviceState.deviceAcceleration=data.value(Acceleration.class).toString();
                String accl_entry = String.format("%.6f", data.value(Acceleration.class).x()) + "," +
                        String.format("%.6f", data.value(Acceleration.class).y())  + "," +
                        String.format("%.6f", data.value(Acceleration.class).z()) ;
                newDeviceState.deviceAccelerationRC=accl_entry;
                String inputString = newDeviceState.deviceAccelerationRC+newDeviceState.deviceGYRORC+newDeviceState.deviceAngleRC;
                metaWearDataBase.get(newDeviceState.deviceNum).add(inputString);
                Log.e("MetaWear"+newDeviceState.deviceNum,metaWearDataBase.get(newDeviceState.deviceNum).get(metaWearDataBase.get(newDeviceState.deviceNum).size()-1));
                //------------------------------------------------------------------------------------------------------------------------------------
                mHandler.obtainMessage(HANDLER_UPDATE_ACCELEROMETER_RESULT, data.value(Acceleration.class)).sendToTarget();
            }));
        }

    }

    //

    class metaWear_stepUpThread extends Thread{

        int deviceNum;

        metaWear_stepUpThread(int deviceNum){
            this.deviceNum=deviceNum;
        }

        public void run(){
            metaWearSensor(accelerometer.get(deviceNum),gyroBmi160.get(deviceNum),newDeviceStateList.get(deviceNum));
        }
    }
}
