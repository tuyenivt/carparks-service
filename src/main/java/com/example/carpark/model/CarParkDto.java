package com.example.carpark.model;

import com.example.carpark.entity.CarPark;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CarParkDto {
    private String address;
    private double latitude;
    private double longitude;
    private int totalLots;
    private int availableLots;

    public static CarParkDto fromEntity(CarPark carPark) {
        return CarParkDto.builder()
                .address(carPark.address)
                .latitude(carPark.latitude)
                .longitude(carPark.longitude)
                .totalLots(carPark.totalLots)
                .availableLots(carPark.availableLots)
                .build();
    }
}
