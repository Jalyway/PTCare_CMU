
package com.example.ptcare_cmu;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.mbientlab.bletoolbox.scanner.MacAddressEntryDialogFragment;
import com.mbientlab.bletoolbox.scanner.ScannedDeviceInfo;
import com.mbientlab.bletoolbox.scanner.ScannedDeviceInfoAdapter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.UUID;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link BleScanner.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link BleScanner#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BleScanner extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    //
    public static final long DEFAULT_SCAN_PERIOD= 5000L;
    private static final int REQUEST_ENABLE_BT = 1, PERMISSION_REQUEST_COARSE_LOCATION= 2;

    TextView txtChange;
    private ScannedDeviceInfoAdapter scannedDevicesAdapter;
    private Button scanControl;
    private Handler mHandler;
    private boolean isScanning= false;
    private BluetoothAdapter btAdapter= null;
    private HashSet<UUID> filterServiceUuids;
    private HashSet<ParcelUuid> api21FilterServiceUuids;
    private boolean isScanReady;
    private com.mbientlab.bletoolbox.scanner.BleScannerFragment.ScannerCommunicationBus commBus= null;
    //

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public BleScanner() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment BleScanner.
     */
    // TODO: Rename and change types and number of parameters
    public static BleScanner newInstance(String param1, String param2) {
        BleScanner fragment = new BleScanner();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        final Activity owner= getActivity();
        if (!(owner instanceof com.mbientlab.bletoolbox.scanner.BleScannerFragment.ScannerCommunicationBus)) {
            throw new ClassCastException(String.format(Locale.US, "%s %s", owner.toString(),
                    owner.getString(R.string.error_scanner_listener)));
        }

        commBus= (com.mbientlab.bletoolbox.scanner.BleScannerFragment.ScannerCommunicationBus) owner;
        btAdapter= ((BluetoothManager) owner.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();

        if (btAdapter == null) {
            new AlertDialog.Builder(owner).setTitle(R.string.dialog_title_error)
                    .setMessage(R.string.error_no_bluetooth_adapter)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            owner.finish();
                        }
                    })
                    .create()
                    .show();
        } else if (!btAdapter.isEnabled()) {
            final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            isScanReady = true;
        }


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        scannedDevicesAdapter= new ScannedDeviceInfoAdapter(getActivity(), R.id.blescan_entry_layout);
        scannedDevicesAdapter.setNotifyOnChange(true);
        mHandler = new Handler();
        return inflater.inflate(R.layout.blescan_device_list, container);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    //
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        UUID[] filterUuids= commBus.getFilterServiceUuids();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            filterServiceUuids = new HashSet<>();
            if (filterUuids != null) {
                filterServiceUuids.addAll(Arrays.asList(filterUuids));
            }
        } else {
            api21FilterServiceUuids= new HashSet<>();
            for (UUID uuid : filterUuids) {
                api21FilterServiceUuids.add(new ParcelUuid(uuid));
            }
        }

        ListView scannedDevices= (ListView) view.findViewById(R.id.blescan_devices);
        scannedDevices.setAdapter(scannedDevicesAdapter);
        scannedDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                stopBleScan();

                commBus.onDeviceSelected(scannedDevicesAdapter.getItem(i).btDevice);
            }
        });
        txtChange = getActivity().findViewById(R.id.ble_scan_title);
        scanControl= (Button) view.findViewById(R.id.blescan_control);
        scanControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isScanning) {
                    stopBleScan();
                } else {
                    startBleScan();
                }
            }
        });

        if (isScanReady) {
            startBleScan();
        }
    }
    //
    @Override
    public void onDestroyView() {
        stopBleScan();
        super.onDestroyView();
    }
    //
    private BluetoothAdapter.LeScanCallback deprecatedScanCallback= null;
    private ScanCallback api21ScallCallback= null;
    //
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_CANCELED) {
                    getActivity().finish();
                } else {
                    startBleScan();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    //

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    new MacAddressEntryDialogFragment().show(getFragmentManager(), "mac_address_entry");
                } else {
                    isScanReady= true;
                    startBleScan();
                }
            }
        }
    }

    //
    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    public interface ScannerCommunicationBus {
        /**
         * Retrieve an array of allowed service UUIDs.  If no filtering should be done, return null.
         * @return Service UUIDs to scan for, null if all discovered devices should be shown
         */
        UUID[] getFilterServiceUuids();

        /**
         * Retrieve how long to scan for Bluetooth LE devices.  Users can return {@link #DEFAULT_SCAN_PERIOD} if
         * they do not want to set their own scan duration value
         * @return Bluetooth LE scan duration, in milliseconds
         */
        long getScanDuration();

        /**
         * Called when the user has selected a Bluetooth device from the device list
         * @param device Device the user selected
         */
        void onDeviceSelected(BluetoothDevice device);
    }
    //
    @TargetApi(22)
    public void startBleScan() {
        if (!checkLocationPermission()) {
            scanControl.setText(R.string.ble_scan);
            return;
        }

        scannedDevicesAdapter.clear();
        isScanning= true;
        txtChange.setText("搜尋中...");
        scanControl.setText(R.string.ble_scan_cancel);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopBleScan();
            }
        }, commBus.getScanDuration());

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            deprecatedScanCallback= new BluetoothAdapter.LeScanCallback() {
                private void foundDevice(final BluetoothDevice btDevice, final int rssi) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            scannedDevicesAdapter.update(new ScannedDeviceInfo(btDevice, rssi));
                        }
                    });
                }
                @Override
                public void onLeScan(BluetoothDevice bluetoothDevice, int rssi, byte[] scanRecord) {
                    ///< Service UUID parsing code taking from stack overflow= http://stackoverflow.com/a/24539704

                    ByteBuffer buffer= ByteBuffer.wrap(scanRecord).order(ByteOrder.LITTLE_ENDIAN);
                    boolean stop= false;
                    while (!stop && buffer.remaining() > 2) {
                        byte length = buffer.get();
                        if (length == 0) break;

                        byte type = buffer.get();
                        switch (type) {
                            case 0x02: // Partial list of 16-bit UUIDs
                            case 0x03: // Complete list of 16-bit UUIDs
                                while (length >= 2) {
                                    UUID serviceUUID= UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", buffer.getShort()));
                                    stop= filterServiceUuids.isEmpty() || filterServiceUuids.contains(serviceUUID);
                                    if (stop) {
                                        foundDevice(bluetoothDevice, rssi);
                                    }

                                    length -= 2;
                                }
                                break;

                            case 0x06: // Partial list of 128-bit UUIDs
                            case 0x07: // Complete list of 128-bit UUIDs
                                while (!stop && length >= 16) {
                                    long lsb= buffer.getLong(), msb= buffer.getLong();
                                    stop= filterServiceUuids.isEmpty() || filterServiceUuids.contains(new UUID(msb, lsb));
                                    if (stop) {
                                        foundDevice(bluetoothDevice, rssi);
                                    }
                                    length -= 16;
                                }
                                break;

                            default:
                                buffer.position(buffer.position() + length - 1);
                                break;
                        }
                    }

                    if (!stop && filterServiceUuids.isEmpty()) {
                        foundDevice(bluetoothDevice, rssi);
                    }
                }
            };
            btAdapter.startLeScan(deprecatedScanCallback);
        } else {
            api21ScallCallback= new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, final ScanResult result) {
                    if (result.getScanRecord() != null && result.getScanRecord().getServiceUuids() != null) {
                        boolean valid= true;
                        for (ParcelUuid it : result.getScanRecord().getServiceUuids()) {
                            valid&= api21FilterServiceUuids.contains(it);
                        }

                        if (valid) {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    scannedDevicesAdapter.update(new ScannedDeviceInfo(result.getDevice(), result.getRssi()));
                                }
                            });
                        }
                    }

                    super.onScanResult(callbackType, result);
                }
            };
            btAdapter.getBluetoothLeScanner().startScan(api21ScallCallback);
        }
    }

    /**
     * Stops the Bluetooth LE scan
     */
    public void stopBleScan() {
        if (isScanning) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                btAdapter.stopLeScan(deprecatedScanCallback);
            } else {
                btAdapter.getBluetoothLeScanner().stopScan(api21ScallCallback);
            }

            isScanning= false;
            txtChange.setText(R.string.select_metawear);
            scanControl.setText(R.string.ble_scan);
        }
    }

    @TargetApi(23)
    private boolean checkLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                getActivity().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission code taken from Radius Networks
            // http://developer.radiusnetworks.com/2015/09/29/is-your-beacon-app-ready-for-android-6.html

            // Android M Permission check
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.title_request_permission);
            builder.setMessage(R.string.error_location_access);
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
            return false;
        }
        return true;
    }
}
