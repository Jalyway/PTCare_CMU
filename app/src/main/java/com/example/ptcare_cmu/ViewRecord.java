package com.example.ptcare_cmu;

/*
 * Created by js-mis on 2017/8/24.
 */

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import au.com.bytecode.opencsv.CSVWriter;


public class ViewRecord extends MainActivity {

    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;
    private Map<String, String> criteriaList;
    private String[][] sprCriteria;
    private int selectID;
    private int Hz = 25;
    private String fileName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_record);
        //
        Button createCriteria = findViewById(R.id.button1);
        Button record2Csv = findViewById(R.id.button2);
        Button showTable = findViewById(R.id.button3);
        Spinner sp = findViewById(R.id.spCriteria);
        //
        dbHelper = new DatabaseHelper(this);
        database = dbHelper.getWritableDatabase();
        MyMotion motion = new MyMotion(database);
        criteriaList = motion.getCriteriaAll();
        sprCriteria = new String[2][criteriaList.size()];

        int i = 0;
        for (Map.Entry<String, String> entry : criteriaList.entrySet()) {
            sprCriteria[0][i] = entry.getValue();
            sprCriteria[1][i] = entry.getKey();
            i++;
        }

        ArrayAdapter<String> adapter;
        if (sprCriteria[0].length != 0) {
            adapter = new ArrayAdapter<>(this,R.layout.support_simple_spinner_dropdown_item,sprCriteria[0]);
            sp.setAdapter(adapter);
            sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectID = position;
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    selectID = Integer.valueOf(sprCriteria[1][0]);
                }
            });
        }
        else {
            adapter = new ArrayAdapter<>(this,R.layout.support_simple_spinner_dropdown_item,new String[]{"無任何準則資料"});
            sp.setAdapter(adapter);
            sp.setEnabled(false);

            createCriteria.setEnabled(false);
            record2Csv.setEnabled(false);
            showTable.setEnabled(false);
        }


    }

    // Listener for buttons click //
    public void btn1Listener(View v) {
        fileName = sprCriteria[0][selectID];   //檔名
        String criteriaID = sprCriteria[1][selectID]; //所選準則 ID
        String[] criteriaInfo = getCriteria(criteriaID);  //得到準則資訊：循環次數、動作代碼、動作秒數

        String[] strArray = criteriaInfo[2].split(", ");  //準則動作秒數(動作時程)
        int[] sch = new int[strArray.length];
        for (int i = 0; i < strArray.length; i++) {
            String numberAsString = strArray[i];
            sch[i] = Integer.parseInt(numberAsString);
        }

        String[] strArray1 = criteriaInfo[1].split(", ");  //準則動作代碼
        int[] mot = new int[strArray1.length];
        for (int i = 0; i < strArray1.length; i++) {
            String numberAsString = strArray1[i];
            mot[i] = Integer.parseInt(numberAsString);
        }

        List motionCode = getMotionCode(Hz, Integer.valueOf(criteriaInfo[0]), sch, mot); //計算無變異差特徵-->需要頻率
        getFeatures(motionCode);
    }

    public void btn2Listener(View v) {
        fileName = sprCriteria[0][selectID];   //檔名
        File removable = ContextCompat.getExternalFilesDirs(this, null)[0];  //存在外部儲存空間
        //Log.e("InternalFileDirs",removable.getPath());
        if (removable.exists() && removable.canRead() && removable.canWrite()) {

            SimpleDateFormat formatter1 = new SimpleDateFormat("YYYYMMddHHmm");
            Date curDate = new Date(System.currentTimeMillis()); //獲取當前時間

            String cur = formatter1.format(curDate);

            File test = new File(removable,fileName+"_"+cur+".csv");
            try {
                test.createNewFile(); // Throws the exception mentioned above
                // test.mkdir();
                // na=test.getParent()+"/test2.csv";

                boolean isSDPresent = true;  // SD卡
                CSVWriter csvWrite = new CSVWriter(new FileWriter(test));
                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor curCSV = db.rawQuery("SELECT * FROM features_all", null);
                if (curCSV.getCount() == 0) {
                    csvWrite.close();
                    curCSV.close();
                    db.close();
                    Toast.makeText(this, "查無資料\n請先執行產生特徵，再執行產生CSV", Toast.LENGTH_LONG).show();
                }
                else {
                    csvWrite.writeNext(curCSV.getColumnNames());

                    for (curCSV.moveToFirst(); !curCSV.isAfterLast(); curCSV.moveToNext()) {
                        String arrStr[] = {
                                curCSV.getString(0), curCSV.getString(1), curCSV.getString(2),
                                curCSV.getString(3), curCSV.getString(4), curCSV.getString(5),
                                curCSV.getString(6), curCSV.getString(7), curCSV.getString(8),
                                curCSV.getString(9), curCSV.getString(10), curCSV.getString(11),
                                curCSV.getString(12), curCSV.getString(13), curCSV.getString(14),
                                curCSV.getString(15), curCSV.getString(16), curCSV.getString(17),
                                curCSV.getString(18), curCSV.getString(19), curCSV.getString(20),
                                curCSV.getString(21), curCSV.getString(22), curCSV.getString(23),
                                curCSV.getString(24), curCSV.getString(25), curCSV.getString(26),
                                curCSV.getString(27), curCSV.getString(28) };
                        csvWrite.writeNext(arrStr);
                    }
                    csvWrite.close();
                    curCSV.close();
                    db.close();
                    AlertDialog.Builder confirmDialog = new AlertDialog.Builder(ViewRecord.this);
                    confirmDialog.setTitle("提示訊息")
                                 .setMessage("資料已經記錄至手機資料庫：\n是否上傳至雲端？")
                                 .setIcon(android.R.drawable.ic_dialog_alert)
                                 .setPositiveButton("確定", new DialogInterface.OnClickListener() {
                                     @Override
                                     public void onClick(DialogInterface dialog, int which) {
                                         startActivity(new Intent(getApplicationContext(), UploadRec.class));
                                     }
                                 })
                                 .setNegativeButton("取消", null)
                                 .create()
                                 .show();
                }
            } catch (Exception e) {
                Log.e(getClass().getSimpleName(), "Exception creating file", e);
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    public void btn3Listener(View v) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String[] sql = new String[]{"jy61_record","features_all"};
        String[] msgstr = {"metawear收入資料筆數 ","產生動作特徵筆數"};

        for(int i=0; i<2; i++) {
            String sqlStr = sql[i];
            SQLiteStatement statement = db.compileStatement("SELECT COUNT(*) FROM  " + sqlStr);
            long count = statement.simpleQueryForLong();
            if (count < Integer.MIN_VALUE || count > Integer.MAX_VALUE) {
                throw new IllegalArgumentException(count + " cannot be cast to int without changing its value.");
            }
            Toast.makeText(this, msgstr[i] + ": " + count, Toast.LENGTH_SHORT).show();
        }
        db.close();
    }


    private String[] getCriteria(String cId) {

        database = dbHelper.getWritableDatabase();
        String sql = "SELECT *  from criteria where _id =?";
        Cursor result = database.rawQuery(sql, new String[]{cId});
        String[] criteria = new String[3];

        for (result.moveToFirst(); !result.isAfterLast(); result.moveToNext()) {
            criteria[0] = result.getString(2);  //循環次數
            criteria[1] = result.getString(3);  //動作代碼
            criteria[2] = result.getString(4);  //動作秒數
        }
        result.close();
        database.close();
        // Toast.makeText(this, criteria[0],Toast.LENGTH_LONG).show();
        return criteria;
    }

    public List getMotionCode(int Hz, int times, int[] schedule, int[] motIndex) {
        // 輸入頻率、動作次數, 動作時程, 動作代碼
        // 運用關節: shoulder, elbow, wrist, hip, knee, ankle, trunk, etc.
        // 基本動作: flextion, extension, abduction, adduction, dorsiflexion, plantarflexion, int-rotation, ext-rotation, pronation, supination, radial-dev, ulnar-dev, inversion, eversion, etc.
        // Hz: 資料量測頻率, 20Hz表一秒鐘有20筆資料
        // times: 一個動作要循環的次數
        // period: 一個動作週期的時間
        // schedule[]: 動作起始時間, ex: {0, 2, 6, 8, 12}
        // idx[]: 動作代碼基底, ex: flexion=[100, 101], extension=[-100, -101], 對應schedule: {0, 100, 0, -101} -> 應比schedule少一個元素
        // int Hz = (int) (1 / f); //一秒有多少筆資料-->即頻率數, 20Hz or 100Hz
        int error = Hz / 5;  //允許誤差值-->控制誤差-->轉換動作前後之1/5頻率數筆資料
        int period = schedule[schedule.length - 1] - schedule[0];//標準動作循環周期時間
        // System.out.println("period:"+  Integer.toString(period));
        // System.out.println("Times:"+  Integer.toString(times)+" "+Integer.toString(motIndex[0])+" "+Integer.toString(motIndex[1])+" "+Integer.toString(motIndex[2])+" "+Integer.toString(motIndex[3]));

        // int L = times * period * Hz + error;//總資料量, 以shoulder為例，每個循環period秒，做times次，一秒紀錄Hz筆，加上最後動作誤差
        List codeList = new ArrayList();
        for (int j = 0; j < times; j++) {
            for (int k = 0; k < schedule.length - 1; k++) {
                int interval = schedule[k + 1] - schedule[k];//前後動作間隔時間
                //從第k個動作時間起, 到前後動作間隔時間*頻率數+誤差次數止, 每次動作均加上前幾個循環的資料數(period*j*Hz)
                for (int i = schedule[k] + period * j * Hz; i < schedule[k] + (interval + period * j) * Hz; i++) {
                    double code = Math.random(); //先產生隨機值 --> 考慮*0.5, 使motion index之間不會相連
                    int upper_bound = schedule[k] + period * j * Hz + error;//第 k 個時間點往後的 error 筆資料
                    int lower_bound = schedule[k] + (interval + period * j) * Hz - error;//第 k 個時間點的下一個時間點往前的 error 筆資料
                    if (i < upper_bound || i > lower_bound) //變換動作區間, 可考慮排除起始狀態(pause, k==0)
                        code = code * 2 - 1;//變換動作區間: change motion index = 0~1, 或 pause = -1~0(使用100-101則因與其他動作代碼差異過大而加大訓練誤差) -> 考慮涵蓋pause和change (-1,1): code*2-1
                    else
                        code = code + motIndex[k]; //不屬於變換動作區間

                    codeList.add(code);
                    // System.out.println(i + ":" + j + ":" + k + "->" + code);
                }
            }
        }
        return codeList;
    }

    private void getFeatures(List motion) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor c = db.rawQuery("select accx,accy,accz,angvx,angvy,angvz,angx,angy,angz  from  jy61_record;", null);

        if (c != null) {
            int rows_num = c.getCount();
            //Toast.makeText(this,"rows_num:"+rows_num,Toast.LENGTH_LONG).show();  //

            double[][] Racc = new double[rows_num][3];//加速度
            double[][] RangV = new double[rows_num][3];//角速度
            double[][] RangL = new double[rows_num][3];//角度

            int k = 0;

            if (rows_num != 0) {
                c.moveToFirst();

                Toast.makeText(ViewRecord.this, "標準資料筆數： " + String.valueOf(motion.size()), Toast.LENGTH_LONG).show();
                for (int i = 0; i < rows_num; i++) {
                    Racc[k][0] = Double.parseDouble(c.getString(0)); //輸入加速度
                    Racc[k][1] = Double.parseDouble(c.getString(1));
                    Racc[k][2] = Double.parseDouble(c.getString(2));
                    RangV[k][0] = Double.parseDouble(c.getString(3)); //輸入角速度
                    RangV[k][1] = Double.parseDouble(c.getString(4));
                    RangV[k][2] = Double.parseDouble(c.getString(5));
                    RangL[k][0] = Double.parseDouble(c.getString(6)); //輸入角度
                    RangL[k][1] = Double.parseDouble(c.getString(7));
                    RangL[k][2] = Double.parseDouble(c.getString(8));
                    k++;
                    c.moveToNext();  //將指標移至下一筆資料
                }
                c.close(); //關閉Cursor
                db.close();


                double[][] acc = new double[rows_num][3]; //相對初始的 加速度
                double[][] angV = new double[rows_num][3]; //相對初始的 角速度
                double[][] angL = new double[rows_num][3]; //相對初始的 角度

                for (int i = 0; i < acc.length - 1; i++) {//相對初始值 --> 也就是會比初始值少一筆資料
                    for (int j = 0; j < 3; j++) {  //Note: 昀姍原始程式有誤, 只有算x軸
                        acc[i][j] = Racc[i + 1][j] - Racc[0][j];//相對加速度
                        angV[i][j] = RangV[i + 1][j] - RangV[0][j];//相對角速度
                        angL[i][j] = RangL[i + 1][j] - RangL[0][j];//相對角度
                    }
                }
                // int hz=100;     //待修改----------------------------------------------
                double dt = 1.0 / Hz; //將頻率轉成時間間隔
                //分析資料2－加速度/角速度變率(利用相對初始值)
                double[][] accVar = new double[rows_num][3]; //加速度 變率
                double[][] angVar = new double[rows_num][3]; //角速度 變率
                double[][] angLVar = new double[rows_num][3]; //角度 變率
                for (int i = 0; i < acc.length - 1; i++) {
                    for (int j = 0; j < 3; j++) {
                        accVar[i][j] = (Racc[i + 1][j] - Racc[i][j]) / dt; //加速度 變率
                        angVar[i][j] = (RangV[i + 1][j] - RangV[i][j]) / dt; //角速度 變率
                        angLVar[i][j] = (RangL[i + 1][j] - RangL[i][j]) / dt; //角度 變率
                    }
                }


                //分析資料3－加速度/角速度變異差(利用相對初始值)
                // int L = countn / hz;//得到資料筆數(每一秒算一個標準差，所以後面多餘的就去除 (ex: 100hz表1秒中有100筆)
                // double[][] accSig = getSig(acc, L);//得到加速度變異差
                // double[][] angVSig = getSig(angV, L);//得到角速度變異差
                double[][] cosAng = getCosData(angL, true);//得到取cos之角速度
                double[][] normAcc = getNormData(acc);//得到標準化(正規化)之加速度
                double[][] normAngV = getNormData(angV);//得到標準化(正規化)之角速度

                // writeData(100, acc, angV, angL, normAcc, normAngV, cosAng, accVar, angVar, angLVar, mo);
                writeData(25, acc, angV, angL, normAcc, normAngV, cosAng, accVar, angVar, angLVar, motion);
                Toast.makeText(this,"資料已寫入",Toast.LENGTH_LONG).show();

            }
            else
                Toast.makeText(this,"沒有錄製數據資料",Toast.LENGTH_LONG).show();
        }
    }

    //得到標準差(前提：輸入的資料要有三個方向、要輸入總共多少筆)
    public double[][] getSig(double[][] data, int L) {  //輸入資料及資料筆數(預設資料感測頻率為100Hz)
        double[][] Sig = new double[L][3];
        for (int i = 0; i < L; i++) { //離均差的平方的平均
            double addx = 0; //定義資料x總和
            double addy = 0; //定義資料y總和
            double addz = 0; //定義資料z總和
            for (int j = i * 100; j < i * 100 + 99; j++) { //計算資料總和(從0~99.100~199...
                addx = addx + data[j][0];
                addy = addy + data[j][1];
                addz = addz + data[j][2];
            }
            double xMu = addx * 0.01; //資料x平均
            double yMu = addy * 0.01;
            double zMu = addz * 0.01;
            //資料離均差總和
            double Sx = 0;
            double Sy = 0;
            double Sz = 0;
            for (int j = i * 100; j < i * 100 + 99; j++) { //計算資料離均差總和(從0~99.100~199...
                Sx = Sx + (data[j][0] - xMu) * (data[j][0] - xMu);
                Sy = Sy + (data[j][1] - yMu) * (data[j][1] - yMu);
                Sz = Sz + (data[j][2] - zMu) * (data[j][2] - zMu);
            }
            Sig[i][0] = Sx / 100;
            Sig[i][1] = Sy / 100;
            Sig[i][2] = Sz / 100;
        }
        return Sig;
    }

    // 標準化, 對角度資料取cos
    public double[][] getCosData(double[][] data, boolean isDeg) { //輸入資料, 資料是否為角度
        double[][] cosData = new double[data.length][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < data.length; j++) {
                double angle = data[j][i];
                if (isDeg) {
                    angle = Math.toRadians(angle);
                }
                cosData[j][i] = Math.cos(angle);
            }
        }
        return cosData; //將三軸角度取cos函數, 使範圍介於-1~1
    }

    // 標準化, 讓資料介於-1~1之間
    public double[][] getNormData(double[][] data) {
        int N = data.length;
        double[][] normData = new double[N][3];
        for (int i = 0; i < 3; i++) {
            double max = 0;
            for (int j = 0; j < N; j++) {
                max = Math.max(max, Math.abs(data[j][i]));
            }
            for (int j = 0; j < N; j++) {
                normData[j][i] = data[j][i] / max;
            }
        }
        return normData; //將各軸資料除以其絕對值最大者(正規化), 使範圍介於-1~1
    }

    //將 9 種特徵資料(features)(不含變異差)寫入資料檔中, 含動作代號 --> 可延伸為寫入資料表中
    public void writeData(int hz,
                          double[][] acc, double[][] angV, double[][] angL,
                          double[][] normAcc, double[][] normAngV, double[][] cosAng,
                          double[][] accVar, double[][] angVar, double[][] angLVar,
                          List motCode) {

        int n = motCode.size(); //最大資料筆數, 相對加速度...等, 加速度與角速度變異差是Hz值而定會是 n/hz
        //  n=n/2;
        //  Toast.makeText(context, String.valueOf(n)+" ", Toast.LENGTH_LONG).show();
        int L = n / hz;
        try {

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.delete("features_all",null,null);

            String sql = "insert into features_all (motionCode,ACCx,ACCy,ACCz,ANGVx,ANGVy,ANGVz,ANGx,ANGy,ANGz,nACCx,nACCy,nACCz,";
            sql +=" nANGVx,nANGVy,nANGVz,cANGx,cANGy,cANGz,dACCx,dACCy,dACCz,dANGVx,dANGVy,dANGVz,dANGx,dANGy,dANGz)";
            sql +="values (?, ?, ?, ?,?, ?, ?, ?,?, ?, ?, ?,?, ?, ?, ?,?, ?, ?, ?,?, ?, ?, ?,?, ?, ?, ?);";

            db.beginTransaction();
            SQLiteStatement stmt = db.compileStatement(sql);
            for (int i = 0; i < n; i++) { //寫入其他資料
                /*
                motCode.get(i) + "," + acc[i][0] + "," + acc[i][1] + "," + acc[i][2] + ","
                        + angV[i][0] + "," + angV[i][1] + "," + angV[i][2] + ","
                        + angL[i][0] + "," + angL[i][1] + "," + angL[i][2] + ","
                        + normAcc[i][0] + "," + normAcc[i][1] + "," + normAcc[i][2] + ","
                        + normAngV[i][0] + "," + normAngV[i][1] + "," + normAngV[i][2] + ","
                        + cosAng[i][0] + "," + cosAng[i][1] + "," + cosAng[i][2] + ","
                        + accVar[i][0] + "," + accVar[i][1] + "," + accVar[i][2] + ","
                        + angVar[i][0] + "," + angVar[i][1] + "," + angVar[i][2] + ","
                        + angLVar[i][0] + "," + angLVar[i][1] + "," + angLVar[i][2]
                        motionCode,
                        ACCx,ACCy,ACCz,ANGVx,ANGVy,ANGVz,ANGx,ANGy,ANGz,
                        nACCx,nACCy,nACCz,nANGVx,nANGVy,nANGVz,cANGx,cANGy,cANGz,
                        dACCx,dACCy,dACCz,dANGVx,dANGVy,dANGVz,dANGx,dANGy,dANGz
                */
                stmt.bindString(1, motCode.get(i).toString());
                stmt.bindString(2,String.valueOf(acc[i][0]));
                stmt.bindString(3,String.valueOf(acc[i][1]));
                stmt.bindString(4,String.valueOf(acc[i][2]));
                stmt.bindString(5,String.valueOf(angV[i][0]));
                stmt.bindString(6,String.valueOf(angV[i][1]));
                stmt.bindString(7,String.valueOf(angV[i][2]));
                stmt.bindString(8,String.valueOf(angL[i][0]));
                stmt.bindString(9,String.valueOf(angL[i][1]));
                stmt.bindString(10,String.valueOf(angL[i][2]));
                stmt.bindString(11,String.valueOf(normAcc[i][0]));
                stmt.bindString(12,String.valueOf(normAcc[i][1]));
                stmt.bindString(13,String.valueOf(normAcc[i][2]));
                stmt.bindString(14,String.valueOf(normAngV[i][0]));
                stmt.bindString(15,String.valueOf(normAngV[i][1]));
                stmt.bindString(16,String.valueOf(normAngV[i][2]));
                stmt.bindString(17,String.valueOf(cosAng[i][0]));
                stmt.bindString(18,String.valueOf(cosAng[i][1]));
                stmt.bindString(19,String.valueOf(cosAng[i][2]));
                stmt.bindString(20, String.valueOf(accVar[i][0]));
                stmt.bindString(21,String.valueOf(accVar[i][1]));
                stmt.bindString(22,String.valueOf(accVar[i][2]));
                stmt.bindString(23,String.valueOf(angVar[i][0]));
                stmt.bindString(24,String.valueOf(angVar[i][1]));
                stmt.bindString(25,String.valueOf(angVar[i][2]));
                stmt.bindString(26, String.valueOf(angLVar[i][0]));
                stmt.bindString(27,String.valueOf(angLVar[i][1]));
                stmt.bindString(28,String.valueOf(angLVar[i][2]));

                long entryID = stmt.executeInsert();
                stmt.clearBindings();
                // db.insert("features_all", null, values);
            }
            db.setTransactionSuccessful();
            db.endTransaction();
            db.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}
