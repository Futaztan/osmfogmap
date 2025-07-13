package com.osmfogmap.settings;

import com.osmfogmap.overlays.FogOverlay;
import com.osmfogmap.overlays.FogOverlayPolygon;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;

import java.util.ArrayList;
import java.util.List;

public class SettingsManager {
    private FogOverlayPolygon fogOverlay;
    MapView mapView;
    private boolean enabledMarker = false;
    public boolean LOCATION_FOLLOW = false;
    public GeoPoint lastloc = new GeoPoint(0.0,0.0);

    public SettingsManager(FogOverlayPolygon foverlay, MapView map)
    {
        fogOverlay=foverlay;
        mapView=map;
    }

    public void marker()
    {
        List<Overlay> toRemove = new ArrayList<>();
        for (Overlay overlay : mapView.getOverlays()) {
            if (overlay instanceof Marker) {
                toRemove.add(overlay);
            }
        }
        mapView.getOverlays().removeAll(toRemove);
        mapView.invalidate();



        if(!enabledMarker)
        {
            for(int i=0; i<fogOverlay.getHoles().size();i++)
            {

                Marker marker = new Marker(mapView);

                GeoPoint geoPoint = fogOverlay.getHoles().get(i);
                marker.setPosition(geoPoint);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                mapView.getOverlays().add(marker);

            }

            mapView.invalidate();
        }
        enabledMarker=!enabledMarker;




    }
    public void deleteProgress()
    {
        fogOverlay.deleteHoles();
    }

}
