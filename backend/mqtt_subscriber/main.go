package main

import (
	"fmt"
	"context"
	"log"
	"encoding/json"
	"time"

	mqtt "github.com/eclipse/paho.mqtt.golang"
	influxdb2 "github.com/influxdata/influxdb-client-go/v2"
)

// DataPoint represents a single data point with category, key, and value.
type DataPoint struct {
	Vin      string      `json:"vin"`
	Category string      `json:"category"`
	Key      string      `json:"key"`
	Value    interface{} `json:"value"`
	Timestamp string     `json:"timestamp"`
}

const (
	mqttBroker = "tcp://emqx:1883"
	mqttClientID = "mqtt_subscriber"
	mqttOnlineTopic = "bydmate/online"
	mqttDataTopic = "bydmate/data"

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

	// Message Handler with goroutine for faster response
	messagePubHandler := func(client mqtt.Client, msg mqtt.Message) {
		go func() {
			fmt.Printf("Received \"%s\" : %s \n", msg.Topic(), msg.Payload())

			var dp DataPoint
			if err := json.Unmarshal(msg.Payload(), &dp); err != nil {
				log.Printf("Failed to parse JSON payload: %v\n", err)
				return
			}
			t, err := time.Parse(time.RFC3339, dp.Timestamp)
			if err != nil {
				log.Printf("Invalid timestamp format: %v\n", err)
				return
			}

			// Write to InfluxDB
			p := influxdb2.NewPointWithMeasurement("bydmate_" + dp.Vin)

			log.Printf("Writing data point to InfluxDB: %s", dp)
			p.SetTime(t)
			// p.AddTag("vin", dp.Vin)
			p.AddTag("category", dp.Category)
			p.AddField(dp.Key, dp.Value)

			if err := writeAPI.WritePoint(context.Background(), p); err != nil {
				fmt.Printf("Error writing to InfluxDB: %v\n", err)
			} else {
				fmt.Println("Successfully wrote to InfluxDB")
			}
		}()
	}

	opts.SetDefaultPublishHandler(messagePubHandler)

	// Connect to MQTT Broker
	client := mqtt.NewClient(opts)
	if token := client.Connect(); token.Wait() && token.Error() != nil {
		log.Fatalf("Failed to connect to MQTT broker: %v", token.Error())
	}
	fmt.Println("Connected to MQTT broker")

	// Subscribe to topic
	if token := client.Subscribe(mqttDataTopic, 1, nil); token.Wait() && token.Error() != nil {
		log.Fatalf("Failed to subscribe to topic: %v", token.Error())
	}
	fmt.Printf("Subscribed to topic: %s\n", mqttDataTopic)

	// Keep the application running
	select {}
}