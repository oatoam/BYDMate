package main

import (
	"encoding/json"
	"log"
	"time"

	mqtt "github.com/eclipse/paho.mqtt.golang"
)

const (
	brokerURI = "tcp://localhost:1883" // EMQX broker address
	clientID  = "byd_mate_test_client"
	topic     = "byd/data"
)

func main() {
	// Load data points from file
	dataPoints, err := loadDataFromFile("../../log/data.txt")
	if err != nil {
		log.Fatalf("Error loading data: %v", err)
	}
	if len(dataPoints) == 0 {
		log.Fatal("No data points loaded from data.txt")
	}

	// MQTT Client Options
	opts := mqtt.NewClientOptions().AddBroker(brokerURI).SetClientID(clientID)
	opts.SetKeepAlive(60 * time.Second)
	opts.SetPingTimeout(1 * time.Second)
	opts.SetCleanSession(true)

	// Connect to MQTT Broker
	client := mqtt.NewClient(opts)
	if token := client.Connect(); token.Wait() && token.Error() != nil {
		log.Fatalf("Failed to connect to MQTT broker: %v", token.Error())
	}
	log.Printf("Connected to MQTT broker: %s\n", brokerURI)

	// Add a small delay to ensure connection is fully established before publishing trigger
	time.Sleep(1 * time.Second)

	// Publish a trigger message once at the beginning
	triggerTopic := "byd/analysis_trigger"
	triggerMessage := "trigger"
	if token := client.Publish(triggerTopic, 1, false, triggerMessage); token.Wait() && token.Error() != nil {
		log.Printf("Failed to publish trigger message: %v\n", token.Error())
	} else {
		log.Printf("Published trigger message to topic '%s': %s\n", triggerTopic, triggerMessage)
	}

	// Publish data points sequentially
	for _, dp := range dataPoints {
		dataPoint := DataPoint{
			Category: dp.Category,
			Key:      dp.Key,
			Value:    dp.Value,
			Timestamp: time.Now().Format(time.RFC3339Nano), // Use current time for consistency
		}

		payload, err := json.Marshal(dataPoint)
		if err != nil {
			log.Printf("Error marshalling data point: %v\n", err)
			continue
		}

		if token := client.Publish(topic, 1, false, payload); token.Wait() && token.Error() != nil {
			log.Printf("Failed to publish message: %v\n", token.Error())
		} else {
			log.Printf("Published message to topic '%s': %s\n", topic, string(payload))
		}
		time.Sleep(100 * time.Millisecond) // Small delay between messages
	}

	log.Println("Finished publishing all data points from data.txt")
	// Keep the client connected for a bit to ensure all messages are sent
	time.Sleep(5 * time.Second)
}