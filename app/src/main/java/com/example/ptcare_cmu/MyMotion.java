package com.example.ptcare_cmu;

/*
 * Created by js-mis on 2016/1/27.
 * Revised by xi-jun on 2019/5/7 at YZU.
 */

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.LinkedHashMap;
import java.util.Map;

public class MyMotion {
    private SQLiteDatabase database;

    // Construction method
    public MyMotion(SQLiteDatabase db) {
        this.database = db;
    }

    //取得對應ID的動作
    public String[] getMotion(String tabId) {
        String[] motion = new String[5];

        String sql = "SELECT  motion_name , motion_description , motion_code, "
                   + "mtype_id, mtype_availability  from motion_guide where _id='"
                   + tabId +"'" ;
        Cursor c = database.rawQuery(sql, null);

        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            motion[0] = c.getString(0);
            motion[1] = c.getString(1);
            motion[2] = c.getString(2);
            motion[3] = c.getString(3);
            motion[4] = String.valueOf(c.getInt(4));
        }
        c.close();
        return motion;
    }

    //取得動作類別，代碼及名稱
    public Map<String, String> getMotionType() {
        Map<String, String> motionTypeList = new LinkedHashMap<>();  //LinkedHashMap可依插入順序排序

        String sql = "SELECT mtype_id, mtype_name  from motion_type order by _id";
        Cursor c = database.rawQuery(sql, null);
        
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            motionTypeList.put(c.getString(0), c.getString(1));
        }

        c.close();
        database.close();

        return motionTypeList;
    }

    //取得所有動作
    public Map<String, String> getMotionAll() {
        Map<String, String> motionList = new LinkedHashMap<>();

        String sql = "SELECT  motion_code , motion_name  " + "  from motion_guide ";
        Cursor c = database.rawQuery(sql, null);
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext())
            motionList.put(c.getString(0), c.getString(1));
        c.close();

        return motionList;
    }


    //取得所有準則
    public Map<String, String> getCriteriaAll() {
        Map<String, String> criteriaList = new LinkedHashMap<>();

        String sql = "SELECT _id , criteria_name " + "  from criteria ";

        Cursor c = database.rawQuery(sql, null);
        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext())
            criteriaList.put(c.getString(0), c.getString(1));
        c.close();

        return criteriaList;
    }

}
