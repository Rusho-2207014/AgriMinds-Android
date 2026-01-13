package com.agriminds.ui.weather;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.agriminds.R;
import com.agriminds.data.entity.WeatherForecast;

import java.util.ArrayList;
import java.util.List;

public class WeatherForecastAdapter extends RecyclerView.Adapter<WeatherForecastAdapter.ForecastViewHolder> {

    private List<WeatherForecast> forecasts = new ArrayList<>();

    public void setForecasts(List<WeatherForecast> forecasts) {
        this.forecasts = forecasts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ForecastViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_weather_forecast, parent, false);
        return new ForecastViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ForecastViewHolder holder, int position) {
        WeatherForecast forecast = forecasts.get(position);

        holder.tvDayName.setText(forecast.getDayName());
        holder.tvDate.setText(forecast.getDate());
        holder.tvWeatherDesc.setText(capitalizeFirstLetter(forecast.getWeatherDescription()));
        holder.tvTempRange.setText(String.format("%.0f°C - %.0f°C",
                forecast.getMinTemp(), forecast.getMaxTemp()));

        holder.tvHumidity.setText(String.format("💧 %d%%", forecast.getHumidity()));
        holder.tvWindSpeed.setText(String.format("🌬️ %.0f km/h", forecast.getWindSpeed()));
        holder.tvRainfall.setText(String.format("🌧️ %.1fmm", forecast.getRainfall()));

        // Show agriculture-specific warnings
        if (forecast.hasFrostWarning()) {
            holder.tvWarning.setVisibility(View.VISIBLE);
            holder.tvWarning.setText("⚠️ Frost (তুষার)");
            holder.tvWarning.setTextColor(0xFFD32F2F);
        } else if (!forecast.isGoodForSpraying()) {
            holder.tvWarning.setVisibility(View.VISIBLE);
            holder.tvWarning.setText("🌬️ Windy (বাতাস)");
            holder.tvWarning.setTextColor(0xFFFF9800);
        } else {
            holder.tvWarning.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return forecasts.size();
    }

    private String capitalizeFirstLetter(String text) {
        if (text == null || text.isEmpty())
            return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    static class ForecastViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayName, tvDate, tvWeatherDesc, tvTempRange;
        TextView tvHumidity, tvWindSpeed, tvRainfall, tvWarning;

        public ForecastViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayName = itemView.findViewById(R.id.tv_day_name);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvWeatherDesc = itemView.findViewById(R.id.tv_weather_desc);
            tvTempRange = itemView.findViewById(R.id.tv_temp_range);
            tvHumidity = itemView.findViewById(R.id.tv_humidity);
            tvWindSpeed = itemView.findViewById(R.id.tv_wind_speed);
            tvRainfall = itemView.findViewById(R.id.tv_rainfall);
            tvWarning = itemView.findViewById(R.id.tv_warning);
        }
    }
}
