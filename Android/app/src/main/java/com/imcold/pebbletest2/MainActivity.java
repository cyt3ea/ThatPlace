package com.imcold.pebbletest2;

import android.*;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final UUID APP_UUID = UUID.fromString("F1E9423C-524E-4CE0-AFD2-572FD42B60E4");

    private static final int KEY_BUTTON_UP = 0;
    private static final int KEY_BUTTON_DOWN = 1;

    private ViewPager mViewPager;
    private PebbleKit.PebbleDataReceiver mDataReceiver;

    private static final String APP_ID = "80e4eede56844462ef3cdc721208c31f";
    private static final String API_KEY = "AIzaSyDb3Uxtv-8eug7r5Ji2s-RVdfZJml9s0Mk";
    private static final int PERMISSION_ACCESS_COARSE_LOCATION = 1;
    private GoogleApiClient googleApiClient;

    private TextView textView, latitude, longitude, location, date;
    private Button getLocation, viewLocations;
    private Set<String> locations;
    SharedPreferences sp;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sp = getSharedPreferences("THATPLACE", Context.MODE_PRIVATE);
        textView = (TextView) findViewById(R.id.textView);
        latitude = (TextView) findViewById(R.id.latitude);
        longitude = (TextView) findViewById(R.id.longitude);
        location = (TextView) findViewById(R.id.location);
        date = (TextView) findViewById(R.id.date);
        getLocation = (Button) findViewById(R.id.get_location);
        getLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getStats(v.getContext());
            }
        });
        viewLocations = (Button) findViewById(R.id.view_locations);
        viewLocations.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), LocationsActivity.class);
                startActivity(intent);
            }
        });
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { android.Manifest.permission.ACCESS_COARSE_LOCATION },
                    PERMISSION_ACCESS_COARSE_LOCATION);
        }



        googleApiClient = new GoogleApiClient.Builder(this, this, this).addApi(LocationServices.API).build();
    }

    public void getStats(Context c) {
        if (ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

            double lat = lastLocation.getLatitude(), lon = lastLocation.getLongitude();
            latitude.setText("Latitude: " + lat);
            longitude.setText("Longitude: " + lon);
            String units = "imperial";
            String weatherURL = String.format("http://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&units=%s&appid=%s",
                    lat, lon, units, APP_ID);
            Log.v("WEATHER: ", weatherURL);
            String locationURL = "https://maps.googleapis.com/maps/api/geocode/json?latlng=" + lat + "," + lon + "&key=" + API_KEY;
            new GetWeatherTask(textView).execute(weatherURL);
            String[] locationsData = new String[3];
            locationsData[0] = locationURL;
            locationsData[1] = lat + "";
            locationsData[2] = lon + "";
            new GetLocationTask(location).execute(locationsData);

//            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(mDataReceiver == null) {
            mDataReceiver = new PebbleKit.PebbleDataReceiver(APP_UUID) {

                @Override
                public void receiveData(Context context, int transactionId, PebbleDictionary dict) {
                    // Always ACK
                    PebbleKit.sendAckToPebble(context, transactionId);
                    Log.i("receiveData", "Got message from Pebble!");

                    // Up received?
                    if(dict.getInteger(KEY_BUTTON_UP) != null) {
                        getStats(context);
                    }

//                    // Down received?
//                    if(dict.getInteger(KEY_BUTTON_DOWN) != null) {
//                        nextPage();
//                    }
                }

            };
            PebbleKit.registerReceivedDataHandler(getApplicationContext(), mDataReceiver);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_ACCESS_COARSE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // All good!
                } else {
                    Toast.makeText(this, "Need your location!", Toast.LENGTH_SHORT).show();
                }

                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (googleApiClient != null) {
            googleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(MainActivity.class.getSimpleName(), "Connected to Google Play Services!");
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(MainActivity.class.getSimpleName(), "Can't connect to Google Play Services!");
    }

    private class GetLocationTask extends AsyncTask<String, Void, String> {
        private TextView textView;

        public GetLocationTask(TextView textView) {
            this.textView = textView;
        }

        @Override
        protected String doInBackground(String... strings) {
            String location = "UNDEFINED";
            String lat = "";
            String lon = "";
            try {
                URL url = new URL(strings[0]);
                lat = strings[1];
                lon = strings[2];
                Log.v("URL: ", url.toString());
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                InputStream stream = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));
                StringBuilder builder = new StringBuilder();

                String inputString;
                while ((inputString = bufferedReader.readLine()) != null) {
                    builder.append(inputString);
                }

                JSONObject topLevel = new JSONObject(builder.toString());
                Log.v("TOP LEVEL: ", topLevel.toString());
                JSONArray main = topLevel.getJSONArray("results");
                Log.v("ARRAY: ", main.toString());
                JSONObject jsonobject= (JSONObject) main.get(0);
                location = jsonobject.get("formatted_address").toString();
                Log.v("GET ADDRESS: " , location);
//                location = String.valueOf(main.getDouble("formatted_address"));

                urlConnection.disconnect();
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return location + "_" + lat + "_" + lon;
        }

        @Override
        protected void onPostExecute(String temp) {
            textView.setText(temp);
            String currentDateandTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            date.setText(currentDateandTime);
            Set<String> tempLocations = sp.getStringSet("locations", null);
            if(tempLocations != null)
                Log.v("LOCATIONS SET: ", tempLocations.toString());
            else
                Log.v("LOCATIONS SET: ", "null");
//            if(!location.getText().toString().equals("Location...")) {
            if (tempLocations != null) {
                tempLocations.add(currentDateandTime + "_" + location.getText().toString());
            } else {
                tempLocations = new HashSet<String>();
                tempLocations.add(currentDateandTime + "_" + location.getText().toString());
            }
            SharedPreferences.Editor edit = sp.edit();
            edit.clear();
            edit.putStringSet("locations", tempLocations);
            edit.commit();
            cancel(true);
        }
    }

    private class GetWeatherTask extends AsyncTask<String, Void, String> {
        private TextView textView;

        public GetWeatherTask(TextView textView) {
            this.textView = textView;
        }

        @Override
        protected String doInBackground(String... strings) {
            String weather = "UNDEFINED";
            try {
                URL url = new URL(strings[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                InputStream stream = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream));
                StringBuilder builder = new StringBuilder();

                String inputString;
                while ((inputString = bufferedReader.readLine()) != null) {
                    builder.append(inputString);
                }

                JSONObject topLevel = new JSONObject(builder.toString());
                JSONObject main = topLevel.getJSONObject("main");
                weather = String.valueOf(main.getDouble("temp"));

                urlConnection.disconnect();
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return weather;
        }

        @Override
        protected void onPostExecute(String temp) {
            textView.setText("Current Weather: " + temp);
            cancel(true);
        }
    }
}

