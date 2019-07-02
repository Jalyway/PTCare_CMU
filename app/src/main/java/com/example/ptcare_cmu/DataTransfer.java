package com.example.ptcare_cmu;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.example.ptcare_cmu.db.DBHelper;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by js-mis on 2017/8/24.
 */

public class DataTransfer {
    private DBHelper dbhelper = null;
    private int hz=100;
    private List motCode1=null;
    private String file_path="";

    public void calMotion(String file_path,Context context)
    { //public List getMotionCode(int Hz, int times, int[] schedule, int[] motIndex)
       // int cytime=5;

        /*
                 4
                rest|inversion|rest|eversion
                0,2,4,6,8
         */
        this.file_path=file_path;
        dbhelper=new DBHelper(context);
        dbhelper.openDatabase();
        // int sch[]={0, 2, 6, 8, 12};
        int sch[]={0,2,4,6,8};
       // int mot[]={-999, 2, -999, -3};
        int mot[]={-999, 14, -999, -15};
      //  motCode1 = getMotionCode(100, 5, sch, mot);//計算無變異差特徵-->需要頻率
        motCode1 = getMotionCode(100, 4, sch, mot);//計算無變異差特徵-->需要頻率
        getFeatures(motCode1);
    }
    private void getFeatures(List mo)
    {
        int countn = fetchCount();
        SQLiteDatabase db = dbhelper.getReadableDatabase();
        Cursor c=db.rawQuery("select accx,accy,accz,angvx,angvy,angvz,angx,angy,angz  from  jy61_record;",null);
        double[][] Racc = new double[countn][3];//加速度
        double[][] RangV = new double[countn][3];//角速度
        double[][] RangL = new double[countn][3];//角度

       // String countn = null;
        int k=0;
        if(null != c){
            int rows_num = c.getCount();	//取得資料表列數
            if(rows_num != 0) {
                c.moveToFirst();

                for(int i=0; i<rows_num; i++) {
                    Racc[k][0] = Double.parseDouble( c.getString(0));//輸入加速度
                    Racc[k][1] = Double.parseDouble( c.getString(1));
                    Racc[k][2] = Double.parseDouble( c.getString(2));
                    RangV[k][0] = Double.parseDouble( c.getString(3));//輸入角速度
                    RangV[k][1] = Double.parseDouble( c.getString(4));
                    RangV[k][2] = Double.parseDouble( c.getString(5));
                    RangL[k][0] = Double.parseDouble( c.getString(6));//輸入角度
                    RangL[k][1] = Double.parseDouble( c.getString(7));
                    RangL[k][2] = Double.parseDouble( c.getString(8));
                    k++;

                    c.moveToNext();		//將指標移至下一筆資料
                }
            }
            c.close();		//關閉Cursor
            db.close();

            double[][] acc = new double[countn][3];//相對初始的 加速度
            double[][] angV = new double[countn][3];//相對初始的 角速度
            double[][] angL = new double[countn][3];//相對初始的 角度

            for (int i = 0; i < acc.length - 1; i++) {//相對初始值 --> 也就是會比初始值少一筆資料
                for (int j = 0; j < 3; j++) {//Note: 昀姍原始程式有誤, 只有算x軸
                    acc[i][j] = Racc[i + 1][j] - Racc[0][j];//相對加速度
                    angV[i][j] = RangV[i + 1][j] - RangV[0][j];//相對角速度
                    angL[i][j] = RangL[i + 1][j] - RangL[0][j];//相對角度
                }
            }
           // int hz=100;     //待修改----------------------------------------------
            double dt = 1.0 / hz;//將頻率轉成時間間隔
            //分析資料2－加速度/角速度變率(利用相對初始值)
            double[][] accVar = new double[countn][3];//加速度 變率
            double[][] angVar = new double[countn][3];//角速度 變率
            double[][] angLVar = new double[countn][3];//角度 變率
            for (int i = 0; i < acc.length - 1; i++) {
                for (int j = 0; j < 3; j++) {
                    accVar[i][j] = (Racc[i + 1][j] - Racc[i][j]) / dt;//加速度 變率
                    angVar[i][j] = (RangV[i + 1][j] - RangV[i][j]) / dt;//角速度 變率
                    angLVar[i][j] = (RangL[i + 1][j] - RangL[i][j]) / dt;//角度 變率
                }
            }


            //分析資料3－加速度/角速度變異差(利用相對初始值)
          //  int L = countn / hz;//得到資料筆數(每一秒算一個標準差，所以後面多餘的就去除 (ex: 100hz表1秒中有100筆)
          //  double[][] accSig = getSig(acc, L);//得到加速度變異差
           // double[][] angVSig = getSig(angV, L);//得到角速度變異差
            double[][] cosAng = getCosData(angL, true);//得到取cos之角速度
            double[][] normAcc = getNormData(acc);//得到標準化(正規化)之加速度
            double[][] normAngV = getNormData(angV);//得到標準化(正規化)之角速度

            writeData(100, acc, angV, angL, normAcc, normAngV, cosAng, accVar, angVar, angLVar, mo);
        }


    }

    //將 9 種特徵資料(features)(不含變異差)寫入資料檔中, 含動作代號 --> 可延伸為寫入資料表中
    public void writeData( int hz,
                          double[][] acc, double[][] angV, double[][] angL,
                          double[][] normAcc, double[][] normAngV, double[][] cosAng,
                          double[][] accVar, double[][] angVar, double[][] angLVar,
                          List motCode) {

        int n = motCode.size();//最大資料筆數, 相對加速度...等, 加速度與角速度變異差是Hz值而定會是 n/hz
        Log.e("size", String.valueOf(n));
        int L = n / hz;
        try {
            //---------------------------------------------------------------------------------------------------------
            OutputStream output;
            try {
                output = new BufferedOutputStream(new FileOutputStream(file_path, true));
                output.write((",ACCx,ACCy,ACCz,ANGVx,ANGVy,ANGVz,ANGx,ANGy,ANGz,nACCx,nACCy,nACCz" +
                        ",nANGVx,nANGVy,nANGVz,cANGx,cANGy,cANGz,dACCx,dACCy,dACCz,dANGVx,dANGVy,dANGVz,dANGx,dANGy,dANGz").getBytes());
                output.write("\n".getBytes());
                output.close();
            } catch (Exception e) {
                Log.e("r", "CSV creation error", e);
            }
            //---------------------------------------------------------------------------------------------------------
            for (int i = 0; i < acc.length; i++) {//寫入其他資料
                OutputStream out;
                try {
                    out = new BufferedOutputStream(new FileOutputStream(file_path, true));
                    out.write(",".getBytes());
                    out.write(String.valueOf(acc[i][0]).getBytes());
                    out.write(",".getBytes());
                    out.write(String.valueOf(acc[i][1]).getBytes());
                    out.write(",".getBytes());
                    out.write(String.valueOf(acc[i][2]).getBytes());
                    out.write(",".getBytes());
                    out.write(String.valueOf(angV[i][0]).getBytes());
                    out.write(",".getBytes());
                    out.write(String.valueOf(angV[i][1]).getBytes());
                    out.write(",".getBytes());
                    out.write(String.valueOf(angV[i][2]).getBytes());
                    out.write(",".getBytes());
                    out.write(String.valueOf(angL[i][0]).getBytes());
                    out.write(",".getBytes());
                    out.write(String.valueOf(angL[i][1]).getBytes());
                    out.write(",".getBytes());
                    out.write(String.valueOf(angL[i][2]).getBytes());
                    out.write(",".getBytes());
                    out.write(String.valueOf(normAcc[i][0]).getBytes());
                    out.write(",".getBytes());
                    out.write(String.valueOf(normAcc[i][1]).getBytes());
                    out.write(",".getBytes());
                    out.write(String.valueOf(normAcc[i][2]).getBytes());
                    out.write(",".getBytes());
                    out.write(String.valueOf(normAngV[i][0]).getBytes());
                    out.write(",".getBytes());
                    out.write(String.valueOf(normAngV[i][1]).getBytes());
                    out.write(",".getBytes());
                    out.write(String.valueOf(normAngV[i][2]).getBytes());
                    out.write(",".getBytes());
                    out.write(String.valueOf(cosAng[i][0]).getBytes());
                    out.write(",".getBytes());
                    out.write(String.valueOf(cosAng[i][1]).getBytes());
                    out.write(",".getBytes());
                    out.write(String.valueOf(cosAng[i][2]).getBytes());
                    out.write(",".getBytes());
                    out.write(String.valueOf(accVar[i][0]).getBytes());
                    out.write(",".getBytes());
                    out.write(String.valueOf(accVar[i][1]).getBytes());
                    out.write(",".getBytes());
                    out.write(String.valueOf(accVar[i][2]).getBytes());
                    out.write(",".getBytes());
                    out.write(String.valueOf(angVar[i][0]).getBytes());
                    out.write(",".getBytes());
                    out.write(String.valueOf(angVar[i][1]).getBytes());
                    out.write(",".getBytes());
                    out.write(String.valueOf(angVar[i][2]).getBytes());
                    out.write(",".getBytes());
                    out.write(String.valueOf(angLVar[i][0]).getBytes());
                    out.write(",".getBytes());
                    out.write(String.valueOf(angLVar[i][1]).getBytes());
                    out.write(",".getBytes());
                    out.write(String.valueOf(angLVar[i][2]).getBytes());
                    out.write("\n".getBytes());
                    out.close();
                    Log.e("FFFFF", String.valueOf(i));
                } catch (Exception e) {
                    Log.e("r", "CSV creation error", e);
                }
            }
            Log.e("r", "insert finish");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //得到標準差(前提：輸入的資料要有三個方向、要輸入總共多少筆)

    public double[][] getSig(double[][] data, int L) {//輸入資料及資料筆數(預設資料感測頻率為100Hz)
        double[][] Sig = new double[L][3];
        for (int i = 0; i < L; i++) {//離均差的平方的平均
            double addx = 0;//定義資料x總和
            double addy = 0;//定義資料y總和
            double addz = 0;//定義資料z總和
            for (int j = i * 100; j < i * 100 + 99; j++) {//計算資料總和(從0~99.100~199...
                addx = addx + data[j][0];
                addy = addy + data[j][1];
                addz = addz + data[j][2];
            }
            double xMu = addx * 0.01;//資料x平均
            double yMu = addy * 0.01;
            double zMu = addz * 0.01;
            //資料離均差總和
            double Sx = 0;
            double Sy = 0;
            double Sz = 0;
            for (int j = i * 100; j < i * 100 + 99; j++) {//計算資料離均差總和(從0~99.100~199...
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

    public double[][] getCosData(double[][] data, boolean isDeg) {//輸入資料, 資料是否為角度
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
        return cosData;//將三軸角度取cos函數, 使範圍介於-1~1
    }
    // 標準化, 讓資料介於-1~1之間

    public double[][] getNormData(double[][] data) {//輸入資料
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
        return normData;//將各軸資料除以其絕對值最大者(正規化), 使範圍介於-1~1
    }

    private String checkRecord()
    {
        SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Toast.makeText(GetAQID.this, qnum+" "+inv+ " ", Toast.LENGTH_SHORT).show();

        Cursor c=db.rawQuery("select count(*)  from  jy61_record;",null);
        String countn1 = null;

        if(null != c){
            if(c.getCount() > 0){
                c.moveToFirst();
                countn1 = c.getString(0);

            }
            c.close();
        }
        db.close();
        return countn1;
}
    private int fetchCount() {
        SQLiteDatabase db = dbhelper.getReadableDatabase();
        String sql = "SELECT COUNT(*) FROM  jy61_record";
        SQLiteStatement statement = db.compileStatement(sql);
        long count = statement.simpleQueryForLong();
        if (count < Integer.MIN_VALUE || count > Integer.MAX_VALUE) {
            throw new IllegalArgumentException
                    (count + " cannot be cast to int without changing its value.");
        }
        db.close();
        return (int) count;
    }
    public List getMotionCode(int Hz, int times, int[] schedule, int[] motIndex) {//輸入頻率、動作次數, 動作時程, 動作代碼
//運用關節: shoulder, elbow, wrist, hip, knee, ankle, trunk, etc.
//基本動作: flextion, extension, abduction, adduction, dorsiflexion, plantarflexion, int-rotation, ext-rotation, pronation, supination, radial-dev, ulnar-dev, inversion, eversion, etc.
// Hz: 資料量測頻率, 20Hz表一秒鐘有20筆資料
// times: 一個動作要循環的次數
// period: 一個動作週期的時間
// schedule[]: 動作起始時間, ex: {0, 2, 6, 8, 12}
// idx[]: 動作代碼基底, ex: flexion=[100, 101], extension=[-100, -101], 對應schedule: {0, 100, 0, -101} -> 應比schedule少一個元素
//        int Hz = (int) (1 / f);//一秒有多少筆資料-->即頻率數, 20Hz or 100Hz
        int error = Hz / 5;//允許誤差值-->控制誤差-->轉換動作前後之1/5頻率數筆資料
        int period = schedule[schedule.length - 1] - schedule[0];//標準動作循環周期時間
   //     System.out.println("period:"+  Integer.toString(period));
   //     System.out.println("Times:"+  Integer.toString(times)+" "+Integer.toString(motIndex[0])+" "+Integer.toString(motIndex[1])+" "+Integer.toString(motIndex[2])+" "+Integer.toString(motIndex[3]));

//        int L = times * period * Hz + error;//總資料量, 以shoulder為例，每個循環period秒，做times次，一秒紀錄Hz筆，加上最後動作誤差
        List codeList = new ArrayList();
        for (int j = 0; j < times; j++) {
            for (int k = 0; k < schedule.length - 1; k++) {
                int interval = schedule[k + 1] - schedule[k];//前後動作間隔時間
                //從第k個動作時間起, 到前後動作間隔時間*頻率數+誤差次數止, 每次動作均加上前幾個循環的資料數(period*j*Hz)
                for (int i = schedule[k] + period * j * Hz; i < schedule[k] + (interval + period * j) * Hz; i++) {
                    double code = Math.random();//先產生隨機值 --> 考慮*0.5, 使motion index之間不會相連
                    int upper_bound = schedule[k] + period * j * Hz + error;//第 k 個時間點往後的 error 筆資料
                    int lower_bound = schedule[k] + (interval + period * j) * Hz - error;//第 k 個時間點的下一個時間點往前的 error 筆資料
                    if (i < upper_bound || i > lower_bound) {//變換動作區間, 可考慮排除起始狀態(pause, k==0)
//                        code = code + 0;//變換動作區間: change motion index = 0~1, 或 pause = -1~0(使用100-101則因與其他動作代碼差異過大而加大訓練誤差) -> 考慮涵蓋pause和change (-1,1): code*2-1
                        code = code * 2 - 1;//變換動作區間: change motion index = 0~1, 或 pause = -1~0(使用100-101則因與其他動作代碼差異過大而加大訓練誤差) -> 考慮涵蓋pause和change (-1,1): code*2-1
                    } else {
                        code = code + motIndex[k];//不屬於變換動作區間
                    }
                    codeList.add(code);
               //     System.out.println(i + ":" + j + ":" + k + "->" + code);
                }
            }
        }
        return codeList;
    }
    private  String[] getCriteria(String cid)
    {
        SQLiteDatabase db = dbhelper.getReadableDatabase();

        String sql = "SELECT *  from criteria where _id =?" ;
        Cursor result = db.rawQuery(sql,  new String[]{cid});		// Query
        String[] criteria=new String[4];


      //  for (result.moveToFirst(); !result.isAfterLast(); result.moveToNext()) {
            result.moveToFirst();
            criteria[0]=result.getString(1);
            criteria[1]=result.getString(2);
            criteria[2]=result.getString(3);
            criteria[3]=result.getString(4);
       // }

        // db.close() ;

        result.close();
        db.close();
        return criteria;
    }
}
