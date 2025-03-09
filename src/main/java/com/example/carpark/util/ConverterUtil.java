package com.example.carpark.util;

import jakarta.enterprise.context.ApplicationScoped;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.ProjCoordinate;

@ApplicationScoped
public class ConverterUtil {
    private final CoordinateTransform transformSVY21ToWGS84;

    public ConverterUtil() {
        var crsFactory = new CRSFactory();
        var svy21 = crsFactory.createFromName("EPSG:3414"); // SVY21
        var wgs84 = crsFactory.createFromName("EPSG:4326"); // WGS84

        transformSVY21ToWGS84 = new CoordinateTransformFactory().createTransform(svy21, wgs84);
    }

    /**
     * <p>Convert SVY21 to WGS84</p>
     * <p>This logic reference from Basic Usage https://github.com/locationtech/proj4j?tab=readme-ov-file#basic-usage</p>
     * @param xCoord (Easting) similar to Longitude
     * @param yCoord (Northing) similar to Latitude
     * @return [Latitude, Longitude]
     */
    public double[] convertSVY21ToWGS84(double xCoord, double yCoord) {
        var result = new ProjCoordinate();
        transformSVY21ToWGS84.transform(new ProjCoordinate(xCoord, yCoord), result);
        return new double[]{result.y, result.x}; // [Latitude, Longitude]
    }
}
