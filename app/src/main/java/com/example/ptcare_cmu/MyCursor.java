package com.example.ptcare_cmu;

/*
 * Created by js-mis on 2015/7/27.
 * Revised by xi-jun on 2019/5/9 at YZU.
 */

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyCursor implements DbConstants {
    private SQLiteDatabase database;

    public MyCursor(SQLiteDatabase db) {
        this.database = db;
    }

    public int getCount() {										      // Returns the number of records
        int count = 0 ; 										      // Save to return results
        String sql = "SELECT COUNT(_id) FROM " + TABLE_NAME ;	      // Query SQL
        Cursor result = database.rawQuery(sql, null);	  // Query
        for (result.moveToFirst(); !result.isAfterLast(); result.moveToNext()) {
            count = result.getInt(0) ;					  // Remove the query results
        }
        result.close();
        return count ;
    }

    // Query data table
    public List<Map<String,Object>> find(int currentPage, int lineSize, String condStr) {
        List<Map<String,Object>> all = new ArrayList<>() ;      // The definition of List set
        Cursor result;
        if(condStr.trim().equals("")) {
            String sql = "SELECT " + Col1 + "," + Col2 + "," + Col3 + "," + Col5 +" FROM  " + TABLE_NAME + "  LIMIT ?,? ";

            String selectionArgs[] = new String[]{
                    String.valueOf((currentPage - 1) * lineSize),
                    String.valueOf(lineSize)};                // The query parameters

            result = database.rawQuery(sql,selectionArgs);
        }
        else {
            String sql = "SELECT " + Col1 + "," + Col2 + "," + Col3 + ","+ Col5 +"  FROM  " + TABLE_NAME + " Where " + Col4 + "=?  LIMIT ?,? ";

            String selectionArgs[] = new String[]{
                    String.valueOf(condStr),
                    String.valueOf((currentPage - 1) * lineSize),
                    String.valueOf(lineSize)};               // The query parameters
            result = database.rawQuery(sql, selectionArgs);
        }

        for (result.moveToFirst(); !result.isAfterLast(); result.moveToNext()) {
            Map<String,Object> map = new HashMap<>() ;
            map.put(Col1, result.getString(0));
            map.put(Col2, result.getString(1));
            map.put(Col3, result.getString(2));
            map.put(Col5, result.getString(3));
            all.add(map);	// To the Collection Conservation
        }
        database.close();	// Close the database connection
        result.close();
        return all;
    }

}
