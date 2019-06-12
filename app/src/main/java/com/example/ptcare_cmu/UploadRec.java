package com.example.ptcare_cmu;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class UploadRec extends MainActivity implements View.OnClickListener, AsyncResponse {
    private static final int PICK_FILE_REQUEST = 1;
    private static final String TAG = MainActivity.class.getSimpleName();
    //
    private String selectedFilePath;
    private UploadFile mUpload;
    private String SERVER_URL = "http://140.128.65.114:8000/PRRFISHome/PTC/uploadc.jsp"; //POST網址
    //
    private ImageView iv_Attachment;
    private Button btn_Upload;
    private TextView tv_FileName;
    PowerManager.WakeLock wakeLock;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.uploadrec);
        //
        iv_Attachment = findViewById(R.id.ivAttachment);
        btn_Upload = findViewById(R.id.btn_upload);
        tv_FileName = findViewById(R.id.tv_fileName);
        iv_Attachment.setOnClickListener(this);
        btn_Upload.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == iv_Attachment) {
            // on attachment icon click
            showFileChooser();
        }
        if (v == btn_Upload) {
            // on upload button Click
            if (selectedFilePath != null) {
                // dialog = ProgressDialog.show(MainActivity.this, "", "Uploading File...", true);
                mUpload = new UploadFile(getApplicationContext(), UploadRec.this);
                mUpload.execute(SERVER_URL, selectedFilePath);  //傳入URL,檔案路徑
                mUpload.delegate = UploadRec.this;
            }
            else
                Toast.makeText(UploadRec.this, "Please choose a File First", Toast.LENGTH_SHORT).show();
        }
    }

    //
    private void showFileChooser() {
        Intent intent = new Intent();
        // sets the select file to all types of files
        intent.setType("text/plain"); // file/* 改
        // intent.setType("*/*"); // file/* 改
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        // allows to select data and return it
        intent.setAction(Intent.ACTION_GET_CONTENT);
        // starts new activity to select file and return data
        startActivityForResult(Intent.createChooser(intent, "Choose File to Upload.."), PICK_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PICK_FILE_REQUEST) {
                if (data == null) {
                    //no data present
                    return;
                }
                //讓螢幕保持喚醒(亮度全開)
                PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
                wakeLock.acquire();

                Uri selectedFileUri = data.getData();
                //Toast.makeText(this, selectedFileUri.toString(), Toast.LENGTH_LONG).show();

                selectedFilePath = FilePath.getPath(this, selectedFileUri);
                Log.i(TAG, "Selected File Path:" + selectedFilePath);

                if (selectedFilePath!=null && !selectedFilePath.equals("")) {
                    String filename = selectedFilePath.substring(selectedFilePath.lastIndexOf("/") + 1);
                    tv_FileName.setText(filename);  //只顯示檔名
                }
                else
                    Toast.makeText(this, "Cannot upload file to server ", Toast.LENGTH_LONG).show();

                wakeLock.release();
            }
        }
    }

    @Override
    public void processFinish(String output){
        Toast.makeText(getApplicationContext(), "資料上傳完成", Toast.LENGTH_SHORT).show();
    }
}
