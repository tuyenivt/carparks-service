package com.example.carpark.service;

import com.example.carpark.entity.CarPark;
import com.example.carpark.model.CarParkInformation;
import com.example.carpark.repository.CarParkRepository;
import com.example.carpark.util.ConverterUtil;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.List;

@ApplicationScoped
public class CarParkService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CarParkService.class);

    private final CarParkRepository carParkRepository;
    private final ConverterUtil converterUtil;
    private final CarParkAvailabilityService carParkAvailabilityService;
    private final RedisService redisService;

    @Inject
    public CarParkService(CarParkRepository carParkRepository, ConverterUtil converterUtil,
                          CarParkAvailabilityService carParkAvailabilityService, RedisService redisService) {
        this.carParkRepository = carParkRepository;
        this.converterUtil = converterUtil;
        this.carParkAvailabilityService = carParkAvailabilityService;
        this.redisService = redisService;
    }

    public Uni<Void> ingestCarParkInfos(List<CarParkInformation> carParkInfos) {
        return filterNonExistingCarParks(carParkInfos)
                .flatMap(this::saveCarParksInBatch);
    }

    private Uni<List<CarParkInformation>> filterNonExistingCarParks(List<CarParkInformation> carParkInfos) {
        if (carParkInfos.isEmpty()) {
            return Uni.createFrom().item(List.of());
        }

        return carParkRepository.findByCarParkNos(carParkInfos.stream().map(CarParkInformation::getCarParkNo).toList())
                .map(carParks -> carParks.stream().map(carPark -> carPark.carParkNo).toList())
                .map(existingNos -> carParkInfos.stream().filter(cp -> !existingNos.contains(cp.getCarParkNo())).toList());
    }

    private Uni<Void> saveCarParksInBatch(List<CarParkInformation> carParkInfos) {
        if (carParkInfos.isEmpty()) {
            return Uni.createFrom().voidItem();
        }

        var entities = carParkInfos.stream().map(this::toCarParkEntity).toList();
        return carParkRepository.persist(carParkInfos.stream().map(this::toCarParkEntity).toList())
                .invoke(() -> LOGGER.info("Batch saved {} car parks", entities.size()));
    }

    private CarPark toCarParkEntity(CarParkInformation carParkInfo) {
        var wgs84 = converterUtil.convertSVY21ToWGS84(carParkInfo.getXCoord(), carParkInfo.getYCoord());
        return CarPark.builder()
                .carParkNo(carParkInfo.getCarParkNo())
                .address(carParkInfo.getAddress())
                .latitude(wgs84[0])
                .longitude(wgs84[1])
                .lastUpdated(new Timestamp(System.currentTimeMillis()))
                .build();
    }

    /**
     * <p>Scheduled task to update car park availability every 2 minutes</p>
     *
     * <p>From API Docs https://data.gov.sg/datasets/d_ca933a644e55d34fe21f28b8052fac63/view</p>
     * <p>Recommended that this endpoint be called every minute, so we can update every 2 minutes to be safe</p>
     */
    @Scheduled(every = "2m")
    @WithTransaction
    public Uni<Void> updateAvailabilityScheduler() {
        var expireSeconds = 120; // 2 minutes ~ @Scheduled(every = "2m")
        return redisService.tryLockUpdateAvailabilityScheduler(expireSeconds)
                .flatMap(locked -> {
                    if (Boolean.TRUE.equals(locked)) {
                        return carParkAvailabilityService.updateAvailability().flatMap(v -> {
                            LOGGER.info("Availability updated successfully");
                            return redisService.releaseLockUpdateAvailabilityScheduler().replaceWithVoid();
                        });
                    } else {
                        LOGGER.info("Another instance is already updating availability");
                        return Uni.createFrom().voidItem();
                    }
                });
    }

    public Uni<List<CarPark>> getNearestCarParks(double latitude, double longitude, int page, int perPage) {
        var offset = (page - 1) * perPage;
        return carParkRepository.findNearestWithAvailability(latitude, longitude, perPage, offset);
    }
}
