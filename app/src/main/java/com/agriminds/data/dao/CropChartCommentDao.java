package com.agriminds.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.agriminds.data.entity.CropChartComment;

import java.util.List;

@Dao
public interface CropChartCommentDao {

    @Insert
    long insert(CropChartComment comment);

    @Update
    void update(CropChartComment comment);

    @Delete
    void delete(CropChartComment comment);

    @Query("SELECT * FROM crop_chart_comments WHERE cropChartId = :cropChartId ORDER BY timestamp DESC")
    LiveData<List<CropChartComment>> getCommentsByCropChart(int cropChartId);

    @Query("SELECT * FROM crop_chart_comments WHERE id = :id")
    LiveData<CropChartComment> getCommentById(int id);

    @Query("SELECT COUNT(*) FROM crop_chart_comments WHERE cropChartId = :cropChartId")
    LiveData<Integer> getCommentCount(int cropChartId);

    @Query("DELETE FROM crop_chart_comments WHERE cropChartId = :cropChartId")
    void deleteCommentsByCropChart(int cropChartId);
}
