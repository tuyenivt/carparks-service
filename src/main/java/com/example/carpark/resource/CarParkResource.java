package com.example.carpark.resource;

import com.example.carpark.exception.CarParkException;
import com.example.carpark.model.CarParkDto;
import com.example.carpark.model.CarParkInformation;
import com.example.carpark.service.CarParkService;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Path("/v1/carparks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Car Park V1", description = "Operations related to car parks")
public class CarParkResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(CarParkResource.class);

    private final CarParkService carParkService;
    private final Vertx vertx;

    @Inject
    public CarParkResource(CarParkService carParkService, Vertx vertx) {
        this.carParkService = carParkService;
        this.vertx = vertx;
    }

    @POST
    @Path("/import-csv")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @WithTransaction
    @Operation(summary = "Import car park data from CSV", description = "Ingests car park data from a provided CSV file")
    @APIResponse(responseCode = "202", description = "CSV data imported successfully")
    @APIResponse(responseCode = "500", description = "CSV import failed")
    public Uni<Response> importCsvData(@RestForm("file") FileUpload csvFile) {
        return vertx.fileSystem()
                .readFile(csvFile.uploadedFile().toString())
                .map(buffer -> CarParkInformation.fromCsvContent(buffer.toString(StandardCharsets.UTF_8)))
                .flatMap(carParkService::ingestCarParkInfos)
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

    @GET
    @Path("/nearest")
    @WithTransaction
    @Operation(summary = "Get nearest car parks", description = "Returns the nearest car parks with available parking lots based on user-provided coordinates")
    @APIResponse(responseCode = "200", description = "List of nearest car parks")
    @APIResponse(responseCode = "400", description = "Missing or invalid latitude/longitude")
    @APIResponse(responseCode = "500", description = "Unexpected server issues")
    public Uni<List<CarParkDto>> getNearestCarParks(
            @QueryParam("latitude") Double latitude,
            @QueryParam("longitude") Double longitude,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("per_page") @DefaultValue("10") int perPage) {
        LOGGER.info("Received request: latitude={}, longitude={}, page={}, per_page={}", latitude, longitude, page, perPage);

        validateGetNearestCarParksInputs(latitude, longitude, page, perPage);

        return carParkService.getNearestCarParks(latitude, longitude, page, perPage)
                .map(carParks -> carParks.stream().map(CarParkDto::fromEntity).toList())
                .invoke(carParkDtos -> LOGGER.info("Returning {} car parks", carParkDtos.size()));
    }

    private void validateGetNearestCarParksInputs(Double latitude, Double longitude, int page, int perPage) {
        if (latitude == null || longitude == null) {
            LOGGER.warn("Missing coordinates: latitude={}, longitude={}", latitude, longitude);
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity("Missing latitude or longitude")
                            .build());
        }

        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            LOGGER.warn("Invalid coordinates: latitude={}, longitude={}", latitude, longitude);
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity("Latitude must be between -90 and 90, longitude between -180 and 180")
                            .build());
        }

        if (page < 1 || perPage < 1) {
            LOGGER.warn("Invalid pagination: page={}, per_page={}", page, perPage);
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity("Page and per_page must be positive integers")
                            .build());
        }
    }
}
