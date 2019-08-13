package com.example.ptcare_cmu;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class UploadRec extends MainActivity implements View.OnClickListener, AsyncResponse {
    private static final int PICK_FILE_REQUEST = 1;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 123;
    private static final String TAG = MainActivity.class.getSimpleName();
    //
    private String[] selectedFilePath = new String[3];
    private UploadFile mUpload;
    //private String SERVER_URL = "http://140.128.65.114:8000/PRRFISHome/PTC/uploadc.jsp";  // POST目標網址
    // http://140.128.65.114:8000/PRRFISHome/PTC/samplefile.jsp  此網址可查看有無上傳成功
    private String orgData_URL, cvtData_URL, resultImg_URL;
    //
    private Button[] btn_choose = new Button[3];
    private Button btn_Upload;
    private TextView[] tv_FileName = new TextView[3];
    private int index;
    private int upload_count = 0;
    PowerManager.WakeLock wakeLock;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.uploadrec);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        //
        btn_choose[0] = findViewById(R.id.btn_choose1);
        btn_choose[1] = findViewById(R.id.btn_choose2);
        btn_choose[2] = findViewById(R.id.btn_choose3);
        btn_Upload = findViewById(R.id.btn_upload);
        tv_FileName[0] = findViewById(R.id.tv_fileName1);
        tv_FileName[1] = findViewById(R.id.tv_fileName2);
        tv_FileName[2] = findViewById(R.id.tv_fileName3);
        btn_choose[0].setOnClickListener(this);
        btn_choose[1].setOnClickListener(this);
        btn_choose[2].setOnClickListener(this);
        btn_Upload.setOnClickListener(this);
        propertySetting();


        // 判斷是否有開啟網路連線
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() != NetworkInfo.State.CONNECTED &&
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() != NetworkInfo.State.CONNECTED) {
            //we are not connected to the network
            Toast.makeText(this,"請先開啟網路連線",Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
        }

        // 取得存取image的權限
        checkPermissionREAD_EXTERNAL_STORAGE(this);
    }

    @Override
    public void onClick(View view) {
        for (int i=0; i<btn_choose.length; i++) {
            if (view == btn_choose[i]) {
                index = i;
                showFileChooser(index);
            }
        }

        if (view == btn_Upload) {
            if (selectedFilePath[0]!=null && selectedFilePath[1]!=null && selectedFilePath[2]!=null) {
                for (int i=0; i<3; i++) {
                    mUpload = new UploadFile(getApplicationContext(),UploadRec.this);

                    if (selectedFilePath[i].endsWith("txt"))
                        mUpload.execute(orgData_URL, selectedFilePath[i]);  //傳入URL,檔案路徑
                    else if (selectedFilePath[i].endsWith("csv"))
                        mUpload.execute(cvtData_URL, selectedFilePath[i]);  //傳入URL,檔案路徑
                    else
                        mUpload.execute(resultImg_URL, selectedFilePath[i]); //傳入URL,檔案路徑

                    mUpload.delegate = UploadRec.this;
                }
            }
            else
                Toast.makeText(UploadRec.this, "Please choose 3 files totally First", Toast.LENGTH_SHORT).show();
        }
    }

    //
    private void showFileChooser(int index) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        if (index == 0) {
            intent.setType("text/plain");
            // starts new activity to select file and return data
            startActivityForResult(Intent.createChooser(intent,"Choose .txt File to Upload.."), PICK_FILE_REQUEST);
        } else if (index == 1) {
            intent.setType("text/plain");
            // starts new activity to select file and return data
            startActivityForResult(Intent.createChooser(intent,"Choose .csv File to Upload.."), PICK_FILE_REQUEST);
        } else if (index == 2) {

            if (checkPermissionREAD_EXTERNAL_STORAGE(this)) {
                intent.setType("image/*");
                // starts new activity to select file and return data
                startActivityForResult(Intent.createChooser(intent,"Choose image File to Upload.."), PICK_FILE_REQUEST);
            }

        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PICK_FILE_REQUEST) {
                if (data == null) {
                    //no data present
                    return;
                }
                //讓螢幕保持喚醒(亮度全開)
                PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
                wakeLock.acquire();

                Uri selectedFileUri = data.getData();
                //Toast.makeText(this, selectedFileUri.toString(), Toast.LENGTH_LONG).show();

                selectedFilePath[index] = FilePath.getPath(this, selectedFileUri);
                Log.i(TAG, "Selected File Path:" + selectedFilePath[index]);

                if (selectedFilePath[index]!=null && !selectedFilePath[index].equals("")) {
                    String filename = selectedFilePath[index].substring(selectedFilePath[index].lastIndexOf("/") + 1);
                    tv_FileName[index].setText(filename);  //只顯示檔名
                }
                else
                    Toast.makeText(this, "Cannot upload file to server ", Toast.LENGTH_LONG).show();

                wakeLock.release();
            }
        }
    }

    @Override
    public void processFinish(String output) {
        upload_count++;
        if (upload_count == 3) {
            Toast.makeText(getApplicationContext(), "資料皆上傳完成", Toast.LENGTH_SHORT).show();
            upload_count = 0;
            for (int i=0; i<3; i++) {
                selectedFilePath[i] = null;
                tv_FileName[i].setText("");
            }
        }
    }

    private void propertySetting() {
        try {
            InputStreamReader inputReader = new InputStreamReader( getResources().getAssets().open("property.txt") );
            BufferedReader bufReader = new BufferedReader(inputReader);
            String line="";
            String Result="";
            while((line = bufReader.readLine()) != null) {
                Result += line;
            }

            this.orgData_URL = new JSONObject(new JSONObject(new JSONObject(Result).getString("Address")).getString("upload")).getString("OriginalData");
            this.cvtData_URL = new JSONObject(new JSONObject(new JSONObject(Result).getString("Address")).getString("upload")).getString("ConvertData");
            this.resultImg_URL = new JSONObject(new JSONObject(new JSONObject(Result).getString("Address")).getString("upload")).getString("ResultImg");
            Log.e("SERVER_URL",orgData_URL);
            Log.e("SERVER_URL",cvtData_URL);
            Log.e("SERVER_URL",resultImg_URL);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    // ------------------------- 圖片和媒體檔案存取權限 -------------------------------------
    public boolean checkPermissionREAD_EXTERNAL_STORAGE(final Context context) {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion >= android.os.Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        (Activity) context, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    showDialog("External storage", context, Manifest.permission.READ_EXTERNAL_STORAGE);
                } else {
                    ActivityCompat.requestPermissions(
                                    (Activity) context,
                                    new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
                                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                }
                return false;
            } else {
                return true;
            }

        } else {
            return true;
        }
    }

    public void showDialog(final String msg, final Context context, final String permission) {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
        alertBuilder.setCancelable(true)
                    .setTitle("Permission necessary")
                    .setMessage(msg + " permission is necessary")
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    ActivityCompat.requestPermissions((Activity) context,
                                            new String[] { permission },
                                            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                                }
                            })
                    .create()
                    .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // do your stuff
                } else {
                    Toast.makeText(getApplicationContext(), "存取圖片檔案功能被拒", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }


}
