package main

import (
	"fmt"
	"context"
	"log"
	"encoding/json"
	"time"

	mqtt "github.com/eclipse/paho.mqtt.golang"
	influxdb2 "github.com/influxdata/influxdb-client-go/v2"
	"github.com/klauspost/compress/zstd"
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
	mqttZDataTopic = "bydmate/zdata"

	influxDBURL = "http://influxdb:8086"
	influxDBToken = "my-super-secret-token"
	influxDBOrg = "byd_org"
	influxDBBucket = "byd_bucket"
)


var decoder, _ = zstd.NewReader(nil, zstd.WithDecoderConcurrency(0))

// Decompress a buffer. We don't supply a destination buffer,
// so it will be allocated by the decoder.
func Decompress(src []byte) ([]byte, error) {
    return decoder.DecodeAll(src, nil)
} 

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

	receivedMessageCount := 0
	writeSuccessCount := 0
	writeFailedCount := 0

	// Message Handler with goroutine for faster response
	messagePubHandler := func(client mqtt.Client, msg mqtt.Message) {
		go func() {
			var payloadBytes []byte
			var err error

			if msg.Topic() == mqttZDataTopic {
				payloadBytes, err = Decompress(msg.Payload())
				if err != nil {
					log.Printf("Failed to decompress ZSTD payload from topic %s: %v\n", msg.Topic(), err)
					return
				}
			} else {
				payloadBytes = msg.Payload()
				
			}
			displayPayload := payloadBytes
			if len(displayPayload) > 100 {
				displayPayload = displayPayload[:100]
			}
			fmt.Printf("Received \"%s\" : %s \n", msg.Topic(), displayPayload)

			var dp DataPoint
			if err := json.Unmarshal(payloadBytes, &dp); err != nil {
				log.Printf("Failed to parse JSON payload from topic %s: %v\n", msg.Topic(), err)
				return
			}
			t, err := time.Parse(time.RFC3339Nano, dp.Timestamp) // Use RFC3339Nano for nanosecond precision
			if err != nil {
				log.Printf("Invalid timestamp format from topic %s: %v\n", msg.Topic(), err)
				return
			}

			receivedMessageCount += 1

			// Write to InfluxDB
			p := influxdb2.NewPointWithMeasurement("bydmate_" + dp.Vin)

			// log.Printf("Writing data point to InfluxDB: %s", dp)
			p.SetTime(t)
			// p.AddTag("vin", dp.Vin)
			p.AddTag("category", dp.Category)
			p.AddField(dp.Key, dp.Value)

			if err := writeAPI.WritePoint(context.Background(), p); err != nil {
				fmt.Printf("Error writing to InfluxDB: %v\n", err)
				writeFailedCount += 1
			} else {
				fmt.Println("Successfully wrote to InfluxDB")
				writeSuccessCount += 1
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

	// Subscribe to topics
	if token := client.Subscribe(mqttDataTopic, 1, nil); token.Wait() && token.Error() != nil {
		log.Fatalf("Failed to subscribe to topic %s: %v", mqttDataTopic, token.Error())
	}
	fmt.Printf("Subscribed to topic: %s\n", mqttDataTopic)

	if token := client.Subscribe(mqttZDataTopic, 1, nil); token.Wait() && token.Error() != nil {
		log.Fatalf("Failed to subscribe to topic %s: %v", mqttZDataTopic, token.Error())
	}
	fmt.Printf("Subscribed to topic: %s\n", mqttZDataTopic)

	// Keep the application running
	select {}
}