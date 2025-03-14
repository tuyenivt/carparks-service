package com.example.carpark.repository;

import com.example.carpark.entity.CarPark;
import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Collection;
import java.util.List;

@ApplicationScoped
public class CarParkRepository implements PanacheRepository<CarPark> {
    public Uni<CarPark> findByCarParkNo(String carParkNo) {
        return find("carParkNo", carParkNo).firstResult();
    }

    public Uni<List<CarPark>> findByCarParkNos(Collection<String> carParkNos) {
        return find("carParkNo IN ?1", carParkNos).list();
    }

    public Uni<List<CarPark>> findNearestWithAvailability(double latitude, double longitude, int limit, int offset) {
        var sql = """
                SELECT car_park_no, address, latitude, longitude, total_lots, available_lots, last_updated
                FROM car_parks
                WHERE available_lots > 0
                ORDER BY location <-> ST_SetSRID(ST_Point(:longitude, :latitude), 4326)
                LIMIT :limit
                OFFSET :offset
                """;
        return getSession()
                .flatMap(session -> session.createNativeQuery(sql, CarPark.class)
                        .setParameter("latitude", latitude)
                        .setParameter("longitude", longitude)
                        .setParameter("limit", limit)
                        .setParameter("offset", offset)
                        .getResultList());
    }
}
