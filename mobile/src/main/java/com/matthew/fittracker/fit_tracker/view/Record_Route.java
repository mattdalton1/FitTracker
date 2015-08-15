package com.matthew.fittracker.fit_tracker.view;

import android.content.Context;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.matthew.fittracker.fit_tracker.R;
import com.matthew.fittracker.fit_tracker.logic.DirectionsJSONParser;


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

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.

    private Button startBtn, stopBtn, pauseBtn, resumeBtn;
    private TextView timeDisplay, distanceDisplay, paceDisplay, caloriesBurnedDisplay, optionsText;
    private LocationManager locationManager;
    private LocationListener locList;

    private ArrayList<LatLng> storedLocations;
    private double currentLon, currentLat, prevLon, prevLat;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record__route);
        refXML();
        startBtn.setOnClickListener(this);
        stopBtn.setOnClickListener(this);
        pauseBtn.setOnClickListener(this);
        resumeBtn.setOnClickListener(this);
        init();
    }
    private void init(){
        currentLon = 0.0; currentLat = 0.0; prevLon = 0.0; prevLat = 0.0;
        storedLocations = new ArrayList<LatLng>();
    }
    private  void refXML(){
        startBtn = (Button) findViewById(R.id.startBtn);
        stopBtn = (Button) findViewById(R.id.stopBtn);
        pauseBtn = (Button) findViewById(R.id.pauseBtn);
        resumeBtn = (Button) findViewById(R.id.resumeBtn);
        distanceDisplay = (TextView) findViewById(R.id.distance);
        timeDisplay = (TextView) findViewById(R.id.time);
        paceDisplay = (TextView) findViewById(R.id.pace);
        caloriesBurnedDisplay = (TextView) findViewById(R.id.calories);
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
        mMap.animateCamera((CameraUpdateFactory.zoomTo(30)));
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
            currentLat = location.getLatitude();
            currentLon = location.getLongitude();

            LatLng currentLocation = new LatLng(currentLat,currentLon);
            storedLocations.add(currentLocation);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 4));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(18), null);

            //Toast.makeText(getBaseContext(), "Current Location:\t" + currentLocation, Toast.LENGTH_LONG).show();
            if(storedLocations.size() > 2) {
                // Retrieve previous location
                LatLng previousLocation = storedLocations.get(storedLocations.size() - 2);
             //   Toast.makeText(getBaseContext(), "Previous Location:\t" + previousLocation, Toast.LENGTH_LONG).show();
                prevLat = previousLocation.latitude;
                prevLon = previousLocation.longitude;

                // Getting the URL to the Google Directions API
                String url = getDirectionsUrl(prevLat, prevLon, currentLat, currentLon);

                DownloadTask downloadTask = new DownloadTask(); // Start downloading json data from Google Directions API
                downloadTask.execute(url);
            }
        }
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }
        public void onProviderEnabled(String provider) {

        }
        public void onProviderDisabled(String provider) {

        }
    }
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.startBtn:
                setUpMapIfNeeded();
               // Toast.makeText(getBaseContext(), "Hello", Toast.LENGTH_LONG).show();
                break;
        }
    }
    private String getDirectionsUrl(double preLat,double preLon, double currLat, double currLon){
        String str_origin = "origin="+preLat+","+preLon;
        String str_dest = "destination="+currLat+","+currLon;

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
        String data ="";
        InputStream iStream =null;
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
    private class DownloadTask extends AsyncTask<String, Void, String> {
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
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String,String>>> > {
        protected List<List<HashMap<String, String>>> doInBackground(
                String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;
            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                // Starts parsing data
                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();
            String distance = "";
            String duration = "";

            if (result.size() < 1) {
                Toast.makeText(getBaseContext(), "No Points", Toast.LENGTH_SHORT).show();
                return;
            }
            // Traversing through all the routes
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    if (j == 0) {    // Get distance from the list
                        distance = (String) point.get("distance");
                        continue;
                    } else if (j == 1) { // Get duration from the list
                        duration = (String) point.get("duration");
                        continue;
                    }
                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
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
