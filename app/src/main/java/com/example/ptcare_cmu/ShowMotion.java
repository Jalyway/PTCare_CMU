package com.example.ptcare_cmu;

/*
 * Created by js-mis on 2015/7/29.
 * Revised by xi-jun on 2019/5/9 at YZU.
 */

import android.content.Intent;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Map;

public class ShowMotion extends MainActivity implements DbConstants {

    private static final int Update_or_Delete = 1;
    private static final int ReQuery = 2;

    private DatabaseHelper dbHelper;
    private LinearLayout myLayout;
    private ListView listView;
    private SimpleAdapter simpleAdapter;

    private String mTypeID;
    private int pageSize = 1;              // The total number of pages
    private int currentPage = 1;           // The current page
    private int allRecorders = 0;          // Total number of records
    private int lineSize = 50;             // Each page shows 15 data
    private int lastItem = 0;              // Save the last record
    private List<Map<String,Object>> allData;  // Save the data adapter


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_motion);
        //
        myLayout = findViewById(R.id.LinearLayout);
        TextView textView = findViewById(R.id.textView);
        // 取得傳入資料
        Bundle bundle = this.getIntent().getExtras();
        mTypeID = bundle.getString("MotionTypeID");
        textView.setText(bundle.getString("Title"));


        showAllData();  // Display data
        Toast.makeText(this,"總筆數:"+String.valueOf(allData.size()),Toast.LENGTH_SHORT).show(); //顯示listView共有幾筆items

        // To determine the total number of pages
        pageSize = (allRecorders + currentPage - 1) / lineSize;
    }


    public void showAllData() {
        dbHelper = new DatabaseHelper(ShowMotion.this);
        MyCursor myCursor = new MyCursor(dbHelper.getReadableDatabase());  // Database query operation
        allRecorders = myCursor.getCount();                          // Total number of records query
        allData = myCursor.find(currentPage, lineSize, mTypeID);        // Remove the query data


        // 創建一新TextView: ...
        TextView loadInfo = new TextView(this);
        loadInfo.setText("... ");
        loadInfo.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        loadInfo.setTextSize(20.0f);
        LinearLayout loadLayout = new LinearLayout(this);
        loadLayout.addView(loadInfo, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        //loadLayout.setGravity(Gravity.CENTER);

        listView = new ListView(ShowMotion.this);
        listView.addFooterView(loadLayout);   // 將新TextView加到listView的最下方
        simpleAdapter = new SimpleAdapter(this, allData, R.layout.tab_info,
                                            new String[]{Col1,Col2,Col3},
                                            new int[]{R.id.mName,R.id.mDesc,R.id.mCode});
        listView.setAdapter(simpleAdapter);  // Set the display data
        myLayout.addView(listView);          // Additional assembly

        // Set up a rolling monitoring
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if ((lastItem == simpleAdapter.getCount()) && (currentPage < pageSize) && (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE)) {
                    currentPage++;                      // Modify the current page
                    listView.setSelection(lastItem);    // Set the display position
                    appendData();                       // Refresh the display data
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                lastItem = firstVisibleItem + visibleItemCount - 1;
            }

            //Append New Data
            private void appendData() {
                MyCursor cur = new MyCursor(dbHelper.getReadableDatabase());
                List<Map<String, Object>> newData = cur.find(currentPage, lineSize, mTypeID);
                allData.addAll(newData);              // Additional data
                simpleAdapter.notifyDataSetChanged();  // Update notification
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                TextView typeId = view.findViewById(R.id.mCode);
                if (typeId != null) {
                    //Toast.makeText(ShowMotion.this,typeId.getText().toString(),Toast.LENGTH_SHORT).show();
//                    Map<String, Object> m = allData.get(position);
//                    String s = m.get(Col1).toString();
//                    Toast.makeText(ShowMotion.this,"m.size:"+ s,Toast.LENGTH_LONG).show();

                    Intent itt = new Intent(getApplicationContext(), ModifyMotion.class);
                    itt.putExtra("mgTabId", String.valueOf(allData.get(position).get(Col5)));
                    //startActivity(itt);
                    startActivityForResult(itt,100);
                }
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //
        switch (resultCode) {
            case Update_or_Delete:
                Intent itt = new Intent(this, ShowMotion.class);
                Bundle bd = new Bundle();
                bd.putString("MotionTypeID",data.getExtras().getString("MotionTypeID"));
                bd.putString("Title",data.getExtras().getString("Title"));
                itt.putExtras(bd);
                startActivity(itt);
                finish();
                Toast.makeText(this,"Update_or_Delete",Toast.LENGTH_SHORT).show(); //
                break;
            case ReQuery:
                setResult(ReQuery);
                finish();
                break;
        }
    }
}
