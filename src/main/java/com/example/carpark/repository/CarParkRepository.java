package com.example.carpark.repository;

import com.example.carpark.entity.CarPark;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CarParkRepository implements PanacheRepository<CarPark> {
    public Uni<CarPark> findByCarParkNo(String carParkNo) {
        return find("carParkNo", carParkNo).firstResult();
    }
}
