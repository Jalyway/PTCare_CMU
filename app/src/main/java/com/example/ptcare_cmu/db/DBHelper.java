package com.example.ptcare_cmu.db;

/**
 * Created by js-mis on 2016/1/21.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import static android.provider.BaseColumns._ID;

public class DBHelper extends SQLiteOpenHelper {


    //資料庫名稱
    private final static String DATABASE_NAME = "myanfis.db";
    public static final String TABLE_NAME = "jy61_record";


    private final static int DATABASE_VERSION = 1;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try{
            final String INIT_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
                    _ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    //  _ID + " INTEGER NOT NULL, " +
                    " device_name TEXT," +
                    " start_time TEXT," +
                    " accx TEXT," +
                    " accy TEXT," +
                    " accz TEXT," +
                    " angvx TEXT," +
                    " angvy TEXT," +
                    " angvz TEXT," +
                    " angx TEXT," +
                    " angy TEXT," +
                    " angz TEXT" +
                    ");";
            db.execSQL(INIT_TABLE);

            db.execSQL("create table features " +
                    "(_id  INTEGER PRIMARY KEY AUTOINCREMENT," +
                    " acc text," + //相對初始的 加速度
                    " angv text," + //相對初始的 角速度
                    " angl text," + //相對初始的 角度
                    " normacc text," + //得到標準化(正規化)之加速度
                    " normangv text," + //得到標準化(正規化)之加速度
                    " cosang text," + //得到取cos之角速度
                    " accvar text," + //加速度 變率
                    " angvar text," + //角速度 變率
                    " anglvar text," + //角度 變率
                    " accsig text," + //得到加速度變異差
                    " angvsig text" + //得到角速度變異差
                    ");");
            db.execSQL("create table features_all " +
                    "(_id  INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "motionCode TEXT, "+
                    "ACCx TEXT, "+
                    "ACCy TEXT, "+
                    "ACCz TEXT, "+
                    "ANGVx TEXT, "+
                    "ANGVy TEXT, "+
                    "ANGVz TEXT, "+
                    "ANGx TEXT, "+
                    "ANGy TEXT, "+
                    "ANGz TEXT, "+
                    "nACCx TEXT, "+
                    "nACCy TEXT, "+
                    "nACCz TEXT, "+
                    "nANGVx TEXT, "+
                    "nANGVy TEXT, "+
                    "nANGVz TEXT, "+
                    "cANGx TEXT, "+
                    "cANGy TEXT, "+
                    "cANGz TEXT, "+
                    "dACCx TEXT, "+
                    "dACCy TEXT, "+
                    "dACCz TEXT, "+
                    "dANGVx TEXT, "+
                    "dANGVy TEXT, "+
                    "dANGVz TEXT, "+
                    "dANGx TEXT, "+
                    "dANGy TEXT, "+
                    "dANGz TEXT"+
                    ");");

            db.execSQL("create table motion_type " +
                    "(_id  INTEGER PRIMARY KEY AUTOINCREMENT," +
                    " mtype_id text," +
                    " mtype_name text" +
                    ");");

            db.execSQL("insert into motion_type(  mtype_id," +
                    "  mtype_name) values('00','預設類別1')");

            db.execSQL("insert into motion_type(  mtype_id," +
                    "  mtype_name) values('01','預設類別2')");

            db.execSQL("create table motion_guide " +
                    "(_id  INTEGER PRIMARY KEY AUTOINCREMENT," +
                    " motion_name text," +
                    " motion_description text," +
                    " motion_code text UNIQUE," +
                    " mtype_id text," +
                    " mtype_availability integer" +
                    ");");

            db.execSQL("create table criteria " +
                    "(_id  INTEGER PRIMARY KEY AUTOINCREMENT," +
                    " criteria_name text," +
                    " criteria_times text," +
                    " criteria_motion_code text," +
                    " criteria_motion_time text" +
                    ");");

             initializeDB(db);


        }


        catch (android.database.sqlite.SQLiteException e) {
            Log.e("TAG", e.getMessage());
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        final String DROP_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
        db.execSQL(DROP_TABLE);
        onCreate(db);
    }
    public void regDB(SQLiteDatabase db) {
        final String check_TABLE = "SELECT name FROM sqlite_master WHERE type='table' AND name='member';";
        db.execSQL(check_TABLE);
        //onCreate(db);
    }
    private void initializeDB(SQLiteDatabase db)
    {
        String[][] motion=new String[20][3];
/*
        motion[0]=new String[]{"change","change中文說明","0","00"};
        motion[1]=new String[]{"pause","pause中文說明","-1","00"};
        motion[2]=new String[]{"pronation","pronation中文說明","17","00"};
        motion[3]=new String[]{"inversion","inversion中文說明","14","00"};
        motion[4]=new String[]{"radial dev.","radial dev.中文說明","11","00"};
        motion[5]=new String[]{"IR","IR中文說明","8","00"};
        motion[6]=new String[]{"ABD","ABD中文說明","5","00"};
        motion[7]=new String[]{"flex.","flex.中文說明","2","00"};
        motion[8]=new String[]{"raiseup","raiseup中文說明","1","00"};
        motion[9]=new String[]{"putdown","putdown中文說明","-2","00"};
        motion[10]=new String[]{"ext.","ext.中文說明","-3","00"};
        motion[11]=new String[]{"ADD","ADD中文說明","-6","00"};
        motion[12]=new String[]{"ER","ER中文說明","-8","00"};
        motion[13]=new String[]{"ulna dev.","ulna dev中文說明","-12","00"};
        motion[14]=new String[]{"eversion","eversion中文說明","-15","00"};
        motion[15]=new String[]{"supination","supination中文說明","-18","00"};
*/
//妤婕提供
        motion[0]=new String[]{"rest_down","手臂靠著大腿外側 掌心向後靜止2秒","0","00"};
        motion[1]=new String[]{"flexion","緩慢舉起手臂4秒內 直到手臂與身體平行","1","00"};
        motion[2]=new String[]{"rest_up","手臂在頭部旁邊 和身體平行靜止2秒","2","00"};
        motion[3]=new String[]{"extension","緩慢放下手臂4秒內 直到手臂與身體平行","3","00"};
        motion[4]=new String[]{"rest_low","手臂垂直放下 掌心貼著大腿靜止2秒","4","00"};
        motion[5]=new String[]{"ABD","緩慢將手臂舉起4秒內 直到與肩膀平行(abduction)","5","00"};
        motion[6]=new String[]{"rest_half","手臂與肩膀平行靜止2秒","6","00"};
        motion[7]=new String[]{"ADD","緩慢放下手臂4秒內 直到手掌碰到大腿(adduction)","7","00"};
        motion[8]=new String[]{"rest_inside","手臂彎曲90度 上臂與身體夾緊 手掌向內靠近腹部靜止2秒","8","00"};
        motion[9]=new String[]{"ER","將手臂旋轉向外4秒內(external rotation)","9","00"};
        motion[10]=new String[]{"rest_outside","手臂彎曲90度 上臂與身體夾緊 手臂向外靜止2秒","10","00"};
        motion[11]=new String[]{"IR","將手臂緩慢往腹部移動4秒內 (internal rotation)","11","00"};
        motion[12]=new String[]{"rest_on","手臂垂直放下貼著大腿 掌心向上靜止2秒","12","00"};
        motion[13]=new String[]{"pronation","2秒內緩慢將掌心旋轉為向下","13","00"};
        motion[14]=new String[]{"rest_under","手臂垂直放下貼著大腿 掌心向下靜止2秒","14","00"};
        motion[15]=new String[]{"supination","2秒內緩慢將掌心旋轉為向上","15","00"};
        motion[16]=new String[]{"rest_right","手臂放在桌上 手臂不動 手掌朝右靜止2秒","16","00"};
        motion[17]=new String[]{"ulnar dev.","2秒內手腕緩慢向左(ulnar deviation)","17","00"};
        motion[18]=new String[]{"rest_left","手臂放在桌上 手臂不動 手掌朝左靜止2秒","18","00"};
        motion[19]=new String[]{"radial dev.","2秒內手腕緩慢向右到(radial deviation)","19","00"};
        ContentValues values = new ContentValues();

        for(int i=0; i< motion.length; i++) {
            //  values.put("_id", motion[i][0].toString().trim());
            values.put("motion_name", motion[i][0].toString().trim());
            values.put("motion_description", motion[i][1].toString().trim());
            values.put("motion_code", motion[i][2].toString().trim());
            values.put("mtype_id", motion[i][3].toString().trim());
            db.insert("motion_guide", null, values);
        }

    }
    public void openDatabase() {
        this.getReadableDatabase();
    }
}