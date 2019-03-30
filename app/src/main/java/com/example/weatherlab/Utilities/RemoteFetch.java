package com.example.weatherlab.Utilities;


import android.content.Context;


import com.example.weatherlab.R;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class RemoteFetch {

    private static final String OPEN_WEATHER_MAP_API = "http://api.openweathermap.org/data/2.5/weather?";

    public static JSONObject getTodayForecast(Context context, String lat, String longitude) {
        try {
            URL url = new URL(OPEN_WEATHER_MAP_API + "lat=" + lat + "&lon=" + longitude + "&units=metric");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

//            connection.addRequestProperty("x-api-key", "f6c3380188904170596692a6172550e9");
            connection.addRequestProperty("x-api-key", context.getResources().getString(R.string.open_weather_maps_app_id));
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder json = new StringBuilder(1024);
            String tmp;
            while ((tmp = reader.readLine()) != null) json.append(tmp).append("\n");
            reader.close();
            JSONObject data = new JSONObject(json.toString());
            System.out.print(data.getInt("cod"));

            if (data.getInt("cod") != 200) {
                return null;
            }
            return data;
        } catch (Exception e) {
            return null;
        }
    }

    private static final String OPEN_WEATHER_MAP_API_SEVEN_DAY = "http://api.openweathermap.org/data/2.5/forecast?";

    public static JSONObject getFiveDayForecast(Context context, String lat, String longitude) {
        try {
            URL url = new URL(OPEN_WEATHER_MAP_API_SEVEN_DAY + "lat=" + lat + "&lon=" + longitude + "&cnt=6&mode=json" + "&units=metric");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.addRequestProperty("x-api-key", context.getResources().getString(R.string.open_weather_maps_app_id));
//            connection.addRequestProperty("x-api-key", "f6c3380188904170596692a6172550e9");
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder json = new StringBuilder(1024);
            String tmp;
            while ((tmp = reader.readLine()) != null) json.append(tmp).append("\n");
            reader.close();
            JSONObject data = new JSONObject(json.toString());
            System.out.print(data.getInt("cod"));
            if (data.getInt("cod") != 200) {
                return null;
            }
            return data;
        } catch (Exception e) {
            return null;
        }
    }
}
