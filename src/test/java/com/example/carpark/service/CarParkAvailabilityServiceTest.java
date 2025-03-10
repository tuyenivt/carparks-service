package com.example.carpark.service;

import com.example.carpark.model.CarParkAvailability;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.mutiny.core.Vertx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CarParkAvailabilityServiceTest {
    ObjectMapper objectMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    CarParkAvailabilityService carParkAvailabilityService;

    @BeforeEach
    void setUp() {
        carParkAvailabilityService = new CarParkAvailabilityService(null, null, objectMapper, Vertx.vertx());
    }

    @Test
    void parseAvailabilityWithValidData() throws JsonProcessingException {
        var json = """
                {
                    "items": [{
                            "timestamp": "2025-03-10T14:43:36+08:00",
                            "carpark_data": [{
                                    "carpark_info": [{
                                            "total_lots": "105",
                                            "lot_type": "C",
                                            "lots_available": "99"
                                        }
                                    ],
                                    "carpark_number": "HE12",
                                    "update_datetime": "2025-03-10T14:42:31"
                                }, {
                                    "carpark_info": [{
                                            "total_lots": "1033",
                                            "lot_type": "C",
                                            "lots_available": "711"
                                        }
                                    ],
                                    "carpark_number": "PL90",
                                    "update_datetime": "2025-03-10T14:42:48"
                                }
                            ]
                        }
                    ]
                }
                """;
        var carParkAvailability = objectMapper.readValue(json, CarParkAvailability.class);
        var result = carParkAvailabilityService.parseAvailability(carParkAvailability);
        assertEquals(2, result.size());
        assertEquals(105, result.get("HE12").getTotalLots());
        assertEquals(99, result.get("HE12").getAvailableLots());
        assertEquals(1033, result.get("PL90").getTotalLots());
        assertEquals(711, result.get("PL90").getAvailableLots());
    }

    @Test
    void parseAvailabilityWithEmptyData() throws JsonProcessingException {
        var json = """
                {
                    "items": [{
                            "carpark_data": [
                            ]
                        }
                    ]
                }
                """;
        var carParkAvailability = objectMapper.readValue(json, CarParkAvailability.class);

        var result = carParkAvailabilityService.parseAvailability(carParkAvailability);

        assertEquals(0, result.size());
    }
}
