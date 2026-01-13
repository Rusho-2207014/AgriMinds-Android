package com.agriminds.ui.weather;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.agriminds.R;
import com.agriminds.data.entity.WeatherData;
import com.agriminds.data.entity.WeatherForecast;
import com.agriminds.services.WeatherApiService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

public class WeatherFragment extends Fragment {

    private static final String TAG = "WeatherFragment";
    private static final int LOCATION_PERMISSION_CODE = 100;

    // Default location (Dhaka, Bangladesh) if location permission denied
    private static final double DEFAULT_LAT = 23.8103;
    private static final double DEFAULT_LON = 90.4125;

    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView tvLocation, tvCurrentTemp, tvWeatherDesc, tvFeelsLike;
    private TextView tvHumidity, tvWindSpeed, tvRainfall;
    private TextView tvFrostWarning, tvSprayingCondition, tvHumidityStatus;
    private RecyclerView recyclerForecast;
    private WeatherForecastAdapter forecastAdapter;
    private View btnRefresh;
    private ProgressBar progressRefresh;
    private Animation rotateAnimation;

    private FusedLocationProviderClient fusedLocationClient;
    private Handler mainHandler;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        System.out.println("============= WEATHER FRAGMENT CREATED =============");
        System.out.println("WeatherFragment onCreateView called!");
        Log.d(TAG, "WeatherFragment onCreateView called");

        View root = inflater.inflate(R.layout.fragment_weather, container, false);

        mainHandler = new Handler(Looper.getMainLooper());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Initialize views
        swipeRefreshLayout = root.findViewById(R.id.swipe_refresh_weather);
        tvLocation = root.findViewById(R.id.tv_location);
        tvCurrentTemp = root.findViewById(R.id.tv_current_temp);
        tvWeatherDesc = root.findViewById(R.id.tv_weather_desc);
        tvFeelsLike = root.findViewById(R.id.tv_feels_like);
        tvHumidity = root.findViewById(R.id.tv_humidity);
        tvWindSpeed = root.findViewById(R.id.tv_wind_speed);
        tvRainfall = root.findViewById(R.id.tv_rainfall);
        tvFrostWarning = root.findViewById(R.id.tv_frost_warning);
        tvSprayingCondition = root.findViewById(R.id.tv_spraying_condition);
        tvHumidityStatus = root.findViewById(R.id.tv_humidity_status);
        btnRefresh = root.findViewById(R.id.btn_refresh);
        progressRefresh = root.findViewById(R.id.progress_refresh);

        recyclerForecast = root.findViewById(R.id.recycler_forecast);
        // Set horizontal layout for scrolling through all 7 days
        recyclerForecast.setLayoutManager(new LinearLayoutManager(getContext(),
                LinearLayoutManager.HORIZONTAL, false));

        forecastAdapter = new WeatherForecastAdapter();
        recyclerForecast.setAdapter(forecastAdapter);

        // Create rotation animation for refresh button
        rotateAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_refresh);

        // Set up swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(this::loadWeatherData);

        // Enable options menu for toolbar refresh button
        setHasOptionsMenu(true);

        // Set up refresh button
        btnRefresh.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Refreshing weather data...", Toast.LENGTH_SHORT).show();
            loadWeatherData();
        });

        // Load weather data
        loadWeatherData();

        return root;
    }

    private void loadWeatherData() {
        System.out.println("============= LOADING WEATHER DATA =============");
        swipeRefreshLayout.setRefreshing(true);

        // Show circular progress with animation
        progressRefresh.setVisibility(View.VISIBLE);
        btnRefresh.setEnabled(false);

        Log.d(TAG, "Loading weather data...");

        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchLocationAndWeather();
        } else {
            // Request location permission
            requestPermissions(new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    LOCATION_PERMISSION_CODE);
        }
    }

    private void fetchLocationAndWeather() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            loadWeatherForDefaultLocation();
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        fetchWeather(location.getLatitude(), location.getLongitude());
                    } else {
                        loadWeatherForDefaultLocation();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get location", e);
                    loadWeatherForDefaultLocation();
                });
    }

    private void loadWeatherForDefaultLocation() {
        Toast.makeText(getContext(), "Using default location: Dhaka", Toast.LENGTH_SHORT).show();
        fetchWeather(DEFAULT_LAT, DEFAULT_LON);
    }

    private void stopRefreshing() {
        swipeRefreshLayout.setRefreshing(false);
        progressRefresh.setVisibility(View.GONE);
        btnRefresh.setEnabled(true);
        Toast.makeText(getContext(), "✅ Refresh is done!", Toast.LENGTH_SHORT).show();
    }

    private void fetchWeather(double latitude, double longitude) {
        Log.d(TAG, "Fetching weather for lat: " + latitude + ", lon: " + longitude);

        // Fetch current weather
        WeatherApiService.fetchCurrentWeather(latitude, longitude, new WeatherApiService.WeatherCallback() {
            @Override
            public void onSuccess(WeatherData weatherData) {
                Log.d(TAG, "Weather data received: " + weatherData.getLocationName());
                mainHandler.post(() -> updateCurrentWeatherUI(weatherData));
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Weather fetch error: " + error);
                mainHandler.post(() -> {
                    progressRefresh.setVisibility(View.GONE);
                    btnRefresh.setEnabled(true);
                    swipeRefreshLayout.setRefreshing(false);

                    // Show user-friendly message
                    String displayMessage = error;
                    if (error.contains("401")) {
                        displayMessage = "❌ Invalid weather API key!\n\nPlease:\n1. Go to openweathermap.org\n2. Sign up for free\n3. Get your API key\n4. Update WeatherApiService.java";
                    }

                    Toast.makeText(getContext(), displayMessage, Toast.LENGTH_LONG).show();
                });
            }
        });

        // Fetch 7-day forecast
        WeatherApiService.fetch7DayForecast(latitude, longitude, new WeatherApiService.ForecastCallback() {
            @Override
            public void onSuccess(List<WeatherForecast> forecasts) {
                Log.d(TAG, "Forecast data received: " + forecasts.size() + " days");
                mainHandler.post(() -> {
                    forecastAdapter.setForecasts(forecasts);
                    stopRefreshing();
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Forecast fetch error: " + error);
                mainHandler.post(() -> {
                    progressRefresh.setVisibility(View.GONE);
                    btnRefresh.setEnabled(true);
                    swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(getContext(), "Forecast error: " + error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void updateCurrentWeatherUI(WeatherData data) {
        if (data == null) {
            Log.e(TAG, "WeatherData is null!");
            return;
        }

        Log.d(TAG, "Updating UI with weather data");
        tvLocation.setText(data.getLocationName());
        tvCurrentTemp.setText(String.format("%.1f°C", data.getCurrentTemp()));
        tvWeatherDesc.setText(capitalizeFirstLetter(data.getWeatherDescription()));
        tvFeelsLike.setText(String.format("Feels like (অনুভূত): %.1f°C", data.getFeelsLike()));
        tvHumidity.setText(String.format("Humidity (আর্দ্রতা): %d%%", data.getHumidity()));
        tvWindSpeed.setText(String.format("Wind (বাতাস): %.1f km/h", data.getWindSpeed()));
        tvRainfall.setText(String.format("Rainfall (বৃষ্টিপাত): %.1f mm", data.getRainfall()));

        // Agriculture-specific alerts
        if (data.isFrostWarning()) {
            tvFrostWarning.setVisibility(View.VISIBLE);
            tvFrostWarning
                    .setText("⚠️ FROST WARNING (তুষার সতর্কতা): Protect sensitive crops (সংবেদনশীল ফসল রক্ষা করুন)!");
        } else {
            tvFrostWarning.setVisibility(View.GONE);
        }

        if (data.isWindyForSpraying()) {
            tvSprayingCondition.setVisibility(View.VISIBLE);
            tvSprayingCondition
                    .setText("🌬️ NOT IDEAL FOR SPRAYING (স্প্রে করার উপযুক্ত নয়): Wind too strong (বাতাস বেশি)");
            tvSprayingCondition.setTextColor(0xFFFF5722);
        } else {
            tvSprayingCondition.setVisibility(View.VISIBLE);
            tvSprayingCondition.setText("✅ GOOD CONDITIONS FOR SPRAYING (স্প্রে করার উপযুক্ত সময়)");
            tvSprayingCondition.setTextColor(0xFF4CAF50);
        }

        tvHumidityStatus.setText("💧 " + data.getHumidityStatus());
    }

    private String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty())
            return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocationAndWeather();
            } else {
                loadWeatherForDefaultLocation();
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.weather_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            Toast.makeText(getContext(), "Refreshing weather...", Toast.LENGTH_SHORT).show();
            loadWeatherData();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
