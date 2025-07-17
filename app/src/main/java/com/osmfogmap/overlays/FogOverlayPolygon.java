package com.osmfogmap.overlays;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.strtree.STRtree;
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

import com.osmfogmap.MainActivity;
import com.osmfogmap.area.AreaManager;
import com.osmfogmap.area.Proj4jAreaCalculator;
import com.osmfogmap.KDtree.KDtreeManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;

public class FogOverlayPolygon extends Overlay {

    private final List<GeoPoint> holes = new ArrayList<>();
    private final GeometryFactory geometryFactory = new GeometryFactory();
    public volatile Geometry revealedGeometry = geometryFactory.createMultiPolygon();
    //private Geometry areaGeometry = geometryFactory.createMultiPolygon();
    private static final int RADIUS = 400;
    private static AreaManager.OnAreaChangeListener listener;
    private final KDtreeManager kdTreeManager = new KDtreeManager();

    private final ExecutorService executor = Executors.newSingleThreadExecutor(); // Single background thread for sequential tasks
    private final Handler mainHandler = new Handler(Looper.getMainLooper());     // Handler for UI thread updates
    MainActivity mainActivity;

    public FogOverlayPolygon(MainActivity main)
    {

        super();
        mainActivity = main;


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
//        updateRevealedGeometry_andArea();
//        kdTreeManager.rebuildKdTree(holes);
//        kdTreeManager.simplifyKDtree(holes);

    }



    public static void setOnAreaChangeListener(AreaManager.OnAreaChangeListener _listener) {
        listener = _listener;
    }

    public List<GeoPoint> getHoles() {
        return holes;
    }

    public void addHole(GeoPoint geoPoint) {
        if (kdTreeManager.processIncomingPoint(geoPoint,holes)) {
            synchronized (holes)
            {
                holes.add(geoPoint);
                kdTreeManager.addPointToKdTree(geoPoint, holes);
            }

            updateRevealedGeometry_andArea();
        }
    }
    public void deleteHoles() {
        GeoPoint lastloc = holes.get(holes.size() - 1);
        holes.clear();
        kdTreeManager.clearKdTree();
        listener.onAreaChanged(-1);
        revealedGeometry = geometryFactory.createMultiPolygon();
        addHole(lastloc);
    }
    public void loadHoles(List<GeoPoint> points) {

        holes.addAll(points);
        kdTreeManager.rebuildKdTree(holes);
        kdTreeManager.simplifyKDtree(holes);
        kdTreeManager.deleteRemovedIslands(revealedGeometry,holes);
        kdTreeManager.rebuildKdTree(holes);
        //updateRevealedGeometry_andArea(true);
        //calculateArea();
        System.out.println("KESZ A LOADHOLES");

    }
    public void loadPolygon(Geometry p)
    {
      
        p = DouglasPeuckerSimplifier.simplify(p,0.0005);
        revealedGeometry = p;
        deleteSmallIslands();
        calculateArea();
    }
    
    private void updateRevealedGeometry_andArea() {

        executor.execute(()->
        {
            long startTime = System.nanoTime(); // Record the start time
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
            unionResult = DouglasPeuckerSimplifier.simplify(unionResult, 0.00025);

            Geometry finalUnionResult = unionResult;

            //AREA CALCULATING
            //double area = UTMarea(finalUnionResult);

            long endTime = System.nanoTime(); // Record the end time
            long durationNano = endTime - startTime;
            double durationMillis = (double) durationNano / 1_000_000.0; // Convert nanoseconds to milliseconds
            Log.d("fv-updateandreveal",String.valueOf(durationMillis));

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
        if (keptPolygons.isEmpty())  revealedGeometry = geometryFactory.createMultiPolygon();
        else revealedGeometry = geometryFactory.createMultiPolygon(keptPolygons.toArray(new Polygon[0]));
    }

    private void calculateArea()
    {
        if(revealedGeometry.isEmpty())
        {
            mainHandler.post(() -> {
                if (listener != null) {
                    listener.onAreaChanged(0.0);
                }
            });
            return;
        }
        executor.execute(()->{

            long startTime = System.nanoTime(); // Record the end time
            //Geometry g = DouglasPeuckerSimplifier.simplify(revealedGeometry,0.0025);
            //mainActivity.geometrymarker(g);
            double area = UTMarea(revealedGeometry);
            mainHandler.post(()-> {
                if(listener!=null)
                    listener.onAreaChanged(area);
                long endTime = System.nanoTime(); // Record the end time
                long durationNano = endTime - startTime;
                double durationMillis = (double) durationNano / 1_000_000.0; // Convert nanoseconds to milliseconds
                Log.d("fv-calculatearea",String.valueOf(durationMillis));
            });
        });


    }


    private double UTMarea(Geometry geometry) {
        long startTime = System.nanoTime(); // Record the end time
        double area = 0;
        for (int i = 0; i < geometry.getNumGeometries(); i++) {
            double a = Proj4jAreaCalculator.getAreaInSquareMeters(geometry.getGeometryN(i));
            area +=a;
            Log.d("area",i +":" + a);
        }

        area = area / 1000000;
        area = Math.floor(area * 100) / 100;
        long endTime = System.nanoTime(); // Record the end time
        long durationNano = endTime - startTime;
        double durationMillis = (double) durationNano / 1_000_000.0; // Convert nanoseconds to milliseconds
        Log.d("fv-utmarea",String.valueOf(durationMillis));
        return area;
    }

    private Polygon createCirclePolygon(GeoPoint center) {
        long startTime = System.nanoTime();

        int numPoints = 16;
        Coordinate[] coords = new Coordinate[numPoints + 1];

        for (int i = 0; i <= numPoints; i++) {
            double angle = 2 * Math.PI * i / numPoints;
            GeoPoint p = destinationPoint(center, RADIUS, Math.toDegrees(angle));
            coords[i] = new Coordinate(p.getLongitude(), p.getLatitude());
        }

        LinearRing ring = geometryFactory.createLinearRing(coords);
        long endTime = System.nanoTime();
        long durationNano = endTime - startTime;
        double durationMillis = (double) durationNano / 1_000_000.0; // Convert nanoseconds to milliseconds
        Log.d("fv-createcircle",String.valueOf(durationMillis));
        return geometryFactory.createPolygon(ring);
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

