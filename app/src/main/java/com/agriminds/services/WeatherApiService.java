package com.agriminds.services;

import android.util.Log;

import com.agriminds.data.entity.WeatherData;
import com.agriminds.data.entity.WeatherForecast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WeatherApiService {
    private static final String TAG = "WeatherApiService";

    // Free OpenWeatherMap API key (1000 calls/day)
    // You can use either: 2898b5405b8ee5a84fe49a56b0d78983 (Default) or
    // 38323c1afe970ca88cb8051b1cff8b9d (AgriMinds)
    private static final String API_KEY = "2898b5405b8ee5a84fe49a56b0d78983"; // Using Default key
    private static final String CURRENT_WEATHER_URL = "https://api.openweathermap.org/data/2.5/weather";
    private static final String FORECAST_URL = "https://api.openweathermap.org/data/2.5/forecast";

    public interface WeatherCallback {
        void onSuccess(WeatherData weatherData);

        void onError(String error);
    }

    public interface ForecastCallback {
        void onSuccess(List<WeatherForecast> forecasts);

        void onError(String error);
    }

    /**
     * Fetch current weather by coordinates
     */
    public static void fetchCurrentWeather(double latitude, double longitude, WeatherCallback callback) {
        new Thread(() -> {
            try {
                String urlString = CURRENT_WEATHER_URL + "?lat=" + latitude + "&lon=" + longitude
                        + "&appid=" + API_KEY + "&units=metric";

                System.out.println("============= WEATHER API CALL =============");
                System.out.println("URL: " + urlString);
                Log.d(TAG, "Fetching weather from: " + urlString);

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int responseCode = conn.getResponseCode();
                System.out.println("Response code: " + responseCode);
                Log.d(TAG, "Response code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    Log.d(TAG, "Response: " + response.toString());

                    WeatherData weatherData = parseCurrentWeather(response.toString());
                    callback.onSuccess(weatherData);
                } else {
                    // Read error response
                    BufferedReader errorReader = new BufferedReader(
                            new InputStreamReader(conn.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    errorReader.close();

                    String errorMsg = "HTTP Error " + responseCode;
                    if (responseCode == 401) {
                        errorMsg += ": Invalid API key. Please wait 5-10 minutes for new key activation";
                    }
                    errorMsg += " - " + errorResponse.toString();

                    System.out.println("ERROR: " + errorMsg);
                    Log.e(TAG, errorMsg);
                    callback.onError(errorMsg);
                }

                conn.disconnect();
            } catch (Exception e) {
                String errorMsg = "Exception: " + e.getMessage();
                System.out.println("EXCEPTION: " + errorMsg);
                e.printStackTrace();
                Log.e(TAG, "Error fetching weather", e);
                callback.onError(errorMsg != null ? errorMsg : "Unknown error");
            }
        }).start();
    }

    /**
     * Fetch 7-day forecast by coordinates
     */
    public static void fetch7DayForecast(double latitude, double longitude, ForecastCallback callback) {
        new Thread(() -> {
            try {
                String urlString = FORECAST_URL + "?lat=" + latitude + "&lon=" + longitude
                        + "&appid=" + API_KEY + "&units=metric&cnt=56"; // 7 days * 8 (3-hour intervals)

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    List<WeatherForecast> forecasts = parse7DayForecast(response.toString());
                    callback.onSuccess(forecasts);
                } else {
                    callback.onError("HTTP Error: " + responseCode);
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error fetching forecast", e);
                callback.onError(e.getMessage());
            }
        }).start();
    }

    private static WeatherData parseCurrentWeather(String jsonResponse) throws Exception {
        JSONObject json = new JSONObject(jsonResponse);
        WeatherData data = new WeatherData();

        data.setLocationName(json.getString("name"));

        JSONObject main = json.getJSONObject("main");
        data.setCurrentTemp(main.getDouble("temp"));
        data.setFeelsLike(main.getDouble("feels_like"));
        data.setHumidity(main.getInt("humidity"));
        data.setMaxTemp(main.getDouble("temp_max"));
        data.setMinTemp(main.getDouble("temp_min"));

        JSONArray weatherArray = json.getJSONArray("weather");
        JSONObject weather = weatherArray.getJSONObject(0);
        data.setWeatherDescription(weather.getString("description"));
        data.setWeatherIcon(weather.getString("icon"));

        JSONObject wind = json.getJSONObject("wind");
        data.setWindSpeed(wind.getDouble("speed") * 3.6); // Convert m/s to km/h

        // Rainfall in last 1 hour (if available)
        if (json.has("rain")) {
            JSONObject rain = json.getJSONObject("rain");
            data.setRainfall(rain.optDouble("1h", 0.0));
        } else {
            data.setRainfall(0.0);
        }

        data.setTimestamp(System.currentTimeMillis());

        return data;
    }

    private static List<WeatherForecast> parse7DayForecast(String jsonResponse) throws Exception {
        JSONObject json = new JSONObject(jsonResponse);
        JSONArray list = json.getJSONArray("list");

        List<WeatherForecast> forecasts = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        SimpleDateFormat fullDayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        // Group by day and aggregate
        String currentDate = "";
        WeatherForecast dayForecast = null;
        double dayMaxTemp = -999;
        double dayMinTemp = 999;
        double totalRainfall = 0;
        int humiditySum = 0;
        double windSpeedSum = 0;
        int count = 0;
        String weatherDesc = "";
        String weatherIcon = "";

        for (int i = 0; i < list.length() && forecasts.size() < 5; i++) {
            JSONObject item = list.getJSONObject(i);
            long timestamp = item.getLong("dt") * 1000;
            Date date = new Date(timestamp);
            String fullDateStr = fullDayFormat.format(date);
            String displayDateStr = dateFormat.format(date);

            JSONObject main = item.getJSONObject("main");
            double temp = main.getDouble("temp");

            JSONArray weatherArray = item.getJSONArray("weather");
            JSONObject weather = weatherArray.getJSONObject(0);

            if (!fullDateStr.equals(currentDate)) {
                // Save previous day if exists
                if (dayForecast != null && count > 0) {
                    dayForecast.setMaxTemp(dayMaxTemp);
                    dayForecast.setMinTemp(dayMinTemp);
                    dayForecast.setRainfall(totalRainfall);
                    dayForecast.setHumidity(humiditySum / count);
                    dayForecast.setWindSpeed((windSpeedSum / count) * 3.6); // m/s to km/h
                    dayForecast.setWeatherDescription(weatherDesc);
                    dayForecast.setWeatherIcon(weatherIcon);
                    forecasts.add(dayForecast);
                }

                // Start new day
                currentDate = fullDateStr;
                dayForecast = new WeatherForecast();
                dayForecast.setDate(displayDateStr);
                dayForecast.setDayName(dayFormat.format(date));

                weatherDesc = weather.getString("description");
                weatherIcon = weather.getString("icon");

                // Check for rain probability
                double pop = item.optDouble("pop", 0.0) * 100;
                dayForecast.setRainfallProbability(pop);

                dayMaxTemp = temp;
                dayMinTemp = temp;
                totalRainfall = 0;
                humiditySum = 0;
                windSpeedSum = 0;
                count = 0;
            }

            // Update day's stats
            if (temp > dayMaxTemp)
                dayMaxTemp = temp;
            if (temp < dayMinTemp)
                dayMinTemp = temp;

            if (item.has("rain")) {
                JSONObject rain = item.getJSONObject("rain");
                totalRainfall += rain.optDouble("3h", 0.0);
            }

            humiditySum += main.getInt("humidity");
            JSONObject wind = item.getJSONObject("wind");
            windSpeedSum += wind.getDouble("speed");
            count++;
        }

        // Add last day
        if (dayForecast != null && count > 0 && forecasts.size() < 5) {
            dayForecast.setMaxTemp(dayMaxTemp);
            dayForecast.setMinTemp(dayMinTemp);
            dayForecast.setRainfall(totalRainfall);
            dayForecast.setHumidity(humiditySum / count);
            dayForecast.setWindSpeed((windSpeedSum / count) * 3.6);
            dayForecast.setWeatherDescription(weatherDesc);
            dayForecast.setWeatherIcon(weatherIcon);
            forecasts.add(dayForecast);
        }

        System.out.println("Total forecast days parsed: " + forecasts.size());
        return forecasts;
    }
}
