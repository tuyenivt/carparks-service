package com.example.carpark.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class CarParkAvailability {
    private List<CarParkItem> items;

    @Data
    public static class CarParkItem {
        @JsonProperty("carpark_data")
        private List<CarParkData> carParkData;
    }

    @Data
    public static class CarParkData {
        @JsonProperty("carpark_info")
        private List<CarParkInfo> carParkInfo;
        @JsonProperty("carpark_number")
        private String carParkNumber;
    }

    @Data
    public static class CarParkInfo {
        @JsonProperty("total_lots")
        private String totalLots;
        @JsonProperty("lots_available")
        private String lotsAvailable;
    }
}
