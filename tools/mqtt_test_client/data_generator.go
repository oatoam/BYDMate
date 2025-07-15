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

		parts := strings.Fields(line)

		// Try to parse as timestamp line
		if len(parts) >= 2 && len(parts[0]) == 5 && parts[0][2] == '-' && len(parts[1]) == 8 && parts[1][2] == ':' && parts[1][5] == ':' {
			// This is a log line, skip it
			continue
		}

		// Try to parse as category key value line
		if len(parts) >= 2 {
			category := parts[0]
			key := parts[1]
			valueStr := strings.Join(parts[2:], " ")
			
			var value interface{}
			// Attempt to parse value as float64
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
				Timestamp: time.Now(), // Placeholder, actual timestamp will be from log line or current time
				Category:  category,
				Key:       key,
				Value:     value,
			})
		} else {
			fmt.Printf("Skipping malformed data line %d: %s\n", lineNum, line)
		}
	}

	if err := scanner.Err(); err != nil {
		return nil, fmt.Errorf("error reading file: %w", err)
	}

	return dataPoints, nil
}
