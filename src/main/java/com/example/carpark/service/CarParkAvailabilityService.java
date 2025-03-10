package com.example.carpark.service;

import com.example.carpark.config.CarParkConfig;
import com.example.carpark.exception.CarParkException;
import com.example.carpark.model.CarParkAvailability;
import com.example.carpark.model.CarParkLotInfo;
import com.example.carpark.repository.CarParkRepository;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class CarParkAvailabilityService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CarParkAvailabilityService.class);

    private final CarParkConfig carParkConfig;
    private final CarParkRepository carParkRepository;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    @Inject
    public CarParkAvailabilityService(CarParkConfig carParkConfig, CarParkRepository carParkRepository,
                                      ObjectMapper objectMapper, Vertx vertx) {
        this.carParkConfig = carParkConfig;
        this.carParkRepository = carParkRepository;
        this.objectMapper = objectMapper;
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.webClient = WebClient.create(vertx);
    }

    public Uni<Void> updateAvailability() {
        return fetchCarParkAvailability()
                .map(this::parseAvailability)
                .flatMap(data ->
                        carParkRepository.findByCarParkNos(data.keySet())
                                .flatMap(carParks -> {
                                    carParks.forEach(carPark -> {
                                        var carParkLotInfo = data.get(carPark.carParkNo);
                                        LOGGER.info("Updating availability for car park: {} - available lots: {}", carPark.carParkNo, carParkLotInfo.getAvailableLots());
                                        carPark.totalLots = carParkLotInfo.getTotalLots();
                                        carPark.availableLots = carParkLotInfo.getAvailableLots();
                                        carPark.lastUpdated = new Timestamp(System.currentTimeMillis());
                                    });
                                    return carParkRepository.persist(carParks); // Batch persist
                                })
                );
    }

    private Uni<CarParkAvailability> fetchCarParkAvailability() {
        return webClient.getAbs(carParkConfig.availabilityApi())
                .timeout(60000) // 60s
                .send()
                .map(response -> {
                    if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                        throw new CarParkException("Failed to fetch availability data: " + response.statusCode());
                    }
                    try {
                        return objectMapper.readValue(response.bodyAsString(), CarParkAvailability.class);
                    } catch (JsonProcessingException e) {
                        throw new CarParkException("Failed to parse JSON response", e);
                    }
                })
                .invoke(() -> LOGGER.info("Fetched availability data successfully"));
    }

    Map<String, CarParkLotInfo> parseAvailability(CarParkAvailability carParkAvailability) {
        var availability = new HashMap<String, CarParkLotInfo>();
        for (var carParkData : carParkAvailability.getItems().getFirst().getCarParkData()) {
            var totalLots = carParkData.getCarParkInfo().stream()
                    .mapToInt(info -> parseIntQuietly(info.getTotalLots()))
                    .sum();
            var totalAvailableLots = carParkData.getCarParkInfo().stream()
                    .mapToInt(info -> parseIntQuietly(info.getLotsAvailable()))
                    .sum();
            var carParkLotInfo = CarParkLotInfo.builder()
                    .carParkNo(carParkData.getCarParkNumber())
                    .totalLots(totalLots)
                    .availableLots(totalAvailableLots)
                    .build();
            availability.put(carParkLotInfo.getCarParkNo(), carParkLotInfo);
        }
        return availability;
    }

    private int parseIntQuietly(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
