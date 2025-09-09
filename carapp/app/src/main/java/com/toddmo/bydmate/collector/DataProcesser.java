package com.toddmo.bydmate.collector;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.widget.Toast;

import androidx.room.Room;

import com.toddmo.bydmate.client.utils.DataHolder;
import com.toddmo.bydmate.client.utils.KLog;

import java.io.File;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import com.github.luben.zstd.Zstd;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DataProcesser {

    private static final String TAG = "DataProcesser";
    private static final String MQTT_BROKER_URL = "tcp://1.2.3.4:1883";
    private static final String MQTT_CLIENT_ID_PREFIX = "BYDMateClient-";
    private static final String MQTT_DATA_TOPIC = "bydmate/data";
    private static final String MQTT_ZDATA_TOPIC = "bydmate/zdata";

    private Context context;
    private MqttAsyncClient mqttClient;
    private AppDatabase db;
    private MqttPayloadDao mqttPayloadDao;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean isNetworkConnected = false;
    private boolean isMqttConnected = false;

    // 存储量上限配置项
    private static final float MIN_FREE_SPACE_PERCENTAGE = 0.10f; // 设备可用存储空间不足10%
    private static final float MAX_APP_STORAGE_PERCENTAGE = 0.30f; // 当前应用占用的存储空间超过系统总空间的30%
    private static final long MIN_FREE_SPACE_AFTER_CLEANUP_BYTES = 1 * 1024 * 1024 * 1024L; // 清理后可用空间不少于1GB

    private ExecutorService mqttExecutor = Executors.newFixedThreadPool(5);
    private ExecutorService dbExecutor = Executors.newFixedThreadPool(5);

    private ExecutorService bgExecutor = Executors.newFixedThreadPool(5);

    // Original mCaches for in-memory caching (if still needed, otherwise remove)
//    Map<String, Map<String, Map<String, String>>> mCaches;
    Object mLock = new Object();

    String VIN;

    public DataProcesser(Context context) {
        this.context = context;
//        this.mCaches = new HashMap<>(); // Keep original in-memory cache for now
        VIN = DataHolder.getInstance().getString("VIN");

        // Initialize Room Database
        db = Room.databaseBuilder(context.getApplicationContext(),
                AppDatabase.class, "data_cache_db")
                .fallbackToDestructiveMigration()
                .build();
        mqttPayloadDao = db.mqttPayloadDao();

        initMqttClient();
        registerNetworkCallback();
        
        // Check for cached data on startup
        sendCachedData();

//        // Original memory usage watcher thread
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                KLog.d("memoryUsage watcher started");
//                while (true) {
//                    KLog.d("getMemoryUsage = " + getMemoryUsage());
//                    try { Thread.sleep(1000); } catch (Exception e) {}
//                }
//            }
//        }).start();
    }

//    long getMemoryUsage() {
//        long total = 0;
//        synchronized (mLock) {
//            if (mCaches == null) return 0;
//            for (Map<String, Map<String, String>> typeCache : mCaches.values()) {
//                if (typeCache == null) continue;
//                for (Map<String, String> keyCache : typeCache.values()) {
//                    if (keyCache == null) continue;
//                    for (Map.Entry<String, String> entry : keyCache.entrySet()) {
//                        total += 8; // Long key (approximate)
//                        if (entry.getValue() != null) {
//                            total += 40; // String object overhead (approximate)
//                            total += entry.getValue().length() * 2; // String characters (UTF-16)
//                        }
//                    }
//                }
//            }
//            return total;
//        }
//    }

    private void initMqttClient() {
        String clientId = MQTT_CLIENT_ID_PREFIX + VIN;
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
                    // User context is the CachedData ID
                    long cachedDataId = (long) token.getUserContext();
                    KLog.d("Message delivered for cached data ID: " + cachedDataId);
                    if (cachedDataId > 0) {
                        dbExecutor.execute(() -> {
                            mqttPayloadDao.deleteById(cachedDataId);
                            KLog.d("deliveryComplete: Removed cached data with ID: " + cachedDataId);
                        });
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
    private void sendPayloadToMqtt(String payload, long cachedDataId) {
        try {
            JSONObject json = new JSONObject(payload);
            sendPayloadToMqtt(json, cachedDataId);
        } catch (JSONException e) {
            dbExecutor.execute(() -> {
                mqttPayloadDao.deleteById(cachedDataId);
                KLog.d("JSONException: Removed cached data with ID: " + cachedDataId);
            });
        }
    }

    private void sendPayloadToMqttSync(String payload, long cachedDataId) {
        try {
            JSONObject json = new JSONObject(payload);
            sendPayloadToMqttSync(json, cachedDataId);
        } catch (JSONException e) {
            dbExecutor.execute(() -> {
                mqttPayloadDao.deleteById(cachedDataId);
                KLog.d("JSONException: Removed cached data with ID: " + cachedDataId);
            });
        }
    }

    private void sendPayloadToMqtt(JSONObject payload, long cachedDataId) {
        mqttExecutor.execute(() -> {
            sendPayloadToMqttSync(payload, cachedDataId);
        });
    }

    final boolean COMPRESS = true;

    private void sendPayloadToMqttSync(JSONObject payload, long cachedDataId) {
        if (mqttClient == null || !mqttClient.isConnected()) {
            if (cachedDataId < 0) {
                KLog.w("MQTT client not connected, caching data.");
                savePayloadToLocalDb(payload);
            } else {
                KLog.w("MQTT client not connected, data was already cached");
            }
            return;
        }

        String payloadStr = payload.toString();
        try {
            byte[] payloadByte = payloadStr.getBytes();
            String topic = MQTT_DATA_TOPIC;

            if (COMPRESS) {
                topic = MQTT_ZDATA_TOPIC;
                payloadByte = Zstd.compress(payloadByte);
            }

            MqttMessage message = new MqttMessage(payloadByte);
            message.setQos(1); // At least once delivery
            message.setRetained(false);

            mqttClient.publish(topic, message, cachedDataId, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    KLog.d("Data sent to MQTT successfully: " + payload);
                    // If data was from DB, remove it
                    if (asyncActionToken.getUserContext() instanceof Long) {
                        long id = (long) asyncActionToken.getUserContext();
                        if (id > 0) {
                            dbExecutor.execute(() -> {
                                mqttPayloadDao.deleteById(id);
                                KLog.d("onSuccess: Removed cached data with ID: " + id);
                            });
                        }
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    KLog.e("Failed to send data to MQTT: " + payload + ", Error: " + exception.getMessage());
                    // If sending failed, ensure it's in DB or re-save it
                    if (!(asyncActionToken.getUserContext() instanceof Long) || (
                            asyncActionToken.getUserContext() instanceof Long && (long)asyncActionToken.getUserContext() < 0)) {
                        // Only save if not already from DB
                        savePayloadToLocalDb(payload);
                    }
                }
            });
        } catch (MqttException e) {
            KLog.e("Error publishing MQTT message id = " + cachedDataId + ": " + e.getMessage());
            if (cachedDataId < 0) {
                savePayloadToLocalDb(payload);
            }
        }
    }

    private void savePayloadToLocalDb(JSONObject payload) {
        dbExecutor.execute(() -> {
            // 写入最新条目
            MqttPayload data = new MqttPayload(payload);
            mqttPayloadDao.insert(data);
            KLog.d("Data cached to local DB: " + data.payload);

//            // 检查存储上限并启动清理
//            if (isStorageLimitReached()) {
//                KLog.w("Storage limit reached. Starting cleanup.");
//                new Handler(android.os.Looper.getMainLooper()).post(() ->
//                    Toast.makeText(context, "BYDMate: storage limit reached", Toast.LENGTH_LONG).show()
//                );
//                startStorageCleanup();
//            }
        });
    }

    boolean isCleaning = false;
    // 启动异步清理逻辑
    private void startStorageCleanup() {
        synchronized (mLock) {
            if (isCleaning) return;
        }
        dbExecutor.execute(() -> {
            long databaseSizeBytes = getAppDatabaseSizeBytes();

            long databaseSizeBytesBeforeClean = databaseSizeBytes;
            KLog.d("Starting storage cleanup. Current databaseSizeBytes: " + databaseSizeBytes + " bytes.");

            synchronized (mLock) {
                isCleaning = true;
            }
            while (isStorageLimitReached()) {
                MqttPayload oldestPayload = mqttPayloadDao.getOldestPayload();
                if (oldestPayload != null) {
                    mqttPayloadDao.deleteById(oldestPayload.id);
                    KLog.d("Cleaned up oldest cached data with ID: " + oldestPayload.id);
                    // 重新获取可用空间，因为删除操作会释放空间
                    databaseSizeBytes = getAvailableExternalStorageBytes();
                } else {
                    KLog.d("No more cached data to clean up.");
                    break; // 没有更多数据可清理
                }
            }
            KLog.d("Storage cleanup finished. Available space after cleanup: " + databaseSizeBytes + " bytes.");

            long finaldatabaseSizeBytes = databaseSizeBytes;
            new Handler(android.os.Looper.getMainLooper()).post(() ->
                            Toast.makeText(context, "BYDMate: Storage cleanup finished. freeed: " +
                    (finaldatabaseSizeBytes - databaseSizeBytesBeforeClean) + " bytes.", Toast.LENGTH_LONG).show()
            );
            synchronized (mLock) {
                isCleaning = false;
            }
        });
    }

    private void sendCachedData() {
        bgExecutor.execute(() -> {
            if (mqttClient == null || !mqttClient.isConnected()) {
                KLog.d("MQTT not connected, cannot send cached data yet 1.");
                return;
            }

            KLog.d("Attempting to send cached data...");
            while (true) {
                List<Long> mqttPayloadIdList = mqttPayloadDao.getAllCachedDataId();
                KLog.d("CachedData size = " + mqttPayloadIdList.size());
                if (mqttPayloadIdList != null && !mqttPayloadIdList.isEmpty()) {
                    for (Long id : mqttPayloadIdList) {
                        if (mqttClient == null || !mqttClient.isConnected()) {
                            KLog.d("MQTT not connected, cannot send cached data yet 2.");
                            return;
                        }
                        MqttPayload data = mqttPayloadDao.getCachedDataById(id);
                        if (data != null) {
                            // TODO: using MqttClient for synchorously sending
                            sendPayloadToMqttSync(data.payload, data.id);
                        }
                    }
                } else {
                    KLog.d("No cached data to send.");
                    return;
                }

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                }
            }

//                List<MqttPayload> mqttPayloadList = mqttPayloadDao.getAllCachedData();
//                if (mqttPayloadList != null && !mqttPayloadList.isEmpty()) {
//                    for (MqttPayload data : mqttPayloadList) {
//                        sendPayloadToMqtt(data.payload, data.id);
//                    }
//                } else {
//                    KLog.d("No cached data to send.");
//                }
        });
    }

    String getCachedTime() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new java.util.Date());
    }

    public void put(String key, String value) {
        put("default", key, value);
    }

    public void put(String type, String key, String value) {
        KLog.v(String.format("%s // %s = %s", type, key, value));
        String timestamp = getCachedTime();

        JSONObject payload = new JSONObject();
        try {
            payload.put("type", type);
            payload.put("key", key);
            payload.put("value", value);
            payload.put("timestamp", timestamp);
            payload.put("vin", VIN);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }


        // Send to MQTT or cache to DB
        sendPayloadToMqtt(payload, -1); // -1 indicates not from DB initially

        // Original in-memory cache logic (keep for now, can be removed if not needed)
//        synchronized (mLock) {
//            if (!mCaches.containsKey(type)) {
//                mCaches.put(type, new HashMap());
//            }
//            Map<String, Map<String, String>> typeCache = mCaches.get(type);
//            if (!typeCache.containsKey(key)) {
//                typeCache.put(key, new HashMap());
//            }
//            Map<String, String> keyCache = typeCache.get(key);
//            keyCache.put(timestamp, value);
//        }
        // 检查是否达到存储上限

    }

    private boolean isStorageLimitReached() {
        long totalStorage = getTotalExternalStorageBytes();
        long availableStorage = getAvailableExternalStorageBytes();
        long appDbSize = getAppDatabaseSizeBytes();

        // 条件1: 设备可用存储空间不足10%
        boolean condition1 = (float) availableStorage / totalStorage < MIN_FREE_SPACE_PERCENTAGE;
        KLog.d("Storage Check - Available: " + availableStorage + ", Total: " + totalStorage + ", Ratio: " + ((float) availableStorage / totalStorage) + ", Limit: " + MIN_FREE_SPACE_PERCENTAGE + ", Condition1: " + condition1);

        // 条件2: 当前应用占用的存储空间超过系统总空间的30%
        boolean condition2 = (float) appDbSize / totalStorage > MAX_APP_STORAGE_PERCENTAGE;
        KLog.d("Storage Check - App DB Size: " + appDbSize + ", Total: " + totalStorage + ", Ratio: " + ((float) appDbSize / totalStorage) + ", Limit: " + MAX_APP_STORAGE_PERCENTAGE + ", Condition2: " + condition2);

        return condition1 || condition2;
    }

    // 获取设备总存储空间 (bytes)
    private long getTotalExternalStorageBytes() {
        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long totalBlocks = stat.getBlockCountLong();
        return totalBlocks * blockSize;
    }

    // 获取设备可用存储空间 (bytes)
    private long getAvailableExternalStorageBytes() {
        File path = Environment.getExternalStorageDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        return availableBlocks * blockSize;
    }

    // 获取应用数据库文件大小 (bytes)
    private long getAppDatabaseSizeBytes() {
        File dbFile = context.getDatabasePath("data_cache_db");
        if (dbFile.exists()) {
            return dbFile.length();
        }
        return 0;
    }
}
