package main

import (
	"context"
	"fmt"
	"log"
	"time"

	"github.com/brianvoe/gofakeit/v6"
	"github.com/google/uuid"
	influxdb2 "github.com/influxdata/influxdb-client-go/v2"
	"github.com/jackc/pgx/v4"
)

const (
	pgConnStr    = "postgresql://user:password@localhost:5432/byd_mate"
	influxDBURL  = "http://localhost:8086"
	influxDBToken = "my-super-secret-token" // Replace with your InfluxDB token
	influxDBOrg  = "byd_org"
	influxDBBucket = "byd_bucket"
)

func main() {
	ctx := context.Background()

	// Connect to PostgreSQL
	pgConn, err := pgx.Connect(ctx, pgConnStr)
	if err != nil {
		log.Fatalf("Unable to connect to PostgreSQL: %v\n", err)
	}
	defer pgConn.Close(ctx)
	fmt.Println("Connected to PostgreSQL!")

	// Connect to InfluxDB
	influxClient := influxdb2.NewClient(influxDBURL, influxDBToken)
	defer influxClient.Close()
	
	// Generate and insert data
	generateAndInsertData(ctx, pgConn, influxClient)

	fmt.Println("Data generation and insertion complete!")
}

func generateAndInsertData(ctx context.Context, pgConn *pgx.Conn, influxClient influxdb2.Client) {
	writeAPI := influxClient.WriteAPIBlocking(influxDBOrg, influxDBBucket)

	// Generate vehicles
	vehicleIDs := make([]uuid.UUID, 0)
	for i := 0; i < 2; i++ { // Generate 2 vehicles
		vehicleID := uuid.New()
		vehicleIDs = append(vehicleIDs, vehicleID)
		name := gofakeit.CarModel()
		vin := gofakeit.CarModel() // Using CarModel as a placeholder for CarVin
		createdAt := time.Now().Add(-time.Duration(gofakeit.Number(1, 365*24)) * time.Hour)

		_, err := pgConn.Exec(ctx, `
			INSERT INTO vehicles (id, name, vin, created_at, updated_at)
			VALUES ($1, $2, $3, $4, $4)
		`, vehicleID, name, vin, createdAt)
		if err != nil {
			log.Printf("Failed to insert vehicle %s: %v\n", vehicleID.String(), err)
		} else {
			fmt.Printf("Inserted vehicle: %s\n", vehicleID.String())
		}
	}

	// Generate trips and trip details
	for _, vehicleID := range vehicleIDs {
		for i := 0; i < 5; i++ { // Generate 5 trips per vehicle
			tripID := uuid.New()
			startTime := time.Now().Add(-time.Duration(gofakeit.Number(1, 30*24)) * time.Hour)
			endTime := startTime.Add(time.Duration(gofakeit.Number(30, 120)) * time.Minute)
			startLat := gofakeit.Latitude()
			startLon := gofakeit.Longitude()
			endLat := gofakeit.Latitude()
			endLon := gofakeit.Longitude()
			totalMileage := gofakeit.Float64Range(10.0, 200.0)
			totalFuelConsumption := gofakeit.Float64Range(1.0, 20.0)
			totalElectricConsumption := gofakeit.Float64Range(5.0, 50.0)
			avgSpeed := gofakeit.Float64Range(30.0, 120.0)
			maxSpeed := gofakeit.Float64Range(avgSpeed, 180.0)
			avgPower := gofakeit.Float64Range(10.0, 50.0)

			_, err := pgConn.Exec(ctx, `
				INSERT INTO trips (id, vehicle_id, start_time, end_time, start_latitude, start_longitude, end_latitude, end_longitude, total_mileage, total_fuel_consumption, total_electric_consumption, avg_speed, max_speed, avg_power, created_at)
				VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15)
			`, tripID, vehicleID, startTime, endTime, startLat, startLon, endLat, endLon, totalMileage, totalFuelConsumption, totalElectricConsumption, avgSpeed, maxSpeed, avgPower, time.Now())
			if err != nil {
				log.Printf("Failed to insert trip %s: %v\n", tripID.String(), err)
			} else {
				fmt.Printf("Inserted trip: %s\n", tripID.String())
			}

			// Generate trip details for InfluxDB
			for t := startTime; t.Before(endTime); t = t.Add(30 * time.Second) {
				p := influxdb2.NewPointWithMeasurement("trip_details").
					AddTag("vehicle_id", vehicleID.String()).
					AddTag("trip_id", tripID.String()).
					AddField("drive_power", gofakeit.Float64Range(0.0, 100.0)).
					AddField("discharge_power", gofakeit.Float64Range(0.0, 20.0)).
					AddField("speed", gofakeit.Float64Range(0.0, 150.0)).
					AddField("altitude", gofakeit.Float64Range(0.0, 500.0)).
					AddField("soc", gofakeit.Float64Range(10.0, 90.0)).
					AddField("fuel_level", gofakeit.Float64Range(0.0, 100.0)).
					AddField("latitude", gofakeit.Latitude()).
					AddField("longitude", gofakeit.Longitude()).
					SetTime(t)
				if err := writeAPI.WritePoint(ctx, p); err != nil {
					log.Printf("Failed to write trip detail point: %v\n", err)
				}
			}
			fmt.Printf("Inserted trip details for trip: %s\n", tripID.String())
		}
	}

	// Generate charges and charge details
	for _, vehicleID := range vehicleIDs {
		for i := 0; i < 3; i++ { // Generate 3 charges per vehicle
			chargeID := uuid.New()
			startTime := time.Now().Add(-time.Duration(gofakeit.Number(1, 15*24)) * time.Hour)
			endTime := startTime.Add(time.Duration(gofakeit.Number(60, 240)) * time.Minute)
			chargeLat := gofakeit.Latitude()
			chargeLon := gofakeit.Longitude()
			startSOC := gofakeit.Float64Range(10.0, 30.0)
			endSOC := gofakeit.Float64Range(80.0, 100.0)
			chargedKWH := gofakeit.Float64Range(30.0, 80.0)
			chargeType := gofakeit.RandomString([]string{"AC", "DC"})

			_, err := pgConn.Exec(ctx, `
				INSERT INTO charges (id, vehicle_id, start_time, end_time, charge_latitude, charge_longitude, start_soc, end_soc, charged_kwh, charge_type, created_at)
				VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
			`, chargeID, vehicleID, startTime, endTime, chargeLat, chargeLon, startSOC, endSOC, chargedKWH, chargeType, time.Now())
			if err != nil {
				log.Printf("Failed to insert charge %s: %v\n", chargeID.String(), err)
			} else {
				fmt.Printf("Inserted charge: %s\n", chargeID.String())
			}

			// Generate charge details for InfluxDB
			for t := startTime; t.Before(endTime); t = t.Add(1 * time.Minute) {
				p := influxdb2.NewPointWithMeasurement("charge_details").
					AddTag("vehicle_id", vehicleID.String()).
					AddTag("charge_id", chargeID.String()).
					AddField("soc", gofakeit.Float64Range(startSOC, endSOC)).
					AddField("charge_power", gofakeit.Float64Range(0.0, 100.0)).
					AddField("battery_temp", gofakeit.Float64Range(20.0, 40.0)).
					AddField("battery_voltage", gofakeit.Float64Range(300.0, 500.0)).
					AddField("charge_voltage", gofakeit.Float64Range(350.0, 550.0)).
					SetTime(t)
				if err := writeAPI.WritePoint(ctx, p); err != nil {
					log.Printf("Failed to write charge detail point: %v\n", err)
				}
			}
			fmt.Printf("Inserted charge details for charge: %s\n", chargeID.String())
		}
	}

	// Generate vehicle telemetry
	for _, vehicleID := range vehicleIDs {
		for i := 0; i < 100; i++ { // Generate 100 telemetry points per vehicle
			t := time.Now().Add(-time.Duration(gofakeit.Number(1, 60)) * time.Minute)
			p := influxdb2.NewPointWithMeasurement("vehicle_telemetry").
				AddTag("vehicle_id", vehicleID.String()).
				AddField("latitude", gofakeit.Latitude()).
				AddField("longitude", gofakeit.Longitude()).
				AddField("speed", gofakeit.Float64Range(0.0, 150.0)).
				AddField("mileage", gofakeit.Float64Range(10000.0, 50000.0)).
				AddField("fuel_level", gofakeit.Float64Range(0.0, 100.0)).
				AddField("soc", gofakeit.Float64Range(0.0, 100.0)).
				AddField("battery_temp", gofakeit.Float64Range(10.0, 50.0)).
				AddField("charge_status", gofakeit.RandomString([]string{"charging", "discharging", "idle"})).
				AddField("power", gofakeit.Float64Range(0.0, 100.0)).
				AddField("altitude", gofakeit.Float64Range(0.0, 1000.0)).
				AddField("oil_level", gofakeit.Float64Range(0.0, 100.0)).
				SetTime(t)
			if err := writeAPI.WritePoint(ctx, p); err != nil {
				log.Printf("Failed to write vehicle telemetry point: %v\n", err)
			}
		}
		fmt.Printf("Inserted vehicle telemetry for vehicle: %s\n", vehicleID.String())
	}
}