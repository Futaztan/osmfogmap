package com.osmfogmap.overlays;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

import com.osmfogmap.area.Proj4jAreaCalculator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;

import org.locationtech.jts.operation.union.UnaryUnionOp;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import com.osmfogmap.MyKDtree;

import smile.neighbor.Neighbor;

public class FogOverlay extends Overlay {
    private List<GeoPoint> holes = new ArrayList<>();
    private GeometryFactory geometryFactory = new GeometryFactory();
    public Geometry areaGeometry = geometryFactory.createMultiPolygon();
    private static final int RADIUS = 400;

    private static OnAreaChangeListener listener;

    public volatile MyKDtree<GeoPoint> currentKdTree;
    public FogOverlay() {  super();

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
//        rebuildKdTree();
//        calculateArea();
        //TODO: terulet szamitas negyzetekre és egyszerusites

    }

    public interface OnAreaChangeListener {
        void onAreaChanged(double newArea);
    }

    public static void setOnAreaChangeListener(OnAreaChangeListener listener) {
        FogOverlay.listener = listener;
    }

    public List<GeoPoint> getHoles() {
        return holes;
    }

    public void deleteHoles() {
        GeoPoint lastloc = holes.get(holes.size()-1);
        holes.clear();
        areaGeometry = geometryFactory.createMultiPolygon();
        addHole(lastloc);

    }

    public void addHole(GeoPoint geoPoint) {
        //holes.add(geoPoint);

        if(processIncomingPoint(geoPoint))
        {
            holes.add(geoPoint);
            rebuildKdTree();
            calculateArea();
        }
    }

    public void loadHoles(List<GeoPoint> points) {

        holes.addAll(points);
        rebuildKdTree();
        //simplifyHoles();
        joKDtreeSimpilfy();
        calculateArea();
    }


    public void joKDtreeSimpilfy()



    {
        // 3. Közeli pontok ritkítása (230m-en belül)
//        Set<GeoPoint> toRemove = new HashSet<>();
//        for (GeoPoint point : holes) {
//            if (toRemove.contains(point)) continue;
//
//            double[] coords = new double[]{point.getLatitude(), point.getLongitude()};
//            List<Neighbor<double[], GeoPoint>> neighbors = currentKdTree.search(coords,230);
//
//            for (Neighbor<double[], GeoPoint> neighbor : neighbors) {
//                GeoPoint other = neighbor.value();
//                if (other != point) {
//                    toRemove.add(other);
//                    currentKdTree.remove(new double[]{other.getLatitude(), other.getLongitude()});
//                }
//            }
//        }
        Set<GeoPoint> pointsToRemove = new HashSet<>();

        for (GeoPoint point : holes)
        {
            if (!pointsToRemove.contains(point))
            {
                double[] coords = new double[]{point.getLatitude(), point.getLongitude()};
                List<Neighbor<double[], GeoPoint>> neighborList = new ArrayList<>();
                currentKdTree.search(coords, 230, neighborList);


                for (int i = 1; i < neighborList.size(); i++)
                {
                    GeoPoint neighbor = neighborList.get(i).value();
                    pointsToRemove.add(neighbor);
                }
            }
        }
        holes.removeAll(pointsToRemove);
        rebuildKdTree();
        calculateArea();
        //TODO MUKODIK GECI

    }
    public void simplifyHoles()
    {


        if(holes.isEmpty()) return;
        Iterator<GeoPoint> iterator = holes.iterator();
        while (iterator.hasNext()) {
            GeoPoint point = iterator.next();
            boolean isIsolated = true;

            for (GeoPoint other : holes)
            {
                if (point == other) continue;
                if (calculateHaversineDistance(point, other) <= 500) {
                    isIsolated = false;
                    break;
                }
            }

            if (isIsolated) {
                iterator.remove();
            }
        }




        for(int i = 0; i < holes.size();i++)
        {
            for(int j = 0; j < holes.size(); j++)
            {
                if(i==j)
                    continue;
                double distance = calculateHaversineDistance(holes.get(i),holes.get(j));

                if(distance < 230)
                {
                    holes.remove(j);
                    if (j < i) i--; // ha előtte törlünk, i is csökkenjen
                    j--; // visszalépés, mert lista rövidebb lett
                }
            }

        }

    }

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {

        if (shadow) return;

        Projection projection = mapView.getProjection();
        int saveCount = canvas.saveLayer(0, 0, canvas.getWidth(), canvas.getHeight(), null);

        Paint fogPaint = new Paint(); //220
        fogPaint.setColor(Color.argb(220, 0, 0, 0)); // fekete köd, áttetsző
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), fogPaint);

        Paint holePaint = new Paint();
        holePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        holePaint.setAntiAlias(false);




//        if (hideGeometry != null && !hideGeometry.isEmpty()) {
//            Path path = new Path();
//            Projection projection = mapView.getProjection();
//
//            for (int i = 0; i < hideGeometry.getNumGeometries(); i++) {
//                Geometry geom = hideGeometry.getGeometryN(i);
//                if (!(geom instanceof Polygon)) continue;
//                Polygon polygon = (Polygon) geom;
//
//                Coordinate[] coords = polygon.getExteriorRing().getCoordinates();
//                if (coords.length == 0) continue;
//
//                Point start = projection.toPixels(new GeoPoint(coords[0].y, coords[0].x), null);
//                path.moveTo(start.x, start.y);
//
//                for (int j = 1; j < coords.length; j++) {
//                    Point p = projection.toPixels(new GeoPoint(coords[j].y, coords[j].x), null);
//                    path.lineTo(p.x, p.y);
//                }
//
//                path.close();
//            }
//
//            canvas.drawPath(path, holePaint);
//        }


        for (GeoPoint geoPoint : holes) {
            Point center = projection.toPixels(geoPoint, null);

            GeoPoint offset = destinationPoint(geoPoint, RADIUS, 90);  // 500 méter, 90° = kelet
            Point edge = projection.toPixels(offset, null);

            float radius = (float) Math.hypot(edge.x - center.x, edge.y - center.y);
            canvas.drawCircle(center.x, center.y, radius, holePaint);
        }

        canvas.restoreToCount(saveCount);
    }

    public void calculateArea()
    {
        if(holes.isEmpty())
            return;
        areaGeometry = buildUnionPolygon();

        double area = UTMarea(areaGeometry);



        if(listener!=null)
            listener.onAreaChanged(area);

    }


    private Geometry buildUnionPolygon() {
        Geometry unionResult;


       if(areaGeometry.isEmpty())
        {
            List<Polygon> circlePolygons = new ArrayList<>();

            for (GeoPoint geoPoint : holes) {
                circlePolygons.add(createRectanglePolygon(geoPoint));
            }
            unionResult= UnaryUnionOp.union(circlePolygons);
        }
        else
        {
            GeoPoint last = holes.get(holes.size()-1);
            Geometry circle = createRectanglePolygon(last);
            unionResult= circle.union(areaGeometry);
        }
        return TopologyPreservingSimplifier.simplify(unionResult,0.008);



    }

    private Polygon createRectanglePolygon(GeoPoint center)
    {
        Coordinate[] coords = new Coordinate[4+1];
        double[] angles = {45,135,225,315};
        int atloDistance = 565;
        for(int i=0; i<4; i++)
        {
            GeoPoint p = destinationPoint(center,atloDistance,angles[i]);
            coords[i] = new Coordinate(p.getLongitude(),p.getLatitude());
        }
        coords[4] = new Coordinate(coords[0].x, coords[0].y);
        LinearRing ring = geometryFactory.createLinearRing(coords);
        return  geometryFactory.createPolygon(coords);
        //return geometryFactory.createPolygon(ring);
    }


    /*public void deleteSmallIslands() {
        List<Polygon> keptPolygons = new ArrayList<>();

        for (int i = 0; i < hideGeometry.getNumGeometries(); i++) {
            Geometry geom = hideGeometry.getGeometryN(i);
            if (geom instanceof Polygon && UTMarea(geom) >= 1500000) {
                keptPolygons.add((Polygon) geom);
            }
        }

        if (keptPolygons.isEmpty()) {
            hideGeometry = geometryFactory.createMultiPolygon();
        } else {
            hideGeometry = geometryFactory.createMultiPolygon(
                    keptPolygons.toArray(new Polygon[0]));
        }
    }*/

    private double UTMarea(Geometry geometry) {

        double area= 0;
        for(int i =0; i<geometry.getNumGeometries();i++)
        {
            area+= Proj4jAreaCalculator.getAreaInSquareMeters(geometry.getGeometryN(i));
        }

        area = area/1000000;
        area = Math.floor(area*100)/100;
        return area;
    }

    //Kiszámít egy célpontot adott távolságra és irányra egy indulóponttól.
    public GeoPoint destinationPoint(GeoPoint start, double distanceMeters, double bearingDegrees) {
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

    private void rebuildKdTree() {

        // A Smile KDTree-je egy double[][] tömböt vár a pontokhoz
        if (holes.isEmpty())
        {
            currentKdTree = null; // Üres fa
            return;
        }
        List<GeoPoint> pointsToBuild = new ArrayList<>(holes);

        double[][] coords = new double[pointsToBuild.size()][2]; // 2 dimenzió (lat, lon)
        GeoPoint[] data = new GeoPoint[pointsToBuild.size()];

        for (int i = 0; i < pointsToBuild.size(); i++) {
            GeoPoint point = pointsToBuild.get(i);
            double[] tomb = new double[]{point.getLatitude(), point.getLongitude()};
            coords[i] = tomb;
            data[i] = point;
        }
        MyKDtree<GeoPoint> newKdTree = new MyKDtree<>(coords, data);
        currentKdTree = newKdTree; // Atomikus csere
    }


    //Feldolgoz egy bejövő GeoPoint-ot. Ellenőrzi, hogy túl közel van-e már egy létező ponthoz.
    private boolean processIncomingPoint(GeoPoint incomingPoint)
    {
        // Ha még nincs fa, vagy az első pont, add hozzá.
        if (currentKdTree == null || holes.isEmpty())
            return true;

        double[] tomb = new double[]{incomingPoint.getLatitude(), incomingPoint.getLongitude()};
        Neighbor<double[], GeoPoint> nearestResult = currentKdTree.nearest(tomb);

        if (nearestResult != null)
        {
            GeoPoint nearestPoint = nearestResult.value();
            double distance = calculateHaversineDistance(incomingPoint, nearestPoint);
            if (distance > 250)
                return true;

        }
        return false;
    }

    /**
     * Haversine képlet a távolság számításához két GPS koordináta között.
     * Visszaadja a távolságot méterben.
     */
    private double calculateHaversineDistance(GeoPoint p1, GeoPoint p2 ) {
        double lat1 = p1.getLatitude();
        double lon1 = p1.getLongitude();
        double lat2 = p2.getLatitude();
        double lon2 = p2.getLongitude();
        final int R = 6371000; // Föld sugara méterben
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // Távolság méterben
    }
}