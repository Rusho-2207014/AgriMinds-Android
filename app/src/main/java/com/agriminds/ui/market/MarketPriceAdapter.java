package com.agriminds.ui.market;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.agriminds.R;
import com.agriminds.data.entity.MarketPrice;

import java.util.ArrayList;
import java.util.List;

public class MarketPriceAdapter extends RecyclerView.Adapter<MarketPriceAdapter.ViewHolder> {

    private List<MarketPrice> mValues = new ArrayList<>();

    public void setMarketPrices(List<MarketPrice> items) {
        mValues = items;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_market_price, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        MarketPrice item = mValues.get(position);
        holder.timeText.setText("Today"); // Placeholder date
        holder.cropName.setText(item.cropName);
        holder.marketName.setText(item.marketName);
        holder.retailPrice.setText(String.format("Retail: ৳%.2f/kg", item.retailPrice));
        holder.wholesalePrice.setText(String.format("Wholesale: ৳%.2f/kg", item.wholesalePrice));
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView cropName;
        public final TextView marketName;
        public final TextView retailPrice;
        public final TextView wholesalePrice;
        public final TextView timeText;

        public ViewHolder(View view) {
            super(view);
            cropName = view.findViewById(R.id.text_crop_name);
            marketName = view.findViewById(R.id.text_market_name);
            retailPrice = view.findViewById(R.id.text_retail_price);
            wholesalePrice = view.findViewById(R.id.text_wholesale_price);
            timeText = new TextView(view.getContext()); // Hack if I missed ID in layout
        }
    }
}
