package com.example.ptcare_cmu;

/*
 * Created by js-mis on 2015/7/29.
 * Revised by xi-jun on 2019/5/14 at YZU.
 */

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class ShowCriteria extends AppCompatActivity {

    private static final int Update_or_Delete = 1;
    private static final int ReQuery = 2;

    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;
    private String criteriaID;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_criteria);
        //
        TextView title = findViewById(R.id.textView);
        TextView cycle = findViewById(R.id.cycle_times);
        TextView mCode = findViewById(R.id.motion_code);
        TextView mName = findViewById(R.id.motion_name);
        TextView mTime = findViewById(R.id.motion_time);
        Button btnCreate = findViewById(R.id.button01);
        Button btnUpdate = findViewById(R.id.button02);
        Button btnDelete = findViewById(R.id.button03);
        Button btnCheck = findViewById(R.id.button04);
        Button btnReQuery = findViewById(R.id.button05);

        //
        Bundle bundle = getIntent().getExtras();
        title.setText(bundle.getString("Title"));
        criteriaID = bundle.getString("CriteriaID"); //取得所選準則
        Toast.makeText(this,"Criteria Id: "+criteriaID, Toast.LENGTH_SHORT).show(); //

        dbHelper = new DatabaseHelper(this);

        String[] textShow = getCriteria(criteriaID);
        cycle.setText(textShow[0]);  //循環次數
        mCode.setText(textShow[1]);  //動作代碼
        mTime.setText(textShow[2]);  //動作秒數
        mName.setText(textShow[3]);  //動作名稱


        btnCreate.setEnabled(false);  //無須新增按鈕
        btnUpdate.setEnabled(false);  //無須更新按鈕
        btnCheck.setEnabled(false);   //無須檢視按鈕
    }

    // 取得準則的各項資訊
    private String[] getCriteria(String cId) {

        database = dbHelper.getWritableDatabase();
        String sql = "SELECT *  from criteria where _id = ?";
        Cursor result = database.rawQuery(sql, new String[]{cId});
        String[] criteria = new String[4];

        for (result.moveToFirst(); !result.isAfterLast(); result.moveToNext()) {
            criteria[0] = result.getString(2);  //循環次數
            criteria[1] = result.getString(3);  //動作代碼
            criteria[2] = result.getString(4);  //動作秒數
        }
        result.close();

        String[] strArray = criteria[1].split(", ");
        String sql2;
        Cursor result2;
        String mCodeName = "";
        for (int i=0; i<strArray.length; i++) {
            sql2 = "SELECT motion_name  from motion_guide where motion_code ='"+ strArray[i] +"'";
            result2 = database.rawQuery(sql2,null);
            result2.moveToFirst();
            mCodeName += result2.getString(0) + ", ";
            result2.close();
        }
        criteria[3] = mCodeName.substring(0, mCodeName.length()-2);

        return criteria;
    }

    // Listener for buttons click
    // 3.刪除按鈕
    public void btnCDeleteLTR(View v) {
        AlertDialog.Builder confirmDialog = new AlertDialog.Builder(ShowCriteria.this);
        confirmDialog.setTitle("提示訊息")
                     .setMessage("是否確定要刪除此項準則?")
                     .setPositiveButton("確認", new DialogInterface.OnClickListener() {
                         @Override
                         public void onClick(DialogInterface dialog, int which) {
                             database = dbHelper.getWritableDatabase();
                             database.delete("criteria","_id="+ criteriaID,null);
                             Toast.makeText(getApplicationContext(),"資料已刪除",Toast.LENGTH_SHORT).show();

                             setResult(Update_or_Delete, new Intent());
                             finish();
                         }
                     })
                     .setNegativeButton("取消",null)
                     .create()
                     .show();
    }

    // 5.重查詢按鈕
    public void btnCReQueryLTR(View v) {
        setResult(ReQuery, new Intent());
        finish();
    }
}
