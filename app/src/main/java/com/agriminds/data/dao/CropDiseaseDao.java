package com.agriminds.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.agriminds.data.entity.CropDisease;

import java.util.List;

@Dao
public interface CropDiseaseDao {
    @Insert
    void insert(CropDisease disease);

    @Query("SELECT * FROM crop_diseases")
    List<CropDisease> getAllDiseases();
}
