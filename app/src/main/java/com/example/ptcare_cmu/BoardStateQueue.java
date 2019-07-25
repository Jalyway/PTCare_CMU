package com.example.ptcare_cmu;

import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BoardStateQueue implements Serializable {
    public List<NewBoardState> newBoardStateList;
    private List<Integer> ListQueue;

    public BoardStateQueue(){
        newBoardStateList=new ArrayList<>();
        ListQueue=new ArrayList<>();
    }

    public void addNewBoard(NewBoardState newBoardState){
        newBoardStateList.add(newBoardState);
        ListQueue.add(newBoardState.newDeviceState.deviceNum);
    }

    public void removeBoard(NewBoardState newBoardState){
        int index=ListQueue.indexOf(newBoardState.newDeviceState.deviceNum);
        newBoardStateList.remove(index);
        Log.e("REMOVE",""+newBoardStateList.size());
    }
}
