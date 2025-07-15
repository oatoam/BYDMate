package main

import (
	"fmt"
	"math/rand"
	"time"
)

// Location represents a geographical coordinate
type Location struct {
	Latitude  float64 `json:"latitude"`
	Longitude float64 `json:"longitude"`
}

// Trip represents a row in the 'trips' table
type Trip struct {
	ID                       string    `json:"id"`
	VehicleID                string    `json:"vehicle_id"`
	StartTime                time.Time `json:"start_time"`
	EndTime                  time.Time `json:"end_time"`
	StartLatitude            float64   `json:"start_latitude"`
	StartLongitude           float64   `json:"start_longitude"`
	EndLatitude              float64   `json:"end_latitude"`
	EndLongitude             float64   `json:"end_longitude"`
	TotalMileage             float64   `json:"total_mileage"`
	TotalFuelConsumption     float64   `json:"total_fuel_consumption"`
	TotalElectricConsumption float64   `json:"total_electric_consumption"`
	AvgSpeed                 float64   `json:"avg_speed"`
	MaxSpeed                 float64   `json:"max_speed"`
	AvgPower                 float64   `json:"avg_power"`
	CreatedAt                time.Time `json:"created_at"`
}

// Charge represents a row in the 'charges' table
type Charge struct {
	ID             string    `json:"id"`
	VehicleID      string    `json:"vehicle_id"`
	StartTime      time.Time `json:"start_time"`
	EndTime        time.Time `json:"end_time"`
	ChargeLatitude float64   `json:"charge_latitude"`
	ChargeLongitude float64   `json:"charge_longitude"`
	StartSOC       float64   `json:"start_soc"`
	EndSOC         float64   `json:"end_soc"`
	ChargedKWH     float64   `json:"charged_kwh"`
	ChargeType     string    `json:"charge_type"`
	CreatedAt      time.Time `json:"created_at"`
}

// DrivingStat represents a row in the 'driving_stats' table
type DrivingStat struct {
	ID                           string    `json:"id"`
	VehicleID                    string    `json:"vehicle_id"`
	StatDate                     string    `json:"stat_date"`
	PeriodType                   string    `json:"period_type"`
	DrivingCount                 int       `json:"driving_count"`
	TotalMileage                 float64   `json:"total_mileage"`
	TotalFuelConsumption         float64   `json:"total_fuel_consumption"`
	TotalElectricConsumption     float64   `json:"total_electric_consumption"`
	AvgMileagePerDrive           float64   `json:"avg_mileage_per_drive"`
	AvgFuelConsumptionPerDrive   float64   `json:"avg_fuel_consumption_per_drive"`
	AvgElectricConsumptionPerDrive float64   `json:"avg_electric_consumption_per_drive"`
	CreatedAt                    time.Time `json:"created_at"`
}

// LocationStat represents a row in the 'location_stats' table
type LocationStat struct {
	ID          string    `json:"id"`
	VehicleID   string    `json:"vehicle_id"`
	LocationType string    `json:"location_type"`
	Name        string    `json:"name"`
	Latitude    float64   `json:"latitude"`
	Longitude   float64   `json:"longitude"`
	Count       int       `json:"count"`
	CreatedAt   time.Time `json:"created_at"`
}

// DataFeature represents a row in the 'data_features' table
type DataFeature struct {
	ID          string    `json:"id"`
	VehicleID   string    `json:"vehicle_id"`
	FeatureType string    `json:"feature_type"`
	FeatureValue float64   `json:"feature_value"`
	StartTime   time.Time `json:"start_time"`
	EndTime     time.Time `json:"end_time"`
	AreaInfo    string    `json:"area_info"` // Store as JSON string
	CreatedAt   time.Time `json:"created_at"`
}

// TripDetail represents a data point in a trip
type TripDetail struct {
	Timestamp      time.Time `json:"timestamp"`
	DrivePower     float64   `json:"drive_power"`
	DischargePower float64   `json:"discharge_power"`
	Speed          float64   `json:"speed"`
	Altitude       float64   `json:"altitude"`
	SOC            float64   `json:"soc"`
	FuelLevel      float64   `json:"fuel_level"`
	Latitude       float64   `json:"latitude"`
	Longitude      float64   `json:"longitude"`
}

// ChargeDetail represents a data point in a charge
type ChargeDetail struct {
	Timestamp      time.Time `json:"timestamp"`
	SOC            float64   `json:"soc"`
	ChargePower    float64   `json:"charge_power"`
	BatteryTemp    float64   `json:"battery_temp"`
	BatteryVoltage float64   `json:"battery_voltage"`
	ChargeVoltage  float64   `json:"charge_voltage"`
}

// VehicleStatus represents the latest status of a vehicle
type VehicleStatus struct {
	VehicleID     string    `json:"vehicle_id"`
	Timestamp     time.Time `json:"timestamp"`
	Latitude      float64   `json:"latitude"`
	Longitude     float64   `json:"longitude"`
	Speed         float64   `json:"speed"`
	Mileage       float64   `json:"mileage"`
	FuelLevel     float64   `json:"fuel_level"`
	SOC           float64   `json:"soc"`
	BatteryTemp   float64   `json:"battery_temp"`
	ChargeStatus  string    `json:"charge_status"`
	Power         float64   `json:"power"`
}

// API Response structure for lists with pagination
type ListResponse struct {
	Total    int         `json:"total"`
	Page     int         `json:"page"`
	PageSize int         `json:"page_size"`
	Data     interface{} `json:"data"`
}

func init() {
	rand.Seed(time.Now().UnixNano())
}

func GenerateMockVehicleStatus() VehicleStatus {
	return VehicleStatus{
		VehicleID:    "mock-vehicle-1",
		Timestamp:    time.Now(),
		Latitude:     34.0522 + rand.Float64()*0.1,
		Longitude:    -118.2437 + rand.Float64()*0.1,
		Speed:        rand.Float64() * 120,
		Mileage:      rand.Float64() * 100000,
		FuelLevel:    rand.Float64() * 100,
		SOC:          rand.Float64() * 100,
		BatteryTemp:  rand.Float64() * 30,
		ChargeStatus: "idle",
		Power:        rand.Float64() * 50,
	}
}

func GenerateMockTrips(page, pageSize int) ListResponse {
	total := 100
	trips := []Trip{}
	for i := 0; i < pageSize; i++ {
		id := fmt.Sprintf("mock-trip-%d", (page-1)*pageSize+i+1)
		startTime := time.Now().Add(-time.Duration(rand.Intn(365*24)) * time.Hour)
		endTime := startTime.Add(time.Duration(rand.Intn(3)+1) * time.Hour)
		trips = append(trips, Trip{
			ID:                       id,
			VehicleID:                fmt.Sprintf("mock-vehicle-%d", rand.Intn(100)),
			StartTime:                startTime,
			EndTime:                  endTime,
			StartLatitude:            34.0 + rand.Float64(),
			StartLongitude:           -118.0 + rand.Float64(),
			EndLatitude:              34.0 + rand.Float64(),
			EndLongitude:             -118.0 + rand.Float64(),
			TotalMileage:             rand.Float64() * 200,
			TotalFuelConsumption:     rand.Float64() * 20,
			TotalElectricConsumption: rand.Float64() * 30,
			AvgSpeed:                 rand.Float64() * 80,
			MaxSpeed:                 rand.Float64() * 120,
			AvgPower:                 rand.Float64() * 50,
			CreatedAt:                time.Now(),
		})
	}
	return ListResponse{
		Total:    total,
		Page:     page,
		PageSize: pageSize,
		Data:     trips,
	}
}

func GenerateMockTripDetails(tripID string) []TripDetail {
	mockDetails := []TripDetail{}
	for i := 0; i < 5; i++ {
		mockDetails = append(mockDetails, TripDetail{
			Timestamp:      time.Now().Add(-time.Duration(5-i) * time.Minute),
			DrivePower:     rand.Float64() * 50,
			DischargePower: rand.Float64() * 10,
			Speed:          rand.Float64() * 100,
			Altitude:       rand.Float64() * 200,
			SOC:            rand.Float64() * 100,
			FuelLevel:      rand.Float64() * 100,
			Latitude:       34.0522 + rand.Float64()*0.01,
			Longitude:      -118.2437 + rand.Float64()*0.01,
		})
	}
	return mockDetails
}

func GenerateMockCharges(page, pageSize int) ListResponse {
	total := 50
	charges := []Charge{}
	for i := 0; i < pageSize; i++ {
		id := fmt.Sprintf("mock-charge-%d", (page-1)*pageSize+i+1)
		startTime := time.Now().Add(-time.Duration(rand.Intn(365*24)) * time.Hour)
		endTime := startTime.Add(time.Duration(rand.Intn(2)+1) * time.Hour)
		chargeType := "AC"
		if rand.Float64() > 0.5 {
			chargeType = "DC"
		}
		charges = append(charges, Charge{
			ID:             id,
			VehicleID:      fmt.Sprintf("mock-vehicle-%d", rand.Intn(100)),
			StartTime:      startTime,
			EndTime:        endTime,
			ChargeLatitude: 34.0 + rand.Float64(),
			ChargeLongitude: -118.0 + rand.Float64(),
			StartSOC:       rand.Float64() * 30,
			EndSOC:         70 + rand.Float64()*30,
			ChargedKWH:     rand.Float64() * 60,
			ChargeType:     chargeType,
			CreatedAt:      time.Now(),
		})
	}
	return ListResponse{
		Total:    total,
		Page:     page,
		PageSize: pageSize,
		Data:     charges,
	}
}

func GenerateMockChargeDetails(chargeID string) []ChargeDetail {
	mockDetails := []ChargeDetail{}
	for i := 0; i < 5; i++ {
		mockDetails = append(mockDetails, ChargeDetail{
			Timestamp:      time.Now().Add(-time.Duration(5-i) * time.Minute),
			SOC:            rand.Float64() * 100,
			ChargePower:    rand.Float64() * 100,
			BatteryTemp:    rand.Float64() * 50,
			BatteryVoltage: rand.Float64() * 500,
			ChargeVoltage:  rand.Float64() * 500,
		})
	}
	return mockDetails
}

func GenerateMockDrivingStats() []DrivingStat {
	mockStats := []DrivingStat{}
	for i := 0; i < 3; i++ {
		mockStats = append(mockStats, DrivingStat{
			ID:                           fmt.Sprintf("mock-driving-stat-%d", i+1),
			VehicleID:                    fmt.Sprintf("mock-vehicle-%d", rand.Intn(100)),
			StatDate:                     time.Now().AddDate(0, 0, -i).Format("2006-01-02"),
			PeriodType:                   "daily",
			DrivingCount:                 rand.Intn(10),
			TotalMileage:                 rand.Float64() * 500,
			TotalFuelConsumption:         rand.Float64() * 50,
			TotalElectricConsumption:     rand.Float64() * 30,
			AvgMileagePerDrive:           rand.Float64() * 100,
			AvgFuelConsumptionPerDrive:   rand.Float64() * 10,
			AvgElectricConsumptionPerDrive: rand.Float64() * 5,
			CreatedAt:                    time.Now(),
		})
	}
	return mockStats
}

func GenerateMockLocationStats() []LocationStat {
	mockLocations := []LocationStat{}
	for i := 0; i < 5; i++ {
		mockLocations = append(mockLocations, LocationStat{
			ID:          fmt.Sprintf("mock-location-stat-%d", i+1),
			VehicleID:   fmt.Sprintf("mock-vehicle-%d", rand.Intn(100)),
			LocationType: "top_destination",
			Name:        fmt.Sprintf("Mock Location %d", i+1),
			Latitude:    34.0 + rand.Float64(),
			Longitude:   -118.0 + rand.Float64(),
			Count:       rand.Intn(50),
			CreatedAt:   time.Now(),
		})
	}
	return mockLocations
}

func GenerateMockDataFeatures() []DataFeature {
	mockFeatures := []DataFeature{}
	for i := 0; i < 2; i++ {
		mockFeatures = append(mockFeatures, DataFeature{
			ID:          fmt.Sprintf("mock-data-feature-%d", i+1),
			VehicleID:   fmt.Sprintf("mock-vehicle-%d", rand.Intn(100)),
			FeatureType: "avg_speed_in_area",
			FeatureValue: rand.Float64() * 100,
			StartTime:   time.Now().Add(-time.Duration(rand.Intn(24)) * time.Hour),
			EndTime:     time.Now(),
			AreaInfo:    fmt.Sprintf(`{"name": "Mock Area %d", "geojson": "{}"}`, i+1),
			CreatedAt:   time.Now(),
		})
	}
	return mockFeatures
}