package com.example.carpark.model;

import com.example.carpark.util.NumberUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CarParkLotInfo {
    private String carParkNo;
    private int totalLots;
    private int availableLots;

    public static CarParkLotInfo merge(CarParkLotInfo carParkLotInfo1, CarParkLotInfo carParkLotInfo2) {
        return CarParkLotInfo.builder()
                .carParkNo(carParkLotInfo1.getCarParkNo())
                .totalLots(carParkLotInfo1.getTotalLots() + carParkLotInfo2.getTotalLots())
                .availableLots(carParkLotInfo1.getAvailableLots() + carParkLotInfo2.getAvailableLots())
                .build();
    }

    public static CarParkLotInfo fromCarParkData(CarParkAvailability.CarParkData carParkData) {
        var totalLots = carParkData.getCarParkInfo().stream()
                .mapToInt(info -> NumberUtil.parseIntQuietly(info.getTotalLots()))
                .sum();
        var totalAvailableLots = carParkData.getCarParkInfo().stream()
                .mapToInt(info -> NumberUtil.parseIntQuietly(info.getLotsAvailable()))
                .sum();
        return CarParkLotInfo.builder()
                .carParkNo(carParkData.getCarParkNumber())
                .totalLots(totalLots)
                .availableLots(totalAvailableLots)
                .build();
    }
}
