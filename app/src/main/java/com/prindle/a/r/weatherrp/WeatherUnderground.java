package com.prindle.a.r.weatherrp;

import android.location.Location;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by RP on 1/18/2015.
 */
public class WeatherUnderground {
    private static final String TAG = WeatherUnderground.class.getSimpleName();
    private static final String WEATHERUNDERGROUND_API_KEY = ""; //ToDo enter API Key
    private static final String WEATHERUNDERGROUND_URL = "https://api.wunderground.com/api/";


    private CurrentWeather mCurrentWeather;
    private List<FutureWeather> mTotalWeather;

    public WeatherUnderground() {
        mCurrentWeather = new CurrentWeather();

    }

    public String getForecastURL(Location location) {
        return WEATHERUNDERGROUND_URL + WEATHERUNDERGROUND_API_KEY + "/forecast/q/" + location.getLatitude() + "," + location.getLongitude() + ".json";
    }

    public String getCurrentURL(Location location) {
        return WEATHERUNDERGROUND_URL + WEATHERUNDERGROUND_API_KEY + "/conditions/q/" + location.getLatitude() + "," + location.getLongitude() + ".json";
    }


    public void setCurrentWeatherData(String jsonData) throws JSONException {
        CurrentWeather currentWeather = new CurrentWeather();
        JSONObject weather = new JSONObject(jsonData);
        JSONObject currently = weather.getJSONObject("current_observation");


        currentWeather.setHumidity(currently.getString("relative_humidity"));
        currentWeather.setTime(Long.parseLong(currently.getString("observation_epoch")));
        currentWeather.setIcon(currently.getString("icon"));
        currentWeather.setIconUrl(currently.getString("icon_url"));
        currentWeather.setPrecip(currently.getString("precip_today_string"));
        currentWeather.setSummary(currently.getString("weather"));
        currentWeather.setTemperature(currently.getDouble("temp_f"));
        currentWeather.setTimezone(currently.getString("local_tz_long"));

        mCurrentWeather = currentWeather;
    }

    public CurrentWeather getCurrentWeather() {
        return mCurrentWeather;
    }


    public void setFutureWeatherData(String jsonData) throws JSONException {

        JSONObject weather = new JSONObject(jsonData);
        JSONObject forecast = weather.getJSONObject("forecast");
        JSONObject txtforecast = forecast.getJSONObject("txt_forecast");
        JSONArray txtforecastday = txtforecast.getJSONArray("forecastday");
        JSONObject simpleforecast = forecast.getJSONObject("simpleforecast");
        JSONArray forecastday = simpleforecast.getJSONArray("forecastday");
        JSONObject day;
        JSONObject date;
        mTotalWeather = new ArrayList<>();

        int x = 0;
        for (int i = 1; i < 4; i++) {
            FutureWeather futureWeather = new FutureWeather();
            day = forecastday.getJSONObject(i);
            date = day.getJSONObject("date");
            futureWeather.setHumidity(day.getDouble("avehumidity"));
            futureWeather.setTime(date.getLong("epoch"));
            futureWeather.setTimezone(date.getString("tz_long"));
            futureWeather.setIcon(day.getString("icon"));
            futureWeather.setIconUrl(day.getString("icon_url"));
            JSONObject txtday = txtforecastday.getJSONObject(x);
            x += 2;
            futureWeather.setPrecipProbability(txtday.getString("pop"));
            futureWeather.setSummary(day.getString("conditions"));
            JSONObject high = day.getJSONObject("high");
            futureWeather.setHighTemperature(Integer.parseInt(high.getString("fahrenheit")));
            JSONObject low = day.getJSONObject("low");
            futureWeather.setLowTemperature(Integer.parseInt(low.getString("fahrenheit")));
            Log.d(TAG, futureWeather.getFormattedTime());
            Log.d(TAG, futureWeather.getHighTemperature() + "");
            mTotalWeather.add(futureWeather);
        }
    }

    public List<FutureWeather> getTotalWeather() {
        return mTotalWeather;
    }
}
