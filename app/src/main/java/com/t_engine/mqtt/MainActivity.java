package com.t_engine.mqtt;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.github.mikephil.charting.charts.LineChart;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private EditText lat;
    private EditText lon;
    private Button dataTriggerEvent;
    private FusedLocationProviderClient fuseLocationProviderClient;

    private GoogleMap googleMap;

    final double[] latitude = {0};
    final double[] longitude = {0};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get the SupportMapFragment and request notification when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        lat = findViewById(R.id.latValue);
        lon = findViewById(R.id.lonValue);

        dataTriggerEvent = findViewById(R.id.dataTriggerEvent);

        dataTriggerEvent.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                triggerLocation("1");
            }
        });

        LocationRequest mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(30000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationCallback mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        latitude[0] = Double.valueOf(String.format("%.2f", location.getLatitude()));
                        longitude[0] = Double.valueOf(String.format("%.2f", location.getLongitude()));
                    }
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 44);
        }
        LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(mLocationRequest, mLocationCallback, null);

        startMQTT();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (latitude[0] != 0.0 && latitude[0] != 0.0) {
                    triggerLocation("0");
                }
            }

        },0,30000);
    }

    private void triggerLocation(String code){
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (code.equals("0")) {
                    lat.setText(String.valueOf(latitude[0]));
                    lon.setText(String.valueOf(longitude[0]));
                }

                String currentLat = lat.getText().toString();
                String currentLon = lon.getText().toString();

                LatLng vietnam = new LatLng(Double.valueOf(currentLat), Double.valueOf(currentLon));
                googleMap.addMarker(new MarkerOptions()
                        .position(vietnam)
                        .title("F0 is here - Lat: " + lat.getText().toString() + " - Lon: " + lon.getText().toString()));
                googleMap.moveCamera(CameraUpdateFactory.newLatLng(vietnam));

                JsonObject coordination = new JsonObject();
                coordination.addProperty("code", code);
                coordination.addProperty("lat", currentLat);
                coordination.addProperty("lon", currentLon);

                JsonObject msg = new JsonObject();
                msg.addProperty("value", coordination.toString());
                msg.addProperty("lat", currentLat);
                msg.addProperty("lon", currentLon);
                sendMQTTMessage(new Gson().toJson(msg));
            }
        });
    }

    MQTTHelper mqttHelper;
    private void startMQTT(){
        mqttHelper = new MQTTHelper(getApplicationContext());
        mqttHelper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
                Log.d(" SEND MESSAGE --- ", "true");
            }

            @Override
            public void connectionLost(Throwable throwable) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                Log.d("MQTT:", String.valueOf(mqttMessage.toString()));
            }


            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
    }

    private void sendMQTTMessage(String payload){
        mqttHelper.connectToPublish(payload);
    }

}