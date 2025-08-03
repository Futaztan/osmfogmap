package com.osmfogmap.settings;

import com.osmfogmap.overlays.FogOverlayPolygon;

import org.osmdroid.util.GeoPoint;

public class SettingsManager {
    private final FogOverlayPolygon fogOverlay;


    public boolean LOCATION_FOLLOW = false;
    public GeoPoint lastloc = new GeoPoint(0.0,0.0);

    public SettingsManager(FogOverlayPolygon foverlay)
    {
        fogOverlay=foverlay;
    }


    public void deleteProgress()
    {
        fogOverlay.deleteHoles();
    }

}
