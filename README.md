# Car Parks Service

## Overview
This API-only application provides the nearest car parks with live parking availability based on user-provided coordinates.<br/>
It ingests static car park data from a CSV file and updates availability from an external API.

## Technologies
- **Language**: Java 21
- **Framework**: Quarkus
- **Build Tool**: Gradle
- **Database**: Postgres with PostGIS
- **Deployment**: Docker, Docker Compose

## Setup Instructions

### Prerequisites
- Java 21 such as [Amazon Corretto](https://docs.aws.amazon.com/corretto/latest/corretto-21-ug/downloads-list.html)
- Docker and Docker Compose such as [Docker Desktop](https://docs.docker.com/desktop/setup/install/windows-install/)

### Running with Docker Compose
Build and start the containers:
```shell
docker-compose up --build
```
The API will be available at [http://localhost:8080](http://localhost:8080)

### Data Ingestion and Updates
- **Car Park Information Data**: Call **Endpoint** `POST /v1/carparks/import-csv` only once time with CSV file.
```shell
curl -X POST http://localhost:8080/v1/carparks/import-csv -F "file=@HDBCarparkInformation.csv"
```
- **Car Park Availability Live Update**: Scheduled task `CarParkService.updateAvailabilityScheduler` run every 2 minutes to fetch and update availability.

## Development Instructions

### Running application
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
A downside of Quarkus is that the native build time takes 10-15 minutes, but the native build is only required for UAT load test release or production release; we can mitigate this with a pre-build native file before release or a parallel native build pipeline during CI/CD doing the test.<br/>

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
