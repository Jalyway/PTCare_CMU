package com.example.ptcare_cmu;

/*
 * Revised by xi-jun on 2019/5/6 at YZU.
 */

import android.app.ActivityManager;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import FISmain.FISMotionSample;

public class MainActivity extends AppCompatActivity {
    DownloadManager downloadManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //
        ImageButton btnPre1, btnPre2, btnPre3, btnPre4, btnPre5, btnPre6;
        btnPre1 = findViewById(R.id.imageButton1);
        btnPre2 = findViewById(R.id.imageButton2);
        btnPre3 = findViewById(R.id.imageButton3);
        btnPre4 = findViewById(R.id.imageButton4);
        btnPre5 = findViewById(R.id.imageButton5);
        btnPre6 = findViewById(R.id.imageButton6);
        btnPre1.setOnClickListener(btnListener);
        btnPre2.setOnClickListener(btnListener);
        btnPre3.setOnClickListener(btnListener);
        btnPre4.setOnClickListener(btnListener);
        btnPre5.setOnClickListener(btnListener);
        btnPre6.setOnClickListener(btnListener);

        Button btn_recog=findViewById(R.id.bt_recognition);
        btn_recog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //
                new Thread(new Runnable(){
                    @Override
                    public void run() {
                        try {
                            Document doc = Jsoup.connect("http://140.128.65.114:8000/PRRFISHome/PTC/fis/motionCriteria1.txt").get();
                            Elements elements = doc.select("body");
                            Log.i("mytag",elements.text());

                            try{
                                FileWriter fw = new FileWriter("/data/data/com.example.ptcare_cmu/motionCriteria1.txt", false);
                                BufferedWriter bw = new BufferedWriter(fw); //將BufferedWeiter與FileWrite物件做連結
                                String[] ss=elements.text().split("\\s");
                                for (int i=0; i<ss.length;i++) {
                                    bw.write(ss[i]+"\n");
                                }
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
                            Document doc = Jsoup.connect("http://140.128.65.114:8000/PRRFISHome/PTC/fis/motionGuide1.txt").get();
                            Elements elements = doc.select("body");
                            Log.i("mytag",elements.text());

                            try{
                                FileWriter fw = new FileWriter("/data/data/com.example.ptcare_cmu/motionGuide1.txt", false);
                                BufferedWriter bw = new BufferedWriter(fw); //將BufferedWeiter與FileWrite物件做連結
                                String[] ss=elements.text().split("\\n");
                                for (int i=0; i<ss.length;i++) {
                                    bw.write(ss[i]+"\n");
                                }
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
                        List<String> result=fisMotionSample.Recognition("/data/data/com.example.ptcare_cmu/FE_tds_change2_1.fis",
                                "/storage/emulated/0/Android/data/com.example.ptcare_cmu/files/201907011236.csv",///data/data/com.example.ptcare_cmu/ExtFlexMotionTest.csv
                                "/data/data/com.example.ptcare_cmu/motionGuide1.txt",
                                "/data/data/com.example.ptcare_cmu/motionCriteria1.txt");
                        for(int i=0; i<result.size();i++){
                            Log.e("Kenny",result.get(i));
                        }
                    }
                }).start();
            }
        });

    }

    View.OnClickListener btnListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch ( ((ImageButton) v).getId() ) {
                case R.id.imageButton1:
                    startActivity(new Intent(getApplicationContext(), EditMotion.class));  //動作
                    break;
                case R.id.imageButton2:
                    startActivity(new Intent(getApplicationContext(), DataBleMonitor.class));
                    break;
                case R.id.imageButton3:
                    startActivity(new Intent(getApplicationContext(), EditCriteria.class));  //準則
                    break;
                case R.id.imageButton4: //從網站接收JY-61產生的原始檔
                    startActivity(new Intent(getApplicationContext(), UploadRec.class)); //上傳資料
                    break;
                case R.id.imageButton5: //產生特徵檔
                    startActivity(new Intent(getApplicationContext(), ViewRecord.class));
                    break;
                case R.id.imageButton6: //下載
//                    intent.setClass(getApplicationContext(), DataTransfer.class); //
//                    startActivity(intent);
                    break;
            }
        }
    };



    // Option Menu 回首頁
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        ActivityManager manager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        String className = manager.getRunningTasks(1).get(0).topActivity.getClassName();
        if (! className.contains("MainActivity")) {
            getMenuInflater().inflate(R.menu.option_menu, menu);
            //Toast.makeText(this,"Not in MainActivity",Toast.LENGTH_SHORT).show();
        }
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.optionMenu:
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
