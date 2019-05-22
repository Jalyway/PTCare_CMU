package com.example.ptcare_cmu;

/*
 * Revised by xi-jun on 2019/5/6 at YZU.
 */

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

public class MainActivity extends AppCompatActivity {

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
    }

    View.OnClickListener btnListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch ( ((ImageButton) v).getId() ) {
                case R.id.imageButton1:
                    startActivity(new Intent(getApplicationContext(), EditMotion.class));  //動作
                    break;
                case R.id.imageButton2:
                    startActivity(new Intent(getApplicationContext(), BluetoothMonitor.class));
                    break;
                case R.id.imageButton3:
                    startActivity(new Intent(getApplicationContext(), EditCriteria.class));  //準則
                    break;
                case R.id.imageButton4: //從網站接收JY-61產生的原始檔
//                    intent.setClass(getApplicationContext(), UploadRec.class); //上傳資料
                    startActivity(new Intent(getApplicationContext(), UploadRec.class));
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
        getMenuInflater().inflate(R.menu.option_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.optionMenu:
                startActivity(new Intent(this, MainActivity.class));
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
