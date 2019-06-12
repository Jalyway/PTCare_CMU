package com.example.ptcare_cmu;

/*
 * Created by js-mis on 2017/9/12.
 * Revised by xi-jun on 2019/5/9 at YZU.
 */

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.Map;

public class ModifyMotion extends MainActivity {

    private static final int Update_or_Delete = 1;
    private static final int ReQuery = 2;

    private EditText mName, mDesc, mCode;
    private Button btnCreate, btnUpdate, btnDelete, btnCheck, btnReQuery;
    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;

    private String tabId;
    private String[][] strArr;
    private int selectPosition;
    private int isMotionChange;

    private AlertDialog.Builder confirmDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.modify_motion);
        //
        mName = findViewById(R.id.editText1);   //動作名稱
        mDesc = findViewById(R.id.editText2);   //動作說明
        mCode = findViewById(R.id.editText3);   //動作代碼
        btnCreate = findViewById(R.id.button1); //新增
        btnUpdate = findViewById(R.id.button2); //更新
        btnDelete = findViewById(R.id.button3); //刪除
        btnCheck = findViewById(R.id.button4);  //檢視
        btnReQuery = findViewById(R.id.button5);  //查詢
        RadioGroup rg = findViewById(R.id.RadioGroup);
        Spinner sp = findViewById(R.id.spMotionType);

        //
        tabId = this.getIntent().getStringExtra("mgTabId");
        Toast.makeText(this,tabId,Toast.LENGTH_SHORT).show();  //顯示選擇第幾個 item

        //
        dbHelper = new DatabaseHelper(ModifyMotion.this);
        database = dbHelper.getWritableDatabase();
        MyMotion motion = new MyMotion(database);
        String[] motionList = motion.getMotion(tabId);
        Map<String, String> motionTypeList = motion.getMotionType();

        strArr = new String[2][motionTypeList.size()];
        String mTypeId = motionList[3];   //動作類別Id
        String isChange = motionList[4];  //是否為轉換動作

        int i = 0, selected = 0;
        for (Map.Entry<String, String> entry : motionTypeList.entrySet()) {
            strArr[0][i] = entry.getValue();
            strArr[1][i] = entry.getKey();
            if (entry.getKey().equals(mTypeId))
                selected = i;
            i++;
        }

        // 設定對應欄位文字
        mName.setText(motionList[0]);
        mDesc.setText(motionList[1]);
        mCode.setText(motionList[2]);
        rg.check(isChange.equals("0") ? R.id.rdbtn_N : R.id.rdbtn_Y);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, strArr[0]);
        sp.setAdapter(adapter);
        sp.setSelection(selected);
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectPosition = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectPosition = 0;
            }
        });

        btnCreate.setEnabled(false); //無須新增按鈕
        btnCheck.setEnabled(false);  //無須檢視按鈕
    }

    // Listener for buttons click
    // 2.更新按鈕
    public void btnUpdateLTR(View v) {
        if (!mName.getText().toString().equals("") && !mCode.getText().toString().equals("")) {

            RadioGroup rg = findViewById(R.id.RadioGroup);  //RadioGroup id
            rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    if (checkedId == R.id.rdbtn_N)
                        isMotionChange = 0;
                    if (checkedId == R.id.rdbtn_Y)
                        isMotionChange = 1;
                }
            });

            //
            confirmDialog = new AlertDialog.Builder(ModifyMotion.this);
            confirmDialog.setTitle("提示訊息")
                         .setMessage("是否確定要更新動作?")
                         .setPositiveButton("確認", new DialogInterface.OnClickListener() {
                             @Override
                             public void onClick(DialogInterface dialog, int which) {

                                 ContentValues cv = new ContentValues();
                                 cv.put("motion_name", mName.getText().toString());
                                 cv.put("motion_description", mDesc.getText().toString());
                                 cv.put("motion_code", mCode.getText().toString());
                                 cv.put("mtype_id", strArr[1][selectPosition]);
                                 cv.put("mtype_availability", isMotionChange);

                                 database = dbHelper.getWritableDatabase();
                                 database.update("motion_guide", cv, "_id=" + tabId, null);
                                 Toast.makeText(ModifyMotion.this,"修改紀錄\n" + "動作名稱:"+mName.getText().toString()
                                                             + "動作代碼:"+mCode.getText().toString(), Toast.LENGTH_LONG).show();

                                 //mName.setText("");
                                 //mDesc.setText("");
                                 //mCode.setText("");

                                 //
                                 Intent rpInt = new Intent();
                                 Bundle bundle = new Bundle();
                                 bundle.putString("MotionTypeID", strArr[1][selectPosition]); //讀取動作類別?
                                 bundle.putString("Title", strArr[0][selectPosition]);
                                 rpInt.putExtras(bundle);
                                 setResult(Update_or_Delete, rpInt);

                                 finish();
                                 //Toast.makeText(ModifyMotion.this,"MotionTypeID: "+strArr[1][selectPosition],Toast.LENGTH_LONG).show();
                             }
                         })
                         .setNegativeButton("取消", null)
                         .create()
                         .show();

        }
    }

    // 3.刪除按鈕
    public void btnDeleteLTR(View v) {
        if (!mName.getText().toString().equals("") && !mCode.getText().toString().equals("")) {
            confirmDialog = new AlertDialog.Builder(ModifyMotion.this);
            confirmDialog.setTitle("提示訊息")
                         .setMessage("是否確定要刪除?")
                         .setPositiveButton("確認", new DialogInterface.OnClickListener() {
                             @Override
                             public void onClick(DialogInterface dialog, int which) {

                                 database = dbHelper.getWritableDatabase();
                                 database.delete("motion_guide","_id="+ tabId,null);
                                 Toast.makeText(getApplicationContext(),"資料已刪除",Toast.LENGTH_SHORT).show();

                                 //mName.setText("");
                                 //mDesc.setText("");
                                 //mCode.setText("");

                                 //
                                 Intent rpInt = new Intent();
                                 Bundle bundle = new Bundle();
                                 bundle.putString("MotionTypeID", strArr[1][selectPosition]); //讀取動作類別?
                                 bundle.putString("Title", strArr[0][selectPosition]);
                                 rpInt.putExtras(bundle);
                                 setResult(Update_or_Delete, rpInt);

                                 finish();
                             }
                         })
                         .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                             @Override
                             public void onClick(DialogInterface dialog, int which) {
                                 dialog.dismiss();
                             }
                         })
                         .create()
                         .show();
        }
        else
            Toast.makeText(this,"動作名稱及代碼不可空白",Toast.LENGTH_SHORT).show();
    }

    // 5.重查詢按鈕
    public void btnReQueryLTR(View v) {
        setResult(ReQuery);
        finish();
    }
}
