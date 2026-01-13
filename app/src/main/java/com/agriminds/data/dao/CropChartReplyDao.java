package com.agriminds.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.agriminds.data.entity.CropChartReply;

import java.util.List;

@Dao
public interface CropChartReplyDao {
    @Insert
    long insert(CropChartReply reply);

    @Update
    void update(CropChartReply reply);

    @Delete
    void delete(CropChartReply reply);

    @Query("SELECT * FROM crop_chart_replies WHERE commentId = :commentId ORDER BY timestamp ASC")
    LiveData<List<CropChartReply>> getRepliesByComment(int commentId);
}
