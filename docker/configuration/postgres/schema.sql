CREATE TABLE IF NOT EXISTS vehicles (
    id UUID PRIMARY KEY,
    name VARCHAR(255),
    vin VARCHAR(255) UNIQUE,
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS trips (
    id UUID PRIMARY KEY,
    vehicle_id UUID REFERENCES vehicles(id),
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    start_latitude NUMERIC,
    start_longitude NUMERIC,
    end_latitude NUMERIC,
    end_longitude NUMERIC,
    total_mileage NUMERIC,
    total_fuel_consumption NUMERIC,
    total_electric_consumption NUMERIC,
    avg_speed NUMERIC,
    max_speed NUMERIC,
    avg_power NUMERIC,
    created_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS trip_stats (
    id SERIAL PRIMARY KEY,
    period_drive_count INTEGER,
    period_drive_mileage NUMERIC(10, 2),
    period_battery_consumption NUMERIC(10, 2),
    period_fuel_consumption NUMERIC(10, 2),
    avg_mileage_per_trip NUMERIC(10, 2),
    avg_fuel_per_trip NUMERIC(10, 2),
    avg_battery_per_trip NUMERIC(10, 2),
    period_top10_destinations JSONB,
    period_top10_origins JSONB
);

CREATE TABLE IF NOT EXISTS charges (
    id UUID PRIMARY KEY,
    vehicle_id UUID REFERENCES vehicles(id),
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    charge_latitude NUMERIC,
    charge_longitude NUMERIC,
    start_soc NUMERIC,
    end_soc NUMERIC,
    charged_kwh NUMERIC,
    charge_type VARCHAR(255),
    created_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS driving_stats (
    id UUID PRIMARY KEY,
    vehicle_id UUID REFERENCES vehicles(id),
    stat_date DATE,
    period_type VARCHAR(255),
    driving_count INTEGER,
    total_mileage NUMERIC,
    total_fuel_consumption NUMERIC,
    total_electric_consumption NUMERIC,
    avg_mileage_per_drive NUMERIC,
    avg_fuel_consumption_per_drive NUMERIC,
    avg_electric_consumption_per_drive NUMERIC,
    created_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS location_stats (
    id UUID PRIMARY KEY,
    vehicle_id UUID REFERENCES vehicles(id),
    location_type VARCHAR(255),
    name VARCHAR(255),
    latitude NUMERIC,
    longitude NUMERIC,
    count INTEGER,
    created_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS data_features (
    id UUID PRIMARY KEY,
    vehicle_id UUID REFERENCES vehicles(id),
    feature_type VARCHAR(255),
    feature_value NUMERIC,
    start_time TIMESTAMPTZ,
    end_time TIMESTAMPTZ,
    area_info JSONB,
    created_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS tracks (
    id SERIAL PRIMARY KEY,
    timestamp TIMESTAMPTZ NOT NULL,
    location GEOMETRY(Point, 4326), -- Using PostGIS GEOMETRY type for location
    altitude NUMERIC(10, 2),
    speed NUMERIC(10, 2),
    pressure NUMERIC(10, 2),
    acceleration NUMERIC(10, 2)
);

CREATE TABLE IF NOT EXISTS refuels (
    id SERIAL PRIMARY KEY,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    start_fuel_level NUMERIC(5, 2),
    end_fuel_level NUMERIC(5, 2)
);

CREATE TABLE IF NOT EXISTS temperature_ac (
    id SERIAL PRIMARY KEY,
    timestamp TIMESTAMPTZ NOT NULL,
    external_temperature NUMERIC(5, 2),
    internal_temperature NUMERIC(5, 2),
    ac_temperature NUMERIC(5, 2),
    ac_fan_speed INTEGER
);

CREATE TABLE IF NOT EXISTS batteries (
    id SERIAL PRIMARY KEY,
    timestamp TIMESTAMPTZ NOT NULL,
    charge_count INTEGER,
    discharge_cycle_count INTEGER,
    total_charge_kwh NUMERIC(10, 2),
    total_discharge_kwh NUMERIC(10, 2),
    ac_charge_kwh NUMERIC(10, 2),
    dc_charge_kwh NUMERIC(10, 2),
    hev_charge_kwh NUMERIC(10, 2)
);