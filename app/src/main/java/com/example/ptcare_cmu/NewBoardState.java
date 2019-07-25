package com.example.ptcare_cmu;

import android.bluetooth.BluetoothDevice;

import com.example.ptcare_cmu.DeviceState;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.GyroBmi160;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class NewBoardState implements Serializable {
    GyroBmi160 gyroBmi160;
    Accelerometer accelerometer;
    DeviceState newDeviceState;
    MetaWearBoard newBoard;
    List<String> metaWearDataflash;

    NewBoardState(MetaWearBoard newBoard,DeviceState newDeviceState) {
        this.newBoard = newBoard;
        this.newDeviceState=newDeviceState;
        this.gyroBmi160=null;
        this.accelerometer=null;
        this.metaWearDataflash= new ArrayList<>();
    }

    @Override
    public boolean equals(Object obj) {
        return false;
    }
}
