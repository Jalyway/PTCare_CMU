package com.example.ptcare_cmu;

/*
 * Created by xi-jun on 2019/5/17 at YZU.
 */

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.myViewHolder> {
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private List<BluetoothDevice> itemDevices;
    private List<Integer> itemRSSIs;
    private OnItemClickListener listener;

    public MyAdapter(OnItemClickListener listener) {
        itemDevices = new ArrayList<>();
        itemRSSIs = new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public myViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        if (viewType == TYPE_HEADER) {
            View vv = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.scan_device_header, viewGroup,false);
            return new myViewHolder(vv);
        }
        else if (viewType == TYPE_ITEM) {
            View vv = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.scan_device_item, viewGroup,false);
            return new myViewHolder(vv);
        }
        throw new RuntimeException("No match for " + viewType + ".");
    }

    @Override
    public void onBindViewHolder(@NonNull myViewHolder vh, int i) {
        int rssi = itemRSSIs.get(i);
        if (i != TYPE_HEADER) {
            vh.setListener(itemDevices.get(i), listener);

            String name = itemDevices.get(i).getName();
            vh.deviceName.setText(name!=null ? name : "Unknown Device");
            vh.deviceAddress.setText(itemDevices.get(i).getAddress());

            vh.textRSSI.setText(String.format("%d dBm", rssi));
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (isPositionHeader(position))
            return TYPE_HEADER;
        return TYPE_ITEM;
    }
    private boolean isPositionHeader(int position) {
        return position == 0;
    }

    @Override
    public int getItemCount() {
        return itemDevices.size();
    }

    //
    public void addDevice(BluetoothDevice device, int rssi) {
        if(!itemDevices.contains(device)) {
            itemDevices.add(device);
            itemRSSIs.add(rssi);
        }
    }
    public void refresh() {
        itemDevices.clear();
        itemRSSIs.clear();
    }
    // <<interface>> //
    public interface OnItemClickListener {
        void onItemClick(BluetoothDevice bluetoothDevice);
    }

    /// self-design ViewHolder
    public class myViewHolder extends RecyclerView.ViewHolder {
        private TextView deviceName;
        private TextView deviceAddress;
        private ImageView imageRSSI;
        private TextView textRSSI;
        private TextView countTv;

        public myViewHolder(View v) {
            super(v);
            deviceName = v.findViewById(R.id.device_name);
            deviceAddress = v.findViewById(R.id.device_address);
            imageRSSI = v.findViewById(R.id.imageRSSI);
            textRSSI = v.findViewById(R.id.textRSSI);
        }

        public void setListener(final BluetoothDevice bluetoothDevice, final OnItemClickListener listener) {
            super.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onItemClick(bluetoothDevice);
                    //itemView.setBackgroundColor(Color.parseColor("#FFFFBB"));
                }
            });
        }
    }
}
