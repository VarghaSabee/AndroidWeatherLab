package com.example.weatherlab;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.weatherlab.Utilities.Forecast;
import com.example.weatherlab.Utilities.ForecastAdapter;
import com.example.weatherlab.Utilities.RemoteFetch;
import com.example.weatherlab.Utilities.SetListViewHeight;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private TextView mTemp, mHumidity, mTempHigh, mTempLow, mName, mWeather, mWeatherIcon, mUpdatedAt;
    private ListView mListViewForecast;
    private List<Forecast> arrayListForecast;
    private Handler handler;
    private LocationManager locationManager;
    private long tenMin = 10 * 60 * 1000;
    private long oneMile = 1609; // meters

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*Main*/
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        Typeface weatherFont = Typeface.createFromAsset(getAssets(), "fonts/Weather-Fonts.ttf");
        Typeface robotoThin = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Thin.ttf");
        Typeface robotoLight = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");

//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        toolbar.setTitle("Simple Weather");
//        toolbar.inflateMenu(R.menu.menu_main);
//        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
//            @Override
//            public boolean onMenuItemClick(MenuItem item) {
//                if (item.getItemId() == R.id.action_refresh) {
//                    requestDataAndPermissions();
//                }
//                return false;
//            }
//        });

        mListViewForecast = (ListView) findViewById(R.id.listView);
        mListViewForecast.setEnabled(false);
        mTemp = (TextView) findViewById(R.id.temp);
        mHumidity = (TextView) findViewById(R.id.humidity);
        mTempHigh = (TextView) findViewById(R.id.tempHigh);
        mTempLow = (TextView) findViewById(R.id.tempLow);
        mName = (TextView) findViewById(R.id.name);
        mWeather = (TextView) findViewById(R.id.weather);
        mWeatherIcon = (TextView) findViewById(R.id.weatherIcon);
        mUpdatedAt = (TextView) findViewById(R.id.updated_at);

        mWeatherIcon.setTypeface(weatherFont);
        mTemp.setTypeface(robotoThin);
        mName.setTypeface(robotoLight);
        mWeather.setTypeface(robotoLight);

        handler = new Handler();
        arrayListForecast = new ArrayList<>();

        /*Main*/
    }


    @Override
    public void onPause() {
        super.onPause();
        locationManager.removeUpdates(this); // remove the location updates
    }

    @Override
    public void onResume() {
        super.onResume();
        requestDataAndPermissions();
    }

    private void updateWeather(final String lat, final String lon) {
        new Thread() {
            public void run() {
                final JSONObject jsonCurrent = RemoteFetch.getTodayForecast(MainActivity.this, lat, lon);
                final JSONObject jsonForecast = RemoteFetch.getFiveDayForecast(MainActivity.this, lat, lon);
                if (jsonCurrent == null && jsonForecast == null) {
                    handler.post(new Runnable() {
                        public void run() {
                            Toast.makeText(MainActivity.this, "GPS Not Enabled", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    handler.post(new Runnable() {
                        public void run() {
                            renderCurrentWeather(jsonCurrent);
                            renderForecastWeather(jsonForecast);

                            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm:ss");
                            String currentDateandTime = sdf.format(new Date());
                            mUpdatedAt.setText(getResources().getString(R.string.updated_at) + currentDateandTime);

                        }
                    });
                }
            }
        }.start();
    }

    private void renderCurrentWeather(JSONObject json) {
        try {
            JSONObject weather = json.getJSONArray("weather").getJSONObject(0);
            JSONObject main = json.getJSONObject("main");
            mName.setText(json.getString("name"));
            mWeather.setText(weather.getString("description"));
            mTemp.setText(main.getLong("temp") + "" + (char) 0x00B0);
            mTempHigh.setText("MAX: " + main.getLong("temp_max") + "" + (char) 0x00B0);
            mTempLow.setText("MIN: " + main.getLong("temp_min") + "" + (char) 0x00B0);
            mHumidity.setText("Humidity " + main.getString("humidity") + "%");
            setWeatherIcon(weather.getInt("id"), json.getJSONObject("sys").getLong("sunrise") * 1000, json.getJSONObject("sys").getLong("sunset") * 1000);
        } catch (JSONException e) {
            Log.e("CURRENT_JSON_ERROR", e.toString());
        }
    }

    private void renderForecastWeather(JSONObject json) {
        try {
            arrayListForecast.clear(); // clear list, prevent duplicates on refresh
            JSONArray list = json.getJSONArray("list");
            for (int i = 0; i < 6; i++) {
                JSONObject listItem = list.getJSONObject(i);
                JSONObject mainItem = listItem.getJSONObject("main");
                JSONObject weather = listItem.getJSONArray("weather").getJSONObject(0);
                Forecast forecast = new Forecast();
                forecast.setHighTemp(String.valueOf(mainItem.getDouble("temp_max")));
                forecast.setLowTemp(String.valueOf(mainItem.getDouble("temp_min")));
                forecast.setWeather(weather.get("description").toString());
                forecast.setWeatherId(weather.get("id").toString());
                arrayListForecast.add(forecast);
            }
            ForecastAdapter testAdapter = new ForecastAdapter(this, 0, arrayListForecast);
            mListViewForecast.setAdapter(testAdapter);
            SetListViewHeight.setListViewHeight(mListViewForecast);
        } catch (JSONException e) {
            Log.e("FORECAST_JSON_ERROR", e.toString());
        }
    }

    private void requestDataAndPermissions() {
        try {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.INTERNET}, 1);
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.INTERNET}, 1);
                }
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, tenMin, oneMile, MainActivity.this);
        } catch (Exception e) {
            moveTaskToBack(true);
            finish();
            Toast.makeText(MainActivity.this, "Permissions needed", Toast.LENGTH_SHORT).show();
        }
    }

    private void setWeatherIcon(int actualId, long sunrise, long sunset) {
        int id = actualId / 100;
        String icon = "";
        if (actualId == 800) {
            long currentTime = new Date().getTime();
            if (currentTime >= sunrise && currentTime < sunset) {
                icon = getString(R.string.weather_sunny);
            } else {
                icon = getString(R.string.weather_clear_night);
            }
        } else {
            switch (id) {
                case 2:
                    icon = getString(R.string.weather_thunder);
                    break;
                case 3:
                    icon = getString(R.string.weather_drizzle);
                    break;
                case 7:
                    icon = getString(R.string.weather_foggy);
                    break;
                case 8:
                    icon = getString(R.string.weather_cloudy);
                    break;
                case 6:
                    icon = getString(R.string.weather_snowy);
                    break;
                case 5:
                    icon = getString(R.string.weather_rainy);
                    break;
            }
        }
        mWeatherIcon.setText(icon);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onLocationChanged(Location location) {
        updateWeather(location.getLatitude() + "", location.getLongitude() + "");
//        updateWeather(48 + "", 23 + "");
    }

    /**
     * Not being used
     */
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_refresh:
                requestDataAndPermissions();
                break;
            case R.id.action_about:
//                Intent about_intent = new Intent(getApplicationContext(), AboutActivity.class);
//                startActivity(about_intent);
                break;
            case R.id.action_home:
                break;
            case R.id.action_exit:
                moveTaskToBack(true);
                finish();
                break;
        }
        return true;
    }


}
