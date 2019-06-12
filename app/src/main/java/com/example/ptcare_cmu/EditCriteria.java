package com.example.ptcare_cmu;

/*
 * Created by js-mis on 2017/9/12.
 * Revised by xi-jun on 2019/5/14 at YZU.
 */

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.Map;

public class EditCriteria extends MainActivity {

    private static final int Update_or_Delete = 1;
    private static final int ReQuery = 2;

    int[] sprID = new int[]{R.id.spinner1,R.id.spinner2,R.id.spinner3,R.id.spinner4,R.id.spinner5,R.id.spinner6};
    int[] etID = new int[]{R.id.sec1,R.id.sec2,R.id.sec3,R.id.sec4,R.id.sec5,R.id.sec6};
    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;

    private EditText cName, cCycles;
    private Button btnCreate, btnUpdate, btnDelete, btnCheck, btnQuery;
    private Spinner[] spinner = new Spinner[sprID.length]; //長度為6
    private EditText[] etSec = new EditText[etID.length];  //長度為6
    private Map<String, String> criteriaList;
    private String[][] sprMotion, strCriteria;
    private int[] selectedID = new int[sprID.length]; //長度為6

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_criteria);
        //
        cName = findViewById(R.id.editText1);
        cCycles = findViewById(R.id.editText2);
        btnCreate = findViewById(R.id.button1); //新增
        btnUpdate = findViewById(R.id.button2); //更新
        btnDelete = findViewById(R.id.button3); //刪除
        btnCheck = findViewById(R.id.button4);  //檢視
        btnQuery = findViewById(R.id.button5);  //查詢
        //
        dbHelper = new DatabaseHelper(this);
        database = dbHelper.getWritableDatabase();

        MyMotion motion = new MyMotion(database);
        Map<String, String> motionList = motion.getMotionAll();
        sprMotion = new String[2][motionList.size()+1];
        //Toast.makeText(this,String.valueOf(motionList.size()),Toast.LENGTH_SHORT).show();  //20

        sprMotion[0][0] = "請選擇動作";
        sprMotion[1][0] = "";
        int i = 1;
        for (Map.Entry<String, String> entry : motionList.entrySet()) {
            sprMotion[0][i] = entry.getValue();
            sprMotion[1][i] = entry.getKey();
            i++;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, sprMotion[0]);

        for (int j=0; j<spinner.length; j++) {
            spinner[j] = findViewById(sprID[j]);
            spinner[j].setAdapter(adapter);
            spinner[j].setOnItemSelectedListener(selectedListener);
            etSec[j] = findViewById(etID[j]);
            etSec[j].setEnabled(false); //預設無法輸入
        }

        //
        btnUpdate.setEnabled(false);  //無須更新按鈕
        btnDelete.setEnabled(false);  //無須刪除按鈕
    }

    AdapterView.OnItemSelectedListener selectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            for (int i=0; i<sprID.length; i++) {
                if (parent.getId() == sprID[i]) {
                    selectedID[i] = position;
                    if (position == 0) {
                        etSec[i].setText("");
                        etSec[i].setEnabled(false);
                    }
                    else
                        etSec[i].setEnabled(true);
                }
            }


//            switch (parent.getId()) {
//                case R.id.spinner1:
//                    selectedID[0] = position;
//                    if (position!=0) etSec[0].setEnabled(true);
//                    break;
//                case R.id.spinner2:
//                    selectedID[1] = position;
//                    if (position!=0) etSec[1].setEnabled(true);
//                    break;
//                case R.id.spinner3:
//                    selectedID[2] = position;
//                    if (position!=0) etSec[2].setEnabled(true);
//                    break;
//                case R.id.spinner4:
//                    selectedID[3] = position;
//                    if (position!=0) etSec[3].setEnabled(true);
//                    break;
//                case R.id.spinner5:
//                    selectedID[4] = position;
//                    if (position!=0) etSec[4].setEnabled(true);
//                    break;
//                case R.id.spinner6:
//                    selectedID[5] = position;
//                    if (position!=0) etSec[5].setEnabled(true);
//                    break;
//            }

        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            //
        }
    };

    // Listener for buttons click
    // 1.新增按鈕
    public void btnCCreateLTR(View v) {
        if (!cName.getText().toString().equals("") && !cCycles.getText().toString().equals("")) {
            //檢查是否至少選一項動作
            String val = "";
            for (int i=0; i<selectedID.length; i++)
                val += sprMotion[1][selectedID[i]];  //各動作代碼 motion_code
            //Toast.makeText(this, "動作代碼:"+val, Toast.LENGTH_LONG).show(); //

            if (! val.equals("")) {
                String resultCode = "";
                int totalSec = 0;
                String resultSec = "0, ";
                int motionNum = 0, secNum = 0;
                for (int i=0; i<selectedID.length; i++) {
                    if (! sprMotion[1][selectedID[i]].equals("")) {
                        motionNum++;  //計算共選擇幾個動作
                        if (! etSec[i].getText().toString().equals("")) {
                            resultCode += sprMotion[1][selectedID[i]] + ", ";  //所有動作代碼
                            totalSec += Integer.parseInt(etSec[i].getText().toString());  //總秒數(持續累加)
                            resultSec += String.valueOf(totalSec) + ", ";  //各動作秒數以字串來記錄
                            secNum++;  //計算共有填入幾個秒數
                            //Toast.makeText(this,String.valueOf(resultSec),Toast.LENGTH_LONG).show(); //
                        }
                        else
                            Toast.makeText(this,"請填入第" + String.valueOf(i+1) + "個動作的秒數",Toast.LENGTH_SHORT).show();
                    }
                }

                //Toast.makeText(this,"motionNum:" + String.valueOf(motionNum),Toast.LENGTH_SHORT).show();
                //Toast.makeText(this,"secNum:" + String.valueOf(secNum),Toast.LENGTH_SHORT).show();
                // 確定選擇的動作及填入的秒數兩者數目一樣，再新增準則
                if (motionNum == secNum) {
                    //新增一筆準則資料
                    ContentValues cv = new ContentValues();
                    cv.put("criteria_name", cName.getText().toString());
                    cv.put("criteria_times", cCycles.getText().toString());  //循環次數
                    cv.put("criteria_motion_code", resultCode.substring(0, resultCode.length()-2)); //動作代碼
                    cv.put("criteria_motion_time", resultSec.substring(0, resultSec.length()-2));   //動作秒數
                    long id = database.insert("criteria", null, cv);
                    if (id > 0) {
                        Toast.makeText(this,"新增紀錄"+id+" 準則名稱:"+cName.getText().toString(),Toast.LENGTH_LONG).show();
                        cName.setText("");
                        cCycles.setText("");
                        for (Spinner spinner : spinner)
                            spinner.setSelection(0);
                    }
                    else
                        Toast.makeText(this,"新增資料失敗",Toast.LENGTH_LONG).show();
                }
            }
            else
                Toast.makeText(this, "沒有選動作, 無法新增", Toast.LENGTH_LONG).show();
        }
        else
            Toast.makeText(this,"必填欄位不可空白",Toast.LENGTH_LONG).show();
    }

    // 4.檢視按鈕
    public void btnCCheckLTR(View v) {

        database = dbHelper.getWritableDatabase();
        MyMotion motion = new MyMotion(database);
        criteriaList = motion.getCriteriaAll();
        strCriteria = new String[2][criteriaList.size()];  //準則清單列表

        int i = 0;
        for (Map.Entry<String, String> entry : criteriaList.entrySet()) {
            strCriteria[0][i] = entry.getValue();
            strCriteria[1][i] = entry.getKey();
            i++;
        }

        //String[] itemList = strCriteria[0];

        if (strCriteria[0].length == 0)
            Toast.makeText(this,"不存在任何準則資料",Toast.LENGTH_LONG).show();
        else {
            AlertDialog.Builder singleChoiceDialog = new AlertDialog.Builder(this, R.style.Theme_AppCompat_Light_Dialog_Alert);
            singleChoiceDialog.setTitle("請選擇要檢視的準則")
                    .setItems(strCriteria[0], new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //當使用者點選對話框時，顯示使用者所點選的項目
                            Intent intent = new Intent(getApplicationContext(), ShowCriteria.class);
                            Bundle bundle = new Bundle();
                            bundle.putString("CriteriaID", strCriteria[1][which]);
                            bundle.putString("Title", strCriteria[0][which]);
                            intent.putExtras(bundle);
                            startActivityForResult(intent,300);
                            //finish();
                        }
                    })
                    .setNegativeButton("取消", null)
                    .create()
                    .show();
        }
    }

    // 5.查詢按鈕
    public void btnCQueryLTR(View v) {
        if (!cName.getText().toString().equals("")) {
            database = dbHelper.getWritableDatabase();
            Cursor c = database.rawQuery("select _id from criteria where criteria_name like ?", new String[]{cName.getText().toString()});
            if (c.getCount() == 0)
                Toast.makeText(this, "查無資料", Toast.LENGTH_SHORT).show();
            else {
                c.moveToFirst();
                //
                Intent intent = new Intent(this, ShowCriteria.class);
                intent.putExtra("Title", cName.getText().toString());
                intent.putExtra("CriteriaID", String.valueOf(c.getInt(0)));
                startActivityForResult(intent,400);
            }
            c.close();
            database.close();
        }
        else
            Toast.makeText(this, "請輸入欲查詢之準則名稱", Toast.LENGTH_SHORT).show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //
        if (resultCode==Update_or_Delete || resultCode==ReQuery) {
            startActivity(new Intent(this, EditCriteria.class));
            finish();
        }
    }

}
