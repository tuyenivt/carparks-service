package com.example.carpark.resource;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class CarParkResourceTest {
    @Test
    void testHealthEndpoint() {
        given()
                .when().get("/v1/carparks/health")
                .then()
                .statusCode(200)
                .body(is("OK"));
    }
}
