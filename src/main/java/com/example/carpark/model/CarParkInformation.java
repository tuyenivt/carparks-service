package com.example.carpark.model;

import com.example.carpark.exception.CarParkException;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CarParkInformation {
    private String carParkNo;
    private String address;
    private double xCoord;
    private double yCoord;

    public static CarParkInformation fromCsvRow(String[] row) {
        if (row.length < 4) {
            throw new IllegalArgumentException("Invalid CarParkInformation at CSV row: " + String.join(",", row));
        }
        return CarParkInformation.builder()
                .carParkNo(row[0])
                .address(row[1])
                .xCoord(Double.parseDouble(row[2]))
                .yCoord(Double.parseDouble(row[3]))
                .build();
    }

    public static List<CarParkInformation> fromCsvContent(String csvContent) {
        try (var csvReader = new CSVReaderBuilder(new StringReader(csvContent)).withSkipLines(1).build()) {
            return csvReader.readAll().stream().map(CarParkInformation::fromCsvRow).toList();
        } catch (IOException | CsvException | IllegalArgumentException e) {
            throw new CarParkException("Failed to parse CSV data", e);
        }
    }
}
