package com.agriminds.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.agriminds.data.entity.Listing;

import java.util.List;

@Dao
public interface ListingDao {
    @Insert
    void insert(Listing listing);

    @Query("SELECT * FROM listings WHERE status = 'ACTIVE' ORDER BY datePosted DESC")
    LiveData<List<Listing>> getAllActiveListings();

    @Query("SELECT * FROM listings WHERE farmerId = :farmerId")
    LiveData<List<Listing>> getListingsByFarmer(int farmerId);
}
