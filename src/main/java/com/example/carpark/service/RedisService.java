package com.example.carpark.service;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.mutiny.redis.client.Request;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class RedisService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisService.class);

    private static final String LOCK_KEY = "update-availability-scheduler-lock";

    private final Redis redisClient;

    @Inject
    public RedisService(Redis redisClient) {
        this.redisClient = redisClient;
    }

    /**
     * Attempts to acquire the lock in a reactive way.
     *
     * @return Uni that emits true if the lock was acquired, false otherwise.
     */
    public Uni<Boolean> tryLockUpdateAvailabilityScheduler(int expireSeconds) {
        var request = Request.cmd(Command.SET)
                .arg(LOCK_KEY)
                .arg("")    // Empty value for required argument.
                .arg("NX")  // Only set if the key does not exist.
                .arg("EX")  // Set an expiration time.
                .arg(String.valueOf(expireSeconds));
        return redisClient.send(request)
                .onItem().transform(response -> {
                    if (response != null && "OK".equalsIgnoreCase(response.toString())) {
                        LOGGER.info("Lock acquired with key: {}", LOCK_KEY);
                        return true;
                    } else {
                        LOGGER.info("Failed to acquire lock with key: {}", LOCK_KEY);
                        return false;
                    }
                });
    }

    /**
     * Releases the lock using DEL command in a reactive way.
     *
     * @return Uni that emits true if the lock was released, false otherwise.
     */
    public Uni<Boolean> releaseLockUpdateAvailabilityScheduler() {
        return redisClient.send(Request.cmd(Command.DEL).arg(LOCK_KEY))
                .onItem().transform(response -> {
                    if (response != null && response.toInteger() > 0) {
                        LOGGER.info("Lock released successfully with key {}", LOCK_KEY);
                        return true;
                    } else {
                        LOGGER.warn("Lock release failed, DEL command did not delete the key {}", LOCK_KEY);
                        return false;
                    }
                });
    }
}
