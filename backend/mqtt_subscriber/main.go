package main

import (
	"fmt"
	"context"
	"log"
	"time"

	mqtt "github.com/eclipse/paho.mqtt.golang"
	influxdb2 "github.com/influxdata/influxdb-client-go/v2"
)

const (
	mqttBroker = "tcp://emqx:1883"
	mqttClientID = "mqtt_subscriber"
	mqttTopic = "byd/data"

	influxDBURL = "http://influxdb:8086"
	influxDBToken = "my-super-secret-token"
	influxDBOrg = "byd_org"
	influxDBBucket = "byd_bucket"
)

func main() {
	// MQTT Client Options
	opts := mqtt.NewClientOptions().AddBroker(mqttBroker).SetClientID(mqttClientID)
	opts.SetKeepAlive(60 * time.Second)
	opts.SetPingTimeout(1 * time.Second)
	opts.SetCleanSession(true)

	// InfluxDB Client
	influxClient := influxdb2.NewClient(influxDBURL, influxDBToken)
	defer influxClient.Close()
	writeAPI := influxClient.WriteAPIBlocking(influxDBOrg, influxDBBucket)

	// Message Handler
	messagePubHandler := func(client mqtt.Client, msg mqtt.Message) {
		fmt.Printf("Received message: %s from topic: %s\n", msg.Payload(), msg.Topic())

		// Write to InfluxDB
		p := influxdb2.NewPointWithMeasurement("mqtt_data").
			AddTag("topic", msg.Topic()).
			SetTime(time.Now())

		// Assuming payload is a simple string for now,
		// in a real scenario, you'd parse the payload (e.g., JSON)
		// and add fields accordingly.
		p.AddField("payload", string(msg.Payload()))

		if err := writeAPI.WritePoint(context.Background(), p); err != nil {
			log.Printf("Error writing to InfluxDB: %v\n", err)
		} else {
			fmt.Println("Successfully wrote to InfluxDB")
		}
	}

	opts.SetDefaultPublishHandler(messagePubHandler)

	// Connect to MQTT Broker
	client := mqtt.NewClient(opts)
	if token := client.Connect(); token.Wait() && token.Error() != nil {
		log.Fatalf("Failed to connect to MQTT broker: %v", token.Error())
	}
	fmt.Println("Connected to MQTT broker")

	// Subscribe to topic
	if token := client.Subscribe(mqttTopic, 1, nil); token.Wait() && token.Error() != nil {
		log.Fatalf("Failed to subscribe to topic: %v", token.Error())
	}
	fmt.Printf("Subscribed to topic: %s\n", mqttTopic)

	// Keep the application running
	select {}
}