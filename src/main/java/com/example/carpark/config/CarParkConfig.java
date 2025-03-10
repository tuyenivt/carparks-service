package com.example.carpark.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "app.carparks")
public interface CarParkConfig {
    String availabilityApi();
}
