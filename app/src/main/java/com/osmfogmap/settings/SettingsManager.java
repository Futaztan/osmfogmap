package com.osmfogmap.settings;

import com.osmfogmap.overlays.FogOverlayPolygon;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

public class SettingsManager {
    private final FogOverlayPolygon fogOverlay;
    MapView mapView;

    public boolean LOCATION_FOLLOW = false;
    public GeoPoint lastloc = new GeoPoint(0.0,0.0);

    public SettingsManager(FogOverlayPolygon foverlay, MapView map)
    {
        fogOverlay=foverlay;
        mapView=map;
    }


    public void deleteProgress()
    {
        fogOverlay.deleteHoles();
    }

}
