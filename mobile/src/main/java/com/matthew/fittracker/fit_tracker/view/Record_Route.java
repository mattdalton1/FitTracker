package com.matthew.fittracker.fit_tracker.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.matthew.fittracker.fit_tracker.R;
import com.matthew.fittracker.fit_tracker.logic.DirectionsJSONParser;
import com.matthew.fittracker.fit_tracker.logic.MiniMenu;
import com.matthew.fittracker.fit_tracker.logic.UpdateTime;


import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Record_Route extends FragmentActivity implements View.OnClickListener {
    private MiniMenu popUp;
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Button startBtn, stopBtn, pauseBtn, resumeBtn, optionsBtn;
    private TextView timeDisplay, distanceDisplay, paceDisplay, caloriesBurnedDisplay, optionsText;
    private CheckBox satOption, terrOption, takeAPhoto;

    private LocationManager locationManager;
    private LocationListener locList;

    private ArrayList<LatLng> storedLocations;
    private double currentLon, currentLat, previousLon, previousLat, totalDistance;
    private long startTime, elapsedTime;
    private boolean stopped, displayPopUp = false;
    private String hours, minutes, seconds;
    private PolylineOptions polylineOptions;
    private MarkerOptions markerOptions;

    private int unitindex;
    private final static double[] multipliers = {
            1.0,1.0936133,0.001,0.000621371192
    };
    private final static String[] unitstrings = {
            "m", "y", "km", "mi"
    };

    static final int REQUEST_IMAGE_CAPTURE_CODE = 100;
    private Bitmap imageData;
    /*
        How often should update the timer to show how much time has elapsed.
        Value in milliseconds. If set to 100, then every tenth of a second will update the timer
     */
    private final int REFRESH_RATE = 100;
    private Handler mHandler = new Handler();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record__route);
        refWidgets();
        startBtn.setOnClickListener(this);
        stopBtn.setOnClickListener(this);
        pauseBtn.setOnClickListener(this);
        resumeBtn.setOnClickListener(this);
        optionsBtn.setOnClickListener(this);
        popUp.setVisibility(View.GONE);
        satOption.setOnClickListener(this);
        terrOption.setOnClickListener(this);
        takeAPhoto.setOnClickListener(this);
        init();
    }
    private void init(){
        currentLon = 0.0; currentLat = 0.0; previousLon = 0.0; previousLat = 0.0; totalDistance = 0.0;
        unitindex = 2;
        storedLocations = new ArrayList<LatLng>();
        stopped = false;
        imageData = null;
        polylineOptions = new PolylineOptions();
        polylineOptions.color(Color.BLUE);
        polylineOptions.width(3);
        markerOptions = new MarkerOptions();
    }
    private void refWidgets(){
        popUp = (MiniMenu) findViewById(R.id.popupWindow);
        startBtn = (Button) findViewById(R.id.startBtn);
        stopBtn = (Button) findViewById(R.id.stopBtn);
        pauseBtn = (Button) findViewById(R.id.pauseBtn);
        resumeBtn = (Button) findViewById(R.id.resumeBtn);
        optionsBtn = (Button) findViewById(R.id.optionsBtn);
        distanceDisplay = (TextView) findViewById(R.id.distance);
        timeDisplay = (TextView) findViewById(R.id.time);
        paceDisplay = (TextView) findViewById(R.id.pace);
        caloriesBurnedDisplay = (TextView) findViewById(R.id.calories);
        satOption = (CheckBox) findViewById(R.id.mapTypeSatellite);
        terrOption = (CheckBox) findViewById(R.id.mapTypeTerrain);
        takeAPhoto = (CheckBox) findViewById(R.id.takePhoto);
    }
    protected void onPause(){
        super.onPause();
    }
    protected void onResume(){
        super.onResume();
    }
    protected void onStop(){
        super.onStop();
    }
    protected void onDestroy(){
        storedLocations.clear();
        mMap = null;
        super.onDestroy();
    }
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }
    private void setUpMap() {
        // Get the Location Manager object from the "System Service LOCATION_SERVICE
        if(locationManager == null){
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE); // Requires acces course location permission in the manifest file
        }
        mMap.setMyLocationEnabled(true); // An indication of current location of device + get updates
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        mMap.animateCamera((CameraUpdateFactory.zoomTo(18)));

        // Create a criteria object to retrieve provider - how accurate do we need, power, depends on the settings you have on maps
        final Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_LOW);

        // Get the name of best provider
        final String provider = locationManager.getBestProvider(criteria, true);
        // Define a listener that responds to location updates
        locList = new locListener();
        locationManager.requestLocationUpdates(provider, 7000, 0, locList); // The LocationManager is a service that listens for GPS coordinates from the device. This code requests that the system call this LocationListener every 8 seconds (8000 milliseconds) provided that the user has moved at least 5 meters from their previous position.
    }
    public class locListener implements LocationListener {

        public void onLocationChanged(Location location) {

            if(location.getAccuracy() < 50){
                currentLat = location.getLatitude();
                currentLon = location.getLongitude();
                LatLng currentLocation = new LatLng(currentLat,currentLon);

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 20));
                markerOptions.position(currentLocation);
                storedLocations.add(currentLocation);

                if(storedLocations.size() > 0){
                    LatLng initialLocation = storedLocations.get(0);
                    mMap.addMarker(new MarkerOptions().position(initialLocation).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                    polylineOptions.add(initialLocation);
                }
                markerOptions.title("Creating Route");
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.bluedot));
                mMap.addMarker(markerOptions);

                polylineOptions.add(currentLocation);
                mMap.addPolyline(polylineOptions);

                //Toast.makeText(getBaseContext(), "Current Location:\t" + currentLocation, Toast.LENGTH_LONG).show();
                if(storedLocations.size() > 1) {
                    // Get Initial position
                    //LatLng initialLocation = storedLocations.get(0);
                    //markerOptions.position(initialLocation);
                    //mMap.addMarker(markerOptions);

                    // Get previous position
                    LatLng previousLocation = storedLocations.get(storedLocations.size() - 2);
                    //LatLng orginalPosition = storedLocations.get(0);

                    previousLat = previousLocation.latitude;
                    previousLon = previousLocation.longitude;

                    double dis = calculateDistance(previousLat, previousLon, currentLat, currentLon);
                    dis = roundDecimal(dis,3);
                    totalDistance += dis;
                    String distanceText = " " + totalDistance + " " + unitstrings[unitindex];
                    distanceDisplay.setText(distanceText);
                }
            }else {}
        }
        public void onStatusChanged(String provider, int status, Bundle extras) {}
        public void onProviderEnabled(String provider) {}
        public void onProviderDisabled(String provider) {}
    }
    /*
        Calculating Distance:
     */
    private double calculateDistance(double prevLat, double prevLon,  double currLat, double currLon){
        double distance = 0.0;
        try{
            float[] results = new float[3];
            Location.distanceBetween(prevLat, prevLon, currLat, currLon, results);
            distance = results[0] * multipliers[unitindex];
        }
        catch(final Exception ex){
            distance = 0.0;
        }
        return distance;
    }
    public double roundDecimal(double value, int decimalPlace){
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(decimalPlace, 6);
        return bd.doubleValue();
    }
    /*

     */
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.startBtn:
                setUpMapIfNeeded();
                startBtn.setVisibility(View.INVISIBLE);
                pauseBtn.setVisibility(View.VISIBLE);
                if(stopped)
                    startTime = System.currentTimeMillis() - elapsedTime;
                else
                    startTime = System.currentTimeMillis();
                /*  Check to ensure that there are no instances of the startTier runnable currently running
                    and then starts a new thread to update the timer every one tenth of a second
                 */
                mHandler.removeCallbacks(startTimer);
                mHandler.postDelayed(startTimer, 0);
                break;
            case R.id.pauseBtn:
                pauseBtn.setVisibility(View.INVISIBLE);
                resumeBtn.setVisibility(View.VISIBLE);
                mHandler.removeCallbacks(startTimer);
                stopped = true;
                onPause();
                break;
            case R.id.resumeBtn:
                resumeBtn.setVisibility(View.INVISIBLE);
                pauseBtn.setVisibility(View.VISIBLE);
                // Check if the stop watch was stopped if it was then the stopwatch should resume from where it left off, this is done by adding the already elapsed time to the new calculations of the elapsed time.
                if(stopped)
                    startTime = System.currentTimeMillis() - elapsedTime;
                else
                    startTime = System.currentTimeMillis();

                mHandler.removeCallbacks(startTimer);
                mHandler.postDelayed(startTimer, 0);
                onResume();
                break;
            case R.id.stopBtn:
                // The handler stops the application from looping in the runnable event
                mHandler.removeCallbacks(startTimer);
                stopped = true;
                Intent resultIntent = new Intent(Record_Route.this, Exercise_Results.class);
                resultIntent.putExtra("imageName", imageData);
                startActivity(resultIntent);
                break;
            case R.id.optionsBtn:
                if(displayPopUp == false) {
                    displayPopUp = true;
                    popUp.setVisibility(View.VISIBLE);
                }
                else if(displayPopUp == true) {
                    displayPopUp = false;
                    popUp.setVisibility(View.INVISIBLE);
                }
                break;
            case R.id.mapTypeSatellite:
                if(satOption.isChecked()){
                    terrOption.setChecked(false);
                    if(locationManager == null) {}
                    else
                        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                }
                break;
            case R.id.mapTypeTerrain:
                if(terrOption.isChecked()){
                    satOption.setChecked(false);
                    if(locationManager == null) {}
                    else
                        mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                }
                break;
            case R.id.takePhoto:
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (cameraIntent.resolveActivity(getPackageManager()) != null)
                    startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE_CODE);
                break;
        }
    }
    /*
        The Android Camera application encodes the photo in the return Intent delivered to onActivityResult() as a small Bitmap in the extras,
        under the key "data".
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == REQUEST_IMAGE_CAPTURE_CODE){
            if(requestCode == RESULT_OK){
                this.imageData = (Bitmap) data.getExtras().get("data");

            }
            else if(resultCode == RESULT_CANCELED) {}
            else {}
        }
    }
    /* Start the timer
     * The timer runnable calculates the elapsed time by subtracting the current time from the start time,
     * then updates the TextView with the elapsed time so it can be seen,
     * then waits for 1/10 second (Refresh rate) and checks the elapsed time again
    */
    private Runnable startTimer = new Runnable(){
        public void run(){
            elapsedTime = System.currentTimeMillis() - startTime;
            UpdateTime timer = new UpdateTime();
            timer.updateTimer(elapsedTime);
            seconds = timer.getSeconds();
            minutes = timer.getMinutes();
            hours = timer.getHours();
            timeDisplay.setText(hours + ":" + minutes + ":"  + seconds);
            mHandler.postDelayed(this, REFRESH_RATE);
        }
    };


}