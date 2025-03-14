package com.example.carpark.service;

import com.example.carpark.entity.CarPark;
import com.example.carpark.repository.CarParkRepository;
import com.example.carpark.util.ConverterUtil;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
class CarParkServiceTest {
    @InjectMock
    CarParkRepository carParkRepository;

    @InjectMock
    ConverterUtil converterUtil;

    @InjectMock
    CarParkAvailabilityService carParkAvailabilityService;

    CarParkService carParkService;

    @BeforeEach
    void setUp() {
        carParkService = new CarParkService(carParkRepository, converterUtil, carParkAvailabilityService);
    }

    @Test
    void ingestCsvData_validCsvData() {
        var csvContent = "carParkNo,address,xCoord,yCoord\nACB,BLK 270/271 ALBERT CENTRE BASEMENT CAR PARK,30314.7936,31490.4942";

        when(converterUtil.convertSVY21ToWGS84(30314.7936, 31490.4942)).thenReturn(new double[]{1.0, 1.0});
        when(carParkRepository.persist(any(CarPark.class))).thenReturn(Uni.createFrom().nullItem());

        carParkService.ingestCsvData(csvContent).await().indefinitely();

        verify(carParkRepository, times(1)).persist(any(CarPark.class));
    }

    @Test
    void ingestCsvData_emptyCsvData() {
        var csvContent = "carParkNo,address,xCoord,yCoord\n";

        carParkService.ingestCsvData(csvContent).await().indefinitely();

        verify(carParkRepository, never()).persist(any(CarPark.class));
    }

    @Test
    void ingestCsvData_invalidCsvData() {
        var csvContent = "carParkNo,address,xCoord,yCoord\nACB,BLK 270/271 ALBERT CENTRE BASEMENT CAR PARK,invalid,invalid";

        assertThrows(NumberFormatException.class, () -> carParkService.ingestCsvData(csvContent).await().indefinitely());

        verify(carParkRepository, never()).persist(any(CarPark.class));
    }

    @Test
    void getNearestCarParks_validCoordinates() {
        var latitude = 1.0;
        var longitude = 1.0;
        var page = 1;
        var perPage = 10;

        when(carParkRepository.findNearestWithAvailability(latitude, longitude, perPage, 0))
                .thenReturn(Uni.createFrom().item(List.of(new CarPark())));

        var result = carParkService.getNearestCarParks(latitude, longitude, page, perPage).await().indefinitely();

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getNearestCarParks_noResults() {
        var latitude = 1.0;
        var longitude = 1.0;
        var page = 1;
        var perPage = 10;

        when(carParkRepository.findNearestWithAvailability(latitude, longitude, perPage, 0))
                .thenReturn(Uni.createFrom().item(List.of()));

        var result = carParkService.getNearestCarParks(latitude, longitude, page, perPage).await().indefinitely();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
