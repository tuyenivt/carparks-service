package com.example.carpark.resource;

import com.example.carpark.exception.CarParkException;
import com.example.carpark.service.CarParkService;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
class CarParkResourceTest {
    @InjectMock
    CarParkService carParkService;

    @Test
    void testHealthEndpoint() {
        given()
                .when().get("/v1/carparks/health")
                .then()
                .statusCode(200)
                .body(is("OK"));
    }

    @Test
    void importCsvDataSuccessfully() {
        var csvData = "car_park_no,address,x_coord,y_coord\nCP1,Address1,1.0,1.0\nCP2,Address2,2.0,2.0";
        when(carParkService.ingestCsvData(any(String.class))).thenReturn(Uni.createFrom().voidItem());
        given()
                .multiPart("file", "carparks.csv", csvData.getBytes(), "text/csv")
                .when().post("/v1/carparks/import-csv")
                .then()
                .statusCode(202)
                .body(containsString("CSV data imported successfully"));
    }

    @Test
    void importCsvDataWithInvalidFile() {
        when(carParkService.ingestCsvData(any(String.class))).thenThrow(CarParkException.class);
        given()
                .multiPart("file", "carparks.csv", "invalid content")
                .when().post("/v1/carparks/import-csv")
                .then()
                .statusCode(500)
                .body(containsString("CSV import failed"));
    }
}