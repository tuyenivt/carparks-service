package com.example.carpark.model;

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
}
