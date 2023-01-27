package com.jejemon.bored.realtimeocr.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class FileUtil {
    private Context context;
    public FileUtil(Context context) {
        this.context = context;
    }

//    public void appendToFile(String string, String fileName) {
//        try {
//            // encryption
//            Key key = new SecretKeySpec("mysecretkey".getBytes(), "AES");
//            Cipher cipher = Cipher.getInstance("AES");
//            cipher.init(Cipher.ENCRYPT_MODE, key);
//            byte[] encryptedString = cipher.doFinal(string.getBytes());
//
//            // append to file
//            File file = getOutputMediaFile(fileName);
//            FileOutputStream fos = new FileOutputStream(file, true);
//            fos.write(encryptedString);
//            fos.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
    public void writeToFile(String data) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("config.txt", Context.MODE_APPEND));
            outputStreamWriter.write(data+"\r\n");
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }
    public String readFromFile() {

        String ret = "";

        try {
            InputStream inputStream = context.openFileInput("config.txt");

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append("\n").append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return ret;
    }
    private Bitmap readStoredImage() {
        Bitmap bitmap = null;
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
                + "/Android/data/"
                + context.getApplicationContext().getPackageName()
                + "/Files");
        String filePath = mediaStorageDir.getPath() + File.separator + "OCRTroubleShoot.jpg";
        try {
            File file = new File(filePath);
            bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        } catch (Exception e) {
            Log.d("ErrorImage" ,e.getMessage());
        }
        return bitmap;
    }
}
