package vld.getorientation;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import vld.getorientation.R;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    float[] mags,accels;

    private static final int matrix_size = 16;
    float[] r = new float[matrix_size];
    float[] outR = new float[matrix_size];
    float[] I = new float[matrix_size];
    float[] values = new float[3];

    private SensorManager sm;
    private Sensor acc,mag;

    double azimuth,pitch,roll;

    TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv = (TextView)findViewById(R.id.tv);

        sm = (SensorManager)getSystemService(SENSOR_SERVICE);
        acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mag = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.accuracy== SensorManager.SENSOR_STATUS_UNRELIABLE)
            return;

        switch (sensorEvent.sensor.getType()){
            case Sensor.TYPE_MAGNETIC_FIELD:
                mags = sensorEvent.values.clone();
                break;
            case Sensor.TYPE_ACCELEROMETER:
                accels = sensorEvent.values.clone();
                break;
        }
        if(mags!=null && accels!=null){
            SensorManager.getRotationMatrix(r, I, accels, mags);
            SensorManager.remapCoordinateSystem(r, SensorManager.AXIS_X,
                    SensorManager.AXIS_Y, outR);
            SensorManager.getOrientation(outR, values);

            azimuth = radToDeg(values[0]);
            pitch = radToDeg(values[1]);
            roll = radToDeg(values[2]);

            tv.setText("Azimuth : "+azimuth+"\nPitch : "+pitch+"\nRoll : "+roll);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public double radToDeg(float f){
        return ((f/Math.PI)*180.0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sm.registerListener(this, acc, SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(this,mag,SensorManager.SENSOR_DELAY_NORMAL);

    }

    @Override
    protected void onStop() {
        super.onStop();
        sm.unregisterListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sm.unregisterListener(this);
    }
}
