package com.osmfogmap.overlays.temp;

import android.util.Log;

import com.osmfogmap.MyKDtree;
import org.osmdroid.util.GeoPoint;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import smile.neighbor.Neighbor;
public class KDtreeManager {
    private MyKDtree<GeoPoint> kdTree;
    public void rebuildKdTree(List<GeoPoint> holes) {
        long startTime = System.nanoTime(); // Record the end time

        if (holes.isEmpty()) {
            kdTree = null;
            return;
        }
        List<GeoPoint> pointsToBuild = new ArrayList<>(holes);

        double[][] coords = new double[pointsToBuild.size()][2];
        GeoPoint[] data = new GeoPoint[pointsToBuild.size()];

        for (int i = 0; i < pointsToBuild.size(); i++) {
            GeoPoint point = pointsToBuild.get(i);
            double[] tomb = new double[]{point.getLatitude(), point.getLongitude()};
            coords[i] = tomb;
            data[i] = point;
        }
        kdTree = new MyKDtree<>(coords, data);
        long endTime = System.nanoTime(); // Record the end time
        long durationNano = endTime - startTime;
        double durationMillis = (double) durationNano / 1_000_000.0; // Convert nanoseconds to milliseconds
        Log.d("fv-rebuildkdtree",String.valueOf(durationMillis));
    }

    public boolean processIncomingPoint(GeoPoint incomingPoint, List<GeoPoint> holes) {
        // Ha még nincs fa, vagy az első pont, add hozzá.
        long startTime = System.nanoTime(); // Record the end time
        if (kdTree == null || holes.isEmpty())
            return true;

        double[] tomb = new double[]{incomingPoint.getLatitude(), incomingPoint.getLongitude()};
        Neighbor<double[], GeoPoint> nearestResult = kdTree.nearest(tomb);

        if (nearestResult != null) {
            GeoPoint nearestPoint = nearestResult.value();
            double distance = calculateHaversineDistance(incomingPoint, nearestPoint);
            long endTime = System.nanoTime(); // Record the end time
            long durationNano = endTime - startTime;
            double durationMillis = (double) durationNano / 1_000_000.0; // Convert nanoseconds to milliseconds
            Log.d("fv-processincoming",String.valueOf(durationMillis));
            if (distance > 100)
                return true;

        }
        return false;
    }


    public double calculateHaversineDistance(GeoPoint p1, GeoPoint p2) {
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


    public void simplifyKDtree(List<GeoPoint> holes)
    {
        Set<GeoPoint> pointsToRemove = new HashSet<>();

        for (GeoPoint point : holes)
        {
            if (!pointsToRemove.contains(point))
            {
                double[] coords = new double[]{point.getLatitude(), point.getLongitude()};
                List<Neighbor<double[], GeoPoint>> neighborList = new ArrayList<>();
                kdTree.search(coords, 230, neighborList);


                for (int i = 1; i < neighborList.size(); i++)
                {
                    GeoPoint neighbor = neighborList.get(i).value();
                    pointsToRemove.add(neighbor);
                }
            }
        }
        holes.removeAll(pointsToRemove);
        rebuildKdTree(holes);
    }
}
