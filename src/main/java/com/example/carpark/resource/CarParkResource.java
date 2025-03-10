package com.example.carpark.resource;

import com.example.carpark.exception.CarParkException;
import com.example.carpark.service.CarParkService;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@Path("/v1/carparks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CarParkResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(CarParkResource.class);

    private final CarParkService carParkService;

    @Inject
    public CarParkResource(CarParkService carParkService) {
        this.carParkService = carParkService;
    }

    @POST
    @Path("/import-csv")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @WithTransaction
    public Uni<Response> importCsvData(@RestForm("file") FileUpload csvFile) {
        return Uni.createFrom().item(() -> {
                    try {
                        return Files.readString(csvFile.uploadedFile(), StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        throw new CarParkException("Failed to read CSV file", e);
                    }
                })
                .flatMap(carParkService::ingestCsvData)
                .map(result -> Response
                        .accepted()
                        .entity("CSV data imported successfully")
                        .build())
                .onFailure(CarParkException.class)
                .recoverWithItem(e -> {
                    LOGGER.error("Failed to import CSV data: {}", e.getMessage());
                    return Response
                            .status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity("CSV import failed: " + e.getMessage())
                            .build();
                });
    }
}
