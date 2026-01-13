package com.agriminds.data.entity;

public class WeatherData {
    private String locationName;
    private double currentTemp;
    private String weatherDescription;
    private String weatherIcon;
    private double feelsLike;
    private int humidity;
    private double windSpeed;
    private double rainfall;
    private double maxTemp;
    private double minTemp;
    private long timestamp;

    public WeatherData() {
    }

    // Getters and Setters
    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public double getCurrentTemp() {
        return currentTemp;
    }

    public void setCurrentTemp(double currentTemp) {
        this.currentTemp = currentTemp;
    }

    public String getWeatherDescription() {
        return weatherDescription;
    }

    public void setWeatherDescription(String weatherDescription) {
        this.weatherDescription = weatherDescription;
    }

    public String getWeatherIcon() {
        return weatherIcon;
    }

    public void setWeatherIcon(String weatherIcon) {
        this.weatherIcon = weatherIcon;
    }

    public double getFeelsLike() {
        return feelsLike;
    }

    public void setFeelsLike(double feelsLike) {
        this.feelsLike = feelsLike;
    }

    public int getHumidity() {
        return humidity;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public double getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(double windSpeed) {
        this.windSpeed = windSpeed;
    }

    public double getRainfall() {
        return rainfall;
    }

    public void setRainfall(double rainfall) {
        this.rainfall = rainfall;
    }

    public double getMaxTemp() {
        return maxTemp;
    }

    public void setMaxTemp(double maxTemp) {
        this.maxTemp = maxTemp;
    }

    public double getMinTemp() {
        return minTemp;
    }

    public void setMinTemp(double minTemp) {
        this.minTemp = minTemp;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    // Helper method to check if temperature is freezing (frost warning for crops)
    public boolean isFrostWarning() {
        return currentTemp <= 2.0 || minTemp <= 0.0;
    }

    // Helper method to check if wind speed is too high for spraying
    public boolean isWindyForSpraying() {
        return windSpeed > 15.0; // Wind speed > 15 km/h not ideal for spraying
    }

    // Helper method to get humidity status for crop planning
    public String getHumidityStatus() {
        if (humidity < 30)
            return "Low (কম) - Risk of dehydration (পানিশূন্যতা)";
        if (humidity > 80)
            return "High (বেশি) - Risk of fungal diseases (ছত্রাক রোগ)";
        return "Optimal for crops (ফসলের জন্য উপযুক্ত)";
    }
}
