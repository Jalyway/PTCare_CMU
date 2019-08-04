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
import android.provider.Settings;
import android.support.annotation.Nullable;
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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import FISmain.FISMotionSample;

public class Download extends MainActivity {
    public static int[] trueMotion_s_id;
    public static List<Double> result;
    private static final int PICK_FILE_REQUEST = 1;
    private String selectedFilePath;
    private EditText userName;
    private Spinner sprMotion;
    private ProgressBar progressBar;
    private Button download_Btn,chooseFile_Btn;
    private TextView tvResult,fileName;
    String str = "偵測結果\n";
    private boolean connected = false;
    private String[] motions = {"請選擇","flex_ext","ABD_ADD","int_ext_rot","pron_supin","rad_uln_dev"};
    private String[] fis_motions = {"請選擇","flex_ext.fis","ABD_ADD.fis","int_ext_rot.fis","pron_supin.fis","rad_uln_dev.fis"};
    private String[] criteria_motions = {"請選擇","motionCriteria_FE.txt","motionCriteria_ABD.txt","motionCriteria_ERIR.txt","motionCriteria_pron.txt","motionCriteria_ul.ra.txt"};

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
            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
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

                Intent intent = new Intent(getApplicationContext(), LineChartActivity.class);
                startActivity(intent);
            }
            return false;
        }
    });


    public void downloadBtnLTR(View v) {

        if (! fileName.getText().toString().equals("") && !sprMotion.getSelectedItem().toString().equals(motions[0])) {
            download_Btn.setEnabled(false);
            tvResult.setText("資料判斷中\n");
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(-1);

            // 創建一個新的 Thread
            subThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        get = new HttpGet("http://192.168.43.181:8084/PRRFISHome/PTC/fis/T01_"+criteria_motions[sprMotion.getSelectedItemPosition()]);
                        response = httpClient.execute(get);
                        resEntity = response.getEntity();
                        writer = EntityUtils.toString(resEntity);

                        try{
                            FileWriter fw = new FileWriter("/data/data/com.example.ptcare_cmu/motionCriteria.txt", false);
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
                        get = new HttpGet("http://192.168.43.181:8084/PRRFISHome/PTC/fis/T01_motionGuide.txt");
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
                        get = new HttpGet("http://192.168.43.181:8084/PRRFISHome/PTC/fis/T01_"+fis_motions[sprMotion.getSelectedItemPosition()]);
                        response = httpClient.execute(get);
                        resEntity = response.getEntity();
                        writer = EntityUtils.toString(resEntity);
                        try{
                            FileWriter fw = new FileWriter("/data/data/com.example.ptcare_cmu/"+fis_motions[sprMotion.getSelectedItemPosition()], false);
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
                    //---------------------------------------------------------------------------------------------------------------------------------
                    try {
                        Scanner sc2 = new Scanner(new File("/data/data/com.example.ptcare_cmu/motionGuide1.txt"));
                        ArrayList motList = new ArrayList();
                        while(sc2.hasNext()) {
                            String[] motIDs = sc2.nextLine().split("\\|");
                            motList.add(motIDs);
                        }
                        Scanner sc = new Scanner(new File("/data/data/com.example.ptcare_cmu/motionCriteria.txt"));
                        int motionCycle = Integer.parseInt(sc.nextLine());
                        String[] mot_do = sc.nextLine().split("\\|");
                        String[] strSchedule = sc.nextLine().split(",");    //總秒數為最後一個index >>  strSchedule[strSchedule.size()-1]
                        trueMotion_s_id=new int[Integer.parseInt(strSchedule[strSchedule.length-1])];     //記錄每一秒應該要是甚麼動作，也就是說index1>>第1秒時正在執行的動作代碼
                        int[] mot_idx_match_to_id=new int[mot_do.length];
                        for(int i = 0; i < mot_do.length; ++i) {
                            for(int j = 0; j < motList.size(); ++j) {
                                String[] motIDs = (String[])motList.get(j);
                                if (mot_do[i].equals(motIDs[0])) {
                                    mot_idx_match_to_id[i] =Integer.parseInt(motIDs[1]);
                                    break;
                                }
                            }
                        }
                        for(int times =0;times<motionCycle;times++) {           //以下動作重複次數
                            for (int diff_motion = 0; diff_motion < mot_idx_match_to_id.length; diff_motion++) {
                                for (int current_t = Integer.parseInt(strSchedule[diff_motion]); current_t < Integer.parseInt(strSchedule[diff_motion + 1]); current_t++)
                                    trueMotion_s_id[current_t] = mot_idx_match_to_id[diff_motion];
                            }
                        }
                        Log.e("resultNum","");
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    //-------------------------------------------------------------------------
                    FISMotionSample fisMotionSample=new FISMotionSample();
                    result=fisMotionSample.Recognition("/data/data/com.example.ptcare_cmu/"+fis_motions[sprMotion.getSelectedItemPosition()],
                            "/storage/emulated/0/Android/data/com.example.ptcare_cmu/files/"+fileName.getText().toString().split("\\.")[0]+"/"+fileName.getText(),///data/data/com.example.ptcare_cmu/ExtFlexMotionTest.csv
                            "/data/data/com.example.ptcare_cmu/motionGuide1.txt",
                            "/data/data/com.example.ptcare_cmu/motionCriteria.txt");
                    for(int i=0; i<result.size(); i++) {
                        Log.e("Kenny", String.valueOf(result.get(i)));
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
            Toast.makeText(this,"請輸入檔案名稱及辨識動作",Toast.LENGTH_SHORT).show();

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
