package com.agriminds.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.agriminds.data.entity.MarketPrice;

import java.util.List;

@Dao
public interface MarketPriceDao {
    @Insert
    void insert(MarketPrice price);

    @Query("SELECT * FROM market_prices ORDER BY date DESC")
    LiveData<List<MarketPrice>> getAllPrices();

    @Query("SELECT * FROM market_prices WHERE district = :district")
    LiveData<List<MarketPrice>> getPricesByDistrict(String district);
}
