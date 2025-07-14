use rdkafka::config::ClientConfig;
use rdkafka::consumer::{Consumer, StreamConsumer};
use rdkafka::Message;
use tokio::sync::mpsc;
use domain::RawDataPoint;
use persistence::InfluxDBRepository;
use serde_json;

pub struct KafkaConsumer {
    consumer: StreamConsumer,
    influxdb_repo: InfluxDBRepository,
    topic: String,
}

impl KafkaConsumer {
    pub fn new(
        broker: String,
        group_id: String,
        topic: String,
        influxdb_repo: InfluxDBRepository,
    ) -> Self {
        let consumer: StreamConsumer = ClientConfig::new()
            .set("group.id", group_id)
            .set("bootstrap.servers", broker)
            .set("enable.auto.commit", "true")
            .create()
            .expect("Consumer creation error");

        KafkaConsumer {
            consumer,
            influxdb_repo,
            topic,
        }
    }

    pub async fn run(self) -> Result<(), Box<dyn std::error::Error>> {
        self.consumer.subscribe(&[&self.topic])?;

        loop {
            match self.consumer.recv().await {
                Ok(m) => {
                    let payload = match m.payload_view::<str>() {
                        Some(Ok(s)) => s,
                        _ => {
                            eprintln!("Error receiving payload from Kafka");
                            continue;
                        }
                    };

                    let data_point: RawDataPoint = serde_json::from_str(payload)?;
                    if let Err(e) = self.influxdb_repo.save_raw_data(vec![data_point]).await {
                        eprintln!("Error saving data to InfluxDB: {}", e);
                    }
                }
                Err(e) => {
                    eprintln!("Kafka consumer error: {}", e);
                }
            }
        }
    }
}