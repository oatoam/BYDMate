# BYDMate

BYDMate is a data analysis and monitoring platform for BYD electric vehicles.

## Project Structure

```
backend/
  data_analyzer/ - Go service for analyzing data from MQTT and InfluxDB
  mqtt_subscriber/ - Go service for subscribing to MQTT topics and writing data to InfluxDB
carapp/
  app/ - Android application for collecting and sending vehicle data
docker/
  docker-compose.yml - Docker Compose configuration for running the project
frontend/
  src/ - Vue.js frontend for visualizing data
log/ - Log data
tools/
  data_generator/ - Tool for generating synthetic data
  mqtt_test_client/ - Tool for testing MQTT
```

## Components

*   **`backend/data_analyzer/`**: This service subscribes to the `byd/analysis_trigger` MQTT topic, retrieves data from InfluxDB, analyzes it, and potentially publishes the results.  It's written in Go.
*   **`backend/mqtt_subscriber/`**: This service subscribes to the `bydmate/data` and `bydmate/zdata` MQTT topics and writes the received data to InfluxDB. It's written in Go.
*   **`carapp/app/`**: This Android application collects vehicle data and sends it to the MQTT broker. It caches the data locally in a SQLite database if the MQTT connection is unavailable.  It uses the `org.eclipse.paho.client.mqttv3` library.
*   **`frontend/src/`**: This Vue.js frontend provides a user interface for visualizing the data collected from the car.
*   **`docker/docker-compose.yml`**: This file defines the services for the project, including the EMQX MQTT broker, the backend services, and the frontend. It sets up the necessary network and volume configurations.

## Data Flow

1.  The `carapp` collects data from the BYD vehicle and sends it to the EMQX MQTT broker via the `bydmate/data` and `bydmate/zdata` topics.
2.  The `mqtt_subscriber` service subscribes to these topics and writes the data to InfluxDB.
3.  The `data_analyzer` service subscribes to the `byd/analysis_trigger` topic. When it receives a message, it queries data from InfluxDB, performs analysis, and potentially publishes the results to another MQTT topic.
4.  The `frontend` retrieves data from the backend (likely via an API) and visualizes it in the user interface.

## Building and Running the Project

To build and run the project, you will need to have Docker and Docker Compose installed.

1.  Clone the repository.
2.  Navigate to the `docker/` directory.
3.  Run `docker-compose up`.

## Development and Testing

Details on development and testing will be added later.
