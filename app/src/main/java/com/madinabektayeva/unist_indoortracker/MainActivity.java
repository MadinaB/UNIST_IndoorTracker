package com.madinabektayeva.unist_indoortracker;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private ImageView map;
    private CustomView customView;
    private TextView text_field;

    private Button startButton;
    private Button stopButton;
    private WifiManager wifiManager;


    private Bitmap bitmap;
    private Canvas pathCanvas;
    private BitmapFactory.Options opt;
    private Paint paint;
    private Path path;


    private float startStepCount;
    private float prevStepCount;
    private float lastStepCount;

    private int linkspeed;
    private int  newRssi;
    private int  level;
    private int  percentage;
    private String macAdd;
    private int leftRowX, rightRowX,upperColumnY, lowerColumnY;

    private boolean running;
    private int x;
    private int y;
    private int heightDifference;
    private String text;
    private String degrees_text;
    private String steps_text;

    private int stepSize;
    private int stepDistance;
    private String move;
    private boolean walkIsBeingRecorded;

    private int degree;

    private static SensorManager sensorServiceCompass;
    private static SensorManager sensorServiceSteps;
    private Sensor compasSensor;
    private Sensor stepCountSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        map = (ImageView) findViewById(R.id.map);
        customView = (CustomView) findViewById(R.id.customView);
        text_field = (TextView) findViewById(R.id.text_field);

        sensorServiceCompass = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorServiceSteps = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        compasSensor = sensorServiceCompass.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        stepCountSensor = sensorServiceSteps.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        startButton = (Button) findViewById(R.id.start_walk);
        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                walkIsBeingRecorded = true;
            }
        });

        stopButton = (Button) findViewById(R.id.stop_walk);
        stopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                walkIsBeingRecorded = false;
            }
        });



        startStepCount = -1;
        prevStepCount = 0;
        lastStepCount = 0;
        stepDistance = 5; //TODO() stepDistance
        degree = 0;
        move = "up";
        stepSize = 50;
        heightDifference = 210;
        walkIsBeingRecorded = false;
        running = false;
        leftRowX = 400;
        rightRowX = 542;
        upperColumnY = 486;
        lowerColumnY = 705;

        bitmap = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888);
        pathCanvas = new Canvas();
        opt = new BitmapFactory.Options();
        opt.inMutable = true;

        path = new Path();

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setARGB(255, 126, 192, 238);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);


        x = leftRowX;
        y = upperColumnY;
        path.moveTo(x, y);

        text ="";
        degrees_text="";
        steps_text="";


        customView.post(new Runnable() {
            @Override
            public void run() {
                bitmap = Bitmap.createBitmap(customView.getWidth(), customView.getHeight(), Bitmap.Config.ARGB_8888);
            }
        });


        if(wifiManager.isWifiEnabled()){
            Log.d("Log", "wifi enabled: " + wifiManager.isWifiEnabled());
            updateWifiData();
            updateLocationBasedOnWifi();
            path.moveTo(x, y);
            updatePath();
        }


    }

    public static int[] getBitmapOffset(ImageView img, Boolean includeLayout) {

        int[] offset = new int[2];
        float[] values = new float[9];

        Matrix m = img.getImageMatrix();
        m.getValues(values);

        offset[0] = (int) values[5];
        offset[1] = (int) values[2];

        if (includeLayout) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) img.getLayoutParams();
            int paddingTop = (int) (img.getPaddingTop());
            int paddingLeft = (int) (img.getPaddingLeft());

            offset[0] += paddingTop + lp.topMargin;
            offset[1] += paddingLeft + lp.leftMargin;
        }
        return offset;
    }


    @Override
    protected void onResume() {
        super.onResume();
        running = true;
        if (compasSensor != null) {
            sensorServiceCompass.registerListener(this, compasSensor, SensorManager.SENSOR_DELAY_FASTEST);
        } else {
            Toast.makeText(MainActivity.this, "Not supported", Toast.LENGTH_SHORT).show();
        }

        if (stepCountSensor != null) {
            sensorServiceSteps.registerListener((SensorEventListener) this, stepCountSensor, SensorManager.SENSOR_DELAY_UI);

        } else {
            Toast.makeText(this, "Sensor not found", Toast.LENGTH_SHORT).show();
        }

    }

    protected void onPause() {
        super.onPause();
        running = false;
        sensorServiceCompass.unregisterListener(this);
        sensorServiceSteps.unregisterListener((SensorListener) this);
    }

    public void onSensorChanged(SensorEvent sensorEvent) {
        if (walkIsBeingRecorded) {

            if (sensorEvent.sensor.getType() == Sensor.TYPE_ORIENTATION) {
                degree = Math.round(sensorEvent.values[0]);
                degrees_text = Integer.toString(degree) + (char) 0x00B0;
            }
            if (sensorEvent.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
                lastStepCount = sensorEvent.values[0];
                if (startStepCount == -1) {
                    startStepCount = sensorEvent.values[0];
                    prevStepCount = startStepCount;
                }
                if (running) {
                    steps_text = "" + String.valueOf(sensorEvent.values[0] - startStepCount);
                }

            }

            updateInfo();

            if (lastStepCount > prevStepCount + stepDistance) {
                prevStepCount = lastStepCount;
                updateNextMove();
                updatePath();
            }


        }
    }

    public void updateWifiData(){
        wifiManager.startScan();
        linkspeed = wifiManager.getConnectionInfo().getLinkSpeed();
        newRssi = wifiManager.getConnectionInfo().getRssi();
        level = wifiManager.calculateSignalLevel(newRssi, 10);
        percentage = (int) ((level/10.0)*100);
        macAdd = wifiManager.getConnectionInfo().getBSSID();
    }

    public void updateInfo(){
        text ="                      "+degrees_text+"             "+steps_text;
        text_field.setText(text);

    }


    public void updateNextMove() {
        if((290<degree &&degree <=360)||(degree>=0&&degree<=20)){
            move = "down";
        }else if(20<degree&&degree<=110){
            move = "left";
        }else if(110<degree&&degree<=200){
            move = "up";
        }else if(200<degree&&degree<=290){
            move = "right";
        }
        /*
        if (0 < degree && degree <= 90) {
            move = "left";
        } else if (90 < degree && degree <= 180) {
            move = "up";
        } else if (180 < degree && degree <= 270) {
            move = "right";
        } else {
            move = "down";
        }*/

        Log.d("Log", "Move: " + move);
        Log.d("Log", "Degree: " + degree);

        if (move.equals("up")) {
            goUp();
        } else if (move.equals("down")) {
            goDown();
        } else if (move.equals("left")) {
            goLeft();
        } else if (move.equals("right")) {
            goRight();
        }

    }


    public void goRight() {
        x += stepSize;
    }

    public void goLeft() {
        x -= stepSize;
    }

    public void goUp() {
        y -= stepSize;

    }

    public void goDown() {
        y += stepSize;
    }

    public boolean onTouchEvent(MotionEvent event) {

        if (!walkIsBeingRecorded) {
            float xCandidate = event.getRawX();
            float yCandidate = event.getRawY();
            x = (int) xCandidate;
            y = (int) yCandidate;
            y-=heightDifference;
            Log.d("Log", "x on Touch: " + x);
            Log.d("Log", "y on Touch: " + y);
            path.moveTo(x, y);
            updatePath();
        }

        return true;

    }

    public void updatePath() {

        equalize();
        customView.drawPathMark(x, y, bitmap);

        if (walkIsBeingRecorded) {
            Log.d("Log", "x: " + x);
            Log.d("Log", "y: " + y);

            pathCanvas = new Canvas(bitmap);
            path.lineTo(x, y);
            pathCanvas.drawPath(path, paint);
        }


    }

    public void equalize(){

        if((upperColumnY-(stepSize-1))<=y&&y<=(upperColumnY+(stepSize-1))){ // row 1  //nearly 50
            y = upperColumnY;
        }else if((lowerColumnY-(stepSize-1))<=y&&y<=(lowerColumnY+(stepSize-1))){ //row2 // nearly 50
            y = lowerColumnY;
        }

        if((leftRowX-(stepSize-1))<=x&&x<=(leftRowX+(stepSize-1))){
            x = leftRowX;
        }else if((rightRowX-(stepSize-1))<=x&&x<=(rightRowX+(stepSize-1))){
            x = rightRowX;
        }

       // if((x<(leftRowX-(stepSize-1))&&250<x)&&((y<(lowerColumnY-(stepSize-1))&&((upperColumnY+(stepSize-1)<y))))){

       // }
        /*
        * if(462<=y&&y<=511){ // row 1
            y = 486;
        }else if(685<=y&&y<=725){ //row2
            y = 705;
        }

        if(384<=x&&x<=424){
            x = 400;
        }else if(520<=x&&x<=565){
            x = 542;
        }
        * */
    }

    public void updateLocationBasedOnWifi(){
        Log.d("log", "macAdd: " + macAdd);
        Log.d("log", "macAdd equals?: " + macAdd.equals("94:d4:69:fa:7e:c0"));
        Log.d("log", "newRssi: " + newRssi);
        Log.d("log", "newRssi fits?: " + (-51<=newRssi&&newRssi<=-40));
        if(macAdd.equals("94:d4:69:fa:7e:c0")&& (-51<=newRssi&&newRssi<=-44)){  // 44, 45, 47, 48, 50
            // point 1
            x = leftRowX; y = 360;
        }else if(macAdd.equals("94:d4:69:fa:7e:c0")&& (-70<=newRssi&&newRssi<=-61)||
                (macAdd.equals("38:20:56:7e:de:80")&& (-46<=newRssi&&newRssi<=-40))){
            // point 2
            x = rightRowX; y = 360;
        }else if(macAdd.equals("94:d4:69:fa:7e:c0")&& (-60<=newRssi&&newRssi<=-55)){
            // point 3
            x = 130; y = upperColumnY;
        }else if(macAdd.equals("94:d4:69:fa:7e:c0")&& (-55<=newRssi&&newRssi<=-52)){
            // point 4
            x = 245; y = upperColumnY;
        }else if(macAdd.equals("94:d4:69:fa:7e:c0")&& (-43<=newRssi&&newRssi<=-33)){  //33 once, 37 a lot, 39, 42, 45
            // point 5
            x = leftRowX; y = upperColumnY;
        }else if(macAdd.equals("38:20:56:7e:de:80")&& (-37<=newRssi&&newRssi<=-30)){
            // point 6
            x = rightRowX; y = upperColumnY;
        }else if((macAdd.equals("38:20:56:7e:de:80")&& (-76<=newRssi&&newRssi<=-68))
                ||(macAdd.equals("94:d4:69:fa:83:40")&& (-52<=newRssi&&newRssi<=-46))
                ){
            // point 7
            x = leftRowX; y = lowerColumnY;
        }else if(macAdd.equals("94:d4:69:fa:83:40")&& (-72<=newRssi&&newRssi<=-66)){
            // point 8
            x = rightRowX; y = lowerColumnY;
        }else if(macAdd.equals("94:d4:69:fa:83:40")&& (-40<=newRssi&&newRssi<=-29)){ // correct 94:d4:69:fa:83:40  29-40
            // point 9
            x = leftRowX; y = 775;
        }else{
            x = leftRowX; y = lowerColumnY;
        }
    }


        public void onAccuracyChanged(Sensor sensor, int i) {

    }


}
