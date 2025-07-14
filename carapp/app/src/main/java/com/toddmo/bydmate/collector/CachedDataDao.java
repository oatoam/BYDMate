package com.toddmo.bydmate.collector;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CachedDataDao {
    @Insert
    long insert(CachedData data);

    @Query("SELECT * FROM cached_data ORDER BY timestamp ASC")
    List<CachedData> getAllCachedData();

    @Query("SELECT * FROM cached_data WHERE id = :id")
    CachedData getCachedDataById(long id);

    @Delete
    void delete(CachedData data);

    @Query("DELETE FROM cached_data WHERE id = :id")
    void deleteById(long id);
}