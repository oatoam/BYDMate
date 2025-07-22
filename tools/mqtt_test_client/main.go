package main

import (
	"encoding/json"
	"flag"
	"log"
	"time"
	"strings"
	"strconv"

	"github.com/klauspost/compress/zstd"

	mqtt "github.com/eclipse/paho.mqtt.golang"
)

var brokerURI = flag.String("broker", "tcp://localhost:1883", "MQTT broker URI, e.g., tcp://localhost:1883") // MQTT broker URI, 可以通过命令行参数 -broker 指定
var pressureTest = flag.Bool("pressure", false, "Enable pressure test mode, which publishes data points at a high frequency") // 是否启用压力测试模式
var interval = flag.Int("interval", 1000, "Interval in milliseconds between data point publications") // 发布数据点的间隔时间，单位为毫秒
var number = flag.Int("number", 100, "Number of data points to publish") // 发布的数据点数量
var compressData = flag.Bool("compress", false, "Compress data using zstd before publishing") // 是否压缩数据

// Create a writer that caches compressors.
// For this operation type we supply a nil Reader.
var encoder, _ = zstd.NewWriter(nil)

// Compress a buffer. 
// If you have a destination buffer, the allocation in the call can also be eliminated.
func Compress(src []byte) []byte {
    return encoder.EncodeAll(src, make([]byte, 0, len(src)))
} 

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
	zdataTopic   := "bydmate/zdata"
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

	sent := 0
	sentError := 0
	sentSuccess := 0

	if pressureTest != nil && *pressureTest {
		log.Println("Pressure test mode enabled, publishing data points at high frequency")
		payload := make([]byte, 64*1024) // Create a 128 KB payload
		for i := range payload {
			payload[i] = byte('A' + i%26) // Fill with random alphabetic characters
		}
		id := 0
		for {
			payloadData := DataPoint{
				Vin:       VIN,
				Category:  "pressure_test",
				Key:       "test_data",
				Value:     strconv.Itoa(id) + "---" + string(payload), // Convert id to string
				Timestamp: time.Now().Format(time.RFC3339Nano),       // Use current time for consistency
			}

			payload, err := json.Marshal(payloadData)
			if err != nil {
				log.Printf("Error marshalling payload: %v\n", err)
				continue
			}

			if (sent >= *number) {
				break
			}

			go func() {
				// log.Printf("before Publish %d sent %d err %d succ %d\n", id, sent, sentError, sentSuccess)
				sent += 1
				
				topicToPublish := dataTopic
				if *compressData {
					payload = Compress(payload)
					topicToPublish = zdataTopic
				}

				if token := client.Publish(topicToPublish, 1, false, payload); token.Wait() && token.Error() != nil {
					sentError += 1
					log.Printf("Failed to publish message: %v\n", token.Error())
				} else {
					sentSuccess += 1
					log.Printf("Published JSON payload to topic '%s' id '%d' (compressed: %t)\n", topicToPublish, id, *compressData)
					log.Printf("sent %d err %d succ %d\n", sent, sentError, sentSuccess)
				}
			}()

			id += 1

			time.Sleep(time.Duration(*interval) * time.Millisecond)// Small delay between messages
		}

		for {
			if sentSuccess + sentError >= sent {
				break
			}
			log.Printf("Waiting for all messages to be sent: %d sent, %d errors, %d successes", sent, sentError, sentSuccess)
			time.Sleep(500 * time.Millisecond) // Small delay between messages
		}
		log.Printf("Pressure test ongoing: sent %d messages, %d errors, %d successes", sent, sentError, sentSuccess)

		return
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

			topicToPublish := dataTopic
			if *compressData {
				payload = Compress(payload)
				topicToPublish = zdataTopic
			}

			if token := client.Publish(topicToPublish, 1, false, payload); token.Wait() && token.Error() != nil {
				log.Printf("Failed to publish message: %v\n", token.Error())
			} else {
				log.Printf("Published message to topic '%s': %s (compressed: %t)\n", topicToPublish, string(payload), *compressData)
			}
		}()

		time.Sleep(time.Duration(*interval) * time.Millisecond)// Small delay between messages
	}
	log.Println("Finished publishing all data points from data.txt")
	// Keep the client connected for a bit to ensure all messages are sent



	time.Sleep(5 * time.Second) // Small delay between messages
}