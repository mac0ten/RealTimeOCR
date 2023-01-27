package com.jejemon.bored.realtimeocr.services;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.OrientationEventListener;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.jejemon.bored.realtimeocr.utils.ImageManipulation;
import com.jejemon.bored.realtimeocr.utils.UtilsDP;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

public class ScreenCaptureService extends Service {
    private final Messenger mMessenger = new Messenger(new IncomingHandler());
    private Messenger mService1Messenger;

    private static final int service1CommunicateService2 = 121;

    private static final String TAG = "ScreenCaptureService";
    private static final String RESULT_CODE = "RESULT_CODE";
    private static final String DATA = "DATA";
    private static final String ACTION = "ACTION";
    private static final String START = "START";
    private static final String STOP = "STOP";
    private static final String CAPTURE = "CAPTURE";
    private static final String SCREENCAP_NAME = "screencap";
    private MediaProjection mMediaProjection;
    private ImageReader mImageReader;
    private Handler mHandler;
    private Display mDisplay;
    private WindowManager windowManager;
    private DisplayMetrics displayMetrics;
    private VirtualDisplay mVirtualDisplay;
    private int mRotation;
    private OrientationChangeCallback mOrientationChangeCallback;
    private TextRecognizer recognizer;
    private Bitmap bitmap = null;
    int[] ocrParams=null;

    Point navBar = null;

    public static final int MSG_PASS_PARAMS = 1;

    public class IncomingHandler extends Handler {

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what){
                case MSG_PASS_PARAMS:
                    int[] args=(int[])msg.obj;
                    ocrParams = args;
                    Log.e("MESSAGE RECEIVED!", "handleMessage: ");
                    mService1Messenger = msg.replyTo;

                break;
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
//        Toast.makeText(getApplicationContext(), "binding", Toast.LENGTH_SHORT).show();
        return mMessenger.getBinder();
    }
    public static Intent getStartIntent(Context context, int resultCode, Intent data) {
        Intent intent = new Intent(context, ScreenCaptureService.class);
        intent.putExtra(ACTION, START);
        intent.putExtra(RESULT_CODE, resultCode);
        intent.putExtra(DATA, data);
        return intent;
    }

    public static Intent getStopIntent(Context context) {
        Intent intent = new Intent(context, ScreenCaptureService.class);
        intent.putExtra(ACTION, STOP);
        return intent;
    }

    public static Intent getCaptureIntent(Context context) {
        Intent intent = new Intent(context, ScreenCaptureService.class);
        intent.putExtra(ACTION, CAPTURE);
        return intent;
    }
    private static boolean isStartCommand(Intent intent) {
        return intent.hasExtra(RESULT_CODE) && intent.hasExtra(DATA)
                && intent.hasExtra(ACTION) && Objects.equals(intent.getStringExtra(ACTION), START);
    }

    private static boolean isStopCommand(Intent intent) {
        return intent.hasExtra(ACTION) && Objects.equals(intent.getStringExtra(ACTION), STOP);
    }

    private static boolean isCaptureCommand(Intent intent) {
        return intent.hasExtra(ACTION) && Objects.equals(intent.getStringExtra(ACTION), CAPTURE);
    }

    private static int getVirtualDisplayFlags() {
        return DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
    }

    private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
        @Override
        public void onImageAvailable(ImageReader reader) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    acquireImage();
                }
            },900);
        }
    }
    private void analyze(Bitmap bitmap){
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        Task<Text> result =
                recognizer.process(image)
                        .addOnSuccessListener(new OnSuccessListener<Text>() {
                            @Override
                            public void onSuccess(Text visionText) {
                                // Task completed successfully
                                // ...
                                Log.e("GOT SOME",visionText.getText().toString());
                                if(mService1Messenger!=null) {
                                    Message message = Message.obtain();
                                    message.what = service1CommunicateService2;
                                    message.obj = visionText.getText();
                                    try {
                                        mService1Messenger.send(message);
                                    } catch (RemoteException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...
                                    }
                                });
    }
    private class OrientationChangeCallback extends OrientationEventListener {

        OrientationChangeCallback(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            final int rotation = mDisplay.getRotation();
            if (rotation != mRotation) {
                mRotation = rotation;
                try {
                    // clean up
                    if (mVirtualDisplay != null) mVirtualDisplay.release();
                    if (mImageReader != null) mImageReader.setOnImageAvailableListener(null, null);

                    // re-create virtual display depending on device width / height
                    createVirtualDisplay();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class MediaProjectionStopCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            Log.e(TAG, "stopping projection.");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mVirtualDisplay != null) mVirtualDisplay.release();
                    if (mImageReader != null) mImageReader.setOnImageAvailableListener(null, null);
                    if (mOrientationChangeCallback != null) mOrientationChangeCallback.disable();
                    mMediaProjection.unregisterCallback(MediaProjectionStopCallback.this);
                }
            });
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        displayMetrics = new DisplayMetrics();
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        // start capture handling thread
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                mHandler = new Handler();
                Looper.loop();
            }
        }.start();
        Toast.makeText(this, "OnCreate", Toast.LENGTH_SHORT).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isStartCommand(intent)) {
            // create notification
//            Pair<Integer, Notification> notification = NotificationUtils.getNotification(this);
//            startForeground(notification.first, notification.second);
            // start projection
            int resultCode = intent.getIntExtra(RESULT_CODE, Activity.RESULT_CANCELED);
            Intent data = intent.getParcelableExtra(DATA);
            startProjection(resultCode, data);
        } else if (isStopCommand(intent)) {
            stopProjection();
            stopSelf();
        } else if(isCaptureCommand(intent)){
            if(bitmap!=null) {
                analyze(bitmap);
//                ocrBitmapCopy.recycle();
            }
        } else {
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    private void startProjection(int resultCode, Intent data) {
        MediaProjectionManager mpManager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (mMediaProjection == null) {
            mMediaProjection = mpManager.getMediaProjection(resultCode, data);
            if (mMediaProjection != null) {
                // display metrics
                windowManager.getDefaultDisplay().getMetrics(displayMetrics);
                WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                mDisplay = windowManager.getDefaultDisplay();

                // create virtual display depending on device width / height
                createVirtualDisplay();

                // register orientation change callback
                mOrientationChangeCallback = new OrientationChangeCallback(this);
                if (mOrientationChangeCallback.canDetectOrientation()) {
                    mOrientationChangeCallback.enable();
                }

                // register media projection stop callback
                mMediaProjection.registerCallback(new MediaProjectionStopCallback(), mHandler);
            }
        }
    }
    private void stopProjection() {
        if (mHandler != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mMediaProjection != null) {
                        mMediaProjection.stop();
                    }
                }
            });
        }
    }

    @SuppressLint("WrongConstant")
    private void createVirtualDisplay() {
        // get width and height
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);

        // start capture reader
        mImageReader = ImageReader.newInstance(displayMetrics.widthPixels, displayMetrics.heightPixels, PixelFormat.RGBA_8888, 2);
        mVirtualDisplay = mMediaProjection.createVirtualDisplay(SCREENCAP_NAME, displayMetrics.widthPixels, displayMetrics.heightPixels,
                displayMetrics.densityDpi, getVirtualDisplayFlags(), mImageReader.getSurface(), null, mHandler);
        mImageReader.setOnImageAvailableListener(new ImageAvailableListener(), mHandler);
//       acquireImage();
    }
    private void acquireImage(){

        FileOutputStream fos = null;
        Bitmap bitmap = null;
        try (Image image = mImageReader.acquireLatestImage()) {
            if (image != null) {


                Image.Plane[] planes = image.getPlanes();
                ByteBuffer buffer = planes[0].getBuffer();
                int pixelStride = planes[0].getPixelStride();
                int rowStride = planes[0].getRowStride();
                int rowPadding = rowStride - pixelStride * displayMetrics.widthPixels;

                int x = ocrParams[0];
                int y = ocrParams[1];
                int width = ocrParams[2];
                int height = ocrParams[3];

                // create bitmap
                bitmap = Bitmap.createBitmap(displayMetrics.widthPixels + rowPadding / pixelStride, displayMetrics.heightPixels, Bitmap.Config.ARGB_8888);
//                bitmap = Bitmap.createBitmap(pixels, displayMetrics.widthPixels,displayMetrics.heightPixels,Bitmap.Config.ARGB_8888);
                bitmap.copyPixelsFromBuffer(buffer);
                // write bitmap to a file
                if(bitmap !=null && ocrParams!=null){
//                    bitmap = Bitmap.createBitmap(bitmap,x,y,width,height);

                    int yTarget = y+ UtilsDP.getStatusBarHeight(this)+UtilsDP.getValueInDP(this,25);
                    int xTarget = x;
                    int wTarget = width-UtilsDP.getValueInDP(this,10);
                    int hTarget = height-UtilsDP.getValueInDP(this,10)-UtilsDP.getStatusBarHeight(this);
                    int testValW = wTarget;
                    int testValH = hTarget;

                    Bitmap newBitmap = bitmap.copy(Bitmap.Config.ARGB_8888,true);
                    Canvas canvas = new Canvas(newBitmap);
                    Paint paint = new Paint();
                    paint.setStrokeWidth(5);
                    paint.setColor(Color.RED);
                    // Parameters for Canvas.drawLines() method:
                    float[] points = {
                            // Starting x coordinate for first line
                            xTarget,
                            // Starting y coordinate for first line
                            yTarget,
                            // Ending x coordinate for first line
                            xTarget+testValW,
                            // Ending y coordinate for first line
                            yTarget,
                            xTarget+testValW,yTarget,xTarget+testValW,yTarget+testValH,
                            xTarget+testValW,yTarget+testValH,xTarget,yTarget+testValH,
                            xTarget,yTarget+testValH,xTarget,yTarget,

                    };
                    canvas.drawLines(points, paint);


                    bitmap = Bitmap.createBitmap(bitmap,xTarget,yTarget,wTarget,hTarget);
                    //TARGET


                    newBitmap = Bitmap.createBitmap(newBitmap,
                            xTarget,
                            yTarget,
                            wTarget,
                            hTarget
                    );
//                    newBitmap = ImageManipulation.invertImage(newBitmap);
                    newBitmap = ImageManipulation.doBlackAndWhite(newBitmap,200);
//                    newBitmap = ImageManipulation.invertImage(newBitmap);
                    newBitmap = ImageManipulation.doRemoveNoise(newBitmap);
//                    storeImage(
//                            ImageManipulation.doBlackAndWhite(
//                                    ImageManipulation.doRemoveNoise(newBitmap)
//                            )
//                    );
                    storeImage(newBitmap);

                    analyze(newBitmap);
//                    analyze(bitmap);
//                    bitmap.recycle();
                }else Toast.makeText(this, "errors", Toast.LENGTH_SHORT).show();
//                Log.e(TAG, "captured image: " + IMAGES_PRODUCED);
            }else{
//                Log.e(TAG, "captured image: " + "EMPTY");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                    Log.e(TAG, "captured image: " + "CLOSED");
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }


        }
    }
    private void storeImage(Bitmap image) {
        File pictureFile = getOutputMediaFile();
        if (pictureFile == null) {
            Log.d(TAG,
                    "Error creating media file, check storage permissions: ");// e.getMessage());
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
    }

    /** Create a File for saving an image or video */
    private  File getOutputMediaFile(){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
                + "/Android/data/"
                + getApplicationContext().getPackageName()
                + "/Files");

        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                return null;
            }
        }
        // Create a media file name
        File mediaFile;
        String mImageName="OCRTroubleShoot.jpg";
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);
        return mediaFile;
    }


}
