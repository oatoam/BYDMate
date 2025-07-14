use tokio::sync::mpsc;
use persistence::InfluxDBRepository;
use data_pipeline::{MqttConsumer, KafkaProducer, KafkaConsumer};
use rumqttc::{AsyncClient, QoS};
use domain::RawDataPoint;
use std::time::Duration;
use rand::Rng;
use chrono::Utc;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    // InfluxDB Repository
    let influxdb_repo = InfluxDBRepository::new(
        "http://localhost:8086".to_string(),
        "byd_org".to_string(),
        "byd_bucket".to_string(),
        "my-super-secret-token".to_string(),
    );

    // Kafka Producer setup
    let (tx_kafka, rx_kafka) = mpsc::channel::<RawDataPoint>(100);
    let kafka_producer = KafkaProducer::new(
        "localhost:9092".to_string(),
        "raw_vehicle_data".to_string(),
        rx_kafka,
    );
    tokio::spawn(kafka_producer.run());

    // MQTT Consumer
    let mqtt_consumer = MqttConsumer::new(
        "localhost",
        "byd_mate_consumer",
        tx_kafka.clone(),
    ).await?;
    tokio::spawn(mqtt_consumer.run());

    // Kafka Consumer
    let kafka_consumer = KafkaConsumer::new(
        "localhost:9092".to_string(),
        "byd_mate_group".to_string(),
        "raw_vehicle_data".to_string(),
        influxdb_repo,
    );
    tokio::spawn(kafka_consumer.run());

    // Simulate MQTT client sending data
    let (mqtt_client, mut connection) = AsyncClient::new(rumqttc::MqttOptions::new("simulator", "localhost", 1883), 10);
    tokio::spawn(async move {
        loop {
            let vin = format!("TEST_VIN_{}", rand::thread_rng().gen_range(0..10));
            let speed = rand::thread_rng().gen_range(0..120);
            let soc = rand::thread_rng().gen_range(0..100);
            let timestamp = Utc::now().timestamp_millis();

            let payload = serde_json::json!({
                "speed": speed.to_string(),
                "soc": soc.to_string(),
                "timestamp": timestamp.to_string(),
            }).to_string();

            let topic = format!("byd-mate/vehicles/{}/metrics", vin);

            if let Err(e) = mqtt_client.publish(topic, QoS::AtMostOnce, false, payload.as_bytes()).await {
                eprintln!("Failed to publish simulated MQTT message: {}", e);
            }
            tokio::time::sleep(Duration::from_secs(1)).await;
        }
    });

    // Keep the main thread alive
    tokio::signal::ctrl_c().await?;
    println!("Shutting down...");

    Ok(())
}
