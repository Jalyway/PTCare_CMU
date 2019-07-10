package com.example.ptcare_cmu;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import FISmain.FISMotionSample;

public class Download extends MainActivity {

    private static final int PICK_FILE_REQUEST = 1;
    private String selectedFilePath;
    private EditText userName;
    private Spinner sprMotion;
    private ProgressBar progressBar;
    private Button download_Btn,chooseFile_Btn;
    private TextView tvResult,fileName;
    String str = "偵測結果\n";
    private boolean connected = false;
    private String[] motions = {"請選擇","flex_ext","ABD ADD","int_ext_rot","pron_supin","rad_uln_dev"};

    Thread subThread;

    HttpClient httpClient = new DefaultHttpClient();
    HttpGet get;
    HttpResponse response;
    HttpEntity resEntity;
    String writer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.download);
        //
        userName = findViewById(R.id.userName);
        fileName = findViewById(R.id.fileName);
        sprMotion = findViewById(R.id.sprMotion);
        download_Btn = findViewById(R.id.download_btn);
        tvResult = findViewById(R.id.tv_result);
        progressBar = findViewById(R.id.progressBar);
        chooseFile_Btn=findViewById(R.id.bt_chooseFIle);


        progressBar.setVisibility(View.INVISIBLE);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, motions);
        sprMotion.setAdapter(adapter);
        sprMotion.setOnItemSelectedListener(selectedListener);

        chooseFile_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFileChooser();
            }
        });


        // 判斷是否有開啟網路連線
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() != NetworkInfo.State.CONNECTED &&
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() != NetworkInfo.State.CONNECTED) {
            //we are not connected to the network
            connected = false;
            Toast.makeText(this,"請開啟網路連線",Toast.LENGTH_LONG).show();
            finish();
        }

        //Toast.makeText(this,"Main Thread: "+Thread.currentThread().getName(),Toast.LENGTH_SHORT).show();
    }

    AdapterView.OnItemSelectedListener selectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            //Toast.makeText(getApplicationContext(),"which:"+ i, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {

        }
    };



    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            if (message.what == 1) {
                progressBar.setVisibility(View.GONE);
                tvResult.setText(message.obj.toString());
            }
            return false;
        }
    });


    public void downloadBtnLTR(View v) {

        if (! fileName.getText().toString().equals("")) {
            download_Btn.setEnabled(false);
            tvResult.setText("資料判斷中\n");
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(-1);

            // 創建一個新的 Thread
            subThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        get = new HttpGet("http://140.128.65.114:8000/PRRFISHome/PTC/fis/motionCriteria1.txt");
                        response = httpClient.execute(get);
                        resEntity = response.getEntity();
                        writer = EntityUtils.toString(resEntity);

                        try{
                            FileWriter fw = new FileWriter("/data/data/com.example.ptcare_cmu/motionCriteria1.txt", false);
                            BufferedWriter bw = new BufferedWriter(fw); //將BufferedWeiter與FileWrite物件做連結
                            bw.write(writer);
                            bw.newLine();
                            bw.close();
                        }catch(IOException e){
                            e.printStackTrace();
                        }
                    }catch(Exception e) {
                        Log.i("mytag", e.toString());
                    }
                    //
                    try {
                        get = new HttpGet("http://140.128.65.114:8000/PRRFISHome/PTC/fis/motionGuide1.txt");
                        response = httpClient.execute(get);
                        resEntity = response.getEntity();
                        writer = EntityUtils.toString(resEntity);

                        try{
                            FileWriter fw = new FileWriter("/data/data/com.example.ptcare_cmu/motionGuide1.txt", false);
                            BufferedWriter bw = new BufferedWriter(fw); //將BufferedWeiter與FileWrite物件做連結
                            bw.write(writer);
                            bw.newLine();
                            bw.close();
                        }catch(IOException e){
                            e.printStackTrace();
                        }
                    }catch(Exception e) {
                        Log.i("mytag", e.toString());
                    }
                    //
                    try {
                        get = new HttpGet("http://140.128.65.114:8000/PRRFISHome/PTC/fis/T01_flex_ext.fis");
                        response = httpClient.execute(get);
                        resEntity = response.getEntity();
                        writer = EntityUtils.toString(resEntity);
                        try{
                            FileWriter fw = new FileWriter("/data/data/com.example.ptcare_cmu/T01_flex_ext.fis", false);
                            BufferedWriter bw = new BufferedWriter(fw); //將BufferedWeiter與FileWrite物件做連結
                            bw.write(writer);
                            bw.newLine();
                            bw.close();
                        }catch(IOException e){
                            e.printStackTrace();
                        }
                    }catch(Exception e) {
                        Log.i("mytag", e.toString());
                    }
                    //-------------------------------------------------------------------------
                    FISMotionSample fisMotionSample=new FISMotionSample();
                    List<String> result=fisMotionSample.Recognition("/data/data/com.example.ptcare_cmu/T01_flex_ext.fis",
                            "/storage/emulated/0/Android/data/com.example.ptcare_cmu/files/"+fileName.getText(),///data/data/com.example.ptcare_cmu/ExtFlexMotionTest.csv
                            "/data/data/com.example.ptcare_cmu/motionGuide1.txt",
                            "/data/data/com.example.ptcare_cmu/motionCriteria1.txt");
                    for(int i=0; i<result.size(); i++) {
                        Log.e("Kenny", result.get(i));
                        str += result.get(i)+"\n";
                    }


                    mHandler.obtainMessage(1, str).sendToTarget();
                }
            });
            subThread.start();



//            while (true) {
//                if (! http.isAlive()) {
//                    tvResult.setText(str);
//                    progressBar.setVisibility(View.GONE);
//                    break;
//                }
//            }


            //Toast.makeText(this,"alive: "+http.isAlive(),Toast.LENGTH_SHORT).show();
            //Toast.makeText(getApplicationContext(),"結果:\n"+str,Toast.LENGTH_SHORT).show();
        }
        else
            Toast.makeText(this,"請輸入檔案名稱",Toast.LENGTH_SHORT).show();

    }


    @Override
    protected void onPause() {
        super.onPause();
        if (subThread != null)
            if (! subThread.isInterrupted())
                subThread.interrupt();
    }

    //
    private void showFileChooser() {
        Intent intent = new Intent();
        // sets the select file to all types of files
        intent.setType("text/*"); // file/* 改
        // intent.setType("*/*"); // file/* 改
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // allows to select data and return it
        intent.setAction(Intent.ACTION_GET_CONTENT);
        // starts new activity to select file and return data
        startActivityForResult(Intent.createChooser(intent, "Choose File to Upload.."), PICK_FILE_REQUEST);
    }
//--------------------------------------------------------------------------------------------------------------
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PICK_FILE_REQUEST) {
                if (data == null) {
                    //no data present
                    return;
                }
                Uri selectedFileUri = data.getData();
                //Toast.makeText(this, selectedFileUri.toString(), Toast.LENGTH_LONG).show();

                selectedFilePath = FilePath.getPath(this, selectedFileUri);

                if (selectedFilePath!=null && !selectedFilePath.equals("")) {
                    String filename = selectedFilePath.substring(selectedFilePath.lastIndexOf("/") + 1);
                    fileName.setText(filename);  //只顯示檔名
                }
                else
                    Toast.makeText(this, "Cannot upload file to server ", Toast.LENGTH_LONG).show();
            }
        }
    }
}
