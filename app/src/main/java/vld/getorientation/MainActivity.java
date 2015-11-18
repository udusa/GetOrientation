package vld.getorientation;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;
import vld.getorientation.R;

public class MainActivity extends AppCompatActivity implements SensorEventListener,LocationListener,BluetoothSPP.OnDataReceivedListener,BluetoothSPP.BluetoothConnectionListener{

    float[] mags,accels,rot;

    private static final int matrix_size = 16;
    float[] r = new float[matrix_size];
    float[] outR = new float[matrix_size];
    float[] I = new float[matrix_size];
    float[] values = new float[3];

    private SensorManager sm;
    private Sensor acc,mag,baro,rotation;

    double azimuth,pitch,roll;

    String altitude="";

    TextView tv_or,tv_gps,tv_status,tv_serial,tv_time;

    private BluetoothSPP bt;

    DateFormat dateFormat;

    TextView[] tvs = new TextView[5];

    FileWriter fw;

    ImageView im;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvs[0]=tv_or = (TextView)findViewById(R.id.tv_or);
        tvs[1]=tv_gps = (TextView)findViewById(R.id.tv_gps);
        tvs[2]=tv_status= (TextView)findViewById(R.id.tv_status);
        tvs[3]=tv_serial= (TextView)findViewById(R.id.tv_serial);
        tvs[4]=tv_time= (TextView)findViewById(R.id.tv_time);

        im = (ImageView)findViewById(R.id.im);

        sm = (SensorManager)getSystemService(SENSOR_SERVICE);
        acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mag = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        baro = sm.getDefaultSensor(Sensor.TYPE_PRESSURE);
        //rotation = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        dateFormat = new SimpleDateFormat("HH:mm:ss");

        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

        bt = new BluetoothSPP(this);
        if(!bt.isBluetoothAvailable()) {
            Toast.makeText(getApplicationContext()
                    , "Bluetooth is not available"
                    , Toast.LENGTH_SHORT).show();
            finish();
        }
        bt.setOnDataReceivedListener(this);
        bt.setBluetoothConnectionListener(this);
        bt.setDeviceTarget(BluetoothState.DEVICE_OTHER);
        Intent intent = new Intent(getApplicationContext(), DeviceList.class);
        startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);

        fw = new FileWriter(); fw.start();

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float pressure = sensorEvent.values[0];

        if(sensorEvent.accuracy==SensorManager.SENSOR_STATUS_UNRELIABLE) {
            altitude = String.valueOf(SensorManager.getAltitude(
                    SensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressure));
            return;
        }

        switch (sensorEvent.sensor.getType()){
            case Sensor.TYPE_MAGNETIC_FIELD:
                mags = sensorEvent.values.clone();
                break;
            case Sensor.TYPE_ACCELEROMETER:
                accels = sensorEvent.values.clone();
                break;
/*            case Sensor.TYPE_ROTATION_VECTOR:
                TextView tv_rot = (TextView)findViewById(R.id.tv_rot);
                rot = sensorEvent.values.clone();
                String s="";
                for (int i =0 ; i<rot.length;i++){
                    //Log.i("Rot"+i,"-----------"+radToDeg(rot[i])+"-----------");
                    s+="Rot "+i+" : "+radToDeg(rot[i])+"\n";
                }
                tv_rot.setText(s);
                break;*/
        }
        if(mags!=null && accels!=null){
            SensorManager.getRotationMatrix(r, I, accels, mags);
            SensorManager.remapCoordinateSystem(r, SensorManager.AXIS_X, SensorManager.AXIS_Y, outR);
            SensorManager.getOrientation(outR, values);

            azimuth = radToDeg(values[0]);
            pitch = radToDeg(values[1]);
            roll = radToDeg(values[2]);

            tv_or.setText("Azimuth : " + azimuth+"\nPitch : "+pitch+"\nRoll : "+roll+"\nAlt : "+altitude);

            String date = dateFormat.format(Calendar.getInstance().getTime());

            tv_time.setText("Time: "+date);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public double radToDeg(float f){
        return ((f/Math.PI)*180.0);
    }

    public void onStart() {
        super.onStart();
        if (!bt.isBluetoothEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, BluetoothState.REQUEST_ENABLE_BT);
        } else {
            if(!bt.isServiceAvailable()) {
                bt.setupService();
                bt.startService(BluetoothState.DEVICE_OTHER);
                //setup();
            }
        }
    }

    public void onDestroy() {
        super.onDestroy();
        bt.stopService();
        fw.stopWriting();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sm.registerListener(this, acc, SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(this, mag, SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(this, baro, SensorManager.SENSOR_DELAY_NORMAL);
        //sm.registerListener(this, rotation, SensorManager.SENSOR_DELAY_NORMAL);
        //fw  = new FileWriter(); fw.start();

    }

    @Override
    protected void onStop() {
        super.onStop();
        //sm.unregisterListener(this);
       // fw.stopWriting();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //sm.unregisterListener(this);
        //fw.stopWriting();
    }

    // LOCATION LISTENER
    @Override
    public void onLocationChanged(Location location) {
        tv_gps.setText("Lat : " + location.getLatitude() + " \nLon : " + location.getLongitude());
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    // SERIAL LISTENER
    @Override
    public void onDataReceived(byte[] data, String message) {
        String[] msg = message.split(",");
        if(msg.length > 1)
            tv_serial.setText("Distance : "+msg[3]+"\nYaw : "+msg[0]+"\nPitch  : "+msg[1]+"\nRoll : "+msg[2]);
        //tv_serial.setText("Data : "+msg.length);
    }

    @Override
    public void onDeviceConnected(String name, String address) {
        tv_status.setText("BT Status : "+"Connected to : "+name);
    }

    @Override
    public void onDeviceDisconnected() {
        tv_status.setText("BT Status : " + "Disconnected!");
    }

    @Override
    public void onDeviceConnectionFailed() {
        tv_status.setText("BT Status : " + "Fail to connect!");
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
            if(resultCode == Activity.RESULT_OK)
                bt.connect(data);
        }else{
            finish();
        }
    }

    private class FileWriter extends Thread{

        private volatile boolean fileCreated = false,doneWriting = false;
        private final String FILE_NAME = "drone_mapping_",PATH = "/sdcard/";
        private int file_num = 0;
        private FileOutputStream fOut;
        private OutputStreamWriter outputWriter;

        private FileWriter(){
            String file_name = PATH+FILE_NAME+file_num+".txt";
            File file = new File(file_name);
            try {
                while(!file.createNewFile()){
                    file_num++;
                    file_name = PATH+FILE_NAME+file_num+".txt";
                    file = new File(file_name);
                }
                Toast.makeText(getBaseContext(),
                        "File : "+file_name+" Created!",
                        Toast.LENGTH_SHORT).show();
                fOut = new FileOutputStream(file);
                outputWriter = new OutputStreamWriter(fOut);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            while (!doneWriting){
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    for (int i=0;i<tvs.length;i++) {
                        String s = tvs[i].getText().toString();
                        //String s = ""+i;
                        outputWriter.append(s + "\n");
                        //Log.i("tvs",s);
                    }
                    outputWriter.append("-------------------------------------\n");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.i("write", "----------------ERROR Write----------------");
                }
            }


        }

        public void stopWriting(){
            doneWriting=true;
            try {
                outputWriter.close();
                fOut.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.i("close", "----------------ERROR Close----------------");
            }
            Toast.makeText(getBaseContext(),
                    "Done Writing!",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
