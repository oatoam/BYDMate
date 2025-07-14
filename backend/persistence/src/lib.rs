use influxdb::{Client, Timestamp};
use influxdb::Query; // Correct import for Query
use domain::RawDataPoint;
use async_trait::async_trait;
use chrono::{DateTime, Utc}; // Import DateTime and Utc

#[async_trait]
pub trait InfluxDBRepositoryTrait {
    async fn save_raw_data(&self, points: Vec<RawDataPoint>) -> Result<(), influxdb::Error>;
}

pub struct InfluxDBRepository {
    client: Client,
    org: String,
    bucket: String,
}

impl InfluxDBRepository {
    pub fn new(url: String, org: String, bucket: String, token: String) -> Self {
        let client = Client::new(url, &token);
        InfluxDBRepository { client, org, bucket }
    }
}

#[async_trait]
impl InfluxDBRepositoryTrait for InfluxDBRepository {
    async fn save_raw_data(&self, points: Vec<RawDataPoint>) -> Result<(), influxdb::Error> {
        let mut queries = Vec::new();
        for point in points {
            let timestamp = Timestamp::from(point.timestamp);
            let query = Query::write_query(timestamp)
                .add_tag("vin", point.vin)
                .add_field(point.key, point.value)
                .build()?;

            queries.push(query);
        }

        self.client.query(&queries).await?;
        Ok(())
    }
}
