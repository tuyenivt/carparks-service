# Car Parks Service

## Overview
This API-only application provides the nearest car parks with live parking availability based on user-provided coordinates.<br/>
It ingests static car park data from a CSV file and updates availability from an external API.

## Technologies
- **Language**: Java 21
- **Framework**: Quarkus (reactive, high-performance, optimized for high throughput, native build, scalable cloud-native)
- **Build Tool**: Gradle
- **Database**: Postgres with PostGIS
- **Deployment**: Docker, Docker Compose

## How to Run the Application

### Prerequisites
- Docker and Docker Compose such as [Docker Desktop](https://docs.docker.com/get-started/introduction/get-docker-desktop/)

### Running Application with Docker Compose
Build and start the containers:
```shell
docker-compose up --build
```
The API will be available at [http://localhost:8080](http://localhost:8080)

### Accessing the OpenAPI Specification
This endpoint provides the OpenAPI specification for the API in JSON format, detailing available endpoints, parameters, and schemas.
```shell
curl http://localhost:8080/api/openapi
```
Open API UI: http://localhost:8080/q/swagger-ui

### Accessing Health Check and Metrics endpoints
```shell
curl http://localhost:8080/q/health
curl http://localhost:8080/q/metrics
```

### Initialing Car Park Information Data and Availability Updates
- **Car Park Information Data**: Call **Endpoint** `POST /v1/carparks/import-csv` only once time with CSV file (same this folder or download from [HDB Carpark Information](https://data.gov.sg/datasets/d_23f946fa557947f93a8043bbef41dd09/view)).
```shell
curl -X POST http://localhost:8080/v1/carparks/import-csv -F "file=@HDBCarparkInformation.csv"
```
- **Car Park Availability Live Update**: Scheduled task `CarParkService.updateAvailabilityScheduler` run every 2 minutes to fetch and update availability.<br/>
If you run the application first time, static data is empty and nothing updated, after import csv and let wait more 2 minutes for scheduled task update availability (you can view console log to see data updating).

### Accessing API Find Nearest Availability Car Parks Based On User-Provided Coordinates
**Endpoint**: `GET /v1/carparks/nearest`

Returns the nearest car parks with available parking lots. (required Car Park Information Data imported and Car Park Availability Data updated)

Query Parameters
- **latitude (required)**: Latitude of the user's location (-90 to 90).
- **longitude (required)**: Longitude of the user's location (-180 to 180).
- **page (optional)**: Page number for pagination (default: 1).
- **per_page (optional)**: Number of results per page (default: 10).

Sample Request
```shell
curl "http://localhost:8080/v1/carparks/nearest?latitude=1.37326&longitude=103.897&page=2&per_page=3"
```

Sample Response
```shell
[{
        "address": "BLK 351-357 HOUGANG AVENUE 7",
        "latitude": 1.3723422711738515,
        "longitude": 103.89908052009055,
        "totalLots": 232,
        "availableLots": 78
    }, {
        "address": "BLK 804 HOUGANG AVENUE 10",
        "latitude": 1.3712221378751506,
        "longitude": 103.89475741107438,
        "totalLots": 43,
        "availableLots": 30
    }, {
        "address": "BLK 364 / 365 UPPER SERANGOON RD",
        "latitude": 1.3701078117066328,
        "longitude": 103.8972275612915,
        "totalLots": 483,
        "availableLots": 284
    }
]
```

Error Responses
- **400 Bad Request**: Missing or invalid latitude/longitude.
- **500 Internal Server Error**: Unexpected server issues.

## Development Instructions

### Prerequisites
- JDK 21 such as [Amazon Corretto](https://docs.aws.amazon.com/corretto/latest/corretto-21-ug/downloads-list.html)

### Running application (Live Coding)
```shell
./gradlew quarkusDev
```

### Running tests
```shell
./gradlew test
```
