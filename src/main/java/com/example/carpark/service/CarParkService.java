package com.example.carpark.service;

import com.example.carpark.entity.CarPark;
import com.example.carpark.exception.CarParkException;
import com.example.carpark.model.CarParkInfo;
import com.example.carpark.repository.CarParkRepository;
import com.example.carpark.util.ConverterUtil;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Timestamp;

@ApplicationScoped
public class CarParkService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CarParkService.class);

    private final CarParkRepository carParkRepository;
    private final ConverterUtil converterUtil;

    @Inject
    public CarParkService(CarParkRepository carParkRepository, ConverterUtil converterUtil) {
        this.carParkRepository = carParkRepository;
        this.converterUtil = converterUtil;
    }

    public Uni<Void> ingestCsvData(String csvContent) {
        try (var csvReader = new CSVReaderBuilder(new StringReader(csvContent)).withSkipLines(1).build()) {
            var rows = csvReader.readAll();
            LOGGER.info("Total rows read (excluding header): {}", rows.size());

            return Uni.combine().all().unis(
                    rows.stream().map(row -> {
                        var carParkInfo = CarParkInfo.fromCsvRow(row);
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
                        LOGGER.info("Car park prepared: {} - address: {}", carParkNo, carPark.address);
                        return carParkRepository.persist(carPark)
                                .invoke(() -> LOGGER.info("Ingested new car park: {}", carParkNo));
                    }).toList()
            ).discardItems();
        } catch (IOException | CsvException e) {
            LOGGER.error("Failed to ingest CSV data: {}", e.getMessage());
            throw new CarParkException("CSV ingestion failed", e);
        }
    }
}
