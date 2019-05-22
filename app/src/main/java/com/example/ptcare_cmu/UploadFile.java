package com.example.ptcare_cmu;

/*
 * Created by js-mis on 2018/1/16.
 */

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class UploadFile extends AsyncTask<String, Integer, String> {

    private ProgressDialog progressdialog;
    private Activity mParentActivity;
    private Context context;
    public AsyncResponse delegate = null;

    public UploadFile(Context context, Activity parentActivity) {
        super();
        mParentActivity = parentActivity;
        this.context = context;
    }

    @Override
    protected String doInBackground(String... params) {
        try {
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";
            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 1024 * 8; //1024* X
            //todo change URL as per client ( MOST IMPORTANT )
            URL url = new URL(params[0]);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // Allow Inputs &amp; Outputs.
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);

            // Set HTTP method to POST.
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Accept-Charset", "UTF-8");

            conn.setRequestProperty("ENCTYPE", "multipart/form-data");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            // conn.setRequestProperty("uploaded_file",params[1]);

            FileInputStream fileInputStream;
            DataOutputStream outputStream;
            outputStream = new DataOutputStream(conn.getOutputStream());
            outputStream.writeBytes(twoHyphens + boundary + lineEnd);

            outputStream.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\"" + lineEnd);
            outputStream.writeBytes(lineEnd);
            outputStream.writeBytes("my_refrence_text");
            outputStream.writeBytes(lineEnd);
            outputStream.writeBytes(twoHyphens + boundary + lineEnd);

            // Charset.forName("UTF-8").encode(params[1]);
            String fn = URLEncoder.encode(params[1], "utf-8"); //中文檔名

            outputStream.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\"" + fn + "\"" + lineEnd);
            outputStream.writeBytes(lineEnd);

            File file = new File(params[1]);
            // FileInputStream fileInputStream = new FileInputStream(file);
            byte[] bytes = new byte[(int) file.length()];

            fileInputStream = new FileInputStream(file); //傳入檔案路徑
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];

            // Read file
            // long totalSize = fileInputStream.length();
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            int progress = 0;
            int i = 0;
            while (bytesRead > 0) {
                outputStream.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                progress = (int) ((i / (float) bytes.length) * 100);
                publishProgress(progress);
                i += maxBufferSize;
            }

            publishProgress(100);

            outputStream.writeBytes(lineEnd);
            outputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            // Responses from the server (code and message)
            int serverResponseCode = conn.getResponseCode();
            String result = null;
            if (serverResponseCode == 200) {
                StringBuilder s_buffer = new StringBuilder();
                InputStream is = new BufferedInputStream(conn.getInputStream());
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String inputLine;
                while ((inputLine = br.readLine()) != null) {
                    s_buffer.append(inputLine);
                }
                result = s_buffer.toString();
            }
            fileInputStream.close();
            outputStream.flush();
            outputStream.close();
            if (result != null) {
                Log.d("result_for upload", result);
                // file_name = getDataFromInputStream(result, "file_name");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    protected void onPostExecute(String result) {
        //  super.onPostExecute(result);
        //  Toast.makeText(context, result, Toast.LENGTH_SHORT).show();
        delegate.processFinish(result);
        progressdialog.dismiss();
    }

    @Override
    protected void onPreExecute() {
        progressdialog = new ProgressDialog(mParentActivity);
        progressdialog.setMessage("Loading..."); //上傳CSV
        progressdialog.setCancelable(false);
        progressdialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressdialog.show();

    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        Log.d("ANDRO_ASYNC", values[0].toString());
        progressdialog.setProgress(values[0]);
    }

}