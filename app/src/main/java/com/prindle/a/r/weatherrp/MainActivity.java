package com.prindle.a.r.weatherrp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class MainActivity extends ActionBarActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private ViewSwitcher switcher;

    private boolean mToggleRefresh;
    private static boolean mOkToUpdateWU;
    private static boolean mAutoLocate;
    private static Location mLocation;
    private static String mCity;
    private static String mWeatherProvider;
    private CurrentWeather mWUCurrentWeather;
    private CurrentWeather mFIOCurrentWeather;
    private List<FutureWeather> mWUTotalWeather;
    private List<FutureWeather> mFIOTotalWeather;
    private ForecastIO mForecastIO;
    private WeatherUnderground mWeatherUnderground;
    private AppLocationService mAppLocationService;
    protected SwipeRefreshLayout mSwipeRefreshLayout;

    private static final String FORECASTIO = "ForecastIO";
    private static final String WEATHERUNDERGROUND = "WeatherUnderground";

    private static Location[] mLatLong;
    private List<String> mAddressList;
    private static final String LOCURL = "http://autocomplete.wunderground.com/aq?query=";


    @InjectView(R.id.todayTimeDateTextView)
    TextView mTimeLabel;
    @InjectView(R.id.todayTempTextView)
    TextView mTemperatureLabel;
    @InjectView(R.id.todayHumidityTextView)
    TextView mTodayHumidityLabel;
    @InjectView(R.id.todayPrecipTextView)
    TextView mTodayPrecipLabel;
    @InjectView(R.id.todaySummaryTextView)
    TextView mTodaySummaryLabel;
    @InjectView(R.id.todayIconImageView)
    ImageView mTodayIconImageView;
    @InjectView(R.id.locationTextView)
    TextView mLocationLabel;
    @InjectView(R.id.forecastGridView)
    GridView mGridView;
    @InjectView(R.id.wULogoImageView)
    ImageView mWUImageView;
    @InjectView(R.id.providerTextView)
    TextView mProviderLabel;
    @InjectView(R.id.locationEditText)
    EditText mLocationEditText;
    @InjectView(R.id.locationListView)
    ListView mLocationListView;
    @InjectView(R.id.locationButton)
    Button mLocationButton;
    @InjectView(R.id.cancelButton)
    Button mCancelButton;

    Handler mWUHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            mOkToUpdateWU = true;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        switcher = (ViewSwitcher) findViewById(R.id.profileSwitcher);
        mToggleRefresh = false;
        mForecastIO = new ForecastIO();
        mLocation = new Location("here");
        mWeatherUnderground = new WeatherUnderground();
        mWUTotalWeather = new ArrayList<>();
        mFIOTotalWeather = new ArrayList<>();
        mOkToUpdateWU = true;

        ButterKnife.inject(MainActivity.this);
        mLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAddressList = new ArrayList<>();
                String lookUp = mLocationEditText.getText().toString();
                getCity(lookUp);
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(switcher.getWindowToken(), 0);
            }
        });
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(switcher.getWindowToken(), 0);
                switcher.showPrevious();
            }
        });
        mLocationEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    mAddressList = new ArrayList<>();
                    String lookUp = mLocationEditText.getText().toString();
                    getCity(lookUp);
                }
                return false;
            }
        });

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        mSwipeRefreshLayout.setOnRefreshListener(mOnRefreshListener);
        mSwipeRefreshLayout.setColorSchemeResources(
                R.color.swipeRefresh3,
                R.color.swipeRefresh1,
                R.color.swipeRefresh3,
                R.color.swipeRefresh4);
        mWeatherProvider = FORECASTIO;
        if (!getCurrentLocation()) {
            showLocationChoice();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        updateWeather();
    }
    @Override
    protected void onPause(){
        super.onPause();
        mAppLocationService.stopLocation();
    }



    protected SwipeRefreshLayout.OnRefreshListener mOnRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            if (mAutoLocate) {
                getCurrentLocation();
            }
            updateWeather();
        }
    };

    public void toggleRefresh() {
        mToggleRefresh = !mToggleRefresh;
        mSwipeRefreshLayout.setRefreshing(mToggleRefresh);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.provider) {
            showProviderChoice();
            return true;
        }
        if (id == R.id.location) {
            showLocationChoice();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadListView() {
        myArrayAdapter listAdapter = new myArrayAdapter(this, R.layout.location_textview, mAddressList);
        mLocationListView.setAdapter(listAdapter);
        mLocationListView.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mCity = mAddressList.get(position);
                mLocation.setLatitude(mLatLong[position].getLatitude());
                mLocation.setLongitude(mLatLong[position].getLongitude());
                mAutoLocate = false;
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                switcher.showPrevious();  // Switches to the weather view
                updateWeather();
            }
        });
    }

    private class myArrayAdapter extends ArrayAdapter<String> {

        HashMap<String, Integer> mIdMap = new HashMap<>();

        public myArrayAdapter(Context context, int textViewResourceId, List<String> objects) {
            super(context, textViewResourceId, objects);
            for (int i = 0; i < objects.size(); ++i) {
                mIdMap.put(objects.get(i), i);
            }
        }
    }


    private void getCity(String city) {
        String locateUrl = LOCURL + city;
        locateUrl = locateUrl.replace(" ","%20");
        if (networkIsAvailable()) {
            //get city name
            Log.v(TAG, locateUrl);
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(locateUrl)
                    .build();
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    alertUserAboutError("OKHTTP Location Error");
                }

                @Override
                public void onResponse(Response response) throws IOException {

                    try {
                        String jsonData = response.body().string();
                        Log.v(TAG, jsonData);
                        if (response.isSuccessful()) {
                            //save location data and display in list
                            loadLocationList(jsonData);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    loadListView();
                                }
                            });

                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Exception caught: ", e);
                    } catch (JSONException e) {
                        Log.e(TAG, "Exception caught: ", e);
                    }

                }
            });
        } else {
            alertUserAboutError("Can't retrieve location at this time.");
        }
    }

    private void loadLocationList(String jsonData) throws JSONException {
        JSONObject addressJson = new JSONObject(jsonData);
        JSONArray results = addressJson.getJSONArray("RESULTS");
        int loop;
        if (results.length() > 0) {
            if (results.length() > 5) {
                loop = 5;
            } else {
                loop = results.length();
            }
            mLatLong = new Location[loop];
            for (int i = 0; i < loop; i++) {
                mAddressList.add(results.getJSONObject(i).getString("name"));
                mLatLong[i]= new Location(results.getJSONObject(i).getString("name"));
                mLatLong[i].setLatitude(Double.parseDouble(results.getJSONObject(i).getString("lat")));
                mLatLong[i].setLongitude(Double.parseDouble(results.getJSONObject(i).getString("lon")));
            }
        }
    }

    private void enterLocation() {
        if (mLocationListView.getAdapter() != null) {
            mLocationListView.setAdapter(null);
            mLocationEditText.setText("");

        }

        switcher.showNext();  // Switches to the location view
        mLocationEditText.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    private boolean getCurrentLocation() {
        //gps or network
        boolean found = true;
        mAppLocationService = new AppLocationService(this);
        Location gpsLocation = mAppLocationService.getLocation(LocationManager.GPS_PROVIDER);
        Location nwLocation = mAppLocationService.getLocation(LocationManager.NETWORK_PROVIDER);
        if (gpsLocation != null) {
            mLocation.setLatitude(gpsLocation.getLatitude());
            mLocation.setLongitude(gpsLocation.getLongitude());
            Toast.makeText(MainActivity.this, "GPS Location", Toast.LENGTH_LONG).show();
            getAddress();
        } else if (nwLocation != null) {
            mLocation.setLatitude(nwLocation.getLatitude());
            mLocation.setLongitude(nwLocation.getLongitude());
            Toast.makeText(MainActivity.this,"Network Location", Toast.LENGTH_LONG).show();
            getAddress();
        } else {
            found = false;
        }
        mAppLocationService.stopLocation();
        return found;
    }

    public void showLocationChoice() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("Forecast Location");
        alertDialog.setMessage("Use Current Location?");
        alertDialog.setNegativeButton("Yes, Locate Automatically",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mAutoLocate = true;
                        if (getCurrentLocation()) {
                            updateWeather();
                        } else {
                            showLocationSettingsAlert();
                        }
                        dialog.cancel();
                    }
                });
        alertDialog.setPositiveButton("No, Enter Location",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mAutoLocate = false;
                        enterLocation();
                        dialog.cancel();
                    }
                });
        alertDialog.show();
    }

    private void getAddress() {
        Geocoder geocoder;
        List<Address> addresses;
        geocoder = new Geocoder(this, Locale.getDefault());
        try {
            addresses = geocoder.getFromLocation(mLocation.getLatitude(), mLocation.getLongitude(), 1);
            mCity = addresses.get(0).getAddressLine(1);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
    }

    public void showLocationSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("CAN'T GET LOCATION");
        alertDialog.setMessage("Location is not enabled! Want to go to settings menu?");
        alertDialog.setPositiveButton("Settings",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(
                                Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        MainActivity.this.startActivity(intent);
                    }
                });
        alertDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        showLocationChoice();
                    }
                });
        alertDialog.show();
    }

    public void showProviderChoice() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("Weather Provider");
        alertDialog.setMessage("Choose a weather provider");
        alertDialog.setPositiveButton("WeatherUnderground",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mWeatherProvider = WEATHERUNDERGROUND;
                        dialog.cancel();
                        updateWeather();
                    }
                });
        alertDialog.setNegativeButton("ForecastIO",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mWeatherProvider = FORECASTIO;
                        dialog.cancel();
                        updateWeather();
                    }
                });
        alertDialog.show();
    }

    public boolean networkIsAvailable() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;
        if (networkInfo != null && networkInfo.isConnected()) {
            isAvailable = true;
        }
        return isAvailable;
    }

    private void alertUserAboutError(String error) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setMessage(error);
        dialog.setTitle("title");
        dialog.show();
    }

    public void updateDisplay() {
        CurrentWeather currentWeather;
        List<FutureWeather> totalWeather;
        if (mWeatherProvider.equals(WEATHERUNDERGROUND)) {
            mWUImageView.setVisibility(View.VISIBLE);
            mProviderLabel.setVisibility(View.INVISIBLE);
            totalWeather = mWUTotalWeather;
            currentWeather = mWUCurrentWeather;
        } else {
            mWUImageView.setVisibility(View.INVISIBLE);
            mProviderLabel.setVisibility(View.VISIBLE);
            totalWeather = mFIOTotalWeather;
            currentWeather = mFIOCurrentWeather;
        }
        if (mAutoLocate) {
            getAddress();
        }
        mLocationLabel.setText(mCity);
        mTimeLabel.setText(currentWeather.getFormattedTime());
        mTemperatureLabel.setText(currentWeather.getTemperature() + "");
        mTodayHumidityLabel.setText(currentWeather.getHumidity());
        mTodayPrecipLabel.setText(currentWeather.getPrecip());
        mTodaySummaryLabel.setText(currentWeather.getSummary());
        mTodayIconImageView.setImageDrawable(getResources().getDrawable(currentWeather.getIconId()));
        ForecastAdapter adapter = new ForecastAdapter(this, totalWeather);
        mGridView.setAdapter(adapter);
        mSwipeRefreshLayout.setRefreshing(false);
    }

    private void updateWeather() {

        switch (mWeatherProvider) {
            case FORECASTIO:
                    getForecastIOWeatherData();
                break;
            case WEATHERUNDERGROUND:
                if (mOkToUpdateWU) {
                    getWUWeatherData();
                    Runnable wUUpdateDelay = new Runnable() {
                        public void run() {
                            long endTime = System.currentTimeMillis() + 10 * 60 * 1000;
                            while (System.currentTimeMillis() < endTime) {
                                synchronized (this) {
                                    try {
                                        wait(endTime -
                                                System.currentTimeMillis());
                                    } catch (Exception e) {
                                        Log.e(TAG, "WU Delay Exception caught: ", e);
                                    }
                                }
                            }
                            mWUHandler.sendEmptyMessage(0);
                        }
                    };
                    Thread wUThread = new Thread(wUUpdateDelay);
                    wUThread.start();
                } else {
                    Toast.makeText(MainActivity.this, getString(R.string.wu_timer_message), Toast.LENGTH_LONG).show();
                    mSwipeRefreshLayout.setRefreshing(false);
                }
                break;
        }

    }

    private void getForecastIOWeatherData() {
        final String forecastUrl = mForecastIO.getForecastURL(mLocation);
        if (networkIsAvailable()) {
            toggleRefresh();
            //get current conditions
            Log.v(TAG, forecastUrl);
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(forecastUrl)
                    .build();
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                            alertUserAboutError("OKHTTP ForecastIO Error");
                        }
                    });
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //mOkToUpdateFIO = false;
                        }
                    });
                    try {
                        String jsonData = response.body().string();
                        Log.v(TAG, jsonData);
                        if (response.isSuccessful()) {
                            //save forecastIO data
                            mForecastIO.setCurrentWeatherData(jsonData);
                            mForecastIO.setFutureWeatherData(jsonData);
                            mFIOTotalWeather = mForecastIO.getTotalWeather();
                            mFIOCurrentWeather = mForecastIO.getCurrentWeather();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateDisplay();
                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    toggleRefresh();
                                    alertUserAboutError("ForecastIO Error");
                                }
                            });
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Exception caught: ", e);
                    } catch (JSONException e) {
                        Log.e(TAG, "Exception caught: ", e);
                    }
                }
            });
        } else {
            alertUserAboutError("OKHTTP error");
        }
    }

    private void getWUWeatherData() {
        final String forecastUrl = mWeatherUnderground.getForecastURL(mLocation);
        final String currentUrl = mWeatherUnderground.getCurrentURL(mLocation);
        if (networkIsAvailable()) {
            toggleRefresh();
            Log.v(TAG, currentUrl);
            //get current conditions
            OkHttpClient currentClient = new OkHttpClient();
            Request currentRequest = new Request.Builder()
                    .url(currentUrl)
                    .build();
            Call currentCall = currentClient.newCall(currentRequest);
            currentCall.enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                            alertUserAboutError("OKHTTP WeatherUnderground Error");
                        }
                    });
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mOkToUpdateWU = false;
                        }
                    });
                    try {
                        String currentjsonData = response.body().string();
                        Log.v(TAG, currentjsonData);
                        if (response.isSuccessful()) {
                            //save current data
                            mWeatherUnderground.setCurrentWeatherData(currentjsonData);
                            mWUCurrentWeather = mWeatherUnderground.getCurrentWeather();
                            //get forecast
                            Log.v(TAG, forecastUrl);
                            OkHttpClient client = new OkHttpClient();
                            Request request = new Request.Builder()
                                    .url(forecastUrl)
                                    .build();
                            Call call = client.newCall(request);
                            call.enqueue(new Callback() {
                                @Override
                                public void onFailure(Request request, IOException e) {

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            toggleRefresh();
                                            alertUserAboutError("OKHTTP Forecast WeatherUnderground Error");
                                        }
                                    });
                                }

                                @Override
                                public void onResponse(Response response) throws IOException {
                                    try {
                                        String forecastjsonData = response.body().string();
                                        Log.v(TAG, forecastjsonData);
                                        if (response.isSuccessful()) {
                                            //save forecast data
                                            mWeatherUnderground.setFutureWeatherData(forecastjsonData);
                                            mWUTotalWeather = mWeatherUnderground.getTotalWeather();
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    updateDisplay();
                                                }
                                            });
                                        } else {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    alertUserAboutError("WeatherUnderground Error");
                                                }
                                            });
                                        }
                                    } catch (IOException e) {
                                        Log.e(TAG, "Forecast Exception caught: ", e);
                                    } catch (JSONException e) {
                                        Log.e(TAG, "Forecast Exception caught: ", e);
                                    }
                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    alertUserAboutError("OKHTTP Forecast Error");
                                }
                            });
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Current Exception caught: ", e);
                    } catch (JSONException e) {
                        Log.e(TAG, "Current Exception caught: ", e);
                    }
                }
            });


        } else {
            alertUserAboutError("Network Not Available");
        }
    }

}
