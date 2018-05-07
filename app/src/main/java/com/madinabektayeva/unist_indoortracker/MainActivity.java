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
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private ImageView map;
    private CustomView customView;
    private Button startButton;
    private Button stopButton;

    private Bitmap bitmap;
    private Canvas pathCanvas;
    private BitmapFactory.Options opt;
    private Paint paint;
    private Path path;


    private float startStepCount;
    private float prevStepCount;
    private float lastStepCount;

    private int x;
    private int y;
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

        sensorServiceCompass = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorServiceSteps = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        compasSensor = sensorServiceCompass.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        stepCountSensor = sensorServiceSteps.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

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
        stepDistance = 1; //TODO() stepDistance
        degree = 0;
        move = "up";
        stepSize = 35;
        walkIsBeingRecorded = false;

        bitmap = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888);
        pathCanvas = new Canvas();
        opt = new BitmapFactory.Options();
        opt.inMutable = true;
        path = new Path();
        paint = new Paint();
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setARGB(255, 126, 192, 238);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);


        x = (int) customView.getWidth() / 2;
        y = (int) customView.getHeight() / 2;


        map.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            public void onGlobalLayout() {
                map.getViewTreeObserver().removeGlobalOnLayoutListener(this);

                int[] locations = new int[2];
                map.getLocationOnScreen(locations);
                x = locations[0];
                y = locations[1];

                int height = map.getHeight();
                int width = map.getWidth();
                int x1 = map.getLeft();
                int y2 = map.getTop();

                int[] offset = getBitmapOffset(map, true);

                Log.d("Log", "x: " + x);
                Log.d("Log", "y: " + y);
                Log.d("Log", "x1: " + x1);
                Log.d("Log", "y2: " + y2);
                Log.d("Log", "height: " + height);
                Log.d("Log", "width: " + width);
                Log.d("Log", "offset 1: " + offset[0]);
                Log.d("Log", "offset 2: " + offset[1]);

                x = offset[1];
                y = offset[0];

            }
        });

        customView.post(new Runnable() {
            @Override
            public void run() {
                bitmap = Bitmap.createBitmap(customView.getWidth(), customView.getHeight(), Bitmap.Config.ARGB_8888);
            }
        });

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
        sensorServiceCompass.unregisterListener(this);
        sensorServiceSteps.unregisterListener((SensorListener) this);
    }

    public void onSensorChanged(SensorEvent sensorEvent) {
        if (walkIsBeingRecorded) {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ORIENTATION) {
                degree = Math.round(sensorEvent.values[0]);
            }
            if (sensorEvent.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
                lastStepCount = sensorEvent.values[0];
                if (startStepCount == -1) {
                    startStepCount = sensorEvent.values[0];
                    prevStepCount = startStepCount;
                }

            }

            if (lastStepCount > prevStepCount + stepDistance) {
                prevStepCount = lastStepCount;
                updateNextMove();
                updatePath();
            }
        }
    }


    public void updateNextMove() {

        if (0 < degree && degree <= 90) {
            move = "left";
        } else if (90 < degree && degree <= 180) {
            move = "up";
        } else if (180 < degree && degree <= 270) {
            move = "right";
        } else {
            move = "down";
        }

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
            Log.d("Log", "x on Touch: " + x);
            Log.d("Log", "y on Touch: " + y);
            Log.d("Log", "x on Touch: " + xCandidate);
            Log.d("Log", "y on Touch: " + yCandidate);
            y-=350;
            path.moveTo(x, y);
            updatePath();
        }

        return true;

    }

    public void updatePath() {

        customView.drawPathMark(x, y, bitmap);

        if (walkIsBeingRecorded) {
            Log.d("Log", "x: " + x);
            Log.d("Log", "y: " + y);

            pathCanvas = new Canvas(bitmap);
            path.lineTo(x, y);
            pathCanvas.drawPath(path, paint);
        }


    }

    public void onAccuracyChanged(Sensor sensor, int i) {

    }


}
