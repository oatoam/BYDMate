package main

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"strconv"
	"time"

	mqtt "github.com/eclipse/paho.mqtt.golang"
	influxdb2 "github.com/influxdata/influxdb-client-go/v2"
	"github.com/influxdata/influxdb-client-go/v2/api"
	"github.com/jackc/pgx/v4"
)

const (
	mqttBroker = "tcp://emqx:1883"
	mqttClientID = "data_analyzer"
	mqttTopic = "byd/analysis_trigger" // 假设有一个主题用于触发分析

	influxDBURL = "http://influxdb:8086"
	influxDBToken = "my-super-secret-token"
	influxDBOrg = "byd_org"
	influxDBBucket = "byd_bucket"

	pgHost = "postgres"
	pgPort = "5432"
	pgUser = "user"
	pgPassword = "password"
	pgDBName = "byd_mate"

	apiPort = ":8081"
)

var useMockData = true // Global flag to control mock data

func main() {
	log.Println("Data Analyzer service starting...") // Added for debugging
	// Initialize InfluxDB Client
	influxClient := influxdb2.NewClient(influxDBURL, influxDBToken)
	defer influxClient.Close()
	queryAPI := influxClient.QueryAPI(influxDBOrg)

	// Initialize PostgreSQL Client with retry logic
	pgConnStr := fmt.Sprintf("host=%s port=%s user=%s password=%s dbname=%s sslmode=disable",
		pgHost, pgPort, pgUser, pgPassword, pgDBName)
	var pgConn *pgx.Conn
	var err error
	for i := 0; i < 10; i++ { // Retry up to 10 times
		pgConn, err = pgx.Connect(context.Background(), pgConnStr)
		if err == nil {
			log.Println("Successfully connected to PostgreSQL")
			break
		}
		log.Printf("Unable to connect to PostgreSQL: %v. Retrying in 5 seconds...", err)
		time.Sleep(5 * time.Second)
	}
	if err != nil {
		log.Fatalf("Failed to connect to PostgreSQL after multiple retries: %v\n", err)
	}
	defer pgConn.Close(context.Background())

	// MQTT Client Options
	opts := mqtt.NewClientOptions().AddBroker(mqttBroker).SetClientID(mqttClientID)
	opts.SetKeepAlive(60 * time.Second)
	opts.SetPingTimeout(1 * time.Second)
	opts.SetCleanSession(true)

	// Message Handler for analysis trigger
	messagePubHandler := func(client mqtt.Client, msg mqtt.Message) {
		fmt.Printf("Analysis trigger received: %s from topic: %s\n", msg.Payload(), msg.Topic())
		// In a real scenario, parse the payload to extract event details
		// For now, let's assume the payload is a simple string that can be used as a trigger.
		triggerEvent := string(msg.Payload())
		log.Printf("Triggering data analysis for event: %s...\n", triggerEvent)
		performDataAnalysis(queryAPI, pgConn, triggerEvent)
	}

	opts.SetDefaultPublishHandler(messagePubHandler)

	// Connect to MQTT Broker
	mqttClient := mqtt.NewClient(opts)
	if token := mqttClient.Connect(); token.Wait() && token.Error() != nil {
		log.Fatalf("Failed to connect to MQTT broker: %v", token.Error())
	}
	fmt.Println("Connected to MQTT broker for analysis triggers")

	// Subscribe to analysis trigger topic
	if token := mqttClient.Subscribe(mqttTopic, 1, nil); token.Wait() && token.Error() != nil {
		log.Fatalf("Failed to subscribe to analysis trigger topic: %v", token.Error())
	}
	fmt.Printf("Subscribed to analysis trigger topic: %s\n", mqttTopic)

	// HTTP API Server
	http.HandleFunc("/analyze", func(w http.ResponseWriter, r *http.Request) {
		fmt.Fprintf(w, "Data analysis API endpoint. Triggered analysis.\n")
		performDataAnalysis(queryAPI, pgConn, "manual_trigger") // Trigger analysis via API call
	})

	http.HandleFunc("/api/vehicle/status", getVehicleStatusHandler(queryAPI)) // Changed pgConn to queryAPI
	http.HandleFunc("/api/trips", getTripsHandler(pgConn))
	http.HandleFunc("/api/trips/", getTripDetailsHandler(queryAPI, pgConn)) // Note the trailing slash for path parameters
	http.HandleFunc("/api/charges", getChargesHandler(pgConn))
	http.HandleFunc("/api/charges/", getChargeDetailsHandler(queryAPI, pgConn)) // Note the trailing slash for path parameters
	http.HandleFunc("/api/driving-stats", getDrivingStatsHandler(pgConn))
	http.HandleFunc("/api/location-stats", getLocationStatsHandler(pgConn))
	http.HandleFunc("/api/data-features", getDataFeaturesHandler(pgConn))

	fmt.Printf("Data Analyzer API server starting on port %s\n", apiPort)
	log.Fatal(http.ListenAndServe(apiPort, nil))
}

// performDataAnalysis function will query InfluxDB, perform analysis, and store results in PostgreSQL
func performDataAnalysis(queryAPI api.QueryAPI, pgConn *pgx.Conn, event string) {
	log.Printf("Performing data analysis for event: %s...\n", event)

	// Define time range for analysis (e.g., last 1 hour)
	// In a real scenario, the time range might be derived from the event payload.
	timeRange := "-1h"

	// Query data from InfluxDB
	// This query fetches all mqtt_data for the given time range.
	// In a real scenario, you might filter by specific tags (e.g., vehicle_id)
	// based on the event.
	query := fmt.Sprintf(`
from(bucket: "%s")
	  |> range(start: %s)
	  |> filter(fn: (r) => r["_measurement"] == "mqtt_data")`, influxDBBucket, timeRange)

	results, err := queryAPI.Query(context.Background(), query)
	if err != nil {
		log.Printf("InfluxDB query error: %v\n", err)
		return
	}

	// Data structures to hold analysis results
	var totalSpeed float64
	var speedCount int
	var totalPower float64
	var powerCount int

	// Process results
	for results.Next() {
		record := results.Record()
		// The _value field now contains the raw JSON string
		jsonStr, ok := record.Value().(string)
		if !ok {
			log.Printf("Error: _value is not a string: %v\n", record.Value())
			continue
		}

		var payload struct {
			Key   string      `json:"key"`
			Value interface{} `json:"value"`
		}

		if err := json.Unmarshal([]byte(jsonStr), &payload); err != nil {
			log.Printf("Error unmarshaling JSON: %v, JSON string: %s\n", err, jsonStr)
			continue
		}

		if payload.Key == "Speed" {
			if s, ok := payload.Value.(float64); ok {
				totalSpeed += s
				speedCount++
			} else {
				log.Printf("Warning: Speed value is not float64, skipping. Value: %v\n", payload.Value)
			}
		} else if payload.Key == "ChargingPower" {
			if p, ok := payload.Value.(float64); ok {
				totalPower += p
				powerCount++
			} else {
				log.Printf("Warning: Power value is not float64, skipping. Value: %v\n", payload.Value)
			}
		}
		// fmt.Printf("Processing InfluxDB Record: %v\n", record.Values()) // 暂时注释掉，减少日志量
	}

	if results.Err() != nil {
		log.Printf("InfluxDB query parsing error: %v\n", results.Err())
		return
	}

	// Perform analysis
	avgSpeed := 0.0
	if speedCount > 0 {
		avgSpeed = totalSpeed / float64(speedCount)
	}

	avgPower := 0.0
	if powerCount > 0 {
		avgPower = totalPower / float64(powerCount)
	}

	log.Printf("Analysis Results for event '%s': Average Speed = %.2f, Average Power = %.2f\n",
		event, avgSpeed, avgPower)

	// Store analysis results in PostgreSQL
	// This part needs to be updated to reflect the new table structure and data.
	// For now, it's a placeholder.
	_, err = pgConn.Exec(context.Background(), `
		INSERT INTO trips (
			id, vehicle_id, start_time, end_time, start_latitude, end_latitude,
			start_longitude, end_longitude, total_mileage, total_fuel_consumption,
			total_electric_consumption, avg_speed, max_speed, avg_power, created_at
		)
		VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15)
	`,
		"mock-trip-id", "mock-vehicle-id", time.Now(), time.Now(), 0.0, 0.0,
		0.0, 0.0, 0.0, 0.0,
		0.0, avgSpeed, 0.0, avgPower, time.Now(),
	)
	if err != nil {
		log.Printf("PostgreSQL insert error: %v\n", err)
	} else {
		fmt.Println("Successfully inserted analysis results into PostgreSQL")
	}

	log.Printf("Data analysis for event '%s' completed.\n", event)
}


// getVehicleStatusHandler returns the latest vehicle status
func getVehicleStatusHandler(queryAPI api.QueryAPI) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		log.Println("Received request for /api/vehicle/status")
		setCORSHeaders(w, r)
		if r.Method == "OPTIONS" { return }
		if r.Method != "GET" { http.Error(w, "Method not allowed", http.StatusMethodNotAllowed); return }

		if useMockData {
			status := GenerateMockVehicleStatus()
			w.Header().Set("Content-Type", "application/json")
			if err := json.NewEncoder(w).Encode(status); err != nil {
				log.Printf("Error encoding JSON response: %v\n", err)
				http.Error(w, "Internal server error", http.StatusInternalServerError)
			}
			return
		}

		// Query the latest vehicle telemetry from InfluxDB
		query := fmt.Sprintf(`
			from(bucket: "%s")
			|> range(start: -1h) // Look back 1 hour for the latest data
			|> filter(fn: (r) => r._measurement == "vehicle_telemetry")
			|> last() // Get the last record for each series
		`, influxDBBucket)

		results, err := queryAPI.Query(context.Background(), query)
		if err != nil {
			log.Printf("InfluxDB query error: %v\n", err)
			http.Error(w, "Internal server error", http.StatusInternalServerError)
			return
		}

		var status VehicleStatus
		found := false
		for results.Next() {
			record := results.Record()
			// Assuming the InfluxDB query returns fields that directly map to VehicleStatus
			// You might need to adjust this based on your actual InfluxDB data structure
			status.VehicleID = record.ValueByKey("vehicle_id").(string)
			status.Timestamp = record.Time()
			status.Latitude = record.ValueByKey("latitude").(float64)
			status.Longitude = record.ValueByKey("longitude").(float64)
			status.Speed = record.ValueByKey("speed").(float64)
			status.Mileage = record.ValueByKey("mileage").(float64)
			status.FuelLevel = record.ValueByKey("fuel_level").(float64)
			status.SOC = record.ValueByKey("soc").(float64)
			status.BatteryTemp = record.ValueByKey("battery_temp").(float64)
			status.ChargeStatus = record.ValueByKey("charge_status").(string)
			status.Power = record.ValueByKey("power").(float64)
			found = true
		}

		if results.Err() != nil {
			log.Printf("InfluxDB query parsing error: %v\n", results.Err())
			http.Error(w, "Internal server error", http.StatusInternalServerError)
			return
		}

		if !found {
			http.Error(w, "No vehicle status data found", http.StatusNotFound)
			return
		}

		w.Header().Set("Content-Type", "application/json")
		if err := json.NewEncoder(w).Encode(status); err != nil {
			log.Printf("Error encoding JSON response: %v\n", err)
			http.Error(w, "Internal server error", http.StatusInternalServerError)
		}
	}
}

// getTripsHandler returns an HTTP handler that fetches trip data from PostgreSQL
func getTripsHandler(pgConn *pgx.Conn) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		log.Println("Received request for /api/trips")
		setCORSHeaders(w, r)
		if r.Method == "OPTIONS" { return }
		if r.Method != "GET" { http.Error(w, "Method not allowed", http.StatusMethodNotAllowed); return }

		query := r.URL.Query()
		pageStr := query.Get("page")
		pageSizeStr := query.Get("page_size")
		sortBy := query.Get("sort_by")
		sortOrder := query.Get("sort_order")

		page, err := strconv.Atoi(pageStr)
		if err != nil || page <= 0 { page = 1 }
		pageSize, err := strconv.Atoi(pageSizeStr)
		if err != nil || pageSize <= 0 { pageSize = 10 }
		offset := (page - 1) * pageSize

		if sortBy == "" { sortBy = "start_time" }
		if sortOrder == "" { sortOrder = "DESC" }

		if useMockData {
			resp := GenerateMockTrips(page, pageSize)
			w.Header().Set("Content-Type", "application/json")
			if err := json.NewEncoder(w).Encode(resp); err != nil {
				log.Printf("Error encoding JSON response: %v\n", err)
				http.Error(w, "Internal server error", http.StatusInternalServerError)
			}
			return
		}

		// Count total trips
		var total int
		err = pgConn.QueryRow(context.Background(), "SELECT COUNT(*) FROM trips").Scan(&total)
		if err != nil {
			log.Printf("Error counting trips: %v\n", err)
			http.Error(w, "Internal server error", http.StatusInternalServerError)
			return
		}

		sqlQuery := fmt.Sprintf(`SELECT
			id, vehicle_id, start_time, end_time, start_latitude, end_latitude,
			start_longitude, end_longitude, total_mileage, total_fuel_consumption,
			total_electric_consumption, avg_speed, max_speed, avg_power, created_at
			FROM trips
			ORDER BY %s %s
			OFFSET $1 LIMIT $2`, sortBy, sortOrder)

		rows, err := pgConn.Query(context.Background(), sqlQuery, offset, pageSize)
		if err != nil {
			log.Printf("Error querying PostgreSQL: %v\n", err)
			http.Error(w, "Internal server error", http.StatusInternalServerError)
			return
		}
		defer rows.Close()

		var trips []Trip
		for rows.Next() {
			var trip Trip
			err := rows.Scan(
				&trip.ID,
				&trip.VehicleID,
				&trip.StartTime,
				&trip.EndTime,
				&trip.StartLatitude,
				&trip.StartLongitude,
				&trip.EndLatitude,
				&trip.EndLongitude,
				&trip.TotalMileage,
				&trip.TotalFuelConsumption,
				&trip.TotalElectricConsumption,
				&trip.AvgSpeed,
				&trip.MaxSpeed,
				&trip.AvgPower,
				&trip.CreatedAt,
			)
			if err != nil {
				log.Printf("Error scanning trip row: %v\n", err)
				http.Error(w, "Internal server error", http.StatusInternalServerError)
				return
			}
			trips = append(trips, trip)
		}

		resp := ListResponse{
			Total:    total,
			Page:     page,
			PageSize: pageSize,
			Data:     trips,
		}

		w.Header().Set("Content-Type", "application/json")
		if err := json.NewEncoder(w).Encode(resp); err != nil {
			log.Printf("Error encoding JSON response: %v\n", err)
			http.Error(w, "Internal server error", http.StatusInternalServerError)
			return
		}
	}
}

// getTripDetailsHandler fetches detailed trip data from InfluxDB
func getTripDetailsHandler(queryAPI api.QueryAPI, pgConn *pgx.Conn) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		log.Println("Received request for /api/trips/{trip_id}/details")
		setCORSHeaders(w, r)
		if r.Method == "OPTIONS" { return }
		if r.Method != "GET" { http.Error(w, "Method not allowed", http.StatusMethodNotAllowed); return }

		tripID := r.URL.Path[len("/api/trips/") : len(r.URL.Path)-len("/details")]
		if tripID == "" {
			http.Error(w, "Trip ID is required", http.StatusBadRequest)
			return
		}

		if useMockData {
			mockDetails := GenerateMockTripDetails(tripID)
			w.Header().Set("Content-Type", "application/json")
			if err := json.NewEncoder(w).Encode(mockDetails); err != nil {
				log.Printf("Error encoding JSON response: %v\n", err)
				http.Error(w, "Internal server error", http.StatusInternalServerError)
			}
			return
		}

		// 1. Get trip start and end time from PostgreSQL
		var startTime, endTime time.Time
		err := pgConn.QueryRow(context.Background(),
			"SELECT start_time, end_time FROM trips WHERE id = $1", tripID).Scan(&startTime, &endTime)
		if err != nil {
			if err == pgx.ErrNoRows {
				http.Error(w, "Trip not found", http.StatusNotFound)
			} else {
				log.Printf("Error querying PostgreSQL for trip details: %v\n", err)
				http.Error(w, "Internal server error", http.StatusInternalServerError)
			}
			return
		}

		// 2. Query InfluxDB for trip_details measurement within the trip's time range
		query := fmt.Sprintf(`
			from(bucket: "%s")
			|> range(start: %s, stop: %s)
			|> filter(fn: (r) => r._measurement == "trip_details" and r.trip_id == "%s")
			|> sort(columns: ["_time"])
		`, influxDBBucket, startTime.Format(time.RFC3339), endTime.Format(time.RFC3339), tripID)

		results, err := queryAPI.Query(context.Background(), query)
		if err != nil {
			log.Printf("InfluxDB query error for trip details: %v\n", err)
			http.Error(w, "Internal server error", http.StatusInternalServerError)
			return
		}

		var details []TripDetail
		for results.Next() {
			record := results.Record()
			detail := TripDetail{
				Timestamp:      record.Time(),
				DrivePower:     record.ValueByKey("drive_power").(float64),
				DischargePower: record.ValueByKey("discharge_power").(float64),
				Speed:          record.ValueByKey("speed").(float64),
				Altitude:       record.ValueByKey("altitude").(float64),
				SOC:            record.ValueByKey("soc").(float64),
				FuelLevel:      record.ValueByKey("fuel_level").(float64),
				Latitude:       record.ValueByKey("latitude").(float64),
				Longitude:      record.ValueByKey("longitude").(float64),
			}
			details = append(details, detail)
		}

		if results.Err() != nil {
			log.Printf("InfluxDB query parsing error for trip details: %v\n", results.Err())
			http.Error(w, "Internal server error", http.StatusInternalServerError)
			return
		}

		w.Header().Set("Content-Type", "application/json")
		if err := json.NewEncoder(w).Encode(details); err != nil {
			log.Printf("Error encoding JSON response for trip details: %v\n", err)
			http.Error(w, "Internal server error", http.StatusInternalServerError)
			return
		}
	}
}

// getChargesHandler returns an HTTP handler that fetches charge data from PostgreSQL
func getChargesHandler(pgConn *pgx.Conn) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		log.Println("DEBUG: Entering getChargesHandler for /api/charges")
		log.Println("Received request for /api/charges")
		setCORSHeaders(w, r)
		if r.Method == "OPTIONS" { return }
		if r.Method != "GET" { http.Error(w, "Method not allowed", http.StatusMethodNotAllowed); return }

		query := r.URL.Query()
		pageStr := query.Get("page")
		pageSizeStr := query.Get("page_size")
		sortBy := query.Get("sort_by")
		sortOrder := query.Get("sort_order")

		page, err := strconv.Atoi(pageStr)
		if err != nil || page <= 0 { page = 1 }
		pageSize, err := strconv.Atoi(pageSizeStr)
		if err != nil || pageSize <= 0 { pageSize = 10 }
		offset := (page - 1) * pageSize

		if sortBy == "" { sortBy = "start_time" }
		if sortOrder == "" { sortOrder = "DESC" }

		if useMockData {
			resp := GenerateMockCharges(page, pageSize)
			w.Header().Set("Content-Type", "application/json")
			if err := json.NewEncoder(w).Encode(resp); err != nil {
				log.Printf("Error encoding JSON response: %v\n", err)
				http.Error(w, "Internal server error", http.StatusInternalServerError)
			}
			return
		}

		var total int
		err = pgConn.QueryRow(context.Background(), "SELECT COUNT(*) FROM charges").Scan(&total)
		if err != nil {
			log.Printf("Error counting charges: %v\n", err)
			http.Error(w, "Internal server error", http.StatusInternalServerError)
			return
		}

		sqlQuery := fmt.Sprintf(`SELECT
			id, vehicle_id, start_time, end_time, charge_latitude, charge_longitude,
			start_soc, end_soc, charged_kwh, charge_type, created_at
			FROM charges
			ORDER BY %s %s
			OFFSET $1 LIMIT $2`, sortBy, sortOrder)

		rows, err := pgConn.Query(context.Background(), sqlQuery, offset, pageSize)
		if err != nil {
			log.Printf("Error querying PostgreSQL: %v\n", err)
			http.Error(w, "Internal server error", http.StatusInternalServerError)
			return
		}
		defer rows.Close()

		var charges []Charge
		for rows.Next() {
			var charge Charge
			err := rows.Scan(
				&charge.ID,
				&charge.VehicleID,
				&charge.StartTime,
				&charge.EndTime,
				&charge.ChargeLatitude,
				&charge.ChargeLongitude,
				&charge.StartSOC,
				&charge.EndSOC,
				&charge.ChargedKWH,
				&charge.ChargeType,
				&charge.CreatedAt,
			)
			if err != nil {
				log.Printf("Error scanning charge row: %v\n", err)
				http.Error(w, "Internal server error", http.StatusInternalServerError)
				return
			}
			charges = append(charges, charge)
		}

		resp := ListResponse{
			Total:    total,
			Page:     page,
			PageSize: pageSize,
			Data:     charges,
		}

		w.Header().Set("Content-Type", "application/json")
		if err := json.NewEncoder(w).Encode(resp); err != nil {
			log.Printf("Error encoding JSON response: %v\n", err)
			http.Error(w, "Internal server error", http.StatusInternalServerError)
			return
		}
	}
}

// getChargeDetailsHandler fetches detailed charge data from InfluxDB
func getChargeDetailsHandler(queryAPI api.QueryAPI, pgConn *pgx.Conn) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		log.Println("Received request for /api/charges/{charge_id}/details")
		setCORSHeaders(w, r)
		if r.Method == "OPTIONS" { return }
		if r.Method != "GET" { http.Error(w, "Method not allowed", http.StatusMethodNotAllowed); return }

		chargeID := r.URL.Path[len("/api/charges/") : len(r.URL.Path)-len("/details")]
		if chargeID == "" {
			http.Error(w, "Charge ID is required", http.StatusBadRequest)
			return
		}

		if useMockData {
			mockDetails := GenerateMockChargeDetails(chargeID)
			w.Header().Set("Content-Type", "application/json")
			if err := json.NewEncoder(w).Encode(mockDetails); err != nil {
				log.Printf("Error encoding JSON response: %v\n", err)
				http.Error(w, "Internal server error", http.StatusInternalServerError)
			}
			return
		}

		// 1. Get charge start and end time from PostgreSQL
		var startTime, endTime time.Time
		err := pgConn.QueryRow(context.Background(),
			"SELECT start_time, end_time FROM charges WHERE id = $1", chargeID).Scan(&startTime, &endTime)
		if err != nil {
			if err == pgx.ErrNoRows {
				http.Error(w, "Charge not found", http.StatusNotFound)
			} else {
				log.Printf("Error querying PostgreSQL for charge details: %v\n", err)
				http.Error(w, "Internal server error", http.StatusInternalServerError)
			}
			return
		}

		// 2. Query InfluxDB for charge_details measurement within the charge's time range
		query := fmt.Sprintf(`
			from(bucket: "%s")
			|> range(start: %s, stop: %s)
			|> filter(fn: (r) => r._measurement == "charge_details" and r.charge_id == "%s")
			|> sort(columns: ["_time"])
		`, influxDBBucket, startTime.Format(time.RFC3339), endTime.Format(time.RFC3339), chargeID)

		results, err := queryAPI.Query(context.Background(), query)
		if err != nil {
			log.Printf("InfluxDB query error for charge details: %v\n", err)
			http.Error(w, "Internal server error", http.StatusInternalServerError)
			return
		}

		var details []ChargeDetail
		for results.Next() {
			record := results.Record()
			detail := ChargeDetail{
				Timestamp:      record.Time(),
				SOC:            record.ValueByKey("soc").(float64),
				ChargePower:    record.ValueByKey("charge_power").(float64),
				BatteryTemp:    record.ValueByKey("battery_temp").(float64),
				BatteryVoltage: record.ValueByKey("battery_voltage").(float64),
				ChargeVoltage:  record.ValueByKey("charge_voltage").(float64),
			}
			details = append(details, detail)
		}

		if results.Err() != nil {
			log.Printf("InfluxDB query parsing error for charge details: %v\n", results.Err())
			http.Error(w, "Internal server error", http.StatusInternalServerError)
			return
		}

		w.Header().Set("Content-Type", "application/json")
		if err := json.NewEncoder(w).Encode(details); err != nil {
			log.Printf("Error encoding JSON response for charge details: %v\n", err)
			http.Error(w, "Internal server error", http.StatusInternalServerError)
			return
			// Query the driving_stats table.
			var stats []DrivingStat
			rows, err := pgConn.Query(context.Background(), `
				SELECT id, vehicle_id, stat_date, period_type, driving_count, total_mileage,
				total_fuel_consumption, total_electric_consumption, avg_mileage_per_drive,
				avg_fuel_consumption_per_drive, avg_electric_consumption_per_drive, created_at
				FROM driving_stats
				ORDER BY stat_date DESC
			`)
			if err != nil {
				log.Printf("Error querying PostgreSQL for driving stats: %v\n", err)
				http.Error(w, "Internal server error", http.StatusInternalServerError)
				return
			}
			defer rows.Close()
	
			for rows.Next() {
				var stat DrivingStat
				err := rows.Scan(
					&stat.ID,
					&stat.VehicleID,
					&stat.StatDate,
					&stat.PeriodType,
					&stat.DrivingCount,
					&stat.TotalMileage,
					&stat.TotalFuelConsumption,
					&stat.TotalElectricConsumption,
					&stat.AvgMileagePerDrive,
					&stat.AvgFuelConsumptionPerDrive,
					&stat.AvgElectricConsumptionPerDrive,
					&stat.CreatedAt,
				)
				if err != nil {
					log.Printf("Error scanning driving stat row: %v\n", err)
					http.Error(w, "Internal server error", http.StatusInternalServerError)
					return
				}
				stats = append(stats, stat)
			}
	
			w.Header().Set("Content-Type", "application/json")
			if err := json.NewEncoder(w).Encode(stats); err != nil {
				log.Printf("Error encoding JSON response for driving stats: %v\n", err)
				http.Error(w, "Internal server error", http.StatusInternalServerError)
				return
			}
		}
	}
}

// getDrivingStatsHandler fetches driving statistics from PostgreSQL
func getDrivingStatsHandler(pgConn *pgx.Conn) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		log.Println("DEBUG: Entering getDrivingStatsHandler for /api/driving-stats")
		log.Println("Received request for /api/driving-stats")
		setCORSHeaders(w, r)
		if r.Method == "OPTIONS" { return }
		if r.Method != "GET" { http.Error(w, "Method not allowed", http.StatusMethodNotAllowed); return }

		if useMockData {
			mockStats := GenerateMockDrivingStats()
			w.Header().Set("Content-Type", "application/json")
			if err := json.NewEncoder(w).Encode(mockStats); err != nil {
				log.Printf("Error encoding JSON response: %v\n", err)
				http.Error(w, "Internal server error", http.StatusInternalServerError)
			}
			return
		}
	}
}

// getLocationStatsHandler fetches location statistics from PostgreSQL
func getLocationStatsHandler(pgConn *pgx.Conn) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		log.Println("Received request for /api/location-stats")
		setCORSHeaders(w, r)
		if r.Method == "OPTIONS" { return }
		if r.Method != "GET" { http.Error(w, "Method not allowed", http.StatusMethodNotAllowed); return }

		if useMockData {
			mockLocations := GenerateMockLocationStats()
			w.Header().Set("Content-Type", "application/json")
			if err := json.NewEncoder(w).Encode(mockLocations); err != nil {
				log.Printf("Error encoding JSON response: %v\n", err)
				http.Error(w, "Internal server error", http.StatusInternalServerError)
			}
			return
		}
	}
}

// getDataFeaturesHandler fetches data features from PostgreSQL
func getDataFeaturesHandler(pgConn *pgx.Conn) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		log.Println("Received request for /api/data-features")
		setCORSHeaders(w, r)
		if r.Method == "OPTIONS" { return }
		if r.Method != "GET" { http.Error(w, "Method not allowed", http.StatusMethodNotAllowed); return }

		if useMockData {
			mockFeatures := GenerateMockDataFeatures()
			w.Header().Set("Content-Type", "application/json")
			if err := json.NewEncoder(w).Encode(mockFeatures); err != nil {
				log.Printf("Error encoding JSON response: %v\n", err)
				http.Error(w, "Internal server error", http.StatusInternalServerError)
			}
			return
		}
		// Query the data_features table.
		var features []DataFeature
		rows, err := pgConn.Query(context.Background(), `
			SELECT id, vehicle_id, feature_type, feature_value, start_time, end_time, area_info, created_at
			FROM data_features
			ORDER BY created_at DESC
		`)
		if err != nil {
			log.Printf("Error querying PostgreSQL for data features: %v\n", err)
			http.Error(w, "Internal server error", http.StatusInternalServerError)
			return
		}
		defer rows.Close()

		for rows.Next() {
			var feature DataFeature
			err := rows.Scan(
				&feature.ID,
				&feature.VehicleID,
				&feature.FeatureType,
				&feature.FeatureValue,
				&feature.StartTime,
				&feature.EndTime,
				&feature.AreaInfo,
				&feature.CreatedAt,
			)
			if err != nil {
				log.Printf("Error scanning data feature row: %v\n", err)
				http.Error(w, "Internal server error", http.StatusInternalServerError)
				return
			}
			features = append(features, feature)
		}

		w.Header().Set("Content-Type", "application/json")
		if err := json.NewEncoder(w).Encode(features); err != nil {
			log.Printf("Error encoding JSON response for data features: %v\n", err)
			http.Error(w, "Internal server error", http.StatusInternalServerError)
			return
		}
	}
}

// setCORSHeaders sets the necessary CORS headers for API responses
func setCORSHeaders(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Access-Control-Allow-Origin", "*")
	w.Header().Set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
	w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Authorization")
}