package com.toddmo.bydmate.collector;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.json.JSONObject;

@Entity(tableName = "cached_mqtt_payload")
public class MqttPayload {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "payload")
    public String payload;

    public MqttPayload(String payload) {
        this.payload = payload;
    }

    public MqttPayload(JSONObject payload) {
        this.payload = payload.toString();
    }
}