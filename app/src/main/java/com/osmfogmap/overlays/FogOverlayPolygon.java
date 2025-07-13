package com.osmfogmap.overlays;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.union.UnaryUnionOp;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;


import android.graphics.Paint;
import android.graphics.Path;
import android.os.Looper;
import android.util.Log;

import com.osmfogmap.area.Proj4jAreaCalculator;
import com.osmfogmap.overlays.temp.KDtreeManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;

public class FogOverlayPolygon extends Overlay {

    private final List<GeoPoint> holes = new ArrayList<>();
    private final GeometryFactory geometryFactory = new GeometryFactory();
    public volatile Geometry revealedGeometry = geometryFactory.createMultiPolygon();
    private static final int RADIUS = 400;
    private static FogOverlayPolygon.OnAreaChangeListener listener;
    private final KDtreeManager kdTreeManager = new KDtreeManager();

    private final ExecutorService executor = Executors.newSingleThreadExecutor(); // Single background thread for sequential tasks
    private final Handler mainHandler = new Handler(Looper.getMainLooper());     // Handler for UI thread updates

    public FogOverlayPolygon()
    {
        //TODO: kdtree egyszerusites utan szar mert messze lesznek a pontok amikbol felepiti a polygont és szogletes lesz
        super();

//        double a = 20;
//        for(int i = 0; i<10000; i++)
//        {
//            GeoPoint x = new GeoPoint(20.0,a);
//            GeoPoint y = new GeoPoint(30.0,a);
//            GeoPoint z = new GeoPoint(40.0,a);
//            GeoPoint w = new GeoPoint(50.0,a);
//            a+=0.001;
//            holes.add(x);
//            holes.add(y);
//            holes.add(z);
//            holes.add(w);
//            //Log.d("log-i",String.valueOf(i));
//        }
//
//        areaGeometry = buildUnionPolygon(true);
//        rebuildKdTree();
//        calculateArea();
    }


    public interface OnAreaChangeListener {
        void onAreaChanged(double newArea);
    }

    public static void setOnAreaChangeListener(FogOverlayPolygon.OnAreaChangeListener listener) {
        FogOverlayPolygon.listener = listener;
    }

    public List<GeoPoint> getHoles() {
        return holes;
    }

    public void addHole(GeoPoint geoPoint) {
        if (kdTreeManager.processIncomingPoint(geoPoint,holes)) {
            holes.add(geoPoint);
            kdTreeManager.rebuildKdTree(holes);


            updateRevealedGeometry_andArea(false);
            calculateArea();
        }

    }

    public void deleteHoles() {
        GeoPoint lastloc = holes.get(holes.size() - 1);
        holes.clear();
        kdTreeManager.rebuildKdTree(holes);
        revealedGeometry = geometryFactory.createMultiPolygon();
        addHole(lastloc);
    }

    public void loadHoles(List<GeoPoint> points) {

        holes.addAll(points);
        kdTreeManager.rebuildKdTree(holes);
        kdTreeManager.simplifyKDtree(holes);
        updateRevealedGeometry_andArea(true);
        calculateArea();

    }
    public void loadPolygon(Geometry p)
    {
        revealedGeometry = p;
        deleteSmallIslands();
        calculateArea();
    }




    private void updateRevealedGeometry_andArea(boolean muchSimplify) {

        executor.execute(()->
        {
            Geometry unionResult;


            if (revealedGeometry.isEmpty())
            {
                List<Polygon> circlePolygons = new ArrayList<>();

                for (GeoPoint geoPoint : holes) {
                    circlePolygons.add(createCirclePolygon(geoPoint));
                }
                unionResult = UnaryUnionOp.union(circlePolygons);
            }
            else
            {
                GeoPoint last = holes.get(holes.size() - 1);
                Geometry circle = createCirclePolygon(last);
                unionResult = circle.union(revealedGeometry);
            }
            if(muchSimplify) unionResult= DouglasPeuckerSimplifier.simplify(unionResult, 0.0005);
            else  unionResult = DouglasPeuckerSimplifier.simplify(unionResult, 0.00025);

            Geometry finalUnionResult = unionResult;

            //AREA CALCULATING
            //double area = UTMarea(finalUnionResult);

            mainHandler.post(()->
            {
                revealedGeometry = finalUnionResult;
                calculateArea();
            });
        });
    }

    public void simplifytest() {

        kdTreeManager.simplifyKDtree(holes);
        //areaGeometry = DouglasPeuckerSimplifier.simplify(areaGeometry,0.0005);
    }

    private void deleteSmallIslands() {
        List<Polygon> keptPolygons = new ArrayList<>();

        for (int i = 0; i < revealedGeometry.getNumGeometries(); i++) {
            Geometry geom = revealedGeometry.getGeometryN(i);
            Log.d("loadarea", String.valueOf(UTMarea(geom)));
            if ( UTMarea(geom) >=1.5 ) {
                keptPolygons.add((Polygon) geom);
            }
        }

        if (keptPolygons.isEmpty()) {
            revealedGeometry = geometryFactory.createMultiPolygon();
        } else {
            revealedGeometry = geometryFactory.createMultiPolygon(
                    keptPolygons.toArray(new Polygon[0]));
        }
    }

    private void calculateArea()
    {
        if(revealedGeometry.isEmpty())
        {
            mainHandler.post(() -> {
                if (listener != null) {
                    listener.onAreaChanged(0.0); // Vagy valamilyen értelmes alapértelmezett érték
                }
            });
            return;
        }
        executor.execute(()->{
            double area = UTMarea(revealedGeometry);
            mainHandler.post(()-> {
                if(listener!=null)
                    listener.onAreaChanged(area);
            });
        });


    }


    private double UTMarea(Geometry geometry) {

        double area = 0;
        for (int i = 0; i < geometry.getNumGeometries(); i++) {
            double a = Proj4jAreaCalculator.getAreaInSquareMeters(geometry.getGeometryN(i));
            area +=a;
            Log.d("area",i +":" + a);
        }

        area = area / 1000000;
        area = Math.floor(area * 100) / 100;
        return area;
    }

    private Polygon createCirclePolygon(GeoPoint center) {
        GeometryFactory factory = new GeometryFactory();
        int numPoints = 16;
        Coordinate[] coords = new Coordinate[numPoints + 1];

        for (int i = 0; i <= numPoints; i++) {
            double angle = 2 * Math.PI * i / numPoints;
            GeoPoint p = destinationPoint(center, RADIUS, Math.toDegrees(angle));
            coords[i] = new Coordinate(p.getLongitude(), p.getLatitude());
        }

        LinearRing ring = factory.createLinearRing(coords);
        return factory.createPolygon(ring);
    }

    private GeoPoint destinationPoint(GeoPoint start, double distanceMeters, double bearingDegrees) {
        double R = 6371000; // földsugár méterben
        double bearingRad = Math.toRadians(bearingDegrees);

        double lat1 = Math.toRadians(start.getLatitude());
        double lon1 = Math.toRadians(start.getLongitude());

        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(distanceMeters / R)
                + Math.cos(lat1) * Math.sin(distanceMeters / R) * Math.cos(bearingRad));

        double lon2 = lon1 + Math.atan2(Math.sin(bearingRad) * Math.sin(distanceMeters / R) * Math.cos(lat1),
                Math.cos(distanceMeters / R) - Math.sin(lat1) * Math.sin(lat2));

        return new GeoPoint(Math.toDegrees(lat2), Math.toDegrees(lon2));
    }


    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        if (shadow) return;

        Projection projection = mapView.getProjection();
        int saveCount = canvas.saveLayer(0, 0, canvas.getWidth(), canvas.getHeight(), null);

        Paint fogPaint = new Paint();
        fogPaint.setColor(Color.argb(220, 0, 0, 0));
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), fogPaint);

        Paint holePaint = new Paint();
        holePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        holePaint.setAntiAlias(true); // Set anti-aliasing to true for smoother edges

        // Draw the pre-calculated areaGeometry (which represents the unfogged area)
        if (revealedGeometry != null && !revealedGeometry.isEmpty()) {
            Path path = new Path();
            for (int i = 0; i < revealedGeometry.getNumGeometries(); i++) {
                Geometry geom = revealedGeometry.getGeometryN(i);
                if (!(geom instanceof Polygon)) continue;
                Polygon polygon = (Polygon) geom;

                // Exterior Ring
                Coordinate[] exteriorCoords = polygon.getExteriorRing().getCoordinates();
                if (exteriorCoords.length > 0) {
                    Point start = projection.toPixels(new GeoPoint(exteriorCoords[0].y, exteriorCoords[0].x), null);
                    path.moveTo(start.x, start.y);
                    for (int j = 1; j < exteriorCoords.length; j++) {
                        Point p = projection.toPixels(new GeoPoint(exteriorCoords[j].y, exteriorCoords[j].x), null);
                        path.lineTo(p.x, p.y);
                    }
                    path.close();
                }

                // Interior Rings (holes within the unfogged area) - important for complex polygons
                for (int k = 0; k < polygon.getNumInteriorRing(); k++) {
                    LinearRing interiorRing = polygon.getInteriorRingN(k);
                    Coordinate[] interiorCoords = interiorRing.getCoordinates();
                    if (interiorCoords.length > 0) {
                        Point start = projection.toPixels(new GeoPoint(interiorCoords[0].y, interiorCoords[0].x), null);
                        path.moveTo(start.x, start.y);
                        for (int j = 1; j < interiorCoords.length; j++) {
                            Point p = projection.toPixels(new GeoPoint(interiorCoords[j].y, interiorCoords[j].x), null);
                            path.lineTo(p.x, p.y);
                        }
                        path.close();
                    }
                }
            }
            canvas.drawPath(path, holePaint);
        }
        canvas.restoreToCount(saveCount);
    }
}

