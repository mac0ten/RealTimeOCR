package com.jejemon.bored.realtimeocr;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.jejemon.bored.realtimeocr.services.FloatingViewService;
import com.jejemon.bored.realtimeocr.services.ScreenCaptureService;
import com.jejemon.bored.realtimeocr.utils.UtilsDP;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private FileUtil fileUtil; //Helps save and read image for target troubleshoot

    //Overlay for floating window request
    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 1111;


    //IMAGEVIEWS
    private ImageView ivTarget;

    //BUTTONS
    private Button btnCheckTarget
                    ,btnStart
                    ,btnStop;

    //Handler for loop
    private Handler loopHandler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fileUtil = new FileUtil(this);

        //Handler loop
        loopHandler = new Handler();

        //BUTTONS Initialized
        initButtons();
        initListeners();
        initImageViews();
    }

    private void initButtons() {
        btnCheckTarget = findViewById(R.id.btnCheckTarget);
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
    }

    private void initListeners() {
        btnCheckTarget.setOnClickListener(this);
        btnStart.setOnClickListener(this);
        btnStop.setOnClickListener(this);
    }

    private void initImageViews() {
        ivTarget = findViewById(R.id.ivTarget);
    }
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btnCheckTarget:
                startProjection();
                loopHandler.removeCallbacksAndMessages(null);
                loopHandler = new Handler();
                loadImageSaved();
                break;
            case R.id.btnStart:
                startServiceFloat();
                startService(new Intent(this, ScreenCaptureService.class));
                break;
            case R.id.btnStop:
                stopService(new Intent(this, FloatingViewService.class));
                stopProjection();
                break;
        }
    }

    private void loadImageSaved(){
        loopHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = fileUtil.readStoredImage();
                Glide.with(MainActivity.this)
                        .load(bitmap)
                        .into(ivTarget);
                loadImageSaved();
            }
        },1000);
    }
    private void startServiceFloat(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
            }else{
                startService(new Intent(this, FloatingViewService.class));
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show();
                } else {
                    startService(new Intent(this, FloatingViewService.class));
                }
            }
        }else if (requestCode==100){
            if (resultCode == Activity.RESULT_OK) {
                startService(ScreenCaptureService.getStartIntent(this, resultCode, data));
            }
        }
    }

    private void startProjection() {
        MediaProjectionManager mProjectionManager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), 100);
    }

    private void stopProjection() {
        startService(ScreenCaptureService.getStopIntent(this));
    }
}