package com.osmfogmap.area;

import org.locationtech.jts.geom.*;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.ProjCoordinate;

public class Proj4jAreaCalculator {

    private static final CRSFactory crsFactory = new CRSFactory();
    private static final CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();

    public static double getAreaInSquareMeters(Geometry geometryWGS84) {
        Geometry geometryUTM = transformToUTM(geometryWGS84);
        return geometryUTM.getArea(); // Ez már m²-ben
    }

    private static Geometry transformToUTM(Geometry geometryWGS84) {
        // WGS84: földrajzi koordináták (long, lat)
        CoordinateReferenceSystem crsWGS84 = crsFactory.createFromParameters(
                "WGS84", "+proj=longlat +datum=WGS84 +no_defs");

        // Első pont alapján zóna és félteke meghatározása
        Coordinate firstCoord = geometryWGS84.getCoordinate();
        double lon = firstCoord.x;
        //ouble lat = firstCoord.y;

        int utmZone = getUTMZone(lon);
        //boolean isNorthern = lat >= 0;

        // UTM zóna + félteke alapján proj4 string
        String utmParams = "+proj=utm +zone=" + utmZone +
                " +datum=WGS84 +units=m +no_defs";

        CoordinateReferenceSystem crsUTM = crsFactory.createFromParameters("UTM", utmParams);

        // Átalakító
        CoordinateTransform transform = ctFactory.createTransform(crsWGS84, crsUTM);

        // Pontonként átalakítás
        Coordinate[] coords = geometryWGS84.getCoordinates();
        boolean closed = coords[0].equals2D(coords[coords.length - 1]);

        int newLength = closed ? coords.length : coords.length + 1;
        Coordinate[] transformedCoords = new Coordinate[newLength];

        for (int i = 0; i < coords.length; i++) {
            ProjCoordinate src = new ProjCoordinate(coords[i].x, coords[i].y);
            ProjCoordinate dst = new ProjCoordinate();
            transform.transform(src, dst);
            transformedCoords[i] = new Coordinate(dst.x, dst.y);
        }

        if (!closed) {
            transformedCoords[transformedCoords.length - 1] = transformedCoords[0];
        }


        GeometryFactory geomFactory = geometryWGS84.getFactory();

        // Ha Polygon, akkor újraépítjük
        if (geometryWGS84 instanceof Polygon) {
            return geomFactory.createPolygon(transformedCoords);
        }

        // Egyéb típusokra fallback (pl. LineString, Point)
        return geomFactory.createGeometry(geometryWGS84.getFactory().createLineString(transformedCoords));
    }

    private static int getUTMZone(double lon) {
        return (int) Math.floor((lon + 180) / 6) + 1;
    }
}
