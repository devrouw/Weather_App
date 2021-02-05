package com.lollipop.tekninoko.utils;

import com.lollipop.tekninoko.model.WeatherResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface BaseApiService {
    @GET("weather")
    Call<WeatherResponse> getWeatherInfo(@Query("lat") String lat,
                                         @Query("lon") String lon,
                                         @Query("units") String units,
                                         @Query("appid") String appid);
}
