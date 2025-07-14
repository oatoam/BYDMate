package com.toddmo.bydmate.collector;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import androidx.room.Room;

import com.toddmo.bydmate.client.utils.KLog;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DataProcesser {

    private static final String TAG = "DataProcesser";
    private static final String MQTT_BROKER_URL = "tcp://123.123.123.123:8800";
    private static final String MQTT_CLIENT_ID_PREFIX = "BYDMateClient-";
    private static final String MQTT_TOPIC = "bydmate/data";

    private Context context;
    private MqttAsyncClient mqttClient;
    private AppDatabase db;
    private CachedDataDao cachedDataDao;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean isNetworkConnected = false;
    private boolean isMqttConnected = false;

    private ExecutorService mqttExecutor = Executors.newSingleThreadExecutor();
    private ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

    // Original mCaches for in-memory caching (if still needed, otherwise remove)
    Map<String, Map<String, Map<Long, String>>> mCaches;
    Object mLock = new Object();

    public DataProcesser(Context context) {
        this.context = context;
        this.mCaches = new HashMap<>(); // Keep original in-memory cache for now

        // Initialize Room Database
        db = Room.databaseBuilder(context.getApplicationContext(),
                AppDatabase.class, "data_cache_db").build();
        cachedDataDao = db.cachedDataDao();

        initMqttClient();
        registerNetworkCallback();
        
        // Check for cached data on startup
        sendCachedData();

        // Original memory usage watcher thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                KLog.d("memoryUsage watcher started");
                while (true) {
                    KLog.d("getMemoryUsage = " + getMemoryUsage());
                    try { Thread.sleep(1000); } catch (Exception e) {}
                }
            }
        }).start();
    }

    long getMemoryUsage() {
        long total = 0;
        synchronized (mLock) {
            if (mCaches == null) return 0;
            for (Map<String, Map<Long, String>> typeCache : mCaches.values()) {
                if (typeCache == null) continue;
                for (Map.Long, String> keyCache : typeCache.values()) {
                    if (keyCache == null) continue;
                    for (Map.Entry<Long, String> entry : keyCache.entrySet()) {
                        total += 8; // Long key (approximate)
                        if (entry.getValue() != null) {
                            total += 40; // String object overhead (approximate)
                            total += entry.getValue().length() * 2; // String characters (UTF-16)
                        }
                    }
                }
            }
            return total;
        }
    }

    private void initMqttClient() {
        String clientId = MQTT_CLIENT_ID_PREFIX + UUID.randomUUID().toString();
        try {
            mqttClient = new MqttAsyncClient(MQTT_BROKER_URL, clientId, new MemoryPersistence());
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    KLog.e("MQTT connection lost: " + cause.getMessage());
                    isMqttConnected = false;
                    // Attempt to reconnect
                    connectMqtt();
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // Not expecting to receive messages for this use case
                    KLog.d("Message arrived: " + topic + " -> " + new String(message.getPayload()));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Message delivered successfully
                    try {
                        // User context is the CachedData ID
                        long cachedDataId = (long) token.getUserContext();
                        KLog.d("Message delivered for cached data ID: " + cachedDataId);
                        dbExecutor.execute(() -> {
                            cachedDataDao.deleteById(cachedDataId);
                            KLog.d("Removed cached data with ID: " + cachedDataId);
                        });
                    } catch (MqttException e) {
                        KLog.e("Error getting message payload from token: " + e.getMessage());
                    }
                }
            });
        } catch (MqttException e) {
            KLog.e("Error creating MQTT client: " + e.getMessage());
        }
    }

    private void connectMqtt() {
        if (mqttClient == null || mqttClient.isConnected() || !isNetworkConnected) {
            KLog.d("MQTT client not ready to connect. Connected: " + (mqttClient != null && mqttClient.isConnected()) + ", Network: " + isNetworkConnected);
            return;
        }

        MqttConnectOptions connectOptions = new MqttConnectOptions();
        connectOptions.setCleanSession(true); // Clean session on connect
        connectOptions.setAutomaticReconnect(true); // Enable automatic reconnect
        connectOptions.setConnectionTimeout(30); // Set connection timeout
        connectOptions.setKeepAliveInterval(60); // Set keep alive interval

        try {
            KLog.d("Attempting to connect to MQTT broker: " + MQTT_BROKER_URL);
            mqttClient.connect(connectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    KLog.d("MQTT connected successfully!");
                    isMqttConnected = true;
                    // Send any cached data after successful connection
                    sendCachedData();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    KLog.e("MQTT connection failed: " + exception.getMessage());
                    isMqttConnected = false;
                }
            });
        } catch (MqttException e) {
            KLog.e("Error connecting to MQTT broker: " + e.getMessage());
            isMqttConnected = false;
        }
    }

    private void disconnectMqtt() {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
                isMqttConnected = false;
                KLog.d("MQTT disconnected.");
            } catch (MqttException e) {
                KLog.e("Error disconnecting MQTT: " + e.getMessage());
            }
        }
    }

    private void registerNetworkCallback() {
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            KLog.e("ConnectivityManager not available.");
            return;
        }

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);
                KLog.d("Network is available.");
                isNetworkConnected = true;
                connectMqtt(); // Attempt to connect MQTT when network becomes available
            }

            @Override
            public void onLost(Network network) {
                super.onLost(network);
                KLog.d("Network is lost.");
                isNetworkConnected = false;
                disconnectMqtt(); // Disconnect MQTT when network is lost
            }

            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                super.onCapabilitiesChanged(network, networkCapabilities);
                boolean oldNetworkConnected = isNetworkConnected;
                isNetworkConnected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                if (isNetworkConnected && !oldNetworkConnected) {
                    KLog.d("Network capabilities changed: Internet available.");
                    connectMqtt();
                } else if (!isNetworkConnected && oldNetworkConnected) {
                    KLog.d("Network capabilities changed: Internet lost.");
                    disconnectMqtt();
                }
            }
        };

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);

        // Initial check of network state
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork != null) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
            if (capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                isNetworkConnected = true;
                KLog.d("Initial network state: Connected.");
            } else {
                isNetworkConnected = false;
                KLog.d("Initial network state: Not connected to internet.");
            }
        } else {
            isNetworkConnected = false;
            KLog.d("Initial network state: No active network.");
        }
    }

    private void sendDataToMqtt(String type, String key, String value, long timestamp, long cachedDataId) {
        if (mqttClient == null || !mqttClient.isConnected()) {
            KLog.w("MQTT client not connected, caching data.");
            saveDataToLocalDb(type, key, value, timestamp);
            return;
        }

        String payload = String.format("{\"type\":\"%s\",\"key\":\"%s\",\"value\":\"%s\",\"timestamp\":%d}",
                type, key, value, timestamp);
        try {
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(1); // At least once delivery
            message.setRetained(false);

            mqttClient.publish(MQTT_TOPIC, message, cachedDataId, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    KLog.d("Data sent to MQTT successfully: " + payload);
                    // If data was from DB, remove it
                    if (asyncActionToken.getUserContext() instanceof Long) {
                        long id = (long) asyncActionToken.getUserContext();
                        dbExecutor.execute(() -> {
                            cachedDataDao.deleteById(id);
                            KLog.d("Removed cached data with ID: " + id);
                        });
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    KLog.e("Failed to send data to MQTT: " + payload + ", Error: " + exception.getMessage());
                    // If sending failed, ensure it's in DB or re-save it
                    if (!(asyncActionToken.getUserContext() instanceof Long)) { // Only save if not already from DB
                        saveDataToLocalDb(type, key, value, timestamp);
                    }
                }
            });
        } catch (MqttException e) {
            KLog.e("Error publishing MQTT message: " + e.getMessage());
            saveDataToLocalDb(type, key, value, timestamp);
        }
    }

    private void saveDataToLocalDb(String type, String key, String value, long timestamp) {
        dbExecutor.execute(() -> {
            CachedData data = new CachedData(type, key, value, timestamp);
            cachedDataDao.insert(data);
            KLog.d("Data cached to local DB: " + type + " // " + key + " = " + value);
        });
    }

    private void sendCachedData() {
        mqttExecutor.execute(() -> {
            if (mqttClient == null || !mqttClient.isConnected()) {
                KLog.d("MQTT not connected, cannot send cached data yet.");
                return;
            }

            KLog.d("Attempting to send cached data...");
            dbExecutor.execute(() -> {
                List<CachedData> cachedDataList = cachedDataDao.getAllCachedData();
                if (cachedDataList != null && !cachedDataList.isEmpty()) {
                    for (CachedData data : cachedDataList) {
                        sendDataToMqtt(data.type, data.key, data.value, data.timestamp, data.id);
                    }
                } else {
                    KLog.d("No cached data to send.");
                }
            });
        });
    }

    long getCachedTime() {
        return new Date().getTime();
    }

    public void put(String key, String value) {
        put("default", key, value);
    }

    public void put(String type, String key, String value) {
        KLog.v(String.format("%s // %s = %s", type, key, value));
        long timestamp = getCachedTime();

        // Send to MQTT or cache to DB
        sendDataToMqtt(type, key, value, timestamp, -1); // -1 indicates not from DB initially

        // Original in-memory cache logic (keep for now, can be removed if not needed)
        synchronized (mLock) {
            if (!mCaches.containsKey(type)) {
                mCaches.put(type, new HashMap());
            }
            Map<String, Map<Long, String>> typeCache = mCaches.get(type);
            if (!typeCache.containsKey(key)) {
                typeCache.put(key, new HashMap());
            }
            Map<Long, String> keyCache = typeCache.get(key);
            keyCache.put(timestamp, value);
        }
    }
}
