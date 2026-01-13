package com.agriminds.data.entity;

public class WeatherForecast {
    private String date;
    private String dayName;
    private double maxTemp;
    private double minTemp;
    private String weatherDescription;
    private String weatherIcon;
    private int humidity;
    private double windSpeed;
    private double rainfall;
    private double rainfallProbability;

    public WeatherForecast() {
    }

    // Getters and Setters
    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDayName() {
        return dayName;
    }

    public void setDayName(String dayName) {
        this.dayName = dayName;
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

    public double getRainfallProbability() {
        return rainfallProbability;
    }

    public void setRainfallProbability(double rainfallProbability) {
        this.rainfallProbability = rainfallProbability;
    }

    // Helper method to check if frost warning applies
    public boolean hasFrostWarning() {
        return minTemp <= 0.0;
    }

    // Helper method to check if good for spraying
    public boolean isGoodForSpraying() {
        return windSpeed <= 15.0 && rainfallProbability < 30.0;
    }
}
