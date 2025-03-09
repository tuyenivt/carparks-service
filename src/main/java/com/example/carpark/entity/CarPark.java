package com.example.carpark.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "car_parks")
public class CarPark extends PanacheEntityBase {
    @Id
    @Column(name = "car_park_no")
    public String carParkNo;

    @Column(nullable = false)
    public String address;

    @Column(nullable = false)
    public double latitude;

    @Column(nullable = false)
    public double longitude;

    @Column(name = "total_lots", nullable = false)
    public int totalLots;

    @Column(name = "available_lots")
    public int availableLots;

    @Column(name = "last_updated")
    public Timestamp lastUpdated;
}
