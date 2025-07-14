package com.toddmo.bydmate.collector;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {CachedData.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract CachedDataDao cachedDataDao();
}