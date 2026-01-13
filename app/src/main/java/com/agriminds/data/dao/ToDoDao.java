package com.agriminds.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.agriminds.data.entity.ToDo;

import java.util.List;

@Dao
public interface ToDoDao {

    @Insert
    void insert(ToDo todo);

    @Update
    void update(ToDo todo);

    @Delete
    void delete(ToDo todo);

    @Query("SELECT * FROM todos WHERE userId = :userId ORDER BY createdAt DESC")
    LiveData<List<ToDo>> getToDosByUser(String userId);
}
