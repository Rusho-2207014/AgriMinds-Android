package com.agriminds.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.agriminds.data.entity.CropChart;

import java.util.List;

@Dao
public interface CropChartDao {

    @Insert
    long insert(CropChart cropChart);

    @Update
    void update(CropChart cropChart);

    @Delete
    void delete(CropChart cropChart);

    @Query("SELECT * FROM crop_charts WHERE farmerId = :farmerId ORDER BY createdAt DESC")
    LiveData<List<CropChart>> getCropChartsByFarmer(String farmerId);

    @Query("SELECT * FROM crop_charts WHERE id = :id")
    LiveData<CropChart> getCropChartById(int id);

    @Query("SELECT * FROM crop_charts WHERE id = :id")
    CropChart getCropChartByIdSync(int id);

    @Query("SELECT * FROM crop_charts WHERE isShared = 1 ORDER BY createdAt DESC")
    LiveData<List<CropChart>> getAllSharedCropCharts();

    @Query("SELECT * FROM crop_charts WHERE farmerId = :farmerId AND season = :season")
    LiveData<List<CropChart>> getCropChartsByFarmerAndSeason(String farmerId, String season);

    @Query("UPDATE crop_charts SET isShared = :isShared WHERE id = :id")
    void updateShareStatus(int id, boolean isShared);

    @Query("UPDATE crop_charts SET hasEverBeenShared = :hasEverBeenShared WHERE id = :id")
    void updateHasEverBeenShared(int id, boolean hasEverBeenShared);

    @Query("DELETE FROM crop_charts WHERE farmerId = :farmerId")
    void deleteAllCropChartsByFarmer(String farmerId);

    @Query("SELECT COUNT(*) FROM crop_charts WHERE farmerId = :farmerId AND hasEverBeenShared = 1")
    int countLifetimeShared(String farmerId);
}
