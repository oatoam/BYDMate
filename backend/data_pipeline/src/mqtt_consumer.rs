use rumqttc::{AsyncClient, Event, EventLoop, MqttOptions, Packet, QoS};
use tokio::sync::mpsc;
use std::time::Duration;
use serde_json::Value;
use domain::RawDataPoint;

pub struct MqttConsumer {
    client: AsyncClient,
    eventloop: EventLoop,
    kafka_producer_tx: mpsc::Sender<RawDataPoint>,
}

impl MqttConsumer {
    pub async fn new(
        broker_addr: &str,
        client_id: &str,
        kafka_producer_tx: mpsc::Sender<RawDataPoint>,
    ) -> Result<Self, rumqttc::Error> {
        let mut mqttoptions = MqttOptions::new(client_id, broker_addr, 1883);
        mqttoptions.set_keep_alive(Duration::from_secs(5));
        let (client, eventloop) = AsyncClient::new(mqttoptions, 10);
        client.subscribe("byd-mate/vehicles/+/metrics", QoS::AtMostOnce).await?;
        Ok(MqttConsumer {
            client,
            eventloop,
            kafka_producer_tx,
        })
    }

    pub async fn run(mut self) -> Result<(), rumqttc::Error> {
        loop {
            match self.eventloop.poll().await {
                Ok(Event::Incoming(Packet::Publish(p))) => {
                    let topic = p.topic;
                    let payload_str = String::from_utf8_lossy(&p.payload);
                    println!("Received MQTT message on topic: {}, payload: {}", topic, payload_str);

                    if let Ok(json_value) = serde_json::from_str::<Value>(&payload_str) {
                        if let Some(vin_part) = topic.split('/').nth(2) {
                            if let Some(metrics) = json_value.as_object() {
                                for (key, value) in metrics {
                                    if let Some(val_str) = value.as_str() {
                                        let data_point = RawDataPoint {
                                            vin: vin_part.to_string(),
                                            timestamp: chrono::Utc::now().timestamp_millis(),
                                            key: key.clone(),
                                            value: val_str.to_string(),
                                        };
                                        if let Err(e) = self.kafka_producer_tx.send(data_point).await {
                                            eprintln!("Failed to send data point to Kafka producer: {}", e);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Ok(Event::Incoming(_)) => {}
                Ok(Event::Outgoing(_)) => {}
                Err(e) => {
                    eprintln!("MQTT eventloop error: {}", e);
                    break;
                }
            }
        }
        Ok(())
    }
}