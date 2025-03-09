package com.example.carpark.service;

import com.example.carpark.entity.CarPark;
import com.example.carpark.repository.CarParkRepository;
import com.example.carpark.util.ConverterUtil;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
class CarParkServiceTest {
    @InjectMock
    CarParkRepository carParkRepository;

    @InjectMock
    ConverterUtil converterUtil;

    CarParkService carParkService;

    @BeforeEach
    void setUp() {
        carParkService = new CarParkService(carParkRepository, converterUtil);
    }

    @Test
    void ingestCsvData_validCsvData() {
        var csvContent = "carParkNo,address,xCoord,yCoord\nACB,BLK 270/271 ALBERT CENTRE BASEMENT CAR PARK,30314.7936,31490.4942";

        when(converterUtil.convertSVY21ToWGS84(30314.7936, 31490.4942)).thenReturn(new double[]{1.0, 1.0});
        when(carParkRepository.findByCarParkNo("ACB")).thenReturn(Uni.createFrom().nullItem());
        when(carParkRepository.persist(any(CarPark.class))).thenReturn(Uni.createFrom().nullItem());

        carParkService.ingestCsvData(csvContent);

        verify(carParkRepository, times(1)).persist(any(CarPark.class));
    }
}
