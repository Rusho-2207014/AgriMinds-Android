package com.agriminds.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.agriminds.data.entity.CropChartStar;

import java.util.List;

@Dao
public interface CropChartStarDao {
    @Insert
    long insert(CropChartStar star);

    @Update
    void update(CropChartStar star);

    @Delete
    void delete(CropChartStar star);

    @Query("SELECT AVG(stars) FROM crop_chart_stars WHERE cropChartId = :cropChartId")
    Float getAverageStars(int cropChartId);

    @Query("SELECT * FROM crop_chart_stars WHERE cropChartId = :cropChartId")
    List<CropChartStar> getStarsByChart(int cropChartId);

    @Query("SELECT * FROM crop_chart_stars WHERE cropChartId = :cropChartId AND expertId = :expertId LIMIT 1")
    CropChartStar getStarByExpert(int cropChartId, String expertId);

    @Query("SELECT AVG(stars) FROM crop_chart_stars INNER JOIN crop_charts ON crop_chart_stars.cropChartId = crop_charts.id WHERE crop_charts.farmerId = :farmerId")
    Float getAverageStarsByFarmer(String farmerId);
}
