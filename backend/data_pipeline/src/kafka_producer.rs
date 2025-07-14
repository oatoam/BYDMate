use rdkafka::config::ClientConfig;
use rdkafka::producer::{FutureProducer, FutureRecord};
use tokio::sync::mpsc;
use domain::RawDataPoint;
use serde_json;

pub struct KafkaProducer {
    producer: FutureProducer,
    rx: mpsc::Receiver<RawDataPoint>,
    topic: String,
}

impl KafkaProducer {
    pub fn new(broker: String, topic: String, rx: mpsc::Receiver<RawDataPoint>) -> Self {
        let producer: FutureProducer = ClientConfig::new()
            .set("bootstrap.servers", broker)
            .set("message.timeout.ms", "5000")
            .create()
            .expect("Producer creation error");

        KafkaProducer { producer, rx, topic }
    }

    pub async fn run(mut self) {
        while let Some(data_point) = self.rx.recv().await {
            let payload = serde_json::to_string(&data_point).expect("Failed to serialize RawDataPoint");
            let key = data_point.vin.clone();

            let record = FutureRecord::to(&self.topic)
                .payload(&payload)
                .key(&key);

            match self.producer.send(record, 0).await {
                Ok(delivery) => println!("Sent: {:?}", delivery),
                Err((e, _)) => eprintln!("Error sending to Kafka: {:?}", e),
            }
        }
    }
}