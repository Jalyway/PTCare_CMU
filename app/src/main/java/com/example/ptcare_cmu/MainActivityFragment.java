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
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.mbientlab.metawear.AsyncDataProducer;
import com.mbientlab.metawear.DeviceInformation;
import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.data.AngularVelocity;
import com.mbientlab.metawear.data.MagneticField;
import com.mbientlab.metawear.data.AngularVelocity;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.AccelerometerBosch;
import com.mbientlab.metawear.module.AccelerometerMma8452q;
import com.mbientlab.metawear.module.Debug;
import com.mbientlab.metawear.module.GyroBmi160;
import com.mbientlab.metawear.module.Switch;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
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
    DeviceState newDeviceState;
    public int tsec=0;
    public Handler mHandler;

    private ConnectedDevicesAdapter connectedDevices= null;

    public MainActivityFragment() {
        stateToBoards = new HashMap<>();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler(new handlerUpdate());
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

        newDeviceState.connecting= true;
        connectedDevices.add(newDeviceState);
        stateToBoards.put(newDeviceState, newBoard);

        final Capture<AsyncDataProducer> orientCapture = new Capture<>();
        final Capture<Accelerometer> accelCapture = new Capture<>();

        newBoard.onUnexpectedDisconnect(status -> getActivity().runOnUiThread(() -> connectedDevices.remove(newDeviceState)));
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
        //--------------------------------------------------------------------------------------------
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
                    newDeviceState.deviceAngle = MainActivityFragment.this.CalculateAngles(data.value(Acceleration.class));
                    Log.w("WWW", newDeviceState.deviceAcceleration);
                    connectedDevices.notifyDataSetChanged();
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
    private String CalculateAngles(Acceleration AccelerationData)
    {
        double Denominator = Math.sqrt(Math.pow(AccelerationData.x(), 2) + Math.pow(AccelerationData.y(), 2) + Math.pow(AccelerationData.z(), 2));

        double AngleX = CalculateAngle(AccelerationData.x(), Denominator);
        double AngleY = CalculateAngle(AccelerationData.y(), Denominator);
        double AngleZ = CalculateAngle(AccelerationData.z(), Denominator);
        String ans="(x:\t"+ String.format("%.3f", AngleX) + "g, " +
                "y:\t" + String.format("%.3f", AngleY) + "g, " +
                "z:\t" + String.format("%.3f", AngleZ) + "g)";
        return(ans);
    } // CalculateAngles
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

//        isSampling = true;
    }

    public void handleStopSampling() {

        accelerometer.acceleration().stop();
        accelerometer.stop();

        gyroBmi160.angularVelocity().stop();
        gyroBmi160.stop();

        mHandler.removeCallbacks(runnable); //停止Timer
//        magnetometer.magneticField().stop();
//        magnetometer.stop();
//        isSampling = false;

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
            return false;
        }
    }
}
