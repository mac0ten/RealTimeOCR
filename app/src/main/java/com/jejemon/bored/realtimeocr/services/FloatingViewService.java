package com.jejemon.bored.realtimeocr.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.jejemon.bored.realtimeocr.FileUtil;
import com.jejemon.bored.realtimeocr.R;
import com.jejemon.bored.realtimeocr.utils.UtilsDP;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FloatingViewService extends Service {

    //Floating service related variables
    private static int LAYOUT_FLAG=0;
    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;
    private DisplayMetrics displayMetrics;
    private MotionEvent event;
    private float initialTouchX, initialTouchY;
    int initialX = 0;
    int initialY = 0;

    int initialWidth;
    int initialHeight;
    int widthConstraints=350;
    int heightConstraints=250;

    //IMAGEVIEWS
    private ImageView ivDragresize;

    //LINEARLAYOUTS
    private LinearLayout toolBarLayout;

    //TEXTVIEWS
    private TextView txtOcr;

    //Object/Array variables
    private ArrayList<String> stringFound = new ArrayList<>();
    /** Flag indicating whether we have called bind on the service. */
    boolean mBound;

    //Message pass/received variables
    private final Messenger mMessenger = new Messenger(new IncomingHandler());
    Messenger mService = null;

    //UTILS
    private FileUtil fileUtil;

    //CONST/FINAL variables
    private static final int service1CommunicateService2 = 121;

    private boolean onTouchDragresizeActions(MotionEvent motionEvent) {
        event = motionEvent;
        switch (motionEvent.getAction()){
            case MotionEvent.ACTION_DOWN:
                initialTouchY = event.getRawY();
                initialTouchX = event.getRawX();
                initialHeight = params.height;
                initialWidth = params.width;
                Log.e("x",params.x+" " + event.getRawX());
                Log.e("y",params.y+" " + event.getRawY() );
                autoAdjustIfConstraints();
                break;
            case MotionEvent.ACTION_MOVE:
                params.width = initialWidth + (int)(event.getRawX() - initialTouchX);
                params.height = initialHeight + (int)(event.getRawY() - initialTouchY);
                Log.e("width",params.width+" " + initialWidth);
                Log.e("height",params.height+" " + initialHeight);
                autoAdjustIfConstraints();
                adjustParams(params.x,params.y,params.width,params.height);
                return true;
            case MotionEvent.ACTION_UP:
                autoAdjustIfConstraints();
                return false;
        }
        return false;
    }
    private void autoAdjustIfConstraints(){
        if(params.width < widthConstraints){
            params.width = widthConstraints;
        }
        if(params.height < heightConstraints){
            params.height = heightConstraints;
        }
        if(params.height > (displayMetrics.heightPixels/1.1)){
            params.height = (int) (displayMetrics.heightPixels/1.1);
        }else if(params.height> displayMetrics.heightPixels - UtilsDP.getValueInDP(FloatingViewService.this,10))
            params.height = displayMetrics.heightPixels - UtilsDP.getValueInDP(FloatingViewService.this,10);
        if(params.width > (displayMetrics.widthPixels/1.1)){
            params.width = (int) (displayMetrics.widthPixels/1.1);
        }else if (params.width> displayMetrics.widthPixels - UtilsDP.getValueInDP(FloatingViewService.this,10))
            params.width = displayMetrics.widthPixels - UtilsDP.getValueInDP(FloatingViewService.this,10);

        if(params.x<0) params.x = 0;
        if(params.y<0) params.y = 0;
        if(params.x>displayMetrics.widthPixels - floatingView.getWidth() )
            params.x = displayMetrics.widthPixels - floatingView.getWidth();
        else if( (params.x>(displayMetrics.widthPixels - (UtilsDP.getValueInDP(FloatingViewService.this,10)+params.width))))
            params.x = displayMetrics.widthPixels - (UtilsDP.getValueInDP(FloatingViewService.this,10)+params.width);
        if(params.y>displayMetrics.heightPixels - floatingView.getHeight() )
            params.y = displayMetrics.heightPixels - floatingView.getHeight();
        else if ((params.y>(displayMetrics.heightPixels - (UtilsDP.getValueInDP(FloatingViewService.this,25)+params.height))))
            params.y = displayMetrics.heightPixels - (UtilsDP.getValueInDP(FloatingViewService.this,25)+params.height);
        windowManager.updateViewLayout(floatingView,params);
    }
    private boolean onTouchFloatViewActions(MotionEvent motionEvent) {
        event = motionEvent;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialX = params.x;
                initialY = params.y;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();

                autoAdjustIfConstraints();
                return true;
            case MotionEvent.ACTION_UP:
                Log.e("XY", "onTouch: "+initialX + " " + initialY);
                autoAdjustIfConstraints();
                return true;
            case MotionEvent.ACTION_MOVE:
                params.x = initialX + (int) (event.getRawX() - initialTouchX);
                params.y = initialY + (int) (event.getRawY() - initialTouchY);
                autoAdjustIfConstraints();
                adjustParams(params.x,params.y,params.width,params.height);

                return true;
        }


        return false;
    }

    public class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            // Handle incoming message from the second service
            switch (msg.what){
                case service1CommunicateService2:
                    Log.e("1st service received!", msg.obj+"");
                    break;
            }
        }
    }
    @Override
    public void onCreate() {
        super.onCreate();

        initUtils();
        initViews();
        initTextViews();
        initImageViews();
        initInitialFloatParams();


        //Init Listeners
        initListeners();






        initialWidth = params.width;
        initialHeight = params.height;
        windowManager.addView(floatingView, params);

    }

    private void initListeners() {
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                boolean returnVal = onTouchFloatViewActions(motionEvent);
                windowManager.updateViewLayout(floatingView, params);
                return returnVal;
            }
        });
        ivDragresize.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return false;
            }
        });
        ivDragresize.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                boolean returnVal = onTouchDragresizeActions(motionEvent);
                windowManager.updateViewLayout(floatingView, params);
                return returnVal;
            }
        });
    }

    private void initImageViews() {
        ivDragresize = floatingView.findViewById(R.id.ivDragresize);
    }

    private void initTextViews() {
        txtOcr = floatingView.findViewById(R.id.textOCR);
    }

    private void initViews() {
        floatingView = LayoutInflater.from(this).inflate(R.layout.float_layout,null);
        toolBarLayout = floatingView.findViewById(R.id.linearLayout);
    }

    private void initInitialFloatParams() {
        widthConstraints = UtilsDP.getValueInDP(this,170);
        heightConstraints = UtilsDP.getValueInDP(this,70);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG ,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 100;
        params.y = 100;
        params.width = widthConstraints;
        params.height = heightConstraints;
    }

    private void initUtils() {
        fileUtil = new FileUtil(this);
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            mService = new Messenger(service);
            mBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            mBound = false;
        }
    };
    public void adjustParams(int x,int y,int width,int height) {
        if (!mBound) return;
        Message msg = Message.obtain(null, ScreenCaptureService.MSG_PASS_PARAMS,  new int[]{x,y,width,height});
        msg.replyTo = mMessenger;
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        bindService(new Intent(this, ScreenCaptureService.class), mConnection,
                Context.BIND_AUTO_CREATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        floatingView.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View view, DragEvent dragEvent) {
                if (dragEvent.getAction() == DragEvent.ACTION_DROP) {
                    float x = dragEvent.getX();
                    float y = dragEvent.getY();

                    if (x < displayMetrics.widthPixels / 2) {
                        params.x = 0;
                    } else {
                        params.x = displayMetrics.widthPixels - floatingView.getWidth();
                    }
                    params.y = (int) y;
                    windowManager.updateViewLayout(floatingView, params);
                }
                return true;
            }
        });
        super.onStartCommand(intent, flags, startId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startMyOwnForeground();
        else
            startForeground(1, new Notification());

        return START_STICKY;

    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startMyOwnForeground(){
        String NOTIFICATION_CHANNEL_ID = "com.example.simpleapp";
        String channelName = "My Background Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) windowManager.removeView(floatingView);
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }


    public void toClipboard(String passManualStringCheckRegex) {

        String pasteData = passManualStringCheckRegex;

        String tmpPaste = pasteData.replaceAll("[-\\[\\]^/,'*:.!><~@#$%+=?|\"\\\\()]+", "");
        Pattern p = Pattern.compile("([A-Za-z]+[\\d@]+[\\w@]*|[\\d@]+[A-Za-z]+[\\w@]*)");
        Matcher matcher = p.matcher(tmpPaste);
        while (matcher.find()) {
            String str = matcher.group();

            if (str.matches("[0-9a-zA-Z]{1,}")) {
                String str2 = str;
                int digi = str2.replaceAll("[^0-9]+", "").length();
                int lett = str2.replaceAll("[^a-zA-Z]", "").length();
                if (digi > 0 && lett > 2 && stringFound.indexOf(str.toLowerCase())<0) {
                    stringFound.add(str.toLowerCase());
                    txtOcr.setText(str.toUpperCase());
                    fileUtil.writeToFile(str.toLowerCase());
                }
            }
        }
        Log.e("READFILE",fileUtil.readFromFile());

    }
}
