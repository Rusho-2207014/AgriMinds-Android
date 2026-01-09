package com.agriminds.ui.market;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.agriminds.R;

public class MarketFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_market, container, false);

        RecyclerView recyclerView = root.findViewById(R.id.recycler_market_prices);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        MarketPriceAdapter adapter = new MarketPriceAdapter();
        recyclerView.setAdapter(adapter);

        // Dummy Data for Demo
        java.util.List<com.agriminds.data.entity.MarketPrice> prices = new java.util.ArrayList<>();

        com.agriminds.data.entity.MarketPrice p1 = new com.agriminds.data.entity.MarketPrice();
        p1.cropName = "Rice (Miniket)";
        p1.marketName = "Kawran Bazar";
        p1.retailPrice = 65.0;
        p1.wholesalePrice = 58.0;
        prices.add(p1);

        com.agriminds.data.entity.MarketPrice p2 = new com.agriminds.data.entity.MarketPrice();
        p2.cropName = "Potato";
        p2.marketName = "Shyam Bazar";
        p2.retailPrice = 40.0;
        p2.wholesalePrice = 32.0;
        prices.add(p2);

        com.agriminds.data.entity.MarketPrice p3 = new com.agriminds.data.entity.MarketPrice();
        p3.cropName = "Onion (Local)";
        p3.marketName = "Jatrabari";
        p3.retailPrice = 110.0;
        p3.wholesalePrice = 95.0;
        prices.add(p3);

        adapter.setMarketPrices(prices);

        return root;
    }
}
