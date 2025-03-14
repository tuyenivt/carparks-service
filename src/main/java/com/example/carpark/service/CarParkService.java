package com.example.carpark.service;

import com.example.carpark.entity.CarPark;
import com.example.carpark.exception.CarParkException;
import com.example.carpark.model.CarParkInformation;
import com.example.carpark.repository.CarParkRepository;
import com.example.carpark.util.ConverterUtil;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.List;

@ApplicationScoped
public class CarParkService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CarParkService.class);

    private final CarParkRepository carParkRepository;
    private final ConverterUtil converterUtil;
    private final CarParkAvailabilityService carParkAvailabilityService;

    @Inject
    public CarParkService(CarParkRepository carParkRepository, ConverterUtil converterUtil,
                          CarParkAvailabilityService carParkAvailabilityService) {
        this.carParkRepository = carParkRepository;
        this.converterUtil = converterUtil;
        this.carParkAvailabilityService = carParkAvailabilityService;
    }

    public Uni<Void> ingestCsvData(String csvContent) {
        try (var csvReader = new CSVReaderBuilder(new StringReader(csvContent)).withSkipLines(1).build()) {
            return Multi.createFrom().items(csvReader.readAll().stream())
                    .onItem().transform(CarParkInformation::fromCsvRow)
                    .onItem().transformToUniAndMerge(this::saveCarPark)
                    .onItem().invoke(carPark -> LOGGER.info("Saved car park: {}", carPark.carParkNo))
                    .onItem().ignore()
                    .toUni();
        } catch (IOException | CsvException e) {
            LOGGER.error("Failed to ingest CSV data: {}", e.getMessage());
            throw new CarParkException("CSV ingestion failed", e);
        }
    }

    private Uni<CarPark> saveCarPark(CarParkInformation carParkInfo) {
        var carParkNo = carParkInfo.getCarParkNo();
        LOGGER.info("Processing car park: {}", carParkNo);
        var wgs84 = converterUtil.convertSVY21ToWGS84(carParkInfo.getXCoord(), carParkInfo.getYCoord());
        var carPark = CarPark.builder()
                .carParkNo(carParkNo)
                .address(carParkInfo.getAddress())
                .latitude(wgs84[0])
                .longitude(wgs84[1])
                .lastUpdated(new Timestamp(System.currentTimeMillis()))
                .build();
        return carParkRepository.persist(carPark)
                .invoke(() -> LOGGER.info("Ingested new car park: {}", carParkNo));
    }

    /**
     * <p>Scheduled task to update car park availability every 2 minutes</p>
     *
     * <p>From API Docs https://data.gov.sg/datasets/d_ca933a644e55d34fe21f28b8052fac63/view</p>
     * <p>Recommended that this endpoint be called every minute, so we can update every 2 minutes to be safe</p>
     */
    @Scheduled(every = "2m")
    @WithTransaction
    public Uni<Void> updateAvailabilityScheduler() {
        return carParkAvailabilityService.updateAvailability();
    }

    public Uni<List<CarPark>> getNearestCarParks(double latitude, double longitude, int page, int perPage) {
        var offset = (page - 1) * perPage;
        return carParkRepository.findNearestWithAvailability(latitude, longitude, perPage, offset);
    }
}
