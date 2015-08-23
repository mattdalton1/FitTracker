package com.matthew.fittracker.fit_tracker.view;

import android.content.Context;
import android.content.Intent;
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
    private double currentLon, currentLat, prevLon, prevLat;
    private long startTime, elapsedTime;
    private boolean stopped, displayPopUp = false;
    private String hours, minutes, seconds;

    static final int REQUEST_IMAGE_CAPTURE = 1;
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
        currentLon = 0.0; currentLat = 0.0; prevLon = 0.0; prevLat = 0.0;
        storedLocations = new ArrayList<LatLng>();
        stopped = false;
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
        //stopListening();
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
        locationManager.requestLocationUpdates(provider, 9000, 0, locList); // The LocationManager is a service that listens for GPS coordinates from the device. This code requests that the system call this LocationListener every 8 seconds (8000 milliseconds) provided that the user has moved at least 5 meters from their previous position.
    }
    public class locListener implements LocationListener {
        public void onLocationChanged(Location location) {
            currentLat = location.getLatitude();
            currentLon = location.getLongitude();
            LatLng currentLocation = new LatLng(currentLat,currentLon);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 18));
            storedLocations.add(currentLocation);

            //Toast.makeText(getBaseContext(), "Current Location:\t" + currentLocation, Toast.LENGTH_LONG).show();
            if(storedLocations.size() >= 2) {
                // Get starting location / position
                LatLng previousLocation = storedLocations.get(1);
                // Getting the URL to the Google Directions API
                String url = getDirectionsUrl(previousLocation, currentLocation);
                DownloadTask downloadTask = new DownloadTask(); // Start downloading json data from Google Directions API
                downloadTask.execute(url);
            }
        }
        public void onStatusChanged(String provider, int status, Bundle extras) {}
        public void onProviderEnabled(String provider) {}
        public void onProviderDisabled(String provider) {}
    }
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
                    startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
                break;
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

    private String getDirectionsUrl(LatLng origin,LatLng dest){
        // Origin of route
        String str_origin = "origin="+origin.latitude+","+origin.longitude;
        // Destination of route
        String str_dest = "destination="+dest.latitude+","+dest.longitude;
        // Sensor enabled
        String sensor = "sensor=false";
        // Building the parameters to the web service
        String parameters = str_origin+"&"+str_dest+"&"+sensor;
        // Output format
        String output = "json";
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/"+output+"?"+parameters;

        return url;
    }
    /** A method to download json data from url */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;

        try{
            URL url = new URL(strUrl);
            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();
            // Connecting to url
            urlConnection.connect();
            // Reading data from url
            iStream = urlConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
            StringBuffer sb  = new StringBuffer();
            String line = "";
            while( ( line = br.readLine())  != null){
                sb.append(line);
            }
            data = sb.toString();
            br.close();
        }catch(Exception e){
            Log.d("Exception while downloading url", e.toString());
        }finally{
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }
    // Fetches data from url passed
    private class DownloadTask extends AsyncTask<String, Void, String>{
        // Downloading data in non-ui thread
        protected String doInBackground(String... url) {
            // For storing data from web service
            String data = "";
            try{
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }
        // Executes in UI thread, after the execution of
        // doInBackground()
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            ParserTask parserTask = new ParserTask();
            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);
        }
    }
    /** A class to parse the Google Places in JSON format */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String,String>>> >{
        // Parsing the data in non-ui thread
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {
            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;
            try{
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                // Starts parsing data
                routes = parser.parse(jObject);
            }catch(Exception e){
                e.printStackTrace();
            }
            return routes;
        }
        // Executes in UI thread, after the parsing process
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();
            String distance = "";
            String duration = "";

            if(result.size()<1){
                Toast.makeText(getBaseContext(), "No Points", Toast.LENGTH_SHORT).show();
                return;
            }
            // Traversing through all the routes
            for(int i=0;i<result.size();i++){
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();
                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);
                // Fetching all the points in i-th route
                for(int j=0;j<path.size();j++){
                    HashMap<String,String> point = path.get(j);
                    if(j==0){    // Get distance from the list
                        distance = (String)point.get("distance");
                        continue;
                    }else if(j==1){ // Get duration from the list
                        duration = (String)point.get("duration");
                        continue;
                    }
                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);
                    points.add(position);
                }
                if (points.size() > 0) {
                    // Place the origin marker
                    LatLng orgLocation = points.get(0);
                    mMap.addMarker(new MarkerOptions().position(orgLocation).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                }
                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(2);
                lineOptions.color(Color.RED);
            }
            distanceDisplay.setText(distance);
            // Drawing polyline in the Google Map for the i-th route
            mMap.addPolyline(lineOptions);
        }
    }
}