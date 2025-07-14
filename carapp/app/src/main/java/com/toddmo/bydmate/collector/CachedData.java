package com.toddmo.bydmate.collector;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cached_data")
public class CachedData {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "type")
    public String type;

    @ColumnInfo(name = "key")
    public String key;

    @ColumnInfo(name = "value")
    public String value;

    @ColumnInfo(name = "timestamp")
    public long timestamp;

    public CachedData(String type, String key, String value, long timestamp) {
        this.type = type;
        this.key = key;
        this.value = value;
        this.timestamp = timestamp;
    }
}