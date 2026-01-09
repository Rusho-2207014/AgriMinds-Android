package com.agriminds.ui;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.agriminds.data.AgrimindsRepository;
import com.agriminds.data.entity.Listing;
import com.agriminds.data.entity.MarketPrice;

import java.util.List;

public class MainViewModel extends AndroidViewModel {

    private AgrimindsRepository repository;

    private final LiveData<List<MarketPrice>> allPrices;
    private final LiveData<List<Listing>> allListings;

    public MainViewModel(Application application) {
        super(application);
        repository = new AgrimindsRepository(application);
        allPrices = repository.getAllPrices();
        allListings = repository.getAllListings();
    }

    public LiveData<List<MarketPrice>> getAllPrices() {
        return allPrices;
    }

    public LiveData<List<Listing>> getAllListings() {
        return allListings;
    }
}
