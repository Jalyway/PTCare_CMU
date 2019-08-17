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
    private boolean isSampling=false;
    private String[] na=null;
    private String[] convert_FilePath=null;
    private Boolean isSDPresent = false;
    private DBHelper dbhelper = null;
    private SQLiteDatabase sdb;
    private String mwMacAddress="000";
    private BoardStateQueue boardStateQueue=new BoardStateQueue();
    private int deviceConnectionNum=0;

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
        //------------------------------------------------------------------------------------------------
        DeviceState newDeviceState= new DeviceState(btDevice);
        NewBoardState newBoardState=new NewBoardState(binder.getMetaWearBoard(btDevice), newDeviceState);
        newDeviceState.connecting= true;
        newDeviceState.deviceNum=deviceConnectionNum;
        connectedDevices.add(newDeviceState);
        stateToBoards.put(newDeviceState, newBoardState.newBoard);
        boardStateQueue.addNewBoard(newBoardState);
        newBoardSetting(newBoardState);
        this.deviceConnectionNum++;
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
        double AngleX = Math.atan(AccelerationData.x()/Math.sqrt(AccelerationData.y()*AccelerationData.y()+AccelerationData.z()*AccelerationData.z()));
        double AngleY = Math.atan(AccelerationData.y()/Math.sqrt(AccelerationData.x()*AccelerationData.x()+AccelerationData.z()*AccelerationData.z()));
        double AngleZ = Math.atan(AccelerationData.z()/Math.sqrt(AccelerationData.y()*AccelerationData.y()+AccelerationData.x()*AccelerationData.x()));

        AngleX=Math.toDegrees(AngleX);
        AngleY=Math.toDegrees(AngleY);
        AngleZ=Math.toDegrees(AngleZ);

        String ans="(x:\t"+ String.format("%.3f", AngleX) + "g, " +
                "y:\t" + String.format("%.3f", AngleY) + "g, " +
                "z:\t" + String.format("%.3f", AngleZ) + "g)";
        newDeviceState.deviceAngle = ans;
        String ang_entry=","+AngleX+","+AngleY+","+AngleZ;
        newDeviceState.deviceAngleRC=ang_entry;
    }
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public void handleStartSampling() {
        isSampling = true;
        for (int i=0; i<boardStateQueue.newBoardStateList.size();i++){
            boardStateQueue.newBoardStateList.get(i).gyroBmi160.packedAngularVelocity().start();
            boardStateQueue.newBoardStateList.get(i).gyroBmi160.start();

            boardStateQueue.newBoardStateList.get(i).accelerometer.packedAcceleration().start();
            boardStateQueue.newBoardStateList.get(i).accelerometer.start();
        }

        tsec=0;
        mHandler.postDelayed(runnable,1000); // 開始Timer
    }

    public void handleStopSampling() {
        for (int i=0; i<boardStateQueue.newBoardStateList.size();i++){
            boardStateQueue.newBoardStateList.get(i).accelerometer.packedAcceleration().stop();
            boardStateQueue.newBoardStateList.get(i).accelerometer.stop();

            boardStateQueue.newBoardStateList.get(i).gyroBmi160.packedAngularVelocity().stop();
            boardStateQueue.newBoardStateList.get(i).gyroBmi160.stop();
        }
        isSampling = false;

        mHandler.removeCallbacks(runnable); //停止Timer
    }
    public Runnable runnable = new Runnable() {
        public void run ( ) {
            //	update( );
            tsec++;
            mHandler.postDelayed(this,1000);
            for (int i=0; i< boardStateQueue.newBoardStateList.size();i++){
                boardStateQueue.newBoardStateList.get(i).newDeviceState.deviceTime=Integer.toString(tsec);
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
        na=new String[boardStateQueue.newBoardStateList.size()];
        convert_FilePath=new String[boardStateQueue.newBoardStateList.size()];

        if (dirs.length > 0) {
            removable= dirs[0];
        }

        if (removable.exists() && removable.canRead() && removable.canWrite()) {

            SimpleDateFormat formatter1 = new SimpleDateFormat("yyyyMMddHHmm ");
            Date curDate = new Date(System.currentTimeMillis());//獲取當前時間

            String cur=formatter1.format(curDate);
            for (int i=0; i<boardStateQueue.newBoardStateList.size();i++){
                File folder = new File(removable+"/"+cur.trim()+"_"+i);
                if(!folder.exists()){// 如果資料夾不存在，創建一個
                    folder.mkdirs();
                } //而如果用.mkdir()方法則不會自動創建

                File test = new File(folder.getPath(),cur.trim()+"_"+i+".txt");
                File convertFile = new File(folder.getPath(),cur.trim()+"_"+i+".csv");
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
//
        for (int i=0;i<na.length;i++){
            Log.e("r", na[i]);
            if (na[i]!=null){
                OutputStream out;
                Log.e("metaWear "+i, String.valueOf(boardStateQueue.newBoardStateList.get(i).metaWearDataflash.size()));
                for (int j=0; j< boardStateQueue.newBoardStateList.get(i).metaWearDataflash.size(); j++){
                    try {
                        out = new BufferedOutputStream(new FileOutputStream(na[i], true));
                        if (boardStateQueue.newBoardStateList.get(i).metaWearDataflash.get(j)!=null){
                            out.write(boardStateQueue.newBoardStateList.get(i).metaWearDataflash.get(j).getBytes());
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

    //
    private void metaWearSensor(NewBoardState newBoardState){
        Accelerometer accelerometer = newBoardState.accelerometer;
        GyroBmi160 gyroBmi160 = newBoardState.gyroBmi160;
        DeviceState newDeviceState = newBoardState.newDeviceState;

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
                newBoardState.metaWearDataflash.add(inputString);
                Log.e("MetaWear"+newDeviceState.deviceNum,newBoardState.metaWearDataflash.get(newBoardState.metaWearDataflash.size()-1));
                //------------------------------------------------------------------------------------------------------------------------------------
                mHandler.obtainMessage(HANDLER_UPDATE_ACCELEROMETER_RESULT, data.value(Acceleration.class)).sendToTarget();
            }));
        }

    }

    private void newBoardSetting(NewBoardState newBoardState){
        MetaWearBoard newBoard=newBoardState.newBoard;
        DeviceState newDeviceState=newBoardState.newDeviceState;
        newBoard.onUnexpectedDisconnect(status -> getActivity().runOnUiThread(() -> {
            connectedDevices.remove(newDeviceState);
            boardStateQueue.removeBoard(newBoardState);
        }));
        //--------------------------------------------------------------------------------------------------------------------------------
        newBoard.connectAsync().onSuccessTask((Continuation<Void, Task<Route>>) task -> {
            MainActivityFragment.this.getActivity().runOnUiThread(() -> {
                newDeviceState.connecting = false;
                connectedDevices.notifyDataSetChanged();
            });

            newBoardState.accelerometer=newBoard.getModule(Accelerometer.class);
            newBoardState.gyroBmi160=newBoard.getModule(GyroBmi160.class);
            Log.e("Kenny","accelerometer++");
            Log.e("Kenny","gyroBmi160++");

            Thread metaWear_setupThread=new metaWear_stepUpThread(newBoardState);
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
                    getActivity().runOnUiThread(() -> {
                        connectedDevices.remove(newDeviceState);
                        boardStateQueue.removeBoard(newBoardState);
                    });
                } else {
                    Snackbar.make(getActivity().findViewById(R.id.activity_main_layout), task.getError().getLocalizedMessage(), Snackbar.LENGTH_SHORT).show();
                    newBoard.tearDown();
                    newBoard.disconnectAsync().continueWith((Continuation<Void, Void>) task1 -> {
                        connectedDevices.remove(newDeviceState);
                        boardStateQueue.removeBoard(newBoardState);
                        return null;
                    });
                }
            }
            return null;
        });
    }

    //

    class metaWear_stepUpThread extends Thread{

        NewBoardState newBoardState;

        metaWear_stepUpThread(NewBoardState newBoardState){
            this.newBoardState=newBoardState;
        }

        public void run(){
            metaWearSensor(newBoardState);
        }
    }
}
