package com.example.ptcare_cmu;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import FISmain.FISMotionSample;

public class Download extends MainActivity {

    private EditText userName, fileName;
    private Spinner sprMotion;
    private ProgressBar progressBar;
    private Button download_Btn;
    private TextView tvResult;
    String str = "\n偵測結果\n";
    private boolean connected = false;
    private String[] motions = {"請選擇","flex_ext","ABD ADD","int_ext_rot","pron_supin","rad_uln_dev"};

    Thread subThread;


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


        progressBar.setVisibility(View.INVISIBLE);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, motions);
        sprMotion.setAdapter(adapter);
        sprMotion.setOnItemSelectedListener(selectedListener);


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
                        Document doc = Jsoup.connect("http://140.128.65.114:8000/PRRFISHome/PTC/fis/motionCriteria1.txt").get();
                        Elements elements = doc.select("body");
                        Log.i("mytag",elements.text());

                        try {
                            FileWriter fw = new FileWriter("/data/data/com.example.ptcare_cmu/motionCriteria1.txt", false);
                            BufferedWriter bw = new BufferedWriter(fw); //將BufferedWeiter與FileWrite物件做連結
                            String[] ss=elements.text().split("\\s");
                            for (int i=0; i<ss.length; i++) {
                                bw.write(ss[i]+"\n");
                            }
                            bw.newLine();
                            bw.close();
                        } catch(IOException e) {
                            e.printStackTrace();
                        }
                    } catch(Exception e) {
                        Log.i("mytag", e.toString());
                    }
                    //
                    try {
                        Document doc = Jsoup.connect("http://140.128.65.114:8000/PRRFISHome/PTC/fis/motionGuide1.txt").get();
                        Elements elements = doc.select("body");
                        Log.i("mytag",elements.text());

                        try {
                            FileWriter fw = new FileWriter("/data/data/com.example.ptcare_cmu/motionGuide1.txt", false);
                            BufferedWriter bw = new BufferedWriter(fw); //將BufferedWeiter與FileWrite物件做連結
                            String[] ss=elements.text().split("\\n");
                            for (int i=0; i<ss.length; i++) {
                                bw.write(ss[i]+"\n");
                            }
                            bw.newLine();
                            bw.close();
                        } catch(IOException e) {
                            e.printStackTrace();
                        }
                    } catch(Exception e) {
                        Log.i("mytag", e.toString());
                    }
                    //-------------------------------------------------------------------------
                    FISMotionSample fisMotionSample = new FISMotionSample();
                    List<String> result = fisMotionSample.Recognition("/data/data/com.example.ptcare_cmu/FE_tds_change2_1.fis",
                            "/data/data/com.example.ptcare_cmu/ExtFlexMotionTest.csv",
                            "/data/data/com.example.ptcare_cmu/motionGuide1.txt",
                            "/data/data/com.example.ptcare_cmu/motionCriteria1.txt");
                    for(int i=0; i<result.size(); i++) {
                        Log.e("Kenny", result.get(i));
                        str += result.get(i);
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

}
