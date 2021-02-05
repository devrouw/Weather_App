package com.lollipop.tekninoko;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.lollipop.tekninoko.model.WeatherResponse;
import com.lollipop.tekninoko.utils.BaseApiService;
import com.lollipop.tekninoko.utils.UtilsApi;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static java.security.AccessController.getContext;

public class MainActivity extends AppCompatActivity implements MapActivity.ButtonInterface {

    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1;
    public static final String SOME_INTENT_FILTER_NAME = "COORDINATES";

    BaseApiService mApiService;

    private TextView txt_kota,txt_tgl,txt_longitude,txt_latitude,txt_wind_speed,txt_humidity,txt_temperature, txt_feels, txt_status;
    private ImageView img_weather;

    private FloatingActionButton btn_location_pick;
    private ProgressDialog loading;

    private RadioGroup rg_temp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mApiService = UtilsApi.getAPIService();
        loading = new ProgressDialog(MainActivity.this);

        txt_kota = findViewById(R.id.txt_kota);
        btn_location_pick = findViewById(R.id.btn_location_pick);
        txt_tgl = findViewById(R.id.txt_tgl);
        txt_longitude = findViewById(R.id.txt_longitude);
        txt_latitude = findViewById(R.id.txt_latitude);
        txt_humidity = findViewById(R.id.txt_humidity);
        txt_wind_speed = findViewById(R.id.txt_wind_speed);
        txt_temperature = findViewById(R.id.txt_temperature);
        txt_feels = findViewById(R.id.txt_feels);
        txt_status = findViewById(R.id.txt_status);
        img_weather = findViewById(R.id.img_weather);
        rg_temp = findViewById(R.id.rg_temp);

        btn_location_pick.setAlpha(0.6f);
        btn_location_pick.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MapActivity mapActivity = new MapActivity();
                mapActivity.show(getSupportFragmentManager(),mapActivity.getTag());
            }
        });

        ((RadioButton)rg_temp.getChildAt(0)).setChecked(true);
        checkPermissions();

    }

    public void onClickedFahrenheit(View view){
        loading.show();
        loading.setContentView(R.layout.loading_screen);
        loading.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        String kota = txt_kota.getText().toString();
        String lng = txt_longitude.getText().toString();
        String lat = txt_latitude.getText().toString();

        getCurrentWeather(kota,lat,lng,"imperial");
    }

    public void onClickCelcius(View view){
        loading.show();
        loading.setContentView(R.layout.loading_screen);
        loading.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        String kota = txt_kota.getText().toString();
        String lng = txt_longitude.getText().toString();
        String lat = txt_latitude.getText().toString();

        getCurrentWeather(kota,lat,lng,"metric");
    }

    private void checkPermissions() {
        if(ContextCompat.checkSelfPermission(
                getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_LOCATION_PERMISSION
            );
        }else{
            getCurrentLocation("imperial");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE_LOCATION_PERMISSION && grantResults.length > 0){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                getCurrentLocation("imperial");
            }else{
                Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getCurrentLocation(final String unit) {
        loading.show();
        loading.setContentView(R.layout.loading_screen);
        loading.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd HH:mm:ss");
        final String currentDateandTime = sdf.format(new Date());

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationServices.getFusedLocationProviderClient(MainActivity.this)
                .requestLocationUpdates(locationRequest, new LocationCallback(){
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        super.onLocationResult(locationResult);
                        LocationServices.getFusedLocationProviderClient(MainActivity.this)
                                .removeLocationUpdates(this);
                        if(locationResult != null && locationResult.getLocations().size() > 0){
                            int latestLocation = locationResult.getLocations().size() - 1;
                            double latitude = locationResult.getLocations().get(latestLocation).getLatitude();
                            double longitude = locationResult.getLocations().get(latestLocation).getLongitude();

                            Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                            List<Address> addresses = null;
                            try {
                                addresses = geocoder.getFromLocation(latitude, longitude, 1);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            String cityName = addresses.get(0).getLocality();
                            txt_tgl.setText(currentDateandTime);

                            getCurrentWeather(cityName, Double.toString(latitude),Double.toString(longitude), unit);
                        }
                    }
                }, Looper.getMainLooper());
    }

    void getCurrentWeather(String cityName, String latitude, String longitude, String unit) {
        txt_latitude.setText(latitude);
        txt_longitude.setText(longitude);
        txt_kota.setText(cityName);

        Call<WeatherResponse> call = mApiService.getWeatherInfo(latitude, longitude, unit,"fdf871cedaf3413c6a23230372c30a02");
        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.code() == 200) {
                    loading.dismiss();
                    WeatherResponse weatherResponse = response.body();
                    assert weatherResponse != null;

                    txt_status.setText(weatherResponse.weather.get(0).main);
                    txt_humidity.setText(weatherResponse.main.humidity+" %");
                    txt_wind_speed.setText(weatherResponse.wind.speed+" km/h");
                    txt_temperature.setText(""+weatherResponse.main.temp+"\u00B0");
                    txt_feels.setText(weatherResponse.main.feels_like+"\u00B0");
                    String url = "http://openweathermap.org/img/w/";
                    Picasso.get().load(url+weatherResponse.weather.get(0).icon+".png").into(img_weather);
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {

            }
        });
    }

    @Override
    public void applyLocation(String kota, String lng, String lat) {
        loading.show();
        loading.setContentView(R.layout.loading_screen);
        loading.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        txt_latitude.setText(lat);
        txt_longitude.setText(lng);

        int selected = rg_temp.getCheckedRadioButtonId();

        RadioButton rb_temp = (RadioButton)findViewById(selected);
        String s_temp = rb_temp.getText().toString().trim();
        String unit = "";

        if(s_temp.equalsIgnoreCase("fahrenheit")){
            unit = "imperial";
        }else{
            unit = "metric";
        }

        getCurrentWeather(kota, lat,lng, unit);
    }
}
