CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE IF NOT EXISTS car_parks (
    car_park_no VARCHAR(50) PRIMARY KEY,
    address TEXT NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    total_lots INTEGER NOT NULL,
    available_lots INTEGER DEFAULT 0,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    location GEOGRAPHY(Point, 4326) GENERATED ALWAYS AS (
        ST_SetSRID(ST_Point(longitude, latitude), 4326)
    ) STORED
);

CREATE INDEX idx_car_parks_location ON car_parks USING GIST (location);
