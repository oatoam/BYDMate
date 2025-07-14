use serde::{Deserialize, Serialize};
use chrono::{DateTime, Utc};

#[derive(Debug, Serialize, Deserialize)]
pub struct RawDataPoint {
    pub vin: String,
    pub timestamp: DateTime<Utc>,
    pub key: String,
    pub value: String,
}