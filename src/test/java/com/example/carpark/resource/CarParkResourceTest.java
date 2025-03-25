package com.example.carpark.resource;

import com.example.carpark.entity.CarPark;
import com.example.carpark.service.CarParkService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@QuarkusTest
class CarParkResourceTest {
    @InjectMock
    CarParkService carParkService;

    @Test
    void importCsvDataSuccessfully() {
        var csvData = "car_park_no,address,x_coord,y_coord\nCP1,Address1,1.0,1.0\nCP2,Address2,2.0,2.0";
        when(carParkService.ingestCarParkInfos(anyList())).thenReturn(Uni.createFrom().voidItem());
        given()
                .multiPart("file", "carparks.csv", csvData.getBytes(), "text/csv")
                .when().post("/v1/carparks/import-csv")
                .then()
                .statusCode(202)
                .body(containsString("CSV data imported successfully"));
    }

    @Test
    void importCsvDataWithInvalidFile() {
        given()
                .multiPart("file", "carparks.csv", "invalid content\ninvalid conten".getBytes(), "text/csv")
                .when().post("/v1/carparks/import-csv")
                .then()
                .statusCode(500)
                .body(containsString("CSV import failed"));
    }

    @Test
    void getNearestCarParksSuccessfully() {
        var carParks = List.of(new CarPark("CP1", "Address1", 1.0, 2.0, 10, 5, new Timestamp(System.currentTimeMillis())));
        when(carParkService.getNearestCarParks(any(Double.class), any(Double.class), any(Integer.class), any(Integer.class)))
                .thenReturn(Uni.createFrom().item(carParks));
        given()
                .queryParam("latitude", 1.0)
                .queryParam("longitude", 2.0)
                .queryParam("page", 1)
                .queryParam("per_page", 10)
                .when().get("/v1/carparks/nearest")
                .then()
                .statusCode(200)
                .body(containsString("Address1"))
                .body(containsString("1.0"))
                .body(containsString("2.0"))
                .body(containsString("10"))
                .body(containsString("5"))
                .body(not(containsString("CP1")));
    }

    @Test
    void getNearestCarParksWithMissingCoordinates() {
        given()
                .queryParam("page", 1)
                .queryParam("per_page", 10)
                .when().get("/v1/carparks/nearest")
                .then()
                .statusCode(400)
                .body(containsString("Missing latitude or longitude"));
    }

    @Test
    void getNearestCarParksWithInvalidCoordinates() {
        given()
                .queryParam("latitude", 100.0)
                .queryParam("longitude", 200.0)
                .queryParam("page", 1)
                .queryParam("per_page", 10)
                .when().get("/v1/carparks/nearest")
                .then()
                .statusCode(400)
                .body(containsString("Latitude must be between -90 and 90, longitude between -180 and 180"));
    }

    @Test
    void getNearestCarParksWithInvalidPagination() {
        given()
                .queryParam("latitude", 1.0)
                .queryParam("longitude", 1.0)
                .queryParam("page", 0)
                .queryParam("per_page", 0)
                .when().get("/v1/carparks/nearest")
                .then()
                .statusCode(400)
                .body(containsString("Page and per_page must be positive integers"));
    }
}