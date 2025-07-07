package com.osmfogmap.locationtrack;

import org.osmdroid.util.GeoPoint;

public interface LocationUpdateCallback {
    void onLocationUpdated(GeoPoint location);
}
