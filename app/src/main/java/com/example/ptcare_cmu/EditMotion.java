package com.example.ptcare_cmu;

/*
 * Created by js-mis on 2017/9/12.
 * Revised by xi-jun on 2019/5/9 at YZU.
 */

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.Map;

public class EditMotion extends MainActivity {

    private static final int Update_or_Delete = 1;
    private static final int ReQuery = 2;

    private EditText mName, mDesc, mCode;
    private Button btnCreate, btnUpdate, btnDelete, btnCheck, btnQuery;
    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;
    private String[][] strArr;
    private int selectPosition;
    private int isMotionChange;
    private String[] itemList;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_motion);
        //
        mName = findViewById(R.id.editText1);   //動作名稱
        mDesc = findViewById(R.id.editText2);   //動作說明
        mCode = findViewById(R.id.editText3);   //動作代碼
        btnCreate = findViewById(R.id.button1); //新增
        btnUpdate = findViewById(R.id.button2); //更新
        btnDelete = findViewById(R.id.button3); //刪除
        btnCheck = findViewById(R.id.button4);  //檢視
        btnQuery = findViewById(R.id.button5);  //查詢
        Spinner sp = findViewById(R.id.spMotionType);

        //
        dbHelper = new DatabaseHelper(EditMotion.this);
        database = dbHelper.getWritableDatabase();
        MyMotion motion = new MyMotion(database);
        Map<String, String> motionTypeList = motion.getMotionType();
        strArr = new String[2][motionTypeList.size()];

        int i=0;
        for (Map.Entry<String, String> entry : motionTypeList.entrySet()) {
            strArr[0][i] = entry.getValue();
            strArr[1][i] = entry.getKey();
            i++;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, strArr[0]);
        sp.setAdapter(adapter);
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

        btnUpdate.setEnabled(false); //無須更新按鈕
        btnDelete.setEnabled(false); //無須刪除按鈕
    }

    // Listener for buttons click
    // 1.新增按鈕
    public void btnCreateLTR(View v) {
        if (!mName.getText().toString().equals("") && !mCode.getText().toString().equals("")) {
            if (hadCodeData(mCode.getText().toString())) {

                RadioGroup rg = findViewById(R.id.RadioGroup);  //RadioGroup id
                rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        // 是否為轉換動作
                        if (checkedId == R.id.rdbtn_N)
                            isMotionChange = 0;
                        if (checkedId == R.id.rdbtn_Y)
                            isMotionChange = 1;
                    }
                });

                //
                ContentValues cv = new ContentValues();
                cv.put("motion_name", mName.getText().toString());
                cv.put("motion_description", mDesc.getText().toString());
                cv.put("motion_code", mCode.getText().toString());
                cv.put("mtype_id", strArr[1][selectPosition]);
                cv.put("mtype_availability", isMotionChange);

                //
                database = dbHelper.getWritableDatabase();
                long id = database.insert("motion_guide",null, cv);
                if (id > 0) {
                    String strT = "新增紀錄"+id+" 動作名稱:"+mName.getText().toString()+" 動作代碼:"+mCode.getText().toString();
                    Toast.makeText(this, strT, Toast.LENGTH_LONG).show();
                }
                mName.setText("");
                mDesc.setText("");
                mCode.setText("");
                //mName.requestFocus();

                database.close();
            }
        }
        else
            Toast.makeText(this,"必填欄位不可空白",Toast.LENGTH_LONG).show();
    }

    // 4.檢視按鈕
    public void btnCheckLTR(View v) {
        itemList = new String[strArr[0].length+1];
        itemList[0] = "所有動作列表";
        for (int j=1; j<strArr[0].length+1; j++)
            itemList[j] = strArr[0][j-1];

        AlertDialog.Builder singleChoiceDialog = new AlertDialog.Builder(EditMotion.this);
        singleChoiceDialog.setTitle("請選擇要檢視的動作類別")
                          .setItems(itemList, new DialogInterface.OnClickListener() {
                              @Override
                              public void onClick(DialogInterface dialog, int which) {
                                  //當使用者點選對話框時，顯示使用者所點選的項目
                                  // which==0時,列出所有動作；否則指定選擇動作型態ID
                                  String mType = "";
                                  if (which != 0)
                                      mType = strArr[1][which-1];
                                  //Toast.makeText(EditMotion.this, "您選擇的是"+which, Toast.LENGTH_SHORT).show();

                                  Intent itt = new Intent(getApplicationContext(), ShowMotion.class);
                                  Bundle bd = new Bundle();
                                  bd.putString("MotionTypeID",mType);
                                  bd.putString("Title", itemList[which]);
                                  itt.putExtras(bd);
                                  //startActivity(itt);
                                  startActivityForResult(itt,100);
                              }
                          })
                          .setNegativeButton("取消", null)
                          .create()
                          .show();
    }

    // 5.查詢按鈕
    public void btnQueryLTR(View v) {
        if (!mName.getText().toString().equals("")) {
            database = dbHelper.getWritableDatabase();
            Cursor c = database.rawQuery("select _id from motion_guide where motion_name=?", new String[]{mName.getText().toString()});
            if (c.getCount() == 0)
                Toast.makeText(this, "查無此筆資料", Toast.LENGTH_SHORT).show();
            else {
                c.moveToFirst();
                //
                Intent intent = new Intent(this, ModifyMotion.class);
                intent.putExtra("mgTabId", String.valueOf(c.getInt(0)));
                startActivityForResult(intent,200);
            }
            c.close();
            database.close();
        }
        else if (!mCode.getText().toString().equals("")) {
            database = dbHelper.getWritableDatabase();
            Cursor c = database.rawQuery("select _id from motion_guide where motion_code=?", new String[]{mCode.getText().toString()});
            if (c.getCount() == 0)
                Toast.makeText(this, "查無資料", Toast.LENGTH_SHORT).show();
            else {
                c.moveToFirst();
                //
                Intent intent = new Intent(this, ModifyMotion.class);
                intent.putExtra("mgTabId", String.valueOf(c.getInt(0)));
                startActivityForResult(intent,200);
            }
            c.close();
            database.close();
        }
        else
            Toast.makeText(this, "請輸入欲查詢之動作名稱或代碼", Toast.LENGTH_SHORT).show();
    }

    // 檢查動作代碼是否重複
    private boolean hadCodeData(String mCode) {
        boolean retValue = false;
        database = dbHelper.getWritableDatabase();
        try {
            Cursor c = database.rawQuery("select _id from motion_guide where motion_code=?", new String[]{mCode});
            if (c.getCount() == 0)
                retValue = true;
            else
                Toast.makeText(this, "動作代碼不可重複", Toast.LENGTH_LONG).show();

            c.close();
            database.close();

        } catch (SQLiteException e) {
            Log.w("TAG", e.getMessage());
        }

        return retValue;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //
        if (resultCode==Update_or_Delete || resultCode==ReQuery) {
            startActivity(new Intent(this, EditMotion.class));
            finish();

            Toast.makeText(this,"Retuen to 新增動作",Toast.LENGTH_SHORT).show();
        }
    }
}
