package main

import (
	"bufio"
	"fmt"
	"os"
	"strconv"
	"strings"
	"time"
)

// DataPoint represents a single data point with category, key, and value.
type DataPoint struct {
	Vin      string      `json:"vin"`
	Category string      `json:"category"`
	Key      string      `json:"key"`
	Value    interface{} `json:"value"`
	Timestamp string     `json:"timestamp"`
}

// LogDataPoint represents a single data point read from log/data.txt
type LogDataPoint struct {
	Timestamp time.Time
	Category  string
	Key       string
	Value     interface{}
}

// loadDataFromFile reads the log/data.txt file and parses all data points.
func loadDataFromFile(filePath string) ([]LogDataPoint, error) {
	file, err := os.Open(filePath)
	if err != nil {
		return nil, fmt.Errorf("failed to open file: %w", err)
	}
	defer file.Close()

	var dataPoints []LogDataPoint
	scanner := bufio.NewScanner(file)
	lineNum := 0
	for scanner.Scan() {
		lineNum++
		line := strings.TrimSpace(scanner.Text())

		if line == "" {
			continue
		}

		// Expected format: "MM-DD HH:MM:SS.ms >> type // Key = Value"
		// Example: "07-06 12:15:38.575 >> ac // Temperature = 26"

		// Split by " >> " to separate timestamp and the rest
		timestampAndRest := strings.SplitN(line, " >> ", 2)
		if len(timestampAndRest) != 2 {
			fmt.Printf("Skipping malformed line (missing ' >> ' separator) %d: %s\n", lineNum, line)
			continue
		}

		// Parse timestamp
		timestampStr := timestampAndRest[0]
		// Assuming the year is current year for parsing
		currentYear := time.Now().Year()
		// Format: "07-06 12:15:38.575" -> "YYYY-MM-DD HH:MM:SS.ms"
		parsedTime, err := time.Parse(fmt.Sprintf("01-02 15:04:05.000"), timestampStr)
		if err != nil {
			fmt.Printf("Skipping malformed line (timestamp parse error) %d: %s, Error: %v\n", lineNum, line, err)
			continue
		}
		// Set the year to the current year
		parsedTime = parsedTime.AddDate(currentYear - parsedTime.Year(), 0, 0)


		// Split the rest by " // " to separate type and key-value pair
		typeAndKeyValue := strings.SplitN(timestampAndRest[1], " // ", 2)
		if len(typeAndKeyValue) != 2 {
			fmt.Printf("Skipping malformed line (missing ' // ' separator) %d: %s\n", lineNum, line)
			continue
		}

		dataType := strings.TrimSpace(typeAndKeyValue[0]) // This is the 'type' (e.g., "ac")

		// Split key-value pair by " = "
		keyAndValue := strings.SplitN(typeAndKeyValue[1], " = ", 2)
		if len(keyAndValue) != 2 {
			fmt.Printf("Skipping malformed line (missing ' = ' separator) %d: %s\n", lineNum, line)
			continue
		}

		key := strings.TrimSpace(keyAndValue[0])
		valueStr := strings.TrimSpace(keyAndValue[1])

		var value interface{}
		// Attempt to parse value as float64, then bool, then int, otherwise keep as string
		if f, err := strconv.ParseFloat(valueStr, 64); err == nil {
			value = f
		} else if b, err := strconv.ParseBool(valueStr); err == nil {
			value = b
		} else if i, err := strconv.ParseInt(valueStr, 10, 64); err == nil {
			value = i
		} else {
			value = valueStr
		}

		dataPoints = append(dataPoints, LogDataPoint{
			Timestamp: parsedTime,
			Category:  dataType, // Using Category for 'type'
			Key:       key,
			Value:     value,
		})
	}

	if err := scanner.Err(); err != nil {
		return nil, fmt.Errorf("error reading file: %w", err)
	}

	return dataPoints, nil
}
