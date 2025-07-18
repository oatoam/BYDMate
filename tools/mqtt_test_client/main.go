package main

import (
	"encoding/json"
	"flag"
	"log"
	"time"
	"strings"

	mqtt "github.com/eclipse/paho.mqtt.golang"
)

var brokerURI = flag.String("broker", "tcp://localhost:1883", "MQTT broker URI, e.g., tcp://localhost:1883") // MQTT broker URI, 可以通过命令行参数 -broker 指定

func main() {
	flag.Parse()

	// Load data points from file
	dataPoints, err := loadDataFromFile("../../log/timedata.txt")
	if err != nil {
		log.Fatalf("Error loading data: %v", err)
	}
	if len(dataPoints) == 0 {
		log.Fatal("No data points loaded from data.txt")
	}

	VIN := "" // Default VIN, can be overridden by data points
	clientID := "byd_mate_test_client"
	dataTopic    := "bydmate/data"
	onlineTopic  := "bydmate/online"

	for _, dp := range dataPoints {
		if strings.Contains(dp.Key, "VIN") {
			VIN = dp.Value.(string) // Store VIN from the first data point
			log.Printf("Detected VIN: %s", VIN)
		}

		if VIN != "" {
			break // Skip publishing if VIN is not set
		}
	}

	clientID = strings.Join([]string{clientID, VIN}, "_") // Append VIN to client ID for uniqueness
	log.Printf("Using client ID: %s", clientID)
	// topic = strings.Join([]string{topic, VIN}, "/") // Append VIN to topic for uniqueness
	// log.Printf("Using topic: %s", topic)

	// MQTT Client Options
	opts := mqtt.NewClientOptions().AddBroker(*brokerURI).SetClientID(clientID)
	opts.SetKeepAlive(60 * time.Second)
	opts.SetPingTimeout(1 * time.Second)
	opts.SetCleanSession(true)

	// Connect to MQTT Broker
	client := mqtt.NewClient(opts)
	if token := client.Connect(); token.Wait() && token.Error() != nil {
		log.Fatalf("Failed to connect to MQTT broker: %v", token.Error())
	}
	log.Printf("Connected to MQTT broker: %s\n", *brokerURI)

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

	if token := client.Publish(onlineTopic, 1, false, VIN); token.Wait() && token.Error() != nil {
		log.Printf("Failed to publish message: %v\n", token.Error())
	} else {
		log.Printf("Published message to topic '%s': %s\n", onlineTopic, VIN)
	}

	// Publish data points sequentially
	for _, dp := range dataPoints {

		go func() {
			dataPoint := DataPoint{
				Vin:      VIN, // Use the detected VIN
				Category: dp.Category,
				Key:      dp.Key,
				Value:    dp.Value,
				Timestamp: dp.Timestamp.Format(time.RFC3339Nano), // Use current time for consistency
			}

			if strings.Contains(dp.Key, "VIN") {
				VIN = dp.Value.(string) // Store VIN from the first data point
				log.Printf("Detected VIN: %s", VIN)
			}

			if strings.Contains(dp.Category, "instrument") {
				return
			}

			if VIN == "" {
				return // Skip publishing if VIN is not set
			}

			payload, err := json.Marshal(dataPoint)
			if err != nil {
				log.Printf("Error marshalling data point: %v\n", err)
				return
			}

			if token := client.Publish(dataTopic, 1, false, payload); token.Wait() && token.Error() != nil {
				log.Printf("Failed to publish message: %v\n", token.Error())
			} else {
				log.Printf("Published message to topic '%s': %s\n", dataTopic, string(payload))
			}
		}()

		time.Sleep(200 * time.Microsecond) // Small delay between messages
	}

	log.Println("Finished publishing all data points from data.txt")
	// Keep the client connected for a bit to ensure all messages are sent
	time.Sleep(1 * time.Second) // Small delay between messages
}