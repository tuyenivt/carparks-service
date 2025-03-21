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

## Thought Process - Analysis
This application provides availability lots nearest distance car parks from the user's current location, a high-throughput application.<br/>

For language and framework choice, quick comparison between Java and Go, Go is fast by its simplicity, goroutines, native build, and fast bootstrap.<br/>
With modern Java, Java is also fast, although not as fast as Go, but with modern version and framework, Java speed is comparable.<br/>
Java Virtual Thread comparable with goroutines and modern frameworks such as Quarkus, support native build (skip JIT) and fast bootstrap also (ms to a second).<br/>
Java also has a large community, library, ecosystem, enterprise support, legacy integration, and mature testing support (Go good support test but more manual).<br/>
I chose Java because I am familiar with Java over Go, but I still want the application to run fast, high-throughput adaptive, and fast bootstrap for horizontal scale cloud native.<br/>
I choose Quarkus because it is fast, supports native build, supports GraalVM for native build, supports Java 21, and build-in reactive programming for high throughput.<br/>

For database choice, this main feature is calculating the distance between 2 coordinates.<br/>
Postgres with PostGIS or MySQL 8 Point also supports geospatial data, but MySQL 8 just basic support and is not fast and accurate comparable with Postgres with PostGIS, which is mature for geospatial data, supports geospatial query, indexing, and calculation.<br/>

For car park information, we need an API for CSV data ingestion.<br/>
The beta link to download CSV is deprecated, now it is: https://data.gov.sg/datasets/d_23f946fa557947f93a8043bbef41dd09/view<br/>
I'll download and put CSV file at the project `data` folder for static load and demo.<br/>
To convert SVY21 to WGS84, I found a library https://github.com/locationtech/proj4j?tab=readme-ov-file#basic-usage and will use it for conversion.<br/>

For car park availability, we need a task scheduler for data updates via https://api.data.gov.sg/v1/transport/carpark-availability.<br/>

We need an API endpoint service to calculate the nearest car park from the user's current location for the main feature.<br/>
The URL parameters: `latitude` and `longitude`<br/>
Return a `JSON array` of car parks `sorted` by `distance ascending` with the `total` and `available parking lots`.<br/>
Return HTTP status code `400` if requests `missing latitude or longitude`<br/>
Support pagination: `page` and `per_page`.<br/>
More input validation: `latitude` and `longitude` range. Latitude values range from -90 to +90 degrees. Longitude ranges from 0° to 180° East and 0° to 180° West<br/>
