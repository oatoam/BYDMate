package com.toddmo.bydmate.collector;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MqttPayloadDao {
    @Insert
    long insert(MqttPayload data);

    @Query("SELECT * FROM cached_mqtt_payload")
    List<MqttPayload> getAllCachedData();

    @Query("SELECT id FROM cached_mqtt_payload")
    List<Long> getAllCachedDataId();

    @Query("SELECT * FROM cached_mqtt_payload WHERE id = :id")
    MqttPayload getCachedDataById(long id);

    @Delete
    void delete(MqttPayload data);

    @Query("DELETE FROM cached_mqtt_payload WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT * FROM cached_mqtt_payload ORDER BY id ASC LIMIT 1")
    MqttPayload getOldestPayload();
}