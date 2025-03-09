package com.example.carpark.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CarParkInfo {
    private String carParkNo;
    private String address;
    private double xCoord;
    private double yCoord;

    public static CarParkInfo fromCsvRow(String[] row) {
        return CarParkInfo.builder()
                .carParkNo(row[0])
                .address(row[1])
                .xCoord(Double.parseDouble(row[2]))
                .yCoord(Double.parseDouble(row[3]))
                .build();
    }
}
